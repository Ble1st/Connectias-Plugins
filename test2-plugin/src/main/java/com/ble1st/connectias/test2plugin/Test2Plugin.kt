package com.ble1st.connectias.test2plugin

import android.util.Base64
import com.ble1st.connectias.plugin.sdk.IPlugin
import com.ble1st.connectias.plugin.sdk.PluginCategory
import com.ble1st.connectias.plugin.sdk.PluginContext
import com.ble1st.connectias.plugin.sdk.PluginMetadata
import com.ble1st.connectias.plugin.sdk.PluginUIController
import com.ble1st.connectias.plugin.ui.ButtonVariant
import com.ble1st.connectias.plugin.ui.TextStyle
import com.ble1st.connectias.plugin.ui.UIStateParcel
import com.ble1st.connectias.plugin.ui.UserActionParcel
import com.ble1st.connectias.plugin.ui.buildPluginUI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Test2 Plugin - Three-Process UI Architecture Demo
 *
 * Demonstrates the new Three-Process UI Architecture with:
 * - State-based rendering using PluginUIBuilder DSL
 * - Camera capture via Hardware Bridge
 * - Image display in isolated UI Process
 * - User action handling via IPC
 *
 * This plugin does NOT extend Fragment - it's purely state-based!
 */
class Test2Plugin : IPlugin {

    private var pluginContext: PluginContext? = null
    private var uiController: PluginUIController? = null

    // Plugin state
    private var isEnabled = false
    private var clickCount = 0
    private var capturedImageBase64: String? = null
    private var cameraStatus = "Bereit"
    private var lastError: String? = null

    // Coroutine scope for async operations
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onLoad(context: PluginContext): Boolean {
        Timber.i("[TEST2_PLUGIN] onLoad called - Three-Process UI Architecture")
        this.pluginContext = context

        // Get UI Controller for Three-Process Architecture (SDK wrapper)
        try {
            uiController = context.getUIController()
            if (uiController != null) {
                Timber.i("[TEST2_PLUGIN] UI Controller connected successfully")
                context.logInfo("Test2Plugin loaded with Three-Process UI support")
            } else {
                Timber.e("[TEST2_PLUGIN] Failed to get UI Controller")
                context.logError("Test2Plugin: UI Controller not available", null)
                return false
            }
        } catch (e: Exception) {
            Timber.e(e, "[TEST2_PLUGIN] Failed to get UI Controller")
            context.logError("Test2Plugin: Failed to initialize UI Controller", e)
            return false
        }

        context.logDebug("Test2Plugin: Initialization complete")
        return true
    }

    override fun onEnable(): Boolean {
        Timber.i("[TEST2_PLUGIN] onEnable called")
        isEnabled = true
        pluginContext?.logInfo("Test2Plugin enabled")

        // Render initial UI
        updateUI()

        return true
    }

    override fun onDisable(): Boolean {
        Timber.i("[TEST2_PLUGIN] onDisable called")
        isEnabled = false
        pluginContext?.logWarning("Test2Plugin disabled")

        // Update UI to show disabled state
        updateUI()

        return true
    }

    override fun onUnload(): Boolean {
        Timber.i("[TEST2_PLUGIN] onUnload called")
        pluginContext?.logInfo("Test2Plugin unloaded")

        // Cleanup
        capturedImageBase64 = null

        return true
    }

    override fun getMetadata(): PluginMetadata {
        return PluginMetadata(
            pluginId = "com.ble1st.connectias.test2plugin",
            pluginName = "Test2 Plugin (Three-Process UI)",
            version = "1.0.0",
            author = "Connectias Team",
            minApiLevel = 33,
            maxApiLevel = 36,
            minAppVersion = "1.0.0",
            nativeLibraries = emptyList(),
            fragmentClassName = "", // No Fragment! State-based UI only
            description = "Demo-Plugin für Three-Process UI Architecture mit Kamera-Funktionalität",
            permissions = listOf("android.permission.CAMERA"),
            category = PluginCategory.UTILITY,
            dependencies = emptyList()
        )
    }

    /**
     * THREE-PROCESS UI: Render UI state using PluginUIBuilder DSL
     *
     * This method is called by the UI Process when it needs to render the UI.
     * We return a declarative UI description (UIStateParcel), not a Fragment!
     */
    override fun onRenderUI(screenId: String): UIStateParcel? {
        Timber.d("[TEST2_PLUGIN] Rendering UI for screen: $screenId")

        return when (screenId) {
            "main" -> renderMainScreen()
            else -> renderMainScreen() // Default to main screen
        }
    }

    /**
     * Renders the main screen with camera functionality
     */
    private fun renderMainScreen(): UIStateParcel {
        return buildPluginUI("main") {
            title("Test2 Plugin - Camera Demo")

            data("clickCount", clickCount)
            data("isEnabled", isEnabled)

            // Plugin Status
            text(
                text = if (isEnabled) "✅ Plugin Aktiv" else "⏸️ Plugin Inaktiv",
                style = TextStyle.HEADLINE
            )

            spacer(height = 16)

            // Plugin Info
            text("Three-Process UI Architecture", style = TextStyle.TITLE)
            text(
                "Dieses Plugin nutzt die neue State-basierte UI ohne Fragment!",
                style = TextStyle.BODY
            )

            spacer(height = 24)

            // Click Counter Demo
            column {
                text("Interaktiver Counter:", style = TextStyle.TITLE)
                text("Klicks: $clickCount", style = TextStyle.HEADLINE)

                row {
                    button(
                        id = "btn_increment",
                        text = "➕ Erhöhen",
                        variant = ButtonVariant.PRIMARY
                    )

                    button(
                        id = "btn_reset",
                        text = "🔄 Reset",
                        variant = ButtonVariant.SECONDARY
                    )
                }
            }

            spacer(height = 24)

            // Camera Section
            column {
                text("📷 Kamera-Funktionen", style = TextStyle.TITLE)
                text("Status: $cameraStatus", style = TextStyle.BODY)

                button(
                    id = "btn_capture",
                    text = "📸 Bild aufnehmen",
                    variant = ButtonVariant.PRIMARY,
                    enabled = isEnabled
                )

                // Show error if any
                lastError?.let { error ->
                    spacer(height = 8)
                    text("❌ Fehler: $error", style = TextStyle.CAPTION)
                }

                // Show captured image
                capturedImageBase64?.let { base64Image ->
                    spacer(height = 16)
                    text("✅ Bild erfolgreich aufgenommen!", style = TextStyle.TITLE)

                    // Display image as a data-URI in the IMAGE.url field.
                    // The UI process renderer supports extracting Base64 from "url" (data:image/...;base64,...).
                    image(
                        id = "captured_image",
                        url = "data:image/jpeg;base64,$base64Image",
                        contentDescription = "Aufgenommenes Bild"
                    )

                    spacer(height = 8)
                    text("Größe: ${base64Image.length} Zeichen (Base64)", style = TextStyle.CAPTION)
                }
            }

            spacer(height = 24)

            // Architecture Info
            column {
                text("🏗️ Architektur-Info", style = TextStyle.TITLE)
                text("✅ Kein Fragment - nur State!", style = TextStyle.BODY)
                text("✅ UI-Rendering in separatem Prozess", style = TextStyle.BODY)
                text("✅ Kamera via Hardware Bridge", style = TextStyle.BODY)
                text("✅ IPC-basierte User-Actions", style = TextStyle.BODY)
            }
        }
    }

    /**
     * THREE-PROCESS UI: Handle user actions from UI Process
     *
     * User actions are sent from the UI Process via IPC when users interact
     * with the UI components (buttons, text fields, etc.)
     */
    override fun onUserAction(action: UserActionParcel) {
        Timber.d("[TEST2_PLUGIN] User action: ${action.actionType} on ${action.targetId}")

        when (action.targetId) {
            "btn_increment" -> {
                clickCount++
                pluginContext?.logDebug("Counter incremented to: $clickCount")
                updateUI()
            }

            "btn_reset" -> {
                clickCount = 0
                pluginContext?.logInfo("Counter reset to 0")
                updateUI()
            }

            "btn_capture" -> {
                captureImage()
            }

            else -> {
                Timber.w("[TEST2_PLUGIN] Unknown action target: ${action.targetId}")
            }
        }
    }

    /**
     * Captures an image using the Hardware Bridge
     */
    private fun captureImage() {
        val context = pluginContext ?: run {
            lastError = "Plugin context not available"
            updateUI()
            return
        }

        cameraStatus = "📷 Aufnahme läuft..."
        lastError = null
        updateUI()

        context.logInfo("Test2Plugin: Starting image capture via Hardware Bridge")

        coroutineScope.launch {
            try {
                val result = context.captureImage()

                result.onSuccess { imageData ->
                    // Convert image to Base64 for display in UI
                    capturedImageBase64 = Base64.encodeToString(
                        imageData,
                        Base64.NO_WRAP
                    )

                    cameraStatus = "✅ Bild erfolgreich aufgenommen"
                    lastError = null

                    context.logInfo("Test2Plugin: Image captured successfully (${imageData.size} bytes)")

                    // Update UI to show the captured image
                    updateUI()

                }.onFailure { error ->
                    cameraStatus = "❌ Aufnahme fehlgeschlagen"
                    lastError = error.message ?: "Unbekannter Fehler"
                    capturedImageBase64 = null

                    context.logError("Test2Plugin: Image capture failed", error)

                    // Update UI to show error
                    updateUI()
                }

            } catch (e: Exception) {
                cameraStatus = "❌ Aufnahme fehlgeschlagen"
                lastError = e.message ?: "Exception: ${e.javaClass.simpleName}"
                capturedImageBase64 = null

                context.logError("Test2Plugin: Image capture exception", e)

                // Update UI to show error
                updateUI()
            }
        }
    }

    /**
     * Updates the UI by sending new state to UI Process
     */
    private fun updateUI() {
        val controller = uiController ?: run {
            Timber.w("[TEST2_PLUGIN] Cannot update UI - controller not available")
            return
        }

        try {
            // Render current state
            val uiState = onRenderUI("main") ?: return

            // Send to UI Process via IPC (through SDK wrapper)
            controller.updateUIState(uiState)

            Timber.d("[TEST2_PLUGIN] UI state updated successfully")
        } catch (e: Exception) {
            Timber.e(e, "[TEST2_PLUGIN] Failed to update UI")
            pluginContext?.logInfo("Test2Plugin: UI update failed - ${e.message}")
        }
    }

    override fun onUILifecycle(event: String) {
        Timber.d("[TEST2_PLUGIN] UI Lifecycle event: $event")
        pluginContext?.logInfo("Test2Plugin: UI lifecycle - $event")

        when (event) {
            "onCreate" -> {
                // UI Process created the UI
                Timber.i("[TEST2_PLUGIN] UI created in UI Process")
            }
            "onResume" -> {
                // UI is visible
                Timber.i("[TEST2_PLUGIN] UI resumed")
            }
            "onPause" -> {
                // UI is hidden
                Timber.i("[TEST2_PLUGIN] UI paused")
            }
            "onDestroy" -> {
                // UI was destroyed
                Timber.i("[TEST2_PLUGIN] UI destroyed")
            }
        }
    }
}

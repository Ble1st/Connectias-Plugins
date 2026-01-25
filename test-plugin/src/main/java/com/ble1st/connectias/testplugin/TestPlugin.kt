package com.ble1st.connectias.testplugin

import android.os.Bundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
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
import timber.log.Timber

/**
 * Test plugin for the Connectias Three-Process UI Architecture.
 *
 * This plugin is intentionally state-based (no Fragment/Compose in plugin).
 * UI is rendered in the isolated UI process.
 */
class TestPlugin : IPlugin {

    private var pluginContext: PluginContext? = null
    private var uiController: PluginUIController? = null

    private var isEnabled = false
    private var clickCount = 0

    // Reduced demo: HTTP + Capture (no preview)
    private var urlInput = "https://httpbin.org/get"
    private var isLoading = false
    private var httpResult: String? = null
    private var httpError: String? = null

    private var cameraStatus = "Bereit"
    private var capturedImageBase64: String? = null
    private var cameraError: String? = null

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onLoad(context: PluginContext): Boolean {
        Timber.i("[TEST_PLUGIN] onLoad called")
        pluginContext = context

        uiController = context.getUIController()
        if (uiController == null) {
            context.logError("TestPlugin: UI Controller not available", null)
            return false
        }

        context.logInfo("TestPlugin loaded (Three-Process UI)")
        return true
    }

    override fun onEnable(): Boolean {
        isEnabled = true
        pluginContext?.logInfo("TestPlugin enabled")
        updateUI()
        return true
    }

    override fun onDisable(): Boolean {
        isEnabled = false
        pluginContext?.logWarning("TestPlugin disabled")
        updateUI()
        return true
    }

    override fun onUnload(): Boolean {
        pluginContext?.logInfo("TestPlugin unloaded")
        httpResult = null
        httpError = null
        capturedImageBase64 = null
        cameraError = null
        return true
    }

    override fun getMetadata(): PluginMetadata {
        return PluginMetadata(
            pluginId = "com.ble1st.connectias.testplugin",
            pluginName = "Test Plugin (Three-Process UI)",
            version = "1.0.0",
            author = "Connectias Team",
            minApiLevel = 33,
            maxApiLevel = 36,
            minAppVersion = "1.0.0",
            nativeLibraries = emptyList(),
            fragmentClassName = null, // No Fragment: State-based UI
            description = "Test-Plugin für Three-Process UI (State-basiert)",
            permissions = listOf("android.permission.INTERNET", "android.permission.CAMERA"),
            category = PluginCategory.UTILITY,
            dependencies = emptyList()
        )
    }

    override fun onRenderUI(screenId: String): UIStateParcel? {
        return when (screenId) {
            "main" -> renderMainScreen()
            else -> renderMainScreen()
        }
    }

    override fun onUserAction(action: UserActionParcel) {
        Timber.d("[TEST_PLUGIN] action=${action.actionType} target=${action.targetId}")

        when (action.targetId) {
            "btn_inc" -> {
                clickCount++
                updateUI()
            }
            "btn_reset" -> {
                clickCount = 0
                updateUI()
            }

            // Crash demo buttons
            "btn_crash_runtime" -> throw RuntimeException("TestPlugin intentional RuntimeException (isolation test)")
            "btn_crash_npe" -> {
                val s: String? = null
                s!!.length
            }
            "btn_crash_illegal_state" -> throw IllegalStateException("TestPlugin intentional IllegalStateException (isolation test)")
            "btn_crash_class_cast" -> {
                val obj: Any = "string"
                @Suppress("UNUSED_VARIABLE")
                val x = obj as Int
            }
            "btn_crash_arithmetic" -> {
                @Suppress("DIVISION_BY_ZERO")
                val x = 10 / 0
                Timber.d("Unreachable: $x")
            }

            // HTTP
            "tf_url" -> {
                urlInput = action.data?.getString("value") ?: urlInput
            }
            "btn_http_get" -> performHttpGet()

            // Camera
            "btn_capture" -> captureImage()
        }
    }

    private fun renderMainScreen(): UIStateParcel {
        return buildPluginUI("main") {
            title("Test Plugin (Three-Process UI)")

            // Status
            text(if (isEnabled) "✅ Plugin Aktiv" else "⏸️ Plugin Inaktiv", style = TextStyle.HEADLINE)
            spacer(12)

            // Counter
            text("Counter: $clickCount", style = TextStyle.TITLE)
            row {
                button(id = "btn_inc", text = "➕ Erhöhen", variant = ButtonVariant.PRIMARY, enabled = isEnabled)
                button(id = "btn_reset", text = "🔄 Reset", variant = ButtonVariant.SECONDARY, enabled = isEnabled)
            }

            spacer(16)

            // Crash tests (reduced set)
            text("Crash Tests (Isolation)", style = TextStyle.TITLE)
            column {
                button(id = "btn_crash_runtime", text = "RuntimeException", variant = ButtonVariant.SECONDARY, enabled = isEnabled)
                button(id = "btn_crash_npe", text = "NullPointerException", variant = ButtonVariant.SECONDARY, enabled = isEnabled)
                button(id = "btn_crash_illegal_state", text = "IllegalStateException", variant = ButtonVariant.SECONDARY, enabled = isEnabled)
                button(id = "btn_crash_class_cast", text = "ClassCastException", variant = ButtonVariant.SECONDARY, enabled = isEnabled)
                button(id = "btn_crash_arithmetic", text = "ArithmeticException", variant = ButtonVariant.SECONDARY, enabled = isEnabled)
            }

            spacer(16)

            // HTTP GET via Hardware Bridge
            text("HTTP GET via Hardware Bridge", style = TextStyle.TITLE)
            textField(
                id = "tf_url",
                label = "URL",
                value = urlInput,
                hint = "https://httpbin.org/get",
                enabled = isEnabled && !isLoading,
                multiline = false
            )
            button(
                id = "btn_http_get",
                text = if (isLoading) "⏳ Lädt…" else "🌐 GET senden",
                variant = ButtonVariant.PRIMARY,
                enabled = isEnabled && !isLoading
            )
            httpError?.let { err ->
                spacer(8)
                text("❌ HTTP Fehler: $err", style = TextStyle.CAPTION)
            }
            httpResult?.let { res ->
                spacer(8)
                text("✅ Antwort (gekürzt):", style = TextStyle.CAPTION)
                text(res, style = TextStyle.BODY)
            }

            spacer(16)

            // Camera capture via Hardware Bridge
            text("Kamera Capture via Hardware Bridge", style = TextStyle.TITLE)
            text("Status: $cameraStatus", style = TextStyle.BODY)
            button(
                id = "btn_capture",
                text = "📸 Bild aufnehmen",
                variant = ButtonVariant.PRIMARY,
                enabled = isEnabled
            )
            cameraError?.let { err ->
                spacer(8)
                text("❌ Kamera Fehler: $err", style = TextStyle.CAPTION)
            }
            capturedImageBase64?.let { base64 ->
                spacer(12)
                text("✅ Bild aufgenommen", style = TextStyle.CAPTION)
                image(
                    id = "img_capture",
                    url = "data:image/jpeg;base64,$base64",
                    contentDescription = "Captured image"
                )
            }
        }
    }

    private fun performHttpGet() {
        val context = pluginContext ?: return

        val url = urlInput.trim().let {
            if (it.startsWith("http://") || it.startsWith("https://")) it else "https://$it"
        }

        isLoading = true
        httpError = null
        httpResult = null
        updateUI()

        coroutineScope.launch {
            try {
                val result = context.httpRequest(url = url, method = "GET")
                result.onSuccess { body ->
                    // Keep UI payload bounded.
                    httpResult = body.take(1500)
                }.onFailure { e ->
                    httpError = e.message ?: e.javaClass.simpleName
                }
            } catch (e: Exception) {
                httpError = e.message ?: e.javaClass.simpleName
            } finally {
                isLoading = false
                updateUI()
            }
        }
    }

    private fun captureImage() {
        val context = pluginContext ?: return

        cameraStatus = "📷 Aufnahme läuft…"
        cameraError = null
        updateUI()

        coroutineScope.launch {
            try {
                val result = context.captureImage()
                result.onSuccess { bytes ->
                    capturedImageBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    cameraStatus = "✅ Bild erfolgreich aufgenommen"
                }.onFailure { e ->
                    capturedImageBase64 = null
                    cameraStatus = "❌ Aufnahme fehlgeschlagen"
                    cameraError = e.message ?: e.javaClass.simpleName
                }
            } catch (e: Exception) {
                capturedImageBase64 = null
                cameraStatus = "❌ Aufnahme fehlgeschlagen"
                cameraError = e.message ?: e.javaClass.simpleName
            } finally {
                updateUI()
            }
        }
    }

    private fun updateUI() {
        val controller = uiController ?: return
        try {
            val state = onRenderUI("main") ?: return
            controller.updateUIState(state)
        } catch (e: Exception) {
            pluginContext?.logError("TestPlugin: UI update failed", e)
        }
    }
}

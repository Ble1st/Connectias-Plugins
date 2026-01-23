package com.ble1st.connectias.testplugin

import android.os.Bundle
import com.ble1st.connectias.plugin.IPlugin
import com.ble1st.connectias.plugin.PluginCategory
import com.ble1st.connectias.plugin.PluginContext
import com.ble1st.connectias.plugin.PluginMetadata
import com.ble1st.connectias.plugin.ui.*
import kotlinx.coroutines.*
import timber.log.Timber

/**
 * Test Plugin for Connectias Plugin System
 * 
 * Uses Three-Process UI Architecture with state-based rendering.
 * Implements IPlugin interface with onRenderUI() and onUserAction().
 */
class TestPlugin : IPlugin {
    
    private var pluginContext: PluginContext? = null
    private var clickCount = 0
    private var isPluginEnabled = false
    
    // HTTP request state
    private var urlInput = "https://httpbin.org/get"
    private var httpResult: String? = null
    private var isLoading = false
    private var httpError: String? = null
    
    // Camera state
    private var cameraImageData: ByteArray? = null
    private var cameraError: String? = null
    
    // Camera Preview state
    private var isPreviewActive = false
    private var previewInfo = ""
    private var previewError = ""
    
    // Coroutine scope for async operations
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    // IPlugin implementation
    override fun onLoad(context: PluginContext): Boolean {
        Timber.i("TestPlugin: onLoad called")
        this.pluginContext = context
        context.logVerbose("TestPlugin: Verbose log - detailed initialization info")
        context.logInfo("TestPlugin loaded successfully")
        context.logDebug("TestPlugin: Debug info - context initialized")
        return true
    }
    
    override fun onEnable(): Boolean {
        Timber.i("TestPlugin: onEnable called")
        isPluginEnabled = true
        pluginContext?.logInfo("TestPlugin enabled")
        pluginContext?.logDebug("TestPlugin: State changed to enabled")
        return true
    }
    
    override fun onDisable(): Boolean {
        Timber.i("TestPlugin: onDisable called")
        isPluginEnabled = false
        pluginContext?.logWarning("TestPlugin disabled - features may not be available")
        pluginContext?.logDebug("TestPlugin: State changed to disabled")
        return true
    }
    
    override fun onUnload(): Boolean {
        Timber.i("TestPlugin: onUnload called")
        coroutineScope.cancel()
        pluginContext?.logInfo("TestPlugin unloaded")
        pluginContext?.logDebug("TestPlugin: Cleanup completed")
        return true
    }
    
    override fun getMetadata(): PluginMetadata {
        return PluginMetadata(
            pluginId = "com.ble1st.connectias.testplugin",
            pluginName = "Test Plugin",
            version = "1.0.0",
            author = "Connectias Team",
            minApiLevel = 33,
            maxApiLevel = 36,
            minAppVersion = "1.0.0",
            nativeLibraries = emptyList(),
            fragmentClassName = null, // No longer uses Fragment
            description = "Ein Test-Plugin für das Connectias Plugin-System mit state-basiertem UI",
            permissions = listOf("android.permission.INTERNET", "android.permission.CAMERA"),
            category = PluginCategory.UTILITY,
            dependencies = emptyList()
        )
    }
    
    override fun onRenderUI(screenId: String): UIStateData? {
        return buildPluginUI(screenId) {
            title("Test Plugin")
            
            // Status section
            column {
                text(
                    text = if (isPluginEnabled) "Plugin Aktiv" else "Plugin Geladen",
                    style = TextStyle.TITLE
                )
                text(
                    text = "Version 1.0.0",
                    style = TextStyle.CAPTION
                )
                spacer(height = 16)
            }
            
            // Counter section
            text(
                text = "Interaktiver Counter",
                style = TextStyle.TITLE
            )
            text(
                text = "$clickCount",
                style = TextStyle.HEADLINE
            )
            
            row {
                button(
                    id = "increment_button",
                    text = "Erhöhen",
                    enabled = true,
                    variant = ButtonVariant.PRIMARY
                )
                button(
                    id = "reset_button",
                    text = "Reset",
                    enabled = true,
                    variant = ButtonVariant.SECONDARY
                )
            }
            
            spacer()
            
            // HTTP Request section
            text(
                text = "HTTP Request (Curl)",
                style = TextStyle.TITLE
            )
            
            textField(
                id = "url_input",
                label = "URL",
                value = urlInput,
                hint = "https://example.com",
                enabled = !isLoading
            )
            
            button(
                id = "curl_button",
                text = if (isLoading) "Lädt..." else "Anfrage senden",
                enabled = !isLoading && urlInput.isNotBlank(),
                variant = ButtonVariant.PRIMARY
            )
            
            if (httpError != null) {
                text(
                    text = "Fehler: $httpError",
                    style = TextStyle.CAPTION
                )
            }
            
            if (httpResult != null) {
                text(
                    text = "Antwort:",
                    style = TextStyle.TITLE
                )
                // Note: Long text results are truncated in UI builder
                // Full result would be shown in a scrollable view in the actual UI
                val resultPreview = httpResult!!.take(200)
                text(
                    text = if (httpResult!!.length > 200) "$resultPreview..." else resultPreview,
                    style = TextStyle.BODY
                )
            }
            
            spacer()
            
            // Camera section
            text(
                text = "Kamera",
                style = TextStyle.TITLE
            )
            
            button(
                id = "capture_image_button",
                text = "Capture Image via Bridge",
                enabled = true,
                variant = ButtonVariant.PRIMARY
            )
            
            if (cameraError != null) {
                text(
                    text = "Fehler: $cameraError",
                    style = TextStyle.CAPTION
                )
            }
            
            if (cameraImageData != null) {
                text(
                    text = "Bild erfasst: ${cameraImageData!!.size} bytes",
                    style = TextStyle.BODY
                )
            }
            
            spacer()
            
            // Camera Preview section
            text(
                text = "Camera Preview (Live)",
                style = TextStyle.TITLE
            )
            
            row {
                button(
                    id = "start_preview_button",
                    text = "Start Preview",
                    enabled = !isPreviewActive,
                    variant = ButtonVariant.PRIMARY
                )
                button(
                    id = "stop_preview_button",
                    text = "Stop Preview",
                    enabled = isPreviewActive,
                    variant = ButtonVariant.SECONDARY
                )
            }
            
            if (previewInfo.isNotEmpty()) {
                text(
                    text = previewInfo,
                    style = TextStyle.BODY
                )
            }
            
            if (previewError.isNotEmpty()) {
                text(
                    text = "Preview Error: $previewError",
                    style = TextStyle.CAPTION
                )
            }
            
            spacer()
            
            // Crash Tests section
            text(
                text = "Crash Tests (Isolation)",
                style = TextStyle.TITLE
            )
            text(
                text = "Teste die Isolation des Plugin-Systems",
                style = TextStyle.CAPTION
            )
            
            row {
                button(
                    id = "crash_runtime_button",
                    text = "RuntimeException",
                    enabled = true,
                    variant = ButtonVariant.TEXT
                )
                button(
                    id = "crash_nullpointer_button",
                    text = "NullPointer",
                    enabled = true,
                    variant = ButtonVariant.TEXT
                )
            }
            
            row {
                button(
                    id = "crash_outofmemory_button",
                    text = "OutOfMemory",
                    enabled = true,
                    variant = ButtonVariant.TEXT
                )
                button(
                    id = "crash_illegalstate_button",
                    text = "IllegalState",
                    enabled = true,
                    variant = ButtonVariant.TEXT
                )
            }
            
            spacer()
            
            // Plugin info
            text(
                text = "Plugin-Informationen:",
                style = TextStyle.TITLE
            )
            text(
                text = "Plugin ID: com.ble1st.connectias.testplugin",
                style = TextStyle.CAPTION
            )
            text(
                text = "Autor: Connectias Team",
                style = TextStyle.CAPTION
            )
            text(
                text = "Kategorie: UTILITY",
                style = TextStyle.CAPTION
            )
        }
    }
    
    override fun onUserAction(action: UserAction) {
        when (action.actionType) {
            UserAction.ACTION_CLICK -> {
                when (action.targetId) {
                    "increment_button" -> {
                        clickCount++
                        pluginContext?.logDebug("Counter incremented to $clickCount")
                    }
                    "reset_button" -> {
                        pluginContext?.logInfo("Counter reset from $clickCount to 0")
                        clickCount = 0
                    }
                    "curl_button" -> {
                        performHttpRequest()
                    }
                    "capture_image_button" -> {
                        captureImageViaBridge()
                    }
                    "start_preview_button" -> {
                        startCameraPreview()
                    }
                    "stop_preview_button" -> {
                        stopCameraPreview()
                    }
                    "crash_runtime_button" -> {
                        pluginContext?.logWarning("TestPlugin: RuntimeException crash triggered")
                        throw RuntimeException("TestPlugin intentional crash - testing plugin isolation")
                    }
                    "crash_nullpointer_button" -> {
                        pluginContext?.logWarning("TestPlugin: NullPointerException crash triggered")
                        val nullObject: String? = null
                        nullObject!!.length // Force NPE
                    }
                    "crash_outofmemory_button" -> {
                        pluginContext?.logWarning("TestPlugin: OutOfMemoryError crash triggered")
                        val list = mutableListOf<ByteArray>()
                        while (true) {
                            list.add(ByteArray(10 * 1024 * 1024)) // Allocate 10MB chunks
                        }
                    }
                    "crash_illegalstate_button" -> {
                        pluginContext?.logWarning("TestPlugin: IllegalStateException crash triggered")
                        throw IllegalStateException("TestPlugin: Invalid state - testing isolation")
                    }
                }
            }
            UserAction.ACTION_TEXT_CHANGED -> {
                when (action.targetId) {
                    "url_input" -> {
                        urlInput = action.data.getString("value") ?: ""
                    }
                }
            }
        }
    }
    
    override fun onUILifecycle(event: String) {
        when (event) {
            UILifecycleEvent.ON_CREATE -> {
                pluginContext?.logDebug("TestPlugin UI created")
            }
            UILifecycleEvent.ON_RESUME -> {
                pluginContext?.logDebug("TestPlugin UI resumed")
            }
            UILifecycleEvent.ON_PAUSE -> {
                pluginContext?.logDebug("TestPlugin UI paused")
            }
            UILifecycleEvent.ON_DESTROY -> {
                pluginContext?.logDebug("TestPlugin UI destroyed")
            }
        }
    }
    
    /**
     * Performs HTTP GET request via Hardware Bridge (v2.0)
     */
    private fun performHttpRequest() {
        if (urlInput.isBlank()) {
            httpError = "URL darf nicht leer sein"
            httpResult = null
            pluginContext?.logWarning("TestPlugin: HTTP request failed - empty URL")
            return
        }
        
        val context = pluginContext
        if (context == null) {
            httpError = "Plugin context not available"
            return
        }
        
        val url = try {
            if (!urlInput.startsWith("http://") && !urlInput.startsWith("https://")) {
                "https://$urlInput"
            } else {
                urlInput
            }
        } catch (e: Exception) {
            httpError = "Ungültige URL: ${e.message}"
            httpResult = null
            context.logError("TestPlugin: Invalid URL format", e)
            return
        }
        
        isLoading = true
        httpError = null
        httpResult = null
        context.logInfo("TestPlugin: Starting HTTP request via Hardware Bridge to $url")
        
        coroutineScope.launch {
            try {
                val result = context.httpRequest(url = url, method = "GET")
                
                result.onSuccess { responseBody ->
                    httpResult = buildString {
                        appendLine("=== Hardware Bridge Response ===")
                        appendLine("URL: $url")
                        appendLine("\nResponse Body:")
                        appendLine(responseBody)
                    httpError = null
                    context.logInfo("TestPlugin: HTTP request successful via Hardware Bridge")
                }.onFailure { error ->
                    val errorMessage = "Fehler: ${error.message ?: error.javaClass.simpleName}"
                    httpError = errorMessage
                    httpResult = null
                    context.logError("TestPlugin: HTTP request failed", error)
                }
                
            } catch (e: Exception) {
                val errorMessage = "Fehler: ${e.message ?: e.javaClass.simpleName}"
                httpError = errorMessage
                httpResult = null
                context.logError("TestPlugin: HTTP request failed", e)
            } finally {
                isLoading = false
            }
        }
    }
    
    /**
     * Start camera preview via Hardware Bridge (v2.0)
     */
    private fun startCameraPreview() {
        coroutineScope.launch {
            isPreviewActive = true
            previewError = ""
            previewInfo = ""
            
            val context = pluginContext
            if (context == null) {
                previewError = "Plugin context not available"
                isPreviewActive = false
                return@launch
            }
            
            context.logInfo("TestPlugin: Starting camera preview via Hardware Bridge")
            
            val result = context.startCameraPreview()
            
            result.onSuccess { preview ->
                previewInfo = buildString {
                    appendLine("Preview Active")
                    appendLine("Resolution: ${preview.width}x${preview.height}")
                    appendLine("Format: ${preview.format}")
                    appendLine("Frame Size: ${preview.frameSize} bytes")
                    appendLine("Buffer Size: ${preview.bufferSize} bytes")
                    appendLine("FD: ${preview.fileDescriptor}")
                }
                context.logInfo("TestPlugin: Camera preview started (${preview.width}x${preview.height})")
            }.onFailure { error ->
                previewError = error.message ?: "Unknown error"
                isPreviewActive = false
                context.logError("TestPlugin: Camera preview failed", error)
            }
        }
    }
    
    /**
     * Stop camera preview via Hardware Bridge (v2.0)
     */
    private fun stopCameraPreview() {
        coroutineScope.launch {
            val context = pluginContext
            if (context == null) {
                previewError = "Plugin context not available"
                return@launch
            }
            
            context.logInfo("TestPlugin: Stopping camera preview")
            
            val result = context.stopCameraPreview()
            
            result.onSuccess {
                isPreviewActive = false
                previewInfo = ""
                context.logInfo("TestPlugin: Camera preview stopped")
            }.onFailure { error ->
                previewError = error.message ?: "Unknown error"
                context.logError("TestPlugin: Stop preview failed", error)
            }
        }
    }
    
    /**
     * Captures image via Hardware Bridge (v2.0)
     */
    private fun captureImageViaBridge() {
        val context = pluginContext
        if (context == null) {
            cameraError = "Plugin context not available"
            return
        }
        
        context.logInfo("TestPlugin: Starting image capture via Hardware Bridge")
        cameraError = null
        
        coroutineScope.launch {
            try {
                val result = context.captureImage()
                
                result.onSuccess { imageData ->
                    cameraImageData = imageData
                    cameraError = null
                    context.logInfo("TestPlugin: Image captured successfully (${imageData.size} bytes)")
                }.onFailure { error ->
                    cameraError = error.message ?: "Unknown error"
                    cameraImageData = null
                    context.logError("TestPlugin: Image capture failed", error)
                }
            } catch (e: Exception) {
                cameraError = e.message ?: "Unknown error"
                cameraImageData = null
                context.logError("TestPlugin: Image capture failed", e)
            }
        }
    }
}

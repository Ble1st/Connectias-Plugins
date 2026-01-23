package com.ble1st.connectias.plugin

import android.content.Context
import com.ble1st.connectias.plugin.native.INativeLibraryManager
import com.ble1st.connectias.plugin.ui.IPluginUIController
import java.io.File

/**
 * Context provided to plugins during their lifecycle
 */
interface PluginContext {
    /**
     * Get the application context
     */
    fun getApplicationContext(): Context

    /**
     * Get the plugin's directory for storing files
     */
    fun getPluginDirectory(): File

    /**
     * Get the native library manager for loading .so files
     */
    fun getNativeLibraryManager(): INativeLibraryManager

    /**
     * Register a service that can be accessed by other plugins
     */
    fun registerService(name: String, service: Any)

    /**
     * Get a registered service by name
     */
    fun getService(name: String): Any?

    /**
     * Log verbose message
     */
    fun logVerbose(message: String)

    /**
     * Log a debug message
     */
    fun logDebug(message: String)

    /**
     * Log info message
     */
    fun logInfo(message: String)

    /**
     * Log warning message
     */
    fun logWarning(message: String)

    /**
     * Log an error message
     */
    fun logError(message: String, throwable: Throwable? = null)

    // ========================================
    // Hardware Bridge APIs (v2.0)
    // ========================================

    /**
     * Capture an image using the device camera via Hardware Bridge
     * Requires CAMERA permission to be granted by user
     *
     * @return Result with image data as ByteArray on success, or error
     */
    suspend fun captureImage(): Result<ByteArray>

    /**
     * Get the UI controller for Three-Process UI Architecture.
     *
     * Plugins use this controller to send UI state updates to the UI Process.
     * The controller provides methods for updating UI, showing dialogs, toasts,
     * and navigation.
     *
     * @return IPluginUIController for sending UI updates via IPC
     */
    fun getUIController(): IPluginUIController
}

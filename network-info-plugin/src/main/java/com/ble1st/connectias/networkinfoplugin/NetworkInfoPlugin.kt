package com.ble1st.connectias.networkinfoplugin

import android.os.Bundle
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
 * Network info plugin for the Three-Process UI Architecture.
 *
 * Note: In an isolated sandbox process, direct access to certain network/wifi
 * system services may be restricted. This plugin uses the Hardware Bridge network API
 * to fetch a public IP (reachability check) and displays it via state-based UI.
 */
class NetworkInfoPlugin : IPlugin {

    private var pluginContext: PluginContext? = null
    private var uiController: PluginUIController? = null

    private var isEnabled = false
    private var isRefreshing = false
    private var publicIpJson: String? = null
    private var lastError: String? = null
    private var lastUpdatedAt: Long? = null

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onLoad(context: PluginContext): Boolean {
        Timber.i("[NETWORK_INFO_PLUGIN] onLoad called")
        pluginContext = context

        uiController = context.getUIController()
        if (uiController == null) {
            context.logError("NetworkInfoPlugin: UI Controller not available", null)
            return false
        }

        context.logInfo("NetworkInfoPlugin loaded (Three-Process UI)")
        updateUI()
        return true
    }

    override fun onEnable(): Boolean {
        isEnabled = true
        pluginContext?.logInfo("NetworkInfoPlugin enabled")
        refresh()
        return true
    }

    override fun onDisable(): Boolean {
        isEnabled = false
        pluginContext?.logWarning("NetworkInfoPlugin disabled")
        updateUI()
        return true
    }

    override fun onUnload(): Boolean {
        pluginContext?.logInfo("NetworkInfoPlugin unloaded")
        publicIpJson = null
        lastError = null
        lastUpdatedAt = null
        return true
    }

    override fun getMetadata(): PluginMetadata {
        return PluginMetadata(
            pluginId = "com.ble1st.connectias.networkinfoplugin",
            pluginName = "Network Info Plugin (Three-Process UI)",
            version = "1.0.0",
            author = "Connectias Team",
            minApiLevel = 33,
            maxApiLevel = 36,
            minAppVersion = "1.0.0",
            nativeLibraries = emptyList(),
            fragmentClassName = null,
            description = "Public IP/Reachability via Hardware Bridge (State-basiert im UI-Prozess)",
            permissions = listOf("android.permission.INTERNET"),
            category = PluginCategory.NETWORK,
            dependencies = emptyList()
        )
    }

    override fun onRenderUI(screenId: String): UIStateParcel? {
        return when (screenId) {
            "main" -> renderMain()
            else -> renderMain()
        }
    }

    override fun onUserAction(action: UserActionParcel) {
        when (action.targetId) {
            "btn_refresh" -> refresh()
        }
    }

    private fun renderMain(): UIStateParcel {
        return buildPluginUI("main") {
            title("Network Info (Three-Process UI)")

            text(if (isEnabled) "✅ Plugin Aktiv" else "⏸️ Plugin Inaktiv", style = TextStyle.HEADLINE)
            spacer(12)

            button(
                id = "btn_refresh",
                text = if (isRefreshing) "⏳ Aktualisiere…" else "🔄 Aktualisieren",
                variant = ButtonVariant.PRIMARY,
                enabled = isEnabled && !isRefreshing
            )

            lastUpdatedAt?.let { ts ->
                spacer(8)
                text("Letztes Update: $ts", style = TextStyle.CAPTION)
            }

            lastError?.let { err ->
                spacer(8)
                text("❌ Fehler: $err", style = TextStyle.CAPTION)
            }

            publicIpJson?.let { body ->
                spacer(12)
                text("Antwort (Public IP):", style = TextStyle.TITLE)
                text(body, style = TextStyle.BODY)
            }
        }
    }

    private fun refresh() {
        val context = pluginContext ?: return
        if (!isEnabled || isRefreshing) return

        isRefreshing = true
        lastError = null
        updateUI()

        coroutineScope.launch {
            try {
                // Small endpoint with public IP information.
                val result = context.httpRequest(url = "https://httpbin.org/ip", method = "GET")
                result.onSuccess { body ->
                    publicIpJson = body.take(2000)
                    lastUpdatedAt = System.currentTimeMillis()
                }.onFailure { e ->
                    lastError = e.message ?: e.javaClass.simpleName
                }
            } catch (e: Exception) {
                lastError = e.message ?: e.javaClass.simpleName
            } finally {
                isRefreshing = false
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
            pluginContext?.logError("NetworkInfoPlugin: UI update failed", e)
        }
    }
}

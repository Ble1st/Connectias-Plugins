package com.ble1st.connectias.pingplugin

import android.os.Bundle
import com.ble1st.connectias.plugin.sdk.IPlugin
import com.ble1st.connectias.plugin.sdk.PluginCategory
import com.ble1st.connectias.plugin.sdk.PluginContext
import com.ble1st.connectias.plugin.sdk.PluginMetadata
import com.ble1st.connectias.plugin.sdk.PluginUIController
import com.ble1st.connectias.plugin.ui.ButtonVariant
import com.ble1st.connectias.plugin.ui.ListItem
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
 * Ping plugin for the Three-Process UI Architecture.
 *
 * Note: Direct ICMP ping is not available from the isolated sandbox process.
 * This plugin uses the Hardware Bridge network API to perform an HTTP request and measures latency.
 */
class PingPlugin : IPlugin {

    private var pluginContext: PluginContext? = null
    private var uiController: PluginUIController? = null

    private var isEnabled = false
    private var isPinging = false
    private var hostInput = "https://httpbin.org/get"
    private var lastError: String? = null

    private val history = ArrayList<PingEntry>()

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private data class PingEntry(
        val id: String,
        val host: String,
        val success: Boolean,
        val latencyMs: Long?,
        val error: String?,
        val timestamp: Long = System.currentTimeMillis()
    )

    override fun onLoad(context: PluginContext): Boolean {
        Timber.i("[PING_PLUGIN] onLoad called")
        pluginContext = context

        uiController = context.getUIController()
        if (uiController == null) {
            context.logError("PingPlugin: UI Controller not available", null)
            return false
        }

        context.logInfo("PingPlugin loaded (Three-Process UI)")
        updateUI()
        return true
    }

    override fun onEnable(): Boolean {
        isEnabled = true
        pluginContext?.logInfo("PingPlugin enabled")
        updateUI()
        return true
    }

    override fun onDisable(): Boolean {
        isEnabled = false
        pluginContext?.logWarning("PingPlugin disabled")
        updateUI()
        return true
    }

    override fun onUnload(): Boolean {
        pluginContext?.logInfo("PingPlugin unloaded")
        history.clear()
        return true
    }

    override fun getMetadata(): PluginMetadata {
        return PluginMetadata(
            pluginId = "com.ble1st.connectias.pingplugin",
            pluginName = "Ping Plugin (Three-Process UI)",
            version = "1.0.0",
            author = "Connectias Team",
            minApiLevel = 33,
            maxApiLevel = 36,
            minAppVersion = "1.0.0",
            nativeLibraries = emptyList(),
            fragmentClassName = null,
            description = "HTTP-Ping (Latency) via Hardware Bridge, State-basiert im UI-Prozess",
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
            "tf_host" -> {
                hostInput = action.data?.getString("value") ?: hostInput
            }
            "btn_ping" -> performHttpPing()
            "btn_clear" -> {
                history.clear()
                lastError = null
                updateUI()
            }
        }
    }

    private fun renderMain(): UIStateParcel {
        val items = history.take(20).map { entry ->
            ListItem(
                id = entry.id,
                title = "${if (entry.success) "✅" else "❌"} ${entry.host}",
                subtitle = entry.latencyMs?.let { "${it}ms" } ?: (entry.error ?: "Fehler")
            )
        }

        return buildPluginUI("main") {
            title("Ping Plugin (HTTP)")

            text(if (isEnabled) "✅ Plugin Aktiv" else "⏸️ Plugin Inaktiv", style = TextStyle.HEADLINE)
            spacer(12)

            text("Host/URL:", style = TextStyle.TITLE)
            textField(
                id = "tf_host",
                label = "URL",
                value = hostInput,
                hint = "https://httpbin.org/get",
                enabled = isEnabled && !isPinging,
                multiline = false
            )
            row {
                button(
                    id = "btn_ping",
                    text = if (isPinging) "⏳ Ping läuft…" else "🌐 Ping starten",
                    variant = ButtonVariant.PRIMARY,
                    enabled = isEnabled && !isPinging
                )
                button(
                    id = "btn_clear",
                    text = "🗑️ Clear",
                    variant = ButtonVariant.SECONDARY,
                    enabled = isEnabled && !isPinging
                )
            }

            lastError?.let { err ->
                spacer(8)
                text("❌ Fehler: $err", style = TextStyle.CAPTION)
            }

            spacer(16)
            text("Historie", style = TextStyle.TITLE)
            list(id = "ping_history", items = items)
        }
    }

    private fun performHttpPing() {
        val context = pluginContext ?: return
        if (!isEnabled || isPinging) return

        val url = hostInput.trim().let {
            if (it.startsWith("http://") || it.startsWith("https://")) it else "https://$it"
        }

        isPinging = true
        lastError = null
        updateUI()

        coroutineScope.launch {
            val startNs = System.nanoTime()
            try {
                val result = context.httpRequest(url = url, method = "GET")
                val elapsedMs = (System.nanoTime() - startNs) / 1_000_000

                result.onSuccess {
                    history.add(
                        0,
                        PingEntry(
                            id = "ping_${System.currentTimeMillis()}",
                            host = url,
                            success = true,
                            latencyMs = elapsedMs,
                            error = null
                        )
                    )
                }.onFailure { e ->
                    history.add(
                        0,
                        PingEntry(
                            id = "ping_${System.currentTimeMillis()}",
                            host = url,
                            success = false,
                            latencyMs = null,
                            error = e.message ?: e.javaClass.simpleName
                        )
                    )
                }
            } catch (e: Exception) {
                lastError = e.message ?: e.javaClass.simpleName
            } finally {
                isPinging = false
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
            pluginContext?.logError("PingPlugin: UI update failed", e)
        }
    }
}

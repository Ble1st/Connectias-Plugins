package com.ble1st.connectias.pingplugin

import android.os.Bundle
import com.ble1st.connectias.plugin.IPlugin
import com.ble1st.connectias.plugin.PluginCategory
import com.ble1st.connectias.plugin.PluginContext
import com.ble1st.connectias.plugin.PluginMetadata
import com.ble1st.connectias.plugin.ui.*
import kotlinx.coroutines.*
import timber.log.Timber

/**
 * Ping Plugin for Connectias Plugin System
 * 
 * Uses Three-Process UI Architecture with state-based rendering.
 * Implements IPlugin interface with onRenderUI() and onUserAction().
 */
class PingPlugin : IPlugin {
    
    private var pluginContext: PluginContext? = null
    private var isPluginEnabled = false
    private val pingHistory = mutableListOf<PingResult>()
    private var isPinging = false
    private var hostInput = ""
    private var showError: String? = null
    
    // Coroutine scope for async operations
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    // IPlugin implementation
    override fun onLoad(context: PluginContext): Boolean {
        Timber.i("PingPlugin: onLoad called")
        this.pluginContext = context
        context.logVerbose("PingPlugin: Verbose log - detailed initialization info")
        context.logInfo("PingPlugin loaded successfully")
        context.logDebug("PingPlugin: Debug info - context initialized")
        return true
    }
    
    override fun onEnable(): Boolean {
        Timber.i("PingPlugin: onEnable called")
        isPluginEnabled = true
        pluginContext?.logInfo("PingPlugin enabled")
        pluginContext?.logDebug("PingPlugin: State changed to enabled")
        return true
    }
    
    override fun onDisable(): Boolean {
        Timber.i("PingPlugin: onDisable called")
        isPluginEnabled = false
        pluginContext?.logWarning("PingPlugin disabled - features may not be available")
        pluginContext?.logDebug("PingPlugin: State changed to disabled")
        return true
    }
    
    override fun onUnload(): Boolean {
        Timber.i("PingPlugin: onUnload called")
        coroutineScope.cancel()
        pluginContext?.logInfo("PingPlugin unloaded")
        pluginContext?.logDebug("PingPlugin: Cleanup completed")
        return true
    }
    
    override fun getMetadata(): PluginMetadata {
        return PluginMetadata(
            pluginId = "com.ble1st.connectias.pingplugin",
            pluginName = "Ping Plugin",
            version = "1.0.0",
            author = "Connectias Team",
            minApiLevel = 33,
            maxApiLevel = 36,
            minAppVersion = "1.0.0",
            nativeLibraries = emptyList(),
            fragmentClassName = null, // No longer uses Fragment
            description = "Ein Netzwerk-Ping-Tool für das Connectias Plugin-System mit ICMP-Ping-Funktionalität",
            permissions = emptyList(),
            category = PluginCategory.NETWORK,
            dependencies = emptyList()
        )
    }
    
    override fun onRenderUI(screenId: String): UIStateData? {
        return buildPluginUI(screenId) {
            title("Ping Plugin")
            
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
            
            // Ping Input section
            text(
                text = "Host anpingen",
                style = TextStyle.TITLE
            )
            
            textField(
                id = "host_input",
                label = "Host oder IP-Adresse",
                value = hostInput,
                hint = "z.B. google.com oder 8.8.8.8",
                enabled = !isPinging
            )
            
            if (showError != null) {
                text(
                    text = showError!!,
                    style = TextStyle.CAPTION
                )
            }
            
            button(
                id = "ping_button",
                text = if (isPinging) "Ping läuft..." else "Ping starten",
                enabled = !isPinging && hostInput.isNotBlank(),
                variant = ButtonVariant.PRIMARY
            )
            
            spacer()
            
            // Latest result
            if (pingHistory.isNotEmpty()) {
                val latestResult = pingHistory.first()
                text(
                    text = "Letztes Ergebnis:",
                    style = TextStyle.TITLE
                )
                text(
                    text = latestResult.host,
                    style = TextStyle.BODY
                )
                if (latestResult.success && latestResult.latency != null) {
                    text(
                        text = "Latenz: ${latestResult.latency}ms",
                        style = TextStyle.BODY
                    )
                } else if (latestResult.error != null) {
                    text(
                        text = "Fehler: ${latestResult.error}",
                        style = TextStyle.CAPTION
                    )
                }
                spacer()
            }
            
            // History
            if (pingHistory.isNotEmpty()) {
                text(
                    text = "Ping-Historie (${pingHistory.size} Einträge):",
                    style = TextStyle.TITLE
                )
                
                val listItems = pingHistory.map { result ->
                    val subtitle = if (result.success && result.latency != null) {
                        "${result.latency}ms - ${formatTimestamp(result.timestamp)}"
                    } else {
                        "${result.error ?: "Unbekannter Fehler"} - ${formatTimestamp(result.timestamp)}"
                    }
                    ListItem(
                        id = result.host + "_" + result.timestamp,
                        title = result.host,
                        subtitle = subtitle
                    )
                }
                list(
                    id = "ping_history_list",
                    items = listItems
                )
                
                spacer()
                
                button(
                    id = "clear_history_button",
                    text = "Historie löschen",
                    enabled = true,
                    variant = ButtonVariant.SECONDARY
                )
                
                spacer()
            }
            
            // Plugin info
            text(
                text = "Plugin-Informationen:",
                style = TextStyle.TITLE
            )
            text(
                text = "Plugin ID: com.ble1st.connectias.pingplugin",
                style = TextStyle.CAPTION
            )
            text(
                text = "Autor: Connectias Team",
                style = TextStyle.CAPTION
            )
            text(
                text = "Kategorie: NETWORK",
                style = TextStyle.CAPTION
            )
            text(
                text = "Hinweis: Socket-basiertes Ping (ICMP erfordert Root)",
                style = TextStyle.CAPTION
            )
        }
    }
    
    override fun onUserAction(action: UserAction) {
        when (action.actionType) {
            UserAction.ACTION_CLICK -> {
                when (action.targetId) {
                    "ping_button" -> {
                        if (hostInput.isBlank()) {
                            showError = "Bitte geben Sie einen Host ein"
                        } else if (!PingService.isValidHost(hostInput)) {
                            showError = "Ungültiger Host-Name oder IP-Adresse"
                        } else {
                            showError = null
                            performPing(hostInput)
                        }
                    }
                    "clear_history_button" -> {
                        pingHistory.clear()
                        pluginContext?.logInfo("Ping history cleared")
                    }
                }
            }
            UserAction.ACTION_TEXT_CHANGED -> {
                when (action.targetId) {
                    "host_input" -> {
                        hostInput = action.data.getString("value") ?: ""
                        showError = null
                    }
                }
            }
            UserAction.ACTION_ITEM_SELECTED -> {
                // Handle list item selection if needed
                pluginContext?.logDebug("Ping history item selected: ${action.targetId}")
            }
        }
    }
    
    override fun onUILifecycle(event: String) {
        when (event) {
            UILifecycleEvent.ON_CREATE -> {
                pluginContext?.logDebug("PingPlugin UI created")
            }
            UILifecycleEvent.ON_RESUME -> {
                pluginContext?.logDebug("PingPlugin UI resumed")
            }
            UILifecycleEvent.ON_PAUSE -> {
                pluginContext?.logDebug("PingPlugin UI paused")
            }
            UILifecycleEvent.ON_DESTROY -> {
                pluginContext?.logDebug("PingPlugin UI destroyed")
            }
        }
    }
    
    private fun performPing(host: String) {
        if (host.isBlank() || !PingService.isValidHost(host)) {
            pluginContext?.logWarning("Invalid host: $host")
            return
        }
        
        isPinging = true
        pluginContext?.logInfo("Starting ping to $host")
        
        coroutineScope.launch {
            try {
                val result = PingService.ping(host)
                pingHistory.add(0, result) // Add to beginning for newest first
                
                if (result.success) {
                    pluginContext?.logInfo("Ping successful to $host: ${result.latency}ms")
                } else {
                    pluginContext?.logWarning("Ping failed to $host: ${result.error}")
                }
            } catch (e: Exception) {
                pluginContext?.logError("Ping error", e)
                val errorResult = PingResult(
                    success = false,
                    error = "Exception: ${e.message}",
                    host = host
                )
                pingHistory.add(0, errorResult)
            } finally {
                isPinging = false
            }
        }
    }
    
    private fun formatTimestamp(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        return when {
            diff < 1000 -> "Gerade eben"
            diff < 60000 -> "${diff / 1000}s"
            diff < 3600000 -> "${diff / 60000}min"
            else -> "${diff / 3600000}h"
        }
    }
}

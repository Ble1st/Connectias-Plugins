package com.ble1st.connectias.networkinfoplugin

import android.content.Context
import android.os.Bundle
import com.ble1st.connectias.plugin.IPlugin
import com.ble1st.connectias.plugin.PluginCategory
import com.ble1st.connectias.plugin.PluginContext
import com.ble1st.connectias.plugin.PluginMetadata
import com.ble1st.connectias.plugin.ui.*
import kotlinx.coroutines.*
import timber.log.Timber

/**
 * Network Info Plugin for Connectias Plugin System
 * 
 * Uses Three-Process UI Architecture with state-based rendering.
 * Implements IPlugin interface with onRenderUI() and onUserAction().
 */
class NetworkInfoPlugin : IPlugin {
    
    private var pluginContext: PluginContext? = null
    private var isPluginEnabled = false
    private var networkInfo: NetworkInfo? = null
    private var isRefreshing = false
    
    // Coroutine scope for async operations
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    // IPlugin implementation
    override fun onLoad(context: PluginContext): Boolean {
        Timber.i("NetworkInfoPlugin: onLoad called")
        this.pluginContext = context
        context.logVerbose("NetworkInfoPlugin: Verbose log - detailed initialization info")
        context.logInfo("NetworkInfoPlugin loaded successfully")
        context.logDebug("NetworkInfoPlugin: Debug info - context initialized")
        return true
    }
    
    override fun onEnable(): Boolean {
        Timber.i("NetworkInfoPlugin: onEnable called")
        isPluginEnabled = true
        pluginContext?.logInfo("NetworkInfoPlugin enabled")
        pluginContext?.logDebug("NetworkInfoPlugin: State changed to enabled")
        
        // Load network info initially
        val context = pluginContext?.getApplicationContext()
        if (context != null) {
            refreshNetworkInfo(context)
        }
        return true
    }
    
    override fun onDisable(): Boolean {
        Timber.i("NetworkInfoPlugin: onDisable called")
        isPluginEnabled = false
        pluginContext?.logWarning("NetworkInfoPlugin disabled - features may not be available")
        pluginContext?.logDebug("NetworkInfoPlugin: State changed to disabled")
        return true
    }
    
    override fun onUnload(): Boolean {
        Timber.i("NetworkInfoPlugin: onUnload called")
        coroutineScope.cancel()
        pluginContext?.logInfo("NetworkInfoPlugin unloaded")
        pluginContext?.logDebug("NetworkInfoPlugin: Cleanup completed")
        return true
    }
    
    override fun getMetadata(): PluginMetadata {
        return PluginMetadata(
            pluginId = "com.ble1st.connectias.networkinfoplugin",
            pluginName = "Network Info Plugin",
            version = "1.0.0",
            author = "Connectias Team",
            minApiLevel = 33,
            maxApiLevel = 36,
            minAppVersion = "1.0.0",
            nativeLibraries = emptyList(),
            fragmentClassName = null, // No longer uses Fragment
            description = "Ein Netzwerk-Informations-Plugin für das Connectias Plugin-System mit Anzeige von IP-Adressen, WiFi-Informationen und Netzwerkdetails",
            permissions = listOf(
                "android.permission.ACCESS_NETWORK_STATE",
                "android.permission.ACCESS_WIFI_STATE"
            ),
            category = PluginCategory.NETWORK,
            dependencies = emptyList()
        )
    }
    
    override fun onRenderUI(screenId: String): UIStateData? {
        return buildPluginUI(screenId) {
            title("Network Info Plugin")
            
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
            
            // Network info display
            networkInfo?.let { info ->
                // Connection status
                text(
                    text = "Verbindungsstatus: ${if (info.isConnected) "Verbunden" else "Nicht verbunden"}",
                    style = TextStyle.BODY
                )
                text(
                    text = "Netzwerktyp: ${info.networkType}",
                    style = TextStyle.BODY
                )
                spacer()
                
                // IP Addresses
                if (info.ipAddresses.isNotEmpty()) {
                    text(
                        text = "IP-Adressen:",
                        style = TextStyle.TITLE
                    )
                    val listItems = info.ipAddresses.map { interfaceInfo ->
                        val subtitle = buildString {
                            interfaceInfo.ipAddress?.let { append("IP: $it") }
                            interfaceInfo.macAddress?.let {
                                if (isNotEmpty()) append(" | ")
                                append("MAC: $it")
                            }
                        }
                        ListItem(
                            id = interfaceInfo.name,
                            title = interfaceInfo.displayName,
                            subtitle = subtitle.ifEmpty { null }
                        )
                    }
                    list(
                        id = "ip_addresses_list",
                        items = listItems
                    )
                    spacer()
                }
                
                // WiFi Info
                info.wifiInfo?.let { wifiInfo ->
                    text(
                        text = "WiFi-Informationen:",
                        style = TextStyle.TITLE
                    )
                    wifiInfo.ssid?.let { ssid ->
                        text(
                            text = "SSID: $ssid",
                            style = TextStyle.BODY
                        )
                    }
                    wifiInfo.ipAddress?.let { ip ->
                        text(
                            text = "IP-Adresse: $ip",
                            style = TextStyle.BODY
                        )
                    }
                    wifiInfo.signalStrength?.let { signal ->
                        val signalPercent = calculateSignalPercent(signal)
                        text(
                            text = "Signalstärke: $signal dBm ($signalPercent%)",
                            style = TextStyle.BODY
                        )
                    }
                    wifiInfo.linkSpeed?.let { speed ->
                        text(
                            text = "Verbindungsgeschwindigkeit: $speed Mbps",
                            style = TextStyle.BODY
                        )
                    }
                    wifiInfo.frequency?.let { freq ->
                        text(
                            text = "Frequenz: $freq MHz",
                            style = TextStyle.BODY
                        )
                    }
                    wifiInfo.bssid?.let { bssid ->
                        text(
                            text = "BSSID: $bssid",
                            style = TextStyle.BODY
                        )
                    }
                    spacer()
                }
                
                // Network Details
                text(
                    text = "Netzwerk-Details:",
                    style = TextStyle.TITLE
                )
                info.gateway?.let { gateway ->
                    text(
                        text = "Gateway: $gateway",
                        style = TextStyle.BODY
                    )
                }
                if (info.dnsServers.isNotEmpty()) {
                    text(
                        text = "DNS-Server: ${info.dnsServers.joinToString(", ")}",
                        style = TextStyle.BODY
                    )
                }
                info.subnetMask?.let { subnet ->
                    text(
                        text = "Subnetz-Maske: $subnet",
                        style = TextStyle.BODY
                    )
                }
            } ?: run {
                // Loading or no data state
                if (isRefreshing) {
                    text(
                        text = "Lade Netzwerkinformationen...",
                        style = TextStyle.BODY
                    )
                } else {
                    text(
                        text = "Keine Netzwerkinformationen verfügbar",
                        style = TextStyle.BODY
                    )
                }
            }
            
            spacer(height = 16)
            
            // Refresh button
            button(
                id = "refresh_button",
                text = if (isRefreshing) "Aktualisiere..." else "Aktualisieren",
                enabled = !isRefreshing,
                variant = ButtonVariant.PRIMARY
            )
            
            spacer()
            
            // Plugin info
            text(
                text = "Plugin-Informationen:",
                style = TextStyle.TITLE
            )
            text(
                text = "Plugin ID: com.ble1st.connectias.networkinfoplugin",
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
                text = "Hinweis: Einige Informationen erfordern spezielle Permissions",
                style = TextStyle.CAPTION
            )
        }
    }
    
    override fun onUserAction(action: UserAction) {
        when (action.actionType) {
            UserAction.ACTION_CLICK -> {
                when (action.targetId) {
                    "refresh_button" -> {
                        val context = pluginContext?.getApplicationContext()
                        if (context != null) {
                            refreshNetworkInfo(context)
                        }
                    }
                }
            }
            UserAction.ACTION_ITEM_SELECTED -> {
                // Handle list item selection if needed
                pluginContext?.logDebug("Item selected: ${action.targetId}")
            }
        }
    }
    
    override fun onUILifecycle(event: String) {
        when (event) {
            UILifecycleEvent.ON_CREATE -> {
                pluginContext?.logDebug("NetworkInfoPlugin UI created")
                // Load network info when UI is created
                val context = pluginContext?.getApplicationContext()
                if (context != null && networkInfo == null) {
                    refreshNetworkInfo(context)
                }
            }
            UILifecycleEvent.ON_RESUME -> {
                pluginContext?.logDebug("NetworkInfoPlugin UI resumed")
            }
            UILifecycleEvent.ON_PAUSE -> {
                pluginContext?.logDebug("NetworkInfoPlugin UI paused")
            }
            UILifecycleEvent.ON_DESTROY -> {
                pluginContext?.logDebug("NetworkInfoPlugin UI destroyed")
            }
        }
    }
    
    private fun refreshNetworkInfo(context: Context) {
        isRefreshing = true
        pluginContext?.logInfo("Refreshing network information")
        
        coroutineScope.launch {
            try {
                val info = withContext(Dispatchers.IO) {
                    NetworkInfoService.getNetworkInfo(context)
                }
                networkInfo = info
                pluginContext?.logInfo("Network information refreshed successfully")
            } catch (e: Exception) {
                pluginContext?.logError("Failed to refresh network information", e)
                Timber.e(e, "Failed to refresh network information")
            } finally {
                isRefreshing = false
            }
        }
    }
    
    /**
     * Calculate signal strength percentage from RSSI
     */
    private fun calculateSignalPercent(rssi: Int): Int {
        // RSSI typically ranges from -100 (weak) to -30 (strong)
        return when {
            rssi >= -50 -> 100
            rssi >= -60 -> 80
            rssi >= -70 -> 60
            rssi >= -80 -> 40
            rssi >= -90 -> 20
            else -> 10
        }
    }
}

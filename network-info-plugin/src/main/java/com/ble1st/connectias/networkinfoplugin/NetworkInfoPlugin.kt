package com.ble1st.connectias.networkinfoplugin

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.ble1st.connectias.plugin.sdk.IPlugin
import com.ble1st.connectias.plugin.sdk.PluginCategory
import com.ble1st.connectias.plugin.sdk.PluginContext
import com.ble1st.connectias.plugin.sdk.PluginMetadata
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Network Info Plugin for Connectias Plugin System
 * 
 * Implements both IPlugin and Fragment
 * The plugin system expects the class to be loaded via fragmentClassName
 * and to implement IPlugin
 */
class NetworkInfoPlugin : Fragment(), IPlugin {
    
    private var pluginContext: PluginContext? = null
    private var isPluginEnabled by mutableStateOf(false)
    private var networkInfo by mutableStateOf<NetworkInfo?>(null)
    private var isRefreshing by mutableStateOf(false)
    
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
        // Use pluginContext instead of requireContext() - fragment may not be attached yet
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
            fragmentClassName = "com.ble1st.connectias.networkinfoplugin.NetworkInfoPlugin",
            description = "Ein Netzwerk-Informations-Plugin für das Connectias Plugin-System mit Anzeige von IP-Adressen, WiFi-Informationen und Netzwerkdetails",
            permissions = listOf(
                "android.permission.ACCESS_NETWORK_STATE",
                "android.permission.ACCESS_WIFI_STATE"
            ),
            category = PluginCategory.NETWORK,
            dependencies = emptyList()
        )
    }
    
    // Fragment implementation
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Timber.d("NetworkInfoPlugin: onCreateView called")
        pluginContext?.logDebug("NetworkInfoPlugin: Creating Compose view")
        
        return ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme {
                    NetworkInfoPluginScreen(
                        isPluginEnabled = isPluginEnabled,
                        networkInfo = networkInfo,
                        isRefreshing = isRefreshing,
                        onRefreshClick = {
                            // Use requireContext() here - fragment is attached in onCreateView
                            refreshNetworkInfo(requireContext())
                        }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.i("NetworkInfoPlugin: View created successfully")
        pluginContext?.logInfo("NetworkInfoPlugin UI view created and ready")
        
        // Load network info initially - fragment is attached in onViewCreated
        if (networkInfo == null) {
            refreshNetworkInfo(requireContext())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("NetworkInfoPlugin: onDestroy called")
        pluginContext?.logDebug("NetworkInfoPlugin: Fragment destroyed")
    }
    
    // Override onPause to resolve conflict between Fragment and IPlugin
    override fun onPause() {
        super<Fragment>.onPause()
        pluginContext?.logDebug("NetworkInfoPlugin: onPause called")
    }
    
    // Override onResume to resolve conflict between Fragment and IPlugin
    override fun onResume() {
        super<Fragment>.onResume()
        pluginContext?.logDebug("NetworkInfoPlugin: onResume called")
    }
    
    private fun refreshNetworkInfo(context: Context) {
        isRefreshing = true
        pluginContext?.logInfo("Refreshing network information")
        
        // Use viewLifecycleOwner if fragment view is created, otherwise use lifecycleScope or GlobalScope
        val coroutineScope = try {
            if (view != null) {
                viewLifecycleOwner.lifecycleScope
            } else if (lifecycle.currentState.isAtLeast(Lifecycle.State.INITIALIZED)) {
                lifecycleScope
            } else {
                GlobalScope
            }
        } catch (e: IllegalStateException) {
            // Fragment not attached, use GlobalScope
            GlobalScope
        }
        
        coroutineScope.launch {
            try {
                val info = NetworkInfoService.getNetworkInfo(context)
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkInfoPluginScreen(
    isPluginEnabled: Boolean,
    networkInfo: NetworkInfo?,
    isRefreshing: Boolean,
    onRefreshClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Network Info Plugin") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(
                        onClick = onRefreshClick,
                        enabled = !isRefreshing
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Aktualisieren")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isPluginEnabled) 
                        MaterialTheme.colorScheme.primaryContainer 
                    else 
                        MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (isPluginEnabled) Icons.Default.CheckCircle else Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = if (isPluginEnabled) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isPluginEnabled) "Plugin Aktiv" else "Plugin Geladen",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Version 1.0.0",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Connection Info Card
            networkInfo?.let { info ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (info.isConnected) 
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        else 
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Verbindungsstatus",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (info.isConnected) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (info.isConnected) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = if (info.isConnected) "Verbunden" else "Nicht verbunden",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        InfoRow(
                            icon = Icons.Default.NetworkCheck,
                            label = "Netzwerktyp",
                            value = info.networkType
                        )
                    }
                }

                // IP Addresses Card
                if (info.ipAddresses.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "IP-Adressen",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.heightIn(max = 200.dp)
                            ) {
                                items(info.ipAddresses) { interfaceInfo ->
                                    InterfaceItem(interfaceInfo)
                                }
                            }
                        }
                    }
                }

                // WiFi Info Card
                info.wifiInfo?.let { wifiInfo ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "WiFi-Informationen",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            wifiInfo.ssid?.let { ssid ->
                                InfoRow(
                                    icon = Icons.Default.Wifi,
                                    label = "SSID",
                                    value = ssid
                                )
                            }
                            
                            wifiInfo.ipAddress?.let { ip ->
                                InfoRow(
                                    icon = Icons.Default.Dns,
                                    label = "IP-Adresse",
                                    value = ip.toString()
                                )
                            }
                            
                            wifiInfo.signalStrength?.let { signal ->
                                val signalPercent = calculateSignalPercent(signal)
                                InfoRow(
                                    icon = Icons.Default.SignalWifi4Bar,
                                    label = "Signalstärke",
                                    value = "${signal} dBm (${signalPercent}%)"
                                )
                            }
                            
                            wifiInfo.linkSpeed?.let { speed ->
                                InfoRow(
                                    icon = Icons.Default.Speed,
                                    label = "Verbindungsgeschwindigkeit",
                                    value = "${speed} Mbps"
                                )
                            }
                            
                            wifiInfo.frequency?.let { freq ->
                                InfoRow(
                                    icon = Icons.Default.Radio,
                                    label = "Frequenz",
                                    value = "${freq} MHz"
                                )
                            }
                            
                            wifiInfo.bssid?.let { bssid ->
                                InfoRow(
                                    icon = Icons.Default.Router,
                                    label = "BSSID",
                                    value = bssid
                                )
                            }
                        }
                    }
                }

                // Network Details Card
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Netzwerk-Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        info.gateway?.let { gateway ->
                            InfoRow(
                                icon = Icons.Default.Router,
                                label = "Gateway",
                                value = gateway
                            )
                        }
                        
                        if (info.dnsServers.isNotEmpty()) {
                            InfoRow(
                                icon = Icons.Default.Dns,
                                label = "DNS-Server",
                                value = info.dnsServers.joinToString(", ")
                            )
                        }
                        
                        info.subnetMask?.let { subnet ->
                            InfoRow(
                                icon = Icons.Default.Settings,
                                label = "Subnetz-Maske",
                                value = subnet
                            )
                        }
                    }
                }
            } ?: run {
                // Loading state
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator()
                            Text("Lade Netzwerkinformationen...")
                        } else {
                            Text("Keine Netzwerkinformationen verfügbar")
                            Button(onClick = onRefreshClick) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Aktualisieren")
                            }
                        }
                    }
                }
            }

            // Info Card
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Plugin-Informationen",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    InfoRow(
                        icon = Icons.Default.Build,
                        label = "Plugin ID",
                        value = "com.ble1st.connectias.networkinfoplugin"
                    )
                    
                    InfoRow(
                        icon = Icons.Default.Person,
                        label = "Autor",
                        value = "Connectias Team"
                    )
                    
                    InfoRow(
                        icon = Icons.Default.Settings,
                        label = "Kategorie",
                        value = "NETWORK"
                    )
                    
                    InfoRow(
                        icon = Icons.Default.Info,
                        label = "Hinweis",
                        value = "Einige Informationen erfordern spezielle Permissions"
                    )
                }
            }
        }
    }
}

@Composable
fun InterfaceItem(interfaceInfo: InterfaceInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (interfaceInfo.isUp) 
                MaterialTheme.colorScheme.surfaceVariant
            else 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (interfaceInfo.isUp) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (interfaceInfo.isUp) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = interfaceInfo.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            
            interfaceInfo.ipAddress?.let { ip ->
                Text(
                    text = "IP: $ip",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            interfaceInfo.macAddress?.let { mac ->
                Text(
                    text = "MAC: $mac",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium
            )
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

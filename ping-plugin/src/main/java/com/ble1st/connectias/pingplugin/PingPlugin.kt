package com.ble1st.connectias.pingplugin

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
import androidx.lifecycle.lifecycleScope
import com.ble1st.connectias.plugin.sdk.IPlugin
import com.ble1st.connectias.plugin.sdk.PluginCategory
import com.ble1st.connectias.plugin.sdk.PluginContext
import com.ble1st.connectias.plugin.sdk.PluginMetadata
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Ping Plugin for Connectias Plugin System
 * 
 * Implements both IPlugin and Fragment
 * The plugin system expects the class to be loaded via fragmentClassName
 * and to implement IPlugin
 */
class PingPlugin : Fragment(), IPlugin {
    
    private var pluginContext: PluginContext? = null
    private var isPluginEnabled by mutableStateOf(false)
    private val pingHistory = mutableStateListOf<PingResult>()
    private var isPinging by mutableStateOf(false)
    
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
            fragmentClassName = "com.ble1st.connectias.pingplugin.PingPlugin",
            description = "Ein Netzwerk-Ping-Tool für das Connectias Plugin-System mit ICMP-Ping-Funktionalität",
            permissions = emptyList(),
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
        Timber.d("PingPlugin: onCreateView called")
        pluginContext?.logDebug("PingPlugin: Creating Compose view")
        
        return ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme {
                    PingPluginScreen(
                        isPluginEnabled = isPluginEnabled,
                        isPinging = isPinging,
                        pingHistory = pingHistory,
                        onPingClick = { host ->
                            performPing(host)
                        },
                        onClearHistory = {
                            pingHistory.clear()
                            pluginContext?.logInfo("Ping history cleared")
                        }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.i("PingPlugin: View created successfully")
        pluginContext?.logInfo("PingPlugin UI view created and ready")
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("PingPlugin: onDestroy called")
        pluginContext?.logDebug("PingPlugin: Fragment destroyed")
    }
    
    private fun performPing(host: String) {
        if (host.isBlank() || !PingService.isValidHost(host)) {
            pluginContext?.logWarning("Invalid host: $host")
            return
        }
        
        isPinging = true
        pluginContext?.logInfo("Starting ping to $host")
        
        val coroutineScope = viewLifecycleOwner.lifecycleScope
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PingPluginScreen(
    isPluginEnabled: Boolean,
    isPinging: Boolean,
    pingHistory: List<PingResult>,
    onPingClick: (String) -> Unit,
    onClearHistory: () -> Unit
) {
    var hostInput by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf<String?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ping Plugin") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    if (pingHistory.isNotEmpty()) {
                        IconButton(onClick = onClearHistory) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear History")
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

            // Ping Input Card
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Host anpingen",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    OutlinedTextField(
                        value = hostInput,
                        onValueChange = { 
                            hostInput = it
                            showError = null
                        },
                        label = { Text("Host oder IP-Adresse") },
                        placeholder = { Text("z.B. google.com oder 8.8.8.8") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !isPinging,
                        trailingIcon = {
                            if (hostInput.isNotEmpty()) {
                                IconButton(onClick = { hostInput = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        isError = showError != null
                    )
                    
                    if (showError != null) {
                        Text(
                            text = showError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    
                    Button(
                        onClick = {
                            if (hostInput.isBlank()) {
                                showError = "Bitte geben Sie einen Host ein"
                            } else if (!PingService.isValidHost(hostInput)) {
                                showError = "Ungültiger Host-Name oder IP-Adresse"
                            } else {
                                showError = null
                                onPingClick(hostInput)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isPinging
                    ) {
                        if (isPinging) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Ping läuft...")
                        } else {
                            Icon(Icons.Default.NetworkCheck, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Ping starten")
                        }
                    }
                }
            }

            // Latest Result Card
            if (pingHistory.isNotEmpty()) {
                val latestResult = pingHistory.first()
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (latestResult.success) 
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        else 
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Letztes Ergebnis",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (latestResult.success) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (latestResult.success) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.error
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = latestResult.host,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                if (latestResult.success && latestResult.latency != null) {
                                    Text(
                                        text = "Latenz: ${latestResult.latency}ms",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else if (latestResult.error != null) {
                                    Text(
                                        text = latestResult.error,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // History Card
            if (pingHistory.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Ping-Historie",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${pingHistory.size} Einträge",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        HorizontalDivider()
                        
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.heightIn(max = 300.dp)
                        ) {
                            items(pingHistory) { result ->
                                HistoryItem(result)
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
                        value = "com.ble1st.connectias.pingplugin"
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
                        value = "Socket-basiertes Ping (ICMP erfordert Root)"
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryItem(result: PingResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (result.success) 
                MaterialTheme.colorScheme.surfaceVariant
            else 
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (result.success) Icons.Default.CheckCircle else Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (result.success) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.error
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result.host,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                if (result.success && result.latency != null) {
                    Text(
                        text = "${result.latency}ms",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (result.error != null) {
                    Text(
                        text = result.error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            Text(
                text = formatTimestamp(result.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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

package com.ble1st.connectias.networkinfoplugin

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo as AndroidNetworkInfo
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketException

/**
 * Network interface information
 */
data class InterfaceInfo(
    val name: String,
    val displayName: String,
    val ipAddress: String?,
    val macAddress: String?,
    val isUp: Boolean,
    val isLoopback: Boolean
)

/**
 * WiFi-specific information
 */
data class WifiInfo(
    val ssid: String?,
    val bssid: String?,
    val signalStrength: Int?,
    val linkSpeed: Int?,
    val frequency: Int?,
    val ipAddress: String?
)

/**
 * Complete network information
 */
data class NetworkInfo(
    val isConnected: Boolean,
    val networkType: String,
    val ipAddresses: List<InterfaceInfo>,
    val wifiInfo: com.ble1st.connectias.networkinfoplugin.WifiInfo?,
    val gateway: String?,
    val dnsServers: List<String>,
    val subnetMask: String?
)

/**
 * Service for collecting network information
 */
object NetworkInfoService {
    
    /**
     * Get complete network information
     */
    suspend fun getNetworkInfo(context: Context): NetworkInfo = withContext(Dispatchers.IO) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        val isConnected = isNetworkConnected(connectivityManager)
        val networkType = getNetworkType(connectivityManager)
        val ipAddresses = getIpAddresses()
        val wifiInfo = getWifiInfo(context)
        val gateway = getGateway()
        val dnsServers = getDnsServers(connectivityManager)
        val subnetMask = getSubnetMask()
        
        NetworkInfo(
            isConnected = isConnected,
            networkType = networkType,
            ipAddresses = ipAddresses,
            wifiInfo = wifiInfo,
            gateway = gateway,
            dnsServers = dnsServers,
            subnetMask = subnetMask
        )
    }
    
    /**
     * Check if network is connected
     */
    private fun isNetworkConnected(connectivityManager: ConnectivityManager): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
            val capabilities = network?.let { connectivityManager.getNetworkCapabilities(it) }
            capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } else {
            @Suppress("DEPRECATION")
            val activeNetwork: AndroidNetworkInfo? = connectivityManager.activeNetworkInfo
            activeNetwork?.isConnected == true
        }
    }
    
    /**
     * Get network type as string
     */
    private fun getNetworkType(connectivityManager: ConnectivityManager): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return "Unbekannt"
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return "Unbekannt"
            
            when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile Daten"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                else -> "Unbekannt"
            }
        } else {
            @Suppress("DEPRECATION")
            val activeNetwork: AndroidNetworkInfo? = connectivityManager.activeNetworkInfo
            when (activeNetwork?.type) {
                ConnectivityManager.TYPE_WIFI -> "WiFi"
                ConnectivityManager.TYPE_MOBILE -> "Mobile Daten"
                ConnectivityManager.TYPE_ETHERNET -> "Ethernet"
                else -> "Unbekannt"
            }
        }
    }
    
    /**
     * Get all IP addresses from network interfaces
     */
    private fun getIpAddresses(): List<InterfaceInfo> {
        val interfaces = mutableListOf<InterfaceInfo>()
        
        try {
            val networkInterfaces = NetworkInterface.getNetworkInterfaces()
            while (networkInterfaces.hasMoreElements()) {
                val networkInterface = networkInterfaces.nextElement()
                
                val addresses = networkInterface.inetAddresses
                val ipAddress = addresses.asSequence()
                    .filter { !it.isLoopbackAddress && it.hostAddress != null }
                    .map { it.hostAddress }
                    .firstOrNull()
                
                if (ipAddress != null || networkInterface.isUp) {
                    val macAddress = try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            // Android 6.0+ requires special handling for MAC address
                            getMacAddress(networkInterface)
                        } else {
                            @Suppress("DEPRECATION")
                            networkInterface.hardwareAddress?.joinToString(":") { 
                                String.format("%02X", it) 
                            }
                        }
                    } catch (e: Exception) {
                        null
                    }
                    
                    interfaces.add(
                        InterfaceInfo(
                            name = networkInterface.name,
                            displayName = getDisplayName(networkInterface.name),
                            ipAddress = ipAddress,
                            macAddress = macAddress,
                            isUp = networkInterface.isUp,
                            isLoopback = networkInterface.isLoopback
                        )
                    )
                }
            }
        } catch (e: SocketException) {
            // Ignore
        }
        
        return interfaces.sortedBy { it.name }
    }
    
    /**
     * Get display name for network interface
     */
    private fun getDisplayName(name: String): String {
        return when {
            name.startsWith("wlan") -> "WiFi ($name)"
            name.startsWith("rmnet") || name.startsWith("rmnet_data") -> "Mobile ($name)"
            name.startsWith("eth") -> "Ethernet ($name)"
            name == "lo" -> "Loopback"
            else -> name
        }
    }
    
    /**
     * Get MAC address (Android 6.0+ compatible)
     */
    @RequiresApi(Build.VERSION_CODES.M)
    private fun getMacAddress(networkInterface: NetworkInterface): String? {
        return try {
            // On Android 6.0+, MAC address access is restricted
            // This will return null for most interfaces
            val hardwareAddress = networkInterface.hardwareAddress
            hardwareAddress?.joinToString(":") { String.format("%02X", it) }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Get WiFi information
     */
    private fun getWifiInfo(context: Context): com.ble1st.connectias.networkinfoplugin.WifiInfo? {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            ?: return null
        
        if (!wifiManager.isWifiEnabled) {
            return null
        }
        
        val androidWifiInfo: android.net.wifi.WifiInfo? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ requires different approach
            wifiManager.connectionInfo
        } else {
            @Suppress("DEPRECATION")
            wifiManager.connectionInfo
        }
        
        if (androidWifiInfo == null) {
            return null
        }
        
        val ssid = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ requires location permission for SSID
            androidWifiInfo.ssid?.replace("\"", "") ?: "Versteckt"
        } else {
            @Suppress("DEPRECATION")
            androidWifiInfo.ssid?.replace("\"", "") ?: "Unbekannt"
        }
        
        val bssid = androidWifiInfo.bssid
        val signalStrength = androidWifiInfo.rssi
        val linkSpeed = androidWifiInfo.linkSpeed
        val frequency = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            androidWifiInfo.frequency
        } else {
            null
        }
        
        // Get IP address from WiFi interface
        val ipAddress = getIpAddresses()
            .firstOrNull { it.name.startsWith("wlan") }
            ?.ipAddress
        
        return com.ble1st.connectias.networkinfoplugin.WifiInfo(
            ssid = ssid,
            bssid = bssid,
            signalStrength = signalStrength,
            linkSpeed = linkSpeed,
            frequency = frequency,
            ipAddress = ipAddress
        )
    }
    
    /**
     * Get gateway address
     */
    private fun getGateway(): String? {
        return try {
            val networkInterfaces = NetworkInterface.getNetworkInterfaces()
            while (networkInterfaces.hasMoreElements()) {
                val networkInterface = networkInterfaces.nextElement()
                if (!networkInterface.isLoopback && networkInterface.isUp) {
                    val addresses = networkInterface.interfaceAddresses
                    for (address in addresses) {
                        // Gateway is typically the first address in the network
                        val broadcast = address.broadcast
                        if (broadcast != null) {
                            val inetAddress = address.address
                            val networkPrefixLength = address.networkPrefixLength
                            // Calculate gateway (typically .1 in the subnet)
                            val ipBytes = inetAddress.address
                            if (ipBytes.size == 4 && networkPrefixLength <= 24) {
                                ipBytes[3] = 1
                                return InetAddress.getByAddress(ipBytes).hostAddress
                            }
                        }
                    }
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Get DNS servers
     */
    private fun getDnsServers(connectivityManager: ConnectivityManager): List<String> {
        val dnsServers = mutableListOf<String>()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return emptyList()
            val linkProperties = connectivityManager.getLinkProperties(network) ?: return emptyList()
            
            linkProperties.dnsServers.forEach { dnsServer ->
                dnsServers.add(dnsServer.hostAddress)
            }
        } else {
            // Fallback: Common DNS servers
            dnsServers.add("8.8.8.8") // Google DNS
            dnsServers.add("8.8.4.4") // Google DNS
        }
        
        return dnsServers
    }
    
    /**
     * Get subnet mask
     */
    private fun getSubnetMask(): String? {
        return try {
            val networkInterfaces = NetworkInterface.getNetworkInterfaces()
            while (networkInterfaces.hasMoreElements()) {
                val networkInterface = networkInterfaces.nextElement()
                if (!networkInterface.isLoopback && networkInterface.isUp) {
                    val addresses = networkInterface.interfaceAddresses
                    for (address in addresses) {
                        val prefixLength = address.networkPrefixLength
                        if (prefixLength > 0) {
                            // Convert prefix length to subnet mask
                            val mask = calculateSubnetMask(prefixLength.toInt())
                            return mask
                        }
                    }
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Calculate subnet mask from prefix length
     */
    private fun calculateSubnetMask(prefixLength: Int): String {
        val mask = (-1L shl (32 - prefixLength)).toInt()
        return String.format(
            "%d.%d.%d.%d",
            (mask shr 24) and 0xFF,
            (mask shr 16) and 0xFF,
            (mask shr 8) and 0xFF,
            mask and 0xFF
        )
    }
}

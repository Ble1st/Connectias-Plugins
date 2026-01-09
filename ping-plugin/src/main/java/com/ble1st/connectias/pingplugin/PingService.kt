package com.ble1st.connectias.pingplugin

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.Socket
import java.net.UnknownHostException
import java.util.concurrent.TimeoutException

/**
 * Result of a ping operation
 */
data class PingResult(
    val success: Boolean,
    val latency: Long? = null, // in milliseconds
    val error: String? = null,
    val host: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Service for performing network ping operations
 * Uses socket-based connection testing since ICMP requires root on Android
 */
object PingService {
    
    /**
     * Ping a host using socket-based connection
     * @param host Hostname or IP address to ping
     * @param timeout Timeout in milliseconds (default: 5000ms)
     * @return PingResult with success status and latency
     */
    suspend fun ping(host: String, timeout: Int = 5000): PingResult = withContext(Dispatchers.IO) {
        try {
            val startTime = System.currentTimeMillis()
            
            // Resolve hostname to IP address
            val address = try {
                InetAddress.getByName(host)
            } catch (e: UnknownHostException) {
                return@withContext PingResult(
                    success = false,
                    error = "Unknown host: $host",
                    host = host
                )
            }
            
            // Try to connect to a common port (80 for HTTP, 443 for HTTPS)
            // This simulates a ping by testing connectivity
            val ports = listOf(80, 443, 22, 53) // HTTP, HTTPS, SSH, DNS
            
            var connected = false
            var connectionTime: Long? = null
            
            for (port in ports) {
                try {
                    val socket = Socket()
                    socket.connect(java.net.InetSocketAddress(address, port), timeout)
                    val endTime = System.currentTimeMillis()
                    connectionTime = endTime - startTime
                    socket.close()
                    connected = true
                    break
                } catch (e: java.net.SocketTimeoutException) {
                    // Try next port
                    continue
                } catch (e: java.net.ConnectException) {
                    // Port is closed, but host is reachable (this is actually a good sign)
                    val endTime = System.currentTimeMillis()
                    connectionTime = endTime - startTime
                    connected = true
                    break
                } catch (e: Exception) {
                    // Try next port
                    continue
                }
            }
            
            if (connected && connectionTime != null) {
                PingResult(
                    success = true,
                    latency = connectionTime,
                    host = host
                )
            } else {
                // Fallback: Just check if host is reachable via InetAddress.isReachable
                // This uses ICMP if available, otherwise falls back to TCP
                val reachable = try {
                    address.isReachable(timeout)
                } catch (e: Exception) {
                    false
                }
                
                if (reachable) {
                    val endTime = System.currentTimeMillis()
                    val latency = endTime - startTime
                    PingResult(
                        success = true,
                        latency = latency,
                        host = host
                    )
                } else {
                    PingResult(
                        success = false,
                        error = "Host unreachable or timeout",
                        host = host
                    )
                }
            }
        } catch (e: UnknownHostException) {
            PingResult(
                success = false,
                error = "Unknown host: ${e.message}",
                host = host
            )
        } catch (e: TimeoutException) {
            PingResult(
                success = false,
                error = "Connection timeout",
                host = host
            )
        } catch (e: Exception) {
            PingResult(
                success = false,
                error = "Error: ${e.message ?: e.javaClass.simpleName}",
                host = host
            )
        }
    }
    
    /**
     * Validate if a host string is valid
     */
    fun isValidHost(host: String): Boolean {
        if (host.isBlank()) return false
        
        // Check if it's a valid IP address
        val ipPattern = Regex("^([0-9]{1,3}\\.){3}[0-9]{1,3}$")
        if (ipPattern.matches(host)) {
            val parts = host.split(".")
            return parts.all { it.toIntOrNull()?.let { num -> num in 0..255 } ?: false }
        }
        
        // Check if it's a valid hostname
        val hostnamePattern = Regex("^[a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?(\\.[a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?)*$")
        return hostnamePattern.matches(host) && host.length <= 253
    }
}

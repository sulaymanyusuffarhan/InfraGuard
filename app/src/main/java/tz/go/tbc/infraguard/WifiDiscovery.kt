package tz.go.tbc.infraguard

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.InetSocketAddress

/** Local Wi-Fi information only. Never sends inventory to a server. */
data class WifiSnapshot(val network: Network, val ip: Inet4Address, val prefix: Int, val gateway: Inet4Address?)
data class DiscoveredHost(val address: String, val openPorts: List<Int>)

object WifiDiscovery {
    fun current(context: Context): WifiSnapshot? {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        val network = manager.activeNetwork ?: return null
        if (manager.getNetworkCapabilities(network)?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) != true) return null
        val props: LinkProperties = manager.getLinkProperties(network) ?: return null
        val link = props.linkAddresses.firstOrNull { it.address is Inet4Address } ?: return null
        val gateway = props.routes.firstOrNull { it.isDefaultRoute && it.gateway is Inet4Address }?.gateway as? Inet4Address
        return WifiSnapshot(network, link.address as Inet4Address, link.prefixLength, gateway)
    }

    // Restrict discovery to a single private IPv4 /24 or smaller subnet, never the entire routed network.
    fun candidates(snapshot: WifiSnapshot): List<String> {
        val bytes = snapshot.ip.address
        val ip = bytes.fold(0L) { acc, b -> (acc shl 8) or (b.toLong() and 255) }
        val privateIp = (ip ushr 24 == 10L) || (ip ushr 20 == 0xAC1L) || (ip ushr 16 == 0xC0A8L)
        if (!privateIp || snapshot.prefix !in 24..30) return emptyList()
        val mask = (0xffffffffL shl (32 - snapshot.prefix)) and 0xffffffffL
        val start = ip and mask
        val end = start or (mask.inv() and 0xffffffffL)
        return ((start + 1) until end).filter { it != ip }.map { value ->
            (24 downTo 0 step 8).joinToString(".") { shift -> ((value ushr shift) and 255).toString() }
        }
    }

    suspend fun gatewayCheck(snapshot: WifiSnapshot): Boolean? = withContext(Dispatchers.IO) {
        val gateway = snapshot.gateway ?: return@withContext null
        // Only TCP 80/443; a closed port does not mean the gateway is down.
        listOf(80, 443).any { port -> tcp(snapshot.network, gateway.hostAddress ?: return@withContext null, port) }
    }

    suspend fun discover(snapshot: WifiSnapshot): List<DiscoveredHost> = coroutineScope {
        val limit = Semaphore(20)
        candidates(snapshot).map { address ->
            async(Dispatchers.IO) {
                limit.withPermit {
                    val ports = listOf(80, 443, 22).filter { tcp(snapshot.network, address, it) }
                    if (ports.isEmpty()) null else DiscoveredHost(address, ports)
                }
            }
        }.awaitAll().filterNotNull().sortedBy { it.address.split('.').last().toInt() }
    }

    private fun tcp(network: Network, address: String, port: Int): Boolean = try {
        network.socketFactory.createSocket().use { socket ->
            socket.connect(InetSocketAddress(address, port), 350)
            true
        }
    } catch (_: java.io.IOException) { false }
}

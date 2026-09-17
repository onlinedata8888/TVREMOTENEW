package com.tvremote.app.adb

import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

data class DiscoveredTv(val ip: String, val name: String)

/**
 * Finds Android TVs on the same Wi-Fi network by probing every host in the
 * phone's /24 subnet on tcp/5555 (the port "Network debugging" opens on
 * Android TV / Google TV devices). This is the same trick `adb connect`
 * itself relies on — we just automate finding the IP.
 */
class TvDiscovery(private val context: Context) {

    suspend fun scan(portTimeoutMs: Int = 250): List<DiscoveredTv> = withContext(Dispatchers.IO) {
        val subnetPrefix = localSubnetPrefix() ?: return@withContext emptyList()
        coroutineScope {
            (1..254).map { host ->
                async {
                    val ip = "$subnetPrefix.$host"
                    if (isAdbPortOpen(ip, portTimeoutMs)) DiscoveredTv(ip, ip) else null
                }
            }.awaitAll().filterNotNull()
        }
    }

    private fun isAdbPortOpen(ip: String, timeoutMs: Int): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ip, 5555), timeoutMs)
                true
            }
        } catch (_: Exception) {
            false
        }
    }

    /** Returns e.g. "192.168.1" from the phone's current Wi-Fi IP address. */
    private fun localSubnetPrefix(): String? {
        val wifiManager = context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return null
        @Suppress("DEPRECATION")
        val ipInt = wifiManager.connectionInfo?.ipAddress ?: return null
        if (ipInt == 0) return null
        val bytes = intArrayOf(
            ipInt and 0xff,
            ipInt shr 8 and 0xff,
            ipInt shr 16 and 0xff,
            ipInt shr 24 and 0xff
        )
        return "${bytes[0]}.${bytes[1]}.${bytes[2]}"
    }
}

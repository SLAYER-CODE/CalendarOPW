package org.distributed.calendar.common

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

object TcpPeerScanner {

    private const val SCAN_TIMEOUT_MS = 300L
    private const val MAX_CONCURRENT = 20

    suspend fun scan(port: Int = 8080): String? = withContext(Dispatchers.IO) {
        val localIPs = NetworkUtils.getLocalIPv4Addresses()
        if (localIPs.isEmpty()) return@withContext null

        val prefixes = localIPs.map { it.substringBeforeLast('.') }.distinct()
        println("TCP scanning subnets ${prefixes.joinToString(", ")}.1-254 for port $port...")

        for (prefix in prefixes) {
            val localInThisSubnet = localIPs.filter { it.startsWith("$prefix.") }
            val candidates = (1..254)
                .map { "$prefix.$it" }
                .filter { it !in localInThisSubnet }

            val batches = candidates.chunked(MAX_CONCURRENT)
            for (batch in batches) {
                val found = scanBatch(batch, port)
                if (found != null) {
                    println("TCP scanner found peer at $found")
                    return@withContext found
                }
            }
        }
        println("TCP scanner: no peers found")
        null
    }

    private suspend fun scanBatch(ips: List<String>, port: Int): String? = coroutineScope {
        val deferred = ips.map { ip ->
            async {
                if (tryConnect(ip, port)) ip else null
            }
        }
        deferred.awaitAll().firstOrNull { it != null }
    }

    private fun tryConnect(ip: String, port: Int): Boolean {
        return try {
            Socket().use { sock ->
                sock.connect(InetSocketAddress(ip, port), SCAN_TIMEOUT_MS.toInt())
                true
            }
        } catch (_: Exception) {
            false
        }
    }
}

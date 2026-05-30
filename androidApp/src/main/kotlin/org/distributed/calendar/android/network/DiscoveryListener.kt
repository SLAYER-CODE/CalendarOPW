package org.distributed.calendar.android.network

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketTimeoutException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.distributed.calendar.common.NetworkUtils
import org.distributed.calendar.core.discovery.DiscoveryListener

class AndroidDiscoveryListener : DiscoveryListener {

    private val localAddresses = NetworkUtils.getLocalIPv4Addresses()

    override suspend fun listen(): String? = withContext(Dispatchers.IO) {
        val socket = DatagramSocket(9999)
        socket.soTimeout = 10_000
        val buffer = ByteArray(1024)
        println("Listening for servers (10s timeout)...")
        var result: String? = null
        try {
            while (result == null) {
                val packet = DatagramPacket(buffer, buffer.size)
                socket.receive(packet)
                val senderIp = packet.address.hostAddress ?: ""
                val message = String(packet.data, 0, packet.length)
                println("Received discovery from $senderIp: $message")
                if (senderIp !in localAddresses && senderIp.isNotEmpty()) {
                    result = senderIp
                } else {
                    println("  Ignoring own broadcast")
                }
            }
        } catch (e: SocketTimeoutException) {
            println("Discovery listen timed out (no peer found yet)")
        } finally {
            socket.close()
        }
        return@withContext result
    }
}

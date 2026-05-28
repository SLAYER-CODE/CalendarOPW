package org.distributed.calendar.android.network

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketTimeoutException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.distributed.calendar.core.discovery.DiscoveryListener

class AndroidDiscoveryListener : DiscoveryListener {

    override suspend fun listen(): String? = withContext(Dispatchers.IO) {
        val socket = DatagramSocket(9999)
        socket.soTimeout = 10_000
        val buffer = ByteArray(1024)
        println("Listening for servers (10s timeout)...")
        try {
            val packet = DatagramPacket(buffer, buffer.size)
            socket.receive(packet)
            val message = String(packet.data, 0, packet.length)
            println("Received discovery: $message")
            return@withContext packet.address.hostAddress
        } catch (e: SocketTimeoutException) {
            println("Discovery listen timed out (no peer found yet)")
            return@withContext null
        } finally {
            socket.close()
        }
    }
}

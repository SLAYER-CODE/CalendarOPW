package org.distributed.calendar.android.network

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.distributed.calendar.core.discovery.DiscoveryBroadcaster

class AndroidDiscoveryBroadcaster : DiscoveryBroadcaster {

    private var active = false

    override suspend fun startBroadcast() = withContext(Dispatchers.IO) {
        active = true
        val socket = DatagramSocket()
        socket.broadcast = true
        while (active) {
            val message = "DISCOVER_SERVER:8080"
            val data = message.toByteArray()
            val packet = DatagramPacket(
                data,
                data.size,
                InetAddress.getByName("255.255.255.255"),
                9999
            )
            socket.send(packet)
            println("Broadcast sent")
            delay(5000)
        }
        socket.close()
    }

    override fun stop() {
        active = false
    }
}

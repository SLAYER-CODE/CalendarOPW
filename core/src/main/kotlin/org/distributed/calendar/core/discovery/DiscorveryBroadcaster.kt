package org.distributed.calendar.core.network.discovery

import kotlinx.coroutines.delay
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class DiscoveryBroadcaster {

    suspend fun startBroadcast() {

        val socket = DatagramSocket()

        socket.broadcast = true

        while (true) {

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
    }
}

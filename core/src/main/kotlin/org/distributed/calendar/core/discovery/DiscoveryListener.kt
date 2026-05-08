package org.distributed.calendar.core.network.discovery

import java.net.DatagramPacket
import java.net.DatagramSocket

class DiscoveryListener {

    fun startListening(): String {

        val socket = DatagramSocket(9999)

        val buffer = ByteArray(1024)

        println("Listening for servers...")

        while (true) {

            val packet = DatagramPacket(buffer, buffer.size)

            socket.receive(packet)

            val message = String(packet.data, 0, packet.length)

            println("Received discovery: $message")

            return packet.address.hostAddress
        }
    }
}

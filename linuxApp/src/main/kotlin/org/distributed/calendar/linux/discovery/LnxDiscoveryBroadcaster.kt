package org.distributed.calendar.linux.discovery

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.distributed.calendar.common.NetworkUtils
import org.distributed.calendar.core.discovery.DiscoveryBroadcaster

class LnxDiscoveryBroadcaster : DiscoveryBroadcaster {

    private var active = false
    private var lastBroadcastAddresses = NetworkUtils.getBroadcastAddresses()

    override suspend fun startBroadcast() = withContext(Dispatchers.IO) {
        active = true
        val socket = DatagramSocket()
        socket.broadcast = true
        while (active) {
            lastBroadcastAddresses = NetworkUtils.getBroadcastAddresses()
            val allTargets = lastBroadcastAddresses + InetAddress.getByName("255.255.255.255")
            val message = "DISCOVER_SERVER:8080"
            val data = message.toByteArray()
            for (target in allTargets) {
                try {
                    val packet = DatagramPacket(data, data.size, target, 9999)
                    socket.send(packet)
                    println("Broadcast sent to ${target.hostAddress}")
                } catch (e: Exception) {
                    println("Broadcast to ${target.hostAddress} failed: ${e.message}")
                }
            }
            delay(5000)
        }
        socket.close()
    }

    override fun stop() {
        active = false
    }
}

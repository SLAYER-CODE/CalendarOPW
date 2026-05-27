package org.distributed.calendar.linux

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.distributed.calendar.linux.discovery.LnxDiscoveryBroadcaster
import org.distributed.calendar.linux.discovery.LnxDiscoveryListener
import org.distributed.calendar.linux.network.LnxWebSocketClient
import org.distributed.calendar.linux.network.LnxWebSocketServer

fun main() = runBlocking {
    launch {
        LnxDiscoveryBroadcaster().startBroadcast()
    }
    launch {
        LnxWebSocketServer().start(8080)
    }
    launch {
        val serverIp = LnxDiscoveryListener().listen()
        if (serverIp != null) {
            println("Peer discovered: $serverIp")
            LnxWebSocketClient().connect(serverIp, 8080)
        }
    }
    while (true) {
        delay(1000)
    }
}

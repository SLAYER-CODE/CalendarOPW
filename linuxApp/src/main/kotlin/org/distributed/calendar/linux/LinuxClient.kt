package org.distributed.calendar.linux

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.distributed.calendar.linux.discovery.LnxDiscoveryListener
import org.distributed.calendar.linux.network.LnxWebSocketClient
import org.distributed.calendar.linux.network.LnxWebSocketServer

class LinuxClient {
    suspend fun start() {
        val server = LnxWebSocketServer()
        server.start(8080)

        val listener = LnxDiscoveryListener()
        val serverIp = listener.listen()
        if (serverIp != null) {
            println("Discovered server: $serverIp")
            val client = LnxWebSocketClient()
            client.connect(serverIp, 8080)
        }
    }
}

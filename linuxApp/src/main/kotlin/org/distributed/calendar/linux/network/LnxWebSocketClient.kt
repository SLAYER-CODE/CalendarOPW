package org.distributed.calendar.linux.network

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.distributed.calendar.common.DeviceManager
import org.distributed.calendar.common.model.*
import org.distributed.calendar.common.PacketSerializer
import org.distributed.calendar.core.network.WebSocketClient

class LnxWebSocketClient : WebSocketClient {

    private val client = HttpClient(CIO) { install(WebSockets) }

    override suspend fun connect(host: String, port: Int) = coroutineScope {
        while (true) {
            try {
                println("Connecting to server...")
                client.webSocket(host = host, port = port, path = "/sync") {
                    println("Connected to server")
                    launch {
                        while (true) {
                            val packet = SyncPacket(
                                packetId = UUID.randomUUID().toString(),
                                type = PacketType.HEARTBEAT,
                                deviceId = DeviceManager.deviceId,
                                timestamp = System.currentTimeMillis(),
                                payload = ""
                            )
                            val serialized = PacketSerializer.serialize(packet)
                            org.distributed.calendar.common.PendingPacketStore.add(packet)
                            send(Frame.Text(serialized))
                            println("Heartbeat sent")
                            delay(5000)
                        }
                    }
                    for (message in incoming) {
                        if (message is Frame.Text) {
                            val text = message.readText()
                            val packet = PacketSerializer.deserialize(text)
                            println("Received packet: $packet")
                            if (packet.type == PacketType.ACK) {
                                org.distributed.calendar.common.PendingPacketStore.acknowledge(packet.payload)
                                println("Packet acknowledged: ${packet.payload}")
                                continue
                            }
                            val ackPacket = SyncPacket(
                                packetId = UUID.randomUUID().toString(),
                                type = PacketType.ACK,
                                deviceId = DeviceManager.deviceId,
                                timestamp = System.currentTimeMillis(),
                                payload = packet.packetId
                            )
                            val serializedAck = PacketSerializer.serialize(ackPacket)
                            send(Frame.Text(serializedAck))
                            println("ACK sent for: ${packet.packetId}")
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                println("Connection lost: ${e.message}")
                println("Reconnecting in 5 seconds...")
                delay(5000)
            }
        }
    }
}

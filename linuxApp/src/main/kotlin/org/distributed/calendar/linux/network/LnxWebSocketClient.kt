package org.distributed.calendar.linux.network

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.distributed.calendar.common.DeviceManager
import org.distributed.calendar.common.PacketSerializer
import org.distributed.calendar.common.PendingPacketStore
import org.distributed.calendar.common.model.*
import org.distributed.calendar.core.network.WebSocketClient
import org.distributed.calendar.core.sync.SyncEngine

class LnxWebSocketClient(
    private val syncEngine: SyncEngine? = null
) : WebSocketClient {

    private val client = HttpClient(CIO) { install(WebSockets) }
    private val inFlight = mutableSetOf<String>()
    private val lastSentMillis = mutableMapOf<String, Long>()

    override suspend fun connect(host: String, port: Int) = coroutineScope {
        while (true) {
            try {
                println("Connecting to server...")
                client.webSocket(host = host, port = port, path = "/sync") {
                    println("Connected to server")

                    val scope = CoroutineScope(Dispatchers.IO + Job())

                    // Resend loop: retry unacknowledged packets every 2s (max once per 3s each)
                    val resendJob = scope.launch {
                        while (true) {
                            try {
                                val pending = PendingPacketStore.getPending()
                                val now = System.currentTimeMillis()
                                for (p in pending) {
                                    if (p.packetId in inFlight) continue
                                    val last = lastSentMillis[p.packetId] ?: 0L
                                    if (now - last < 3_000) continue
                                    val serialized = PacketSerializer.serialize(p)
                                    send(Frame.Text(serialized))
                                    lastSentMillis[p.packetId] = System.currentTimeMillis()
                                    inFlight.add(p.packetId)
                                }
                            } catch (_: Exception) { }
                            delay(2_000)
                        }
                    }

                    // Heartbeat loop every 5s
                    val heartbeatJob = scope.launch {
                        while (true) {
                            val packet = SyncPacket(
                                packetId = UUID.randomUUID().toString(),
                                type = PacketType.HEARTBEAT,
                                deviceId = DeviceManager.deviceId,
                                timestamp = System.currentTimeMillis(),
                                payload = ""
                            )
                            PendingPacketStore.add(packet)
                            try {
                                val serialized = PacketSerializer.serialize(packet)
                                send(Frame.Text(serialized))
                                lastSentMillis[packet.packetId] = System.currentTimeMillis()
                                inFlight.add(packet.packetId)
                            } catch (e: Exception) {
                                println("Heartbeat send failed: ${e.message}")
                            }
                            delay(5_000)
                        }
                    }

                    // Send SYNC_REQUEST to get remote state
                    try {
                        val syncReq = SyncPacket(
                            packetId = UUID.randomUUID().toString(),
                            type = PacketType.SYNC_REQUEST,
                            deviceId = DeviceManager.deviceId,
                            timestamp = System.currentTimeMillis(),
                            payload = ""
                        )
                        PendingPacketStore.add(syncReq)
                        val serialized = PacketSerializer.serialize(syncReq)
                        send(Frame.Text(serialized))
                        lastSentMillis[syncReq.packetId] = System.currentTimeMillis()
                        inFlight.add(syncReq.packetId)
                        println("SYNC_REQUEST sent")
                    } catch (e: Exception) {
                        println("SYNC_REQUEST send failed: ${e.message}")
                    }

                    // Read incoming frames
                    try {
                        for (message in incoming) {
                            if (message is Frame.Text) {
                                val text = message.readText()
                                try {
                                    val packet = PacketSerializer.deserialize(text)
                                    println("Received: ${packet.type} from ${packet.deviceId}")

                                    if (packet.type == PacketType.ACK) {
                                        inFlight.remove(packet.payload)
                                        lastSentMillis.remove(packet.payload)
                                        PendingPacketStore.acknowledge(packet.payload)
                                        println("ACK processed: ${packet.payload}")
                                        continue
                                    }

                                    syncEngine?.applyPacket(packet)

                                    // Send ACK for non-ACK packets
                                    val ackPacket = SyncPacket(
                                        packetId = UUID.randomUUID().toString(),
                                        type = PacketType.ACK,
                                        deviceId = DeviceManager.deviceId,
                                        timestamp = System.currentTimeMillis(),
                                        payload = packet.packetId
                                    )
                                    send(Frame.Text(PacketSerializer.serialize(ackPacket)))
                                } catch (e: Exception) {
                                    println("Failed to handle frame: ${e.message}")
                                }
                            }
                        }
                    } finally {
                        resendJob.cancel()
                        heartbeatJob.cancel()
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                println("Connection lost: ${e.message}")
                println("Reconnecting in 5 seconds...")
                delay(5_000)
            }
        }
    }
}

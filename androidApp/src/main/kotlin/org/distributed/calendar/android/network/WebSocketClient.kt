package org.distributed.calendar.android.network

import android.content.Context
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import org.distributed.calendar.common.DeviceManager
import org.distributed.calendar.common.model.*
import org.distributed.calendar.common.PacketSerializer
import org.distributed.calendar.core.network.WebSocketClient
import org.distributed.calendar.core.sync.SyncEngine

class AndroidWebSocketClient(
    private val context: Context,
    private val syncEngine: SyncEngine? = null
) : WebSocketClient {

    private val client = HttpClient(OkHttp) { install(WebSockets) }
    private val pendingStore = org.distributed.calendar.android.AndroidPendingPacketStore(context)
    private val lastSentMillis = ConcurrentHashMap<String, Long>()
    private val inFlight = ConcurrentHashMap.newKeySet<String>()
    private val sendMutex = Mutex()

    override suspend fun connect(host: String, port: Int) {
        while (true) {
            try {
                println("Connecting to server $host...")
                client.webSocket(host = host, port = port, path = "/sync") {
                    println("Connected to server $host")
                    pendingStore.initLoad()
                    val scope = CoroutineScope(Dispatchers.IO + Job())

                    val resendJob = scope.launch {
                        while (true) {
                            try {
                                val pending = pendingStore.getPending()
                                val now = System.currentTimeMillis()
                                for (p in pending) {
                                    if (inFlight.contains(p.packetId)) continue
                                    val last = lastSentMillis[p.packetId] ?: 0L
                                    if (now - last < 3000) continue
                                    try {
                                        sendPacket(p)
                                        lastSentMillis[p.packetId] = System.currentTimeMillis()
                                        inFlight.add(p.packetId)
                                    } catch (e: Exception) {
                                        println("Failed to resend ${p.packetId}: ${e.message}")
                                    }
                                }
                            } catch (_: Exception) { }
                            delay(2000)
                        }
                    }

                    val heartbeatJob = scope.launch {
                        while (true) {
                            val packet = SyncPacket(
                                packetId = UUID.randomUUID().toString(),
                                type = PacketType.HEARTBEAT,
                                deviceId = DeviceManager.deviceId,
                                timestamp = System.currentTimeMillis(),
                                payload = ""
                            )
                            pendingStore.add(packet)
                            try {
                                sendPacket(packet)
                                lastSentMillis[packet.packetId] = System.currentTimeMillis()
                                inFlight.add(packet.packetId)
                            } catch (e: Exception) {
                                println("Heartbeat send failed: ${e.message}")
                            }
                            delay(5000)
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
                        pendingStore.add(syncReq)
                        sendPacket(syncReq)
                        lastSentMillis[syncReq.packetId] = System.currentTimeMillis()
                        inFlight.add(syncReq.packetId)
                        println("SYNC_REQUEST sent")
                    } catch (e: Exception) {
                        println("SYNC_REQUEST send failed: ${e.message}")
                    }

                    try {
                        for (incomingFrame in incoming) {
                            if (incomingFrame is Frame.Text) {
                                val text = incomingFrame.readText()
                                try {
                                    val packet = PacketSerializer.deserialize(text)
                                    println("Received packet: ${packet.type} from ${packet.deviceId}")
                                    if (packet.type == PacketType.ACK) {
                                        val ackedId = packet.payload
                                        pendingStore.acknowledge(ackedId)
                                        inFlight.remove(ackedId)
                                        lastSentMillis.remove(ackedId)
                                        println("ACK processed for: $ackedId")
                                        continue
                                    }

                                    if (packet.type == PacketType.SYNC_RESPONSE) {
                                        syncEngine?.updateDeviceIp(packet.deviceId, host)
                                    }

                                    syncEngine?.applyPacket(packet)

                                    val ackPacket = SyncPacket(
                                        packetId = UUID.randomUUID().toString(),
                                        type = PacketType.ACK,
                                        deviceId = DeviceManager.deviceId,
                                        timestamp = System.currentTimeMillis(),
                                        payload = packet.packetId
                                    )
                                    send(PacketSerializer.serialize(ackPacket))
                                } catch (e: Exception) {
                                    println("Failed to handle incoming frame: ${e.message}")
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
                delay(5000)
            }
        }
    }

    private suspend fun DefaultClientWebSocketSession.sendPacket(packet: SyncPacket) {
        val serialized = PacketSerializer.serialize(packet)
        send(Frame.Text(serialized))
    }
}

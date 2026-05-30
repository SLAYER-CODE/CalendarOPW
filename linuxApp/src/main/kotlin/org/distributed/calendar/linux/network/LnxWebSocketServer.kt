package org.distributed.calendar.linux.network

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.net.InetSocketAddress
import java.util.UUID
import org.distributed.calendar.common.DeviceManager
import org.distributed.calendar.common.model.*
import org.distributed.calendar.common.PacketSerializer
import org.distributed.calendar.core.network.WebSocketServer
import org.distributed.calendar.core.network.PeerSession
import org.distributed.calendar.core.network.SessionRegistry
import org.distributed.calendar.core.device.DeviceRegistry
import org.distributed.calendar.core.sync.SyncEngine

private class KtorPeerSession(
    override val deviceId: String,
    private val session: DefaultWebSocketServerSession
) : PeerSession {
    override suspend fun send(text: String) {
        session.send(Frame.Text(text))
    }
}

class LnxWebSocketServer(
    private val syncEngine: SyncEngine = SyncEngine()
) : WebSocketServer {

    private var server: ApplicationEngine? = null

    override suspend fun start(port: Int) {
        server = embeddedServer(Netty, port = port) {
            install(WebSockets)
            routing {
                webSocket("/sync") {
                    println("Client connected")
                    var connectedDeviceId: String? = null
                    try {
                        for (frame in incoming) {
                            if (frame is Frame.Text) {
                                val text = frame.readText()
                                val packet = PacketSerializer.deserialize(text)
                                connectedDeviceId = packet.deviceId
                                val peerSession = KtorPeerSession(packet.deviceId, this)
                                SessionRegistry.register(packet.deviceId, peerSession)
                                DeviceRegistry.heartbeat(packet.deviceId)
                                syncEngine.applyPacket(packet)
                                val theCall = call
                                val remoteIp = if (theCall is NettyApplicationCall) {
                                    val addr = theCall.context.channel().remoteAddress()
                                    if (addr is InetSocketAddress) addr.hostString else addr.toString()
                                } else ""
                                syncEngine.updateDeviceIp(packet.deviceId, remoteIp)
                                if (packet.type != PacketType.ACK) {
                                    val ackPacket = SyncPacket(
                                        packetId = UUID.randomUUID().toString(),
                                        type = PacketType.ACK,
                                        deviceId = DeviceManager.deviceId,
                                        timestamp = System.currentTimeMillis(),
                                        payload = packet.packetId
                                    )
                                    val serializedAck = PacketSerializer.serialize(ackPacket)
                                    send(Frame.Text(serializedAck))
                                }
                                println(DeviceRegistry.getOnlineDevices())
                                println("Received packet: $packet")
                            }
                        }
                    } finally {
                        connectedDeviceId?.let { id ->
                            SessionRegistry.remove(id)
                            println("Session cleaned up: $id")
                        }
                    }
                }
            }
        }
        println("WebSocket server starting on port $port")
        server!!.start(wait = false)
    }

    override fun stop() {
        server?.stop(1000, 2000)
        println("WebSocket server stopped")
    }
}

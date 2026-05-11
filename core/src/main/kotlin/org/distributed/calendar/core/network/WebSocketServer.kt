package org.distributed.calendar.core.network

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.util.UUID
import org.distributed.calendar.core.device.DeviceManager
import org.distributed.calendar.core.device.DeviceRegistry
import org.distributed.calendar.core.model.*
import org.distributed.calendar.core.sync.SyncEngine

class WebSocketServer {

  private val syncEngine = SyncEngine()

  fun startWebSocketServer() {

    val server =
            embeddedServer(Netty, port = 8080) {
              install(WebSockets)

              routing {
                webSocket("/sync") {
                  println("Client connected")

                  for (frame in incoming) {

                    if (frame is Frame.Text) {

                      val text = frame.readText()
                      val packet = PacketSerializer.deserialize(text)
                      SessionRegistry.register(packet.deviceId, this)
                      DeviceRegistry.heartbeat(packet.deviceId)

                      syncEngine.applyPacket(packet)
                      if (packet.type != PacketType.ACK) {

                        val ackPacket =
                                SyncPacket(
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
                }
              }
            }
    println("WebSocket server starting on port 8080")
    server.start(wait = false)
  }
}

package org.distributed.calendar.core.network

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import org.distributed.calendar.core.sync.SyncEngine

class WebSocketServer {

  private val syncEngine = SyncEngine()

  fun startWebSocketServer() {

    embeddedServer(Netty, port = 8080) {
              install(WebSockets)

              routing {
                webSocket("/sync") {
                  println("Client connected")

                  for (frame in incoming) {

                    if (frame is Frame.Text) {

                      val text = frame.readText()

                      val packet = PacketSerializer.deserialize(text)

                      syncEngine.applyPacket(packet)

                      println("Received packet: $packet")
                    }
                  }
                }
              }
            }
            .start(wait = true)
  }
}

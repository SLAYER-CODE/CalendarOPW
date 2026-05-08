package org.distributed.calendar.core.network

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import org.distributed.calendar.core.model.*

class WebSocketClient {

  private val client = HttpClient(CIO) { install(WebSockets) }

  suspend fun connect() {

    client.webSocket(host = "127.0.0.1", port = 8080, path = "/sync") {
      println("Connected to server")

      val packet =
              SyncPacket(
                      type = PacketType.HEARTBEAT,
                      deviceId = "android-01",
                      timestamp = System.currentTimeMillis(),
                      payload = ""
              )

      val serialized = PacketSerializer.serialize(packet)

      send(Frame.Text(serialized))

      for (message in incoming) {

        if (message is Frame.Text) {

          println("Received: ${message.readText()}")
        }
      }
    }
  }
}

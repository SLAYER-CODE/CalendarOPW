package org.distributed.calendar.core.network

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.distributed.calendar.core.device.DeviceManager
import org.distributed.calendar.core.model.*

class WebSocketClient(private val host: String) {

  private val client = HttpClient(CIO) { install(WebSockets) }

  suspend fun connect() = coroutineScope {
    while (true) {

      try {

        println("Connecting to server...")

        client.webSocket(host = host, port = 8080, path = "/sync") {
          println("Connected to server")

          launch {
            while (true) {

              val packet =
                      SyncPacket(
                              type = PacketType.HEARTBEAT,
                              deviceId = DeviceManager.deviceId,
                              timestamp = System.currentTimeMillis(),
                              payload = ""
                      )

              val serialized = PacketSerializer.serialize(packet)

              send(Frame.Text(serialized))

              println("Heartbeat sent")

              delay(5000)
            }
          }

          for (message in incoming) {

            if (message is Frame.Text) {

              println("Received: ${message.readText()}")
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

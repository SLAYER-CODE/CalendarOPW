import kotlinx.coroutines.runBlocking
import org.distributed.calendar.core.network.WebSocketClient
import org.distributed.calendar.core.network.discovery.DiscoveryListener

fun main() = runBlocking {
  val serverIp = DiscoveryListener().startListening()

  println("Server discovered at: $serverIp")

  val client = WebSocketClient(serverIp)

  client.connect()
}

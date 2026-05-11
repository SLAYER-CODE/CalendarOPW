import kotlinx.coroutines.runBlocking
import org.distributed.calendar.core.network.WebSocketClient
import org.distributed.calendar.core.network.discovery.DiscoveryListener

fun main() = runBlocking {
  println("Search Discover ...")
  val serverIp = DiscoveryListener().startListening()
  println("Server discovered Connected: $serverIp")
  val client = WebSocketClient(serverIp)

  client.connect()
}

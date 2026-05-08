import kotlinx.coroutines.runBlocking
import org.distributed.calendar.core.network.WebSocketClient

fun main() = runBlocking {

    val client = WebSocketClient()

    client.connect()
}

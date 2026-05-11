import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.distributed.calendar.core.network.WebSocketServer
import org.distributed.calendar.core.network.discovery.DiscoveryBroadcaster
import kotlinx.coroutines.delay
fun main() = runBlocking {

    launch {
        DiscoveryBroadcaster()
            .startBroadcast()
    }

    launch {
        WebSocketServer()
            .startWebSocketServer()
    }

    while (true) {
        delay(1000)
    }
}

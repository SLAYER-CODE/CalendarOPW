import org.distributed.calendar.core.network.WebSocketServer
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.launch

import org.distributed.calendar.core.network.discovery.DiscoveryBroadcaster
import org.distributed.calendar.core.network.WebSocketServer
fun main() = runBlocking {

    launch {

        DiscoveryBroadcaster().startBroadcast()
    }

    WebSocketServer().startWebSocketServer()
}

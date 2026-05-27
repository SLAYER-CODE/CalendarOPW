import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.distributed.calendar.common.DeviceManager
import org.distributed.calendar.linux.discovery.LnxDiscoveryBroadcaster
import org.distributed.calendar.linux.discovery.LnxDiscoveryListener
import org.distributed.calendar.linux.network.LnxWebSocketClient
import org.distributed.calendar.linux.network.LnxWebSocketServer
import org.distributed.calendar.linux.ui.LinuxDesktopApp
import org.distributed.calendar.ui.theme.CalendarTheme
import java.awt.Toolkit

fun main() {
    val screen = Toolkit.getDefaultToolkit().screenSize
    println("Screen: ${screen.width}x${screen.height}")
    println("Starting linuxApp P2P node (device: ${DeviceManager.deviceId})")

    val scope = CoroutineScope(Dispatchers.IO)
    scope.launch { LnxDiscoveryBroadcaster().startBroadcast() }
    scope.launch { LnxWebSocketServer().start(8080) }
    scope.launch {
        val serverIp = LnxDiscoveryListener().listen()
        if (serverIp != null) {
            println("Peer discovered: $serverIp")
            LnxWebSocketClient().connect(serverIp, 8080)
        }
    }

    application {
        val windowState = rememberWindowState(
            width = screen.width.dp,
            height = screen.height.dp
        )
        Window(
            onCloseRequest = ::exitApplication,
            title = "Distributed Calendar — Linux",
            state = windowState
        ) {
            CalendarTheme {
                LinuxDesktopApp()
            }
        }
    }
}

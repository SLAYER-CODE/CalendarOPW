import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.distributed.calendar.common.DeviceManager
import org.distributed.calendar.common.PacketDeduplicator
import org.distributed.calendar.common.PendingPacketStore
import org.distributed.calendar.common.model.Device
import org.distributed.calendar.common.model.Event
import org.distributed.calendar.core.device.DeviceRegistry
import org.distributed.calendar.core.sync.PacketResender
import org.distributed.calendar.core.sync.SyncEngine
import org.distributed.calendar.linux.discovery.LnxDiscoveryBroadcaster
import org.distributed.calendar.linux.discovery.LnxDiscoveryListener
import org.distributed.calendar.linux.network.LnxWebSocketClient
import org.distributed.calendar.linux.network.LnxWebSocketServer
import org.distributed.calendar.linux.ui.LinuxDesktopApp
import org.distributed.calendar.ui.model.EventUiModel
import org.distributed.calendar.ui.model.PeerUiModel
import org.distributed.calendar.ui.theme.CalendarTheme
import java.awt.Toolkit
import java.io.File

private fun Event.toUiModel() = EventUiModel(
    id = id,
    title = title,
    description = description ?: "",
    timestamp = timestamp,
    duration = duration,
    priority = priority,
    sourceDevice = sourceDeviceId
)

private fun Device.toPeerUiModel() = PeerUiModel(
    deviceId = deviceId,
    name = name,
    isOnline = DeviceRegistry.isOnline(deviceId)
)

fun main() {
    val screen = Toolkit.getDefaultToolkit().screenSize
    println("Screen: ${screen.width}x${screen.height}")
    println("Starting linuxApp P2P node (device: ${DeviceManager.deviceId})")

    PendingPacketStore.setPersistDir(File("pending_packets"))

    val syncEngine = SyncEngine()
    val scope = CoroutineScope(Dispatchers.IO)

    scope.launch { LnxDiscoveryBroadcaster().startBroadcast() }
    scope.launch { LnxWebSocketServer(syncEngine).start(8080) }
    scope.launch {
        while (true) {
            val serverIp = LnxDiscoveryListener().listen()
            if (serverIp != null) {
                println("Peer discovered: $serverIp")
                LnxWebSocketClient().connect(serverIp, 8080)
                break
            }
            delay(5_000)
        }
    }
    scope.launch { PacketResender().start() }

    // Periodic cleanup of stale state
    scope.launch {
        while (true) {
            delay(60_000)
            PacketDeduplicator.cleanup()
            DeviceRegistry.cleanup()
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
                var events by remember { mutableStateOf(listOf<EventUiModel>()) }
                var peers by remember { mutableStateOf(listOf<PeerUiModel>()) }

                LaunchedEffect(syncEngine) {
                    syncEngine.addChangeListener {
                        events = syncEngine.getEvents().map { it.toUiModel() }
                        peers = syncEngine.getDevices().map { it.toPeerUiModel() }
                    }
                }

                LinuxDesktopApp(
                    events = events,
                    peers = peers,
                    onCreateEvent = { title, description, duration ->
                        syncEngine.createEvent(title, description, duration)
                    }
                )
            }
        }
    }
}

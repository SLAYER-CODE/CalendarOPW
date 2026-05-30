import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.distributed.calendar.common.DeviceManager
import org.distributed.calendar.common.NetworkUtils
import org.distributed.calendar.common.PacketDeduplicator
import org.distributed.calendar.common.PendingPacketStore
import org.distributed.calendar.common.TcpPeerScanner
import org.distributed.calendar.common.model.Device
import org.distributed.calendar.common.model.Event
import org.distributed.calendar.core.device.DeviceRegistry
import org.distributed.calendar.core.sync.PacketResender
import org.distributed.calendar.core.sync.SyncEngine
import org.distributed.calendar.linux.LinuxNotifier
import org.distributed.calendar.linux.discovery.LnxDiscoveryBroadcaster
import org.distributed.calendar.linux.discovery.LnxDiscoveryListener
import org.distributed.calendar.linux.network.LnxWebSocketClient
import org.distributed.calendar.linux.network.LnxWebSocketServer
import org.distributed.calendar.linux.ui.LinuxDesktopApp
import org.distributed.calendar.ui.model.EventUiModel
import org.distributed.calendar.ui.model.PeerUiModel
import org.distributed.calendar.ui.theme.CalendarTheme
import java.awt.Color
import java.awt.image.BufferedImage
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

private fun createTrayIcon(): BitmapPainter {
    val img = BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB)
    val g = img.createGraphics()
    g.color = Color(123, 31, 162)
    g.fillRect(0, 0, 16, 16)
    g.dispose()
    return BitmapPainter(img.toComposeImageBitmap())
}

fun main() {
    println("Starting linuxApp P2P node (device: ${DeviceManager.deviceId})")
    NetworkUtils.printLocalAddresses()

    PendingPacketStore.setPersistDir(File("pending_packets"))

    val syncEngine = SyncEngine()
    val scope = CoroutineScope(Dispatchers.IO)
    val knownEventIds = mutableSetOf<String>()

    LinuxNotifier.synchronizing()
    LinuxNotifier.searchStarted()

    scope.launch { LnxDiscoveryBroadcaster().startBroadcast() }
    scope.launch { LnxWebSocketServer(syncEngine).start(8080) }
    scope.launch {
        while (true) {
            var serverIp = LnxDiscoveryListener().listen()
            if (serverIp == null) {
                println("UDP discovery timed out — trying TCP scan...")
                serverIp = TcpPeerScanner.scan(8080)
            }
            if (serverIp != null) {
                val localIPs = NetworkUtils.getLocalIPv4Addresses()
                if (serverIp in localIPs) {
                    println("Ignoring self-IP: $serverIp")
                    delay(5_000)
                    continue
                }
                println("Peer discovered: $serverIp")
                LinuxNotifier.peerFound(serverIp)
                LnxWebSocketClient(syncEngine).connect(serverIp, 8080)
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

    // Seed known event IDs so we only notify about NEW events
    syncEngine.getEvents().forEach { knownEventIds.add(it.id) }

    // Register notification listener for new remote events
    syncEngine.addChangeListener {
        val current = syncEngine.getEvents()
        val localId = DeviceManager.deviceId
        val newRemote = current.filter { e ->
            e.id !in knownEventIds && e.sourceDeviceId != localId
        }
        if (newRemote.isNotEmpty()) {
            newRemote.forEach { knownEventIds.add(it.id) }
            newRemote.forEach { event ->
                val device = syncEngine.getDevices()
                    .find { it.deviceId == event.sourceDeviceId }
                val name = device?.name ?: event.sourceDeviceId.take(8)
                val summary = event.title
                LinuxNotifier.eventReceived(summary, name)
            }
        }
    }

    application {
        val windowState = rememberWindowState(
            width = 500.dp,
            height = 500.dp
        )
        var isVisible by remember { mutableStateOf(true) }
        var events by remember { mutableStateOf(listOf<EventUiModel>()) }
        var peers by remember { mutableStateOf(listOf<PeerUiModel>()) }

        LaunchedEffect(syncEngine) {
            syncEngine.addChangeListener {
                events = syncEngine.getEvents().map { it.toUiModel() }
                peers = syncEngine.getDevices().map { it.toPeerUiModel() }
            }
        }

        LaunchedEffect(Unit) {
            while (true) {
                delay(5_000)
                peers = syncEngine.getDevices().map { it.toPeerUiModel() }
                events = syncEngine.getEvents().map { it.toUiModel() }
            }
        }

        Tray(
            icon = createTrayIcon(),
            tooltip = "Distributed Calendar",
            onAction = { isVisible = !isVisible },
            menu = {
                Item("Show") { isVisible = true }
                Item("Quit") { exitApplication() }
            }
        )

        if (isVisible) {
            Window(
                onCloseRequest = { isVisible = false },
                title = "Distributed Calendar — Linux",
                state = windowState
            ) {
                CalendarTheme {
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
}

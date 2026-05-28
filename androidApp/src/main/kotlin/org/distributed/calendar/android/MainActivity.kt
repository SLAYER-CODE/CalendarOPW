package org.distributed.calendar.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.distributed.calendar.android.service.SyncForegroundService
import org.distributed.calendar.common.model.Device
import org.distributed.calendar.common.model.Event
import org.distributed.calendar.core.device.DeviceRegistry
import org.distributed.calendar.core.sync.SyncEngine
import org.distributed.calendar.ui.MainScreen
import org.distributed.calendar.ui.components.AppSidebar
import org.distributed.calendar.ui.components.SidebarTab
import org.distributed.calendar.ui.model.EventUiModel
import org.distributed.calendar.ui.model.PeerUiModel
import org.distributed.calendar.ui.model.Screen
import org.distributed.calendar.ui.theme.CalendarTheme

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

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startService(
            Intent(this, SyncForegroundService::class.java)
        )

        setContent {
            CalendarTheme {
                var currentScreen by remember { mutableStateOf(Screen.EVENT_LIST) }
                var events by remember { mutableStateOf(listOf<EventUiModel>()) }
                var peers by remember { mutableStateOf(listOf<PeerUiModel>()) }

                LaunchedEffect(Unit) {
                    var engine: SyncEngine? = null
                    while (engine == null) {
                        engine = SyncForegroundService.syncEngine
                        if (engine == null) {
                            kotlinx.coroutines.delay(500)
                        }
                    }
                    val e = engine
                    e.addChangeListener {
                        events = e.getEvents().map { it.toUiModel() }
                        peers = e.getDevices().map { it.toPeerUiModel() }
                    }
                }

                fun createEvent(title: String, description: String, duration: Long) {
                    val engine = SyncForegroundService.syncEngine ?: return
                    engine.createEvent(title, description, duration)
                }

                Row(Modifier.fillMaxSize()) {
                    AppSidebar(
                        tabs = listOf(
                            SidebarTab("events", "Events", "📅"),
                            SidebarTab("new_event", "New Event", "➕"),
                            SidebarTab("peers", "Peers", "👥")
                        ),
                        selectedTabId = when (currentScreen) {
                            Screen.EVENT_LIST -> "events"
                            Screen.CREATE_EVENT -> "new_event"
                            Screen.PEER_LIST -> "peers"
                        },
                        onTabSelected = { id ->
                            currentScreen = when (id) {
                                "events" -> Screen.EVENT_LIST
                                "new_event" -> Screen.CREATE_EVENT
                                "peers" -> Screen.PEER_LIST
                                else -> Screen.EVENT_LIST
                            }
                        },
                        isExpanded = false,
                        onToggle = { currentScreen = Screen.EVENT_LIST }
                    )
                    MainScreen(
                        events = events,
                        peers = peers,
                        onCreateEvent = { title, description, duration ->
                            createEvent(title, description, duration)
                        },
                        currentScreen = currentScreen,
                        onScreenChange = { currentScreen = it },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

package org.distributed.calendar.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.distributed.calendar.ui.model.EventUiModel
import org.distributed.calendar.ui.model.PeerUiModel
import org.distributed.calendar.ui.model.Screen
import org.distributed.calendar.ui.screens.CreateEventScreen
import org.distributed.calendar.ui.screens.EventListScreen
import org.distributed.calendar.ui.screens.PeerListScreen

@Composable
fun MainScreen(
    currentScreen: Screen? = null,
    onScreenChange: (Screen) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val internalScreen = remember { mutableStateOf(Screen.EVENT_LIST) }
    val screen = currentScreen ?: internalScreen.value
    val navigate: (Screen) -> Unit = if (currentScreen != null) onScreenChange else { s -> internalScreen.value = s }
    var events by remember { mutableStateOf(sampleEvents()) }
    var peers by remember { mutableStateOf(samplePeers()) }

    when (screen) {
        Screen.EVENT_LIST -> {
            EventListScreen(
                events = events,
                onCreateEvent = { navigate(Screen.CREATE_EVENT) },
                onEventClick = { _ ->
                    // TODO: navigate to event detail/edit
                },
                onPeersClick = { navigate(Screen.PEER_LIST) },
                modifier = modifier
            )
        }

        Screen.CREATE_EVENT -> {
            CreateEventScreen(
                onBack = { navigate(Screen.EVENT_LIST) },
                onSave = { title, description, duration ->
                    // TODO: create SyncPacket and broadcast to peers
                    // use current epoch millis from platform-aware source
                    val now = currentTimeMillis()
                    val newEvent = EventUiModel(
                        id = "evt_$now",
                        title = title,
                        description = description,
                        timestamp = now,
                        duration = duration,
                        priority = 0,
                        sourceDevice = "local"
                    )
                    events = listOf(newEvent) + events
                    navigate(Screen.EVENT_LIST)
                },
                modifier = modifier
            )
        }

        Screen.PEER_LIST -> {
            PeerListScreen(
                peers = peers,
                onBack = { navigate(Screen.EVENT_LIST) },
                modifier = modifier
            )
        }
    }
}

private fun sampleEvents(): List<EventUiModel> {
    val now = currentTimeMillis()
    return listOf(
        EventUiModel(
            id = "1",
            title = "Team Standup",
            description = "Daily sync meeting",
            timestamp = now + 3600000,
            duration = 30,
            priority = 1,
            sourceDevice = "device-a"
        ),
        EventUiModel(
            id = "2",
            title = "Lunch",
            description = "Lunch break",
            timestamp = now + 7200000,
            duration = 60,
            priority = 0,
            sourceDevice = "device-b"
        )
    )
}

private fun samplePeers(): List<PeerUiModel> = listOf(
    PeerUiModel(
        deviceId = "device-a",
        name = "Linux Desktop",
        isOnline = true
    ),
    PeerUiModel(
        deviceId = "device-b",
        name = "Android Phone",
        isOnline = false
    )
)

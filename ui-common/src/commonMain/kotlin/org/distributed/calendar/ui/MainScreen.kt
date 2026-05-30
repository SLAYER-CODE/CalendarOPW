package org.distributed.calendar.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
    events: List<EventUiModel>,
    peers: List<PeerUiModel>,
    onCreateEvent: (title: String, description: String, duration: Long) -> Unit,
    currentScreen: Screen? = null,
    onScreenChange: (Screen) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val internalScreen = remember { mutableStateOf(Screen.EVENT_LIST) }
    val screen = currentScreen ?: internalScreen.value
    val navigate: (Screen) -> Unit = if (currentScreen != null) onScreenChange else { s -> internalScreen.value = s }

    AnimatedContent(
        targetState = screen,
        transitionSpec = {
            slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
        },
        label = "screen_transition"
    ) { targetScreen ->
        when (targetScreen) {
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
                        onCreateEvent(title, description, duration)
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
}

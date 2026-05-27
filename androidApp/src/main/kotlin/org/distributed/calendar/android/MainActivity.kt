package org.distributed.calendar.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.distributed.calendar.ui.MainScreen
import org.distributed.calendar.ui.components.AppSidebar
import org.distributed.calendar.ui.components.SidebarTab
import org.distributed.calendar.ui.model.Screen
import org.distributed.calendar.ui.theme.CalendarTheme
import org.distributed.calendar.android.service.SyncForegroundService

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startService(
            Intent(this, SyncForegroundService::class.java)
        )

        setContent {
            CalendarTheme {
                var currentScreen by remember { mutableStateOf(Screen.EVENT_LIST) }

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
                        onToggle = {}
                    )
                    MainScreen(
                        currentScreen = currentScreen,
                        onScreenChange = { currentScreen = it },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

package org.distributed.calendar.linux.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.distributed.calendar.ui.components.AppSidebar
import org.distributed.calendar.ui.components.SidebarTab
import org.distributed.calendar.ui.model.EventUiModel
import org.distributed.calendar.ui.model.PeerUiModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person

private val tabs = listOf(
    SidebarTab("events", "Events", Icons.Default.DateRange),
    SidebarTab("new_event", "New Event", Icons.Default.Add),
    SidebarTab("peers", "Peers", Icons.Default.Person)
)

@Composable
fun LinuxDesktopApp(
    events: List<EventUiModel>,
    peers: List<PeerUiModel>,
    onCreateEvent: (title: String, description: String, duration: Long) -> Unit
) {
    var selectedTabId by remember { mutableStateOf("events") }
    var isExpanded by remember { mutableStateOf(true) }

    Row(modifier = Modifier.fillMaxSize()) {
        AppSidebar(
            tabs = tabs,
            selectedTabId = selectedTabId,
            onTabSelected = { selectedTabId = it },
            isExpanded = isExpanded,
            onToggle = { isExpanded = !isExpanded }
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            AnimatedContent(
                targetState = selectedTabId,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "panel_transition"
            ) { tabId ->
                when (tabId) {
                    "events" -> EventsPanel(
                        events = events,
                        onCreateClick = { selectedTabId = "new_event" }
                    )
                    "new_event" -> NewEventPanel(
                        onSave = { title, description, duration ->
                            onCreateEvent(title, description, duration)
                            selectedTabId = "events"
                        },
                        onCancel = { selectedTabId = "events" }
                    )
                    "peers" -> PeersPanel(peers = peers)
                }
            }
        }
    }
}

// ─── Event List ─────────────────────────────────────────────────────────────

@Composable
private fun EventsPanel(
    events: List<EventUiModel>,
    onCreateClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 24.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Events",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            Button(onClick = onCreateClick) {
                Text("  + New Event  ")
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        if (events.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No events yet.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(events, key = { it.id }) { event ->
                    EventRow(event)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun EventRow(event: EventUiModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(event.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
            if (event.description.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(event.description, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ─── New Event Form ─────────────────────────────────────────────────────────

@Composable
private fun NewEventPanel(
    onSave: (title: String, description: String, duration: Long) -> Unit,
    onCancel: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var durationText by remember { mutableStateOf("60") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(32.dp)
    ) {
        Text("New Event", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(28.dp))

        Text("Title", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            placeholder = { Text("Event title") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(20.dp))

        Text("Description", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            placeholder = { Text("Event description") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(20.dp))

        Text("Duration (minutes)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = durationText,
            onValueChange = { durationText = it.filter { c -> c.isDigit() } },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(28.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = {
                    val duration = durationText.toLongOrNull() ?: 60L
                    onSave(title, description, duration)
                },
                enabled = title.isNotBlank()
            ) {
                Text("  Save  ")
            }
            Button(onClick = onCancel) {
                Text("  Cancel  ")
            }
        }
    }
}

// ─── Peers Panel ────────────────────────────────────────────────────────────

@Composable
private fun PeersPanel(peers: List<PeerUiModel>) {
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 24.dp, vertical = 14.dp)
        ) {
            Text("Peers", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        if (peers.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Waiting for UDP discovery...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 24.dp, vertical = 10.dp)
            ) {
                Text("Device", Modifier.weight(2f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Name", Modifier.weight(2f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Status", Modifier.weight(1f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(peers, key = { it.deviceId }) { peer ->
                    PeerTableRow(peer)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun PeerTableRow(peer: PeerUiModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(peer.deviceId, Modifier.weight(2f), fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground)
        Text(peer.name, Modifier.weight(2f), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            val dotColor = if (peer.isOnline) Color(0xFF43A047) else Color(0xFFE53935)
            Box(Modifier.size(8.dp).background(dotColor, RoundedCornerShape(4.dp)))
            Spacer(Modifier.width(8.dp))
            Text(
                if (peer.isOnline) "Online" else "Offline",
                fontSize = 13.sp,
                color = dotColor
            )
        }
    }
}

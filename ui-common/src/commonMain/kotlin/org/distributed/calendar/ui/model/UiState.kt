package org.distributed.calendar.ui.model

enum class Screen {
    EVENT_LIST,
    CREATE_EVENT,
    PEER_LIST
}

data class EventUiModel(
    val id: String,
    val title: String,
    val description: String,
    val timestamp: Long,
    val duration: Long,
    val priority: Int,
    val sourceDevice: String
)

data class PeerUiModel(
    val deviceId: String,
    val name: String,
    val isOnline: Boolean
)

package org.distributed.calendar.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Event(
    val id: String,
    val title: String,
    val description: String? = null,
    val timestamp: Long,
    val duration: Long,
    val priority: Int,
    val repeatRule: String? = null,
    val updatedAt: Long,
    val sourceDeviceId: String,
    val status: EventStatus = EventStatus.PENDING
)

@Serializable
enum class EventStatus {
    PENDING,
    SCHEDULED,
    RUNNING,
    COMPLETED,
    FAILED
}

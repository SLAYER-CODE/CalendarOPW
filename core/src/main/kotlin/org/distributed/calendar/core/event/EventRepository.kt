package org.distributed.calendar.core.event

import org.distributed.calendar.common.model.Event

interface EventRepository {
    suspend fun insert(event: Event)
    suspend fun delete(eventId: String)
    suspend fun getAll(): List<Event>
}

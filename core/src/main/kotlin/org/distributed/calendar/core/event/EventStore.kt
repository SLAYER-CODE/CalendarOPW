package org.distributed.calendar.core.event

import org.distributed.calendar.core.model.Event
import java.util.concurrent.ConcurrentHashMap

object EventStore {

    private val events = ConcurrentHashMap<String, Event>()

    fun addEvent(event: Event) {

        events[event.id] = event
    }

    fun updateEvent(event: Event) {

        events[event.id] = event
    }

    fun deleteEvent(eventId: String) {

        events.remove(eventId)
    }

    fun getAllEvents(): List<Event> {

        return events.values.toList()
    }
}

package org.distributed.calendar.core.event

import org.distributed.calendar.core.db.DatabaseFactory
import org.distributed.calendar.core.model.Event

object EventRepository {

  private val queries = DatabaseFactory.database.eventsQueries

  fun insert(event: Event) {

    queries.insertEvent(
            id = event.id,
            title = event.title,
description = event.description ?: "",
            timestamp = event.timestamp,
            duration = event.duration,
            priority = event.priority.toLong(),
            sourceDeviceId = event.sourceDeviceId ,
            updatedAt = event.updatedAt
    )
  }

  fun delete(eventId: String) {

    queries.deleteEvent(eventId)
  }

  fun getAll(): List<Event> {

    return queries.selectAll().executeAsList().map {
      Event(
              id = it.id,
              title = it.title,
              description = it.description?: "",
              timestamp = it.timestamp,
              duration = it.duration,
              priority = it.priority.toInt(),
              sourceDeviceId = it.sourceDeviceId ,
              updatedAt = it.updatedAt
      )
    }
  }
}

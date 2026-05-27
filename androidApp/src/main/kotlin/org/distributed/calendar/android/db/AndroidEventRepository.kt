package org.distributed.calendar.android.db

import app.cash.sqldelight.db.SqlDriver
import org.distributed.calendar.common.model.Event
import org.distributed.calendar.core.event.EventRepository
import org.distributed.calendar.db.CalendarDatabase

class AndroidEventRepository(private val driver: SqlDriver) : EventRepository {

    private val database = CalendarDatabase(driver)
    private val queries = database.eventsQueries

    override suspend fun insert(event: Event) {
        queries.insertEvent(
            id = event.id,
            title = event.title,
            description = event.description ?: "",
            timestamp = event.timestamp,
            duration = event.duration,
            priority = event.priority.toLong(),
            sourceDeviceId = event.sourceDeviceId,
            updatedAt = event.updatedAt
        )
    }

    override suspend fun delete(eventId: String) {
        queries.deleteEvent(eventId)
    }

    override suspend fun getAll(): List<Event> {
        return queries.selectAll().executeAsList().map {
            Event(
                id = it.id,
                title = it.title,
                description = it.description ?: "",
                timestamp = it.timestamp,
                duration = it.duration,
                priority = it.priority.toInt(),
                sourceDeviceId = it.sourceDeviceId,
                updatedAt = it.updatedAt
            )
        }
    }
}

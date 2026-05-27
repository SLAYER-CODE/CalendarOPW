package org.distributed.calendar.android.db

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import org.distributed.calendar.core.db.DatabaseFactory
import org.distributed.calendar.db.CalendarDatabase

class AndroidDatabaseFactory(private val context: Context) : DatabaseFactory {
    override fun createDriver(): SqlDriver {
        val driver = AndroidSqliteDriver(CalendarDatabase.Schema, context, "calendar.db")
        CalendarDatabase.Schema.create(driver)
        return driver
    }
}

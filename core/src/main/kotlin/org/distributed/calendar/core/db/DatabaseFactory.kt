package org.distributed.calendar.core.db

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import org.distributed.calendar.db.CalendarDatabase

object DatabaseFactory {

    val database: CalendarDatabase by lazy {

        val driver =
            JdbcSqliteDriver("jdbc:sqlite:calendar.db")

        CalendarDatabase.Schema.create(driver)

        CalendarDatabase(driver)
    }
}

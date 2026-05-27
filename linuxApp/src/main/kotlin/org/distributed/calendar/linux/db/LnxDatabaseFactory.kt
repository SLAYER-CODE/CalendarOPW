package org.distributed.calendar.linux.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import org.distributed.calendar.core.db.DatabaseFactory
import org.distributed.calendar.db.CalendarDatabase

class LnxDatabaseFactory : DatabaseFactory {
    override fun createDriver(): SqlDriver {
        val driver = JdbcSqliteDriver("jdbc:sqlite:calendar.db")
        CalendarDatabase.Schema.create(driver)
        return driver
    }
}

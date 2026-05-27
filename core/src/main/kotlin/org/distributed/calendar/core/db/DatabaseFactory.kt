package org.distributed.calendar.core.db

import app.cash.sqldelight.db.SqlDriver

interface DatabaseFactory {
    fun createDriver(): SqlDriver
}

package org.distributed.calendar.core.network

import io.ktor.server.websocket.*
import java.util.concurrent.ConcurrentHashMap

object SessionRegistry {

    private val sessions =
        ConcurrentHashMap<String, DefaultWebSocketServerSession>()

    fun register(
        deviceId: String,
        session: DefaultWebSocketServerSession
    ) {

        sessions[deviceId] = session

        println("Registered session: $deviceId")
    }

    fun remove(deviceId: String) {

        sessions.remove(deviceId)

        println("Removed session: $deviceId")
    }

    fun getSessions():
        Map<String, DefaultWebSocketServerSession> {

        return sessions
    }
}

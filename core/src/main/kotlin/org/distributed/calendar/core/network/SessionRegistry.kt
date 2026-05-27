package org.distributed.calendar.core.network

import java.util.concurrent.ConcurrentHashMap

object SessionRegistry {

    private val sessions = ConcurrentHashMap<String, PeerSession>()

    fun register(deviceId: String, session: PeerSession) {
        sessions[deviceId] = session
        println("Registered session: $deviceId")
    }

    fun remove(deviceId: String) {
        sessions.remove(deviceId)
        println("Removed session: $deviceId")
    }

    fun getSessions(): Map<String, PeerSession> = sessions

    fun getSession(deviceId: String): PeerSession? = sessions[deviceId]
}

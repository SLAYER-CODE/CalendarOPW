package org.distributed.calendar.core.network

interface PeerSession {
    val deviceId: String
    suspend fun send(text: String)
}

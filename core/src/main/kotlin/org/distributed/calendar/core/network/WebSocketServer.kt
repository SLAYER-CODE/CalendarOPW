package org.distributed.calendar.core.network

interface WebSocketServer {
    suspend fun start(port: Int)
    fun stop()
}

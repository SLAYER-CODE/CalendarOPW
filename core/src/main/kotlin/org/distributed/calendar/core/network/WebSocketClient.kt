package org.distributed.calendar.core.network

interface WebSocketClient {
    suspend fun connect(host: String, port: Int)
}

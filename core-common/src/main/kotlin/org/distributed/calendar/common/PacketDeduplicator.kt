package org.distributed.calendar.common

import java.util.concurrent.ConcurrentHashMap

object PacketDeduplicator {

    private val packets = ConcurrentHashMap<String, Long>()

    fun isDuplicate(packetId: String): Boolean {
        val existing = packets.putIfAbsent(packetId, System.currentTimeMillis())
        return existing != null
    }
}

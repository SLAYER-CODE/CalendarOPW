package org.distributed.calendar.common

import java.util.concurrent.ConcurrentHashMap

object PacketDeduplicator {

    private val packets = ConcurrentHashMap<String, Long>()
    private const val MAX_AGE_MS = 300_000L // 5 minutes

    fun isDuplicate(packetId: String): Boolean {
        val now = System.currentTimeMillis()
        val existing = packets.putIfAbsent(packetId, now)
        return existing != null
    }

    fun cleanup() {
        val cutoff = System.currentTimeMillis() - MAX_AGE_MS
        packets.entries.removeIf { it.value < cutoff }
    }
}

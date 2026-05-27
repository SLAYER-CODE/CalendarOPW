package org.distributed.calendar.common

import org.distributed.calendar.common.model.SyncPacket
import java.util.concurrent.ConcurrentHashMap

object PendingPacketStore {

    private val pending = ConcurrentHashMap<String, SyncPacket>()

    fun add(packet: SyncPacket) {
        pending[packet.packetId] = packet
    }

    fun acknowledge(packetId: String) {
        pending.remove(packetId)
    }

    fun getPending(): List<SyncPacket> = pending.values.toList()
}

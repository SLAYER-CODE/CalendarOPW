package org.distributed.calendar.core.sync

import kotlinx.coroutines.delay
import org.distributed.calendar.common.PacketSerializer
import org.distributed.calendar.common.PendingPacketStore
import org.distributed.calendar.core.network.SessionRegistry

class PacketResender {

    suspend fun start() {
        while (true) {
            try {
                val sessions = SessionRegistry.getSessions()
                if (sessions.isNotEmpty()) {
                    val pending = PendingPacketStore.getPending()
                    for (packet in pending) {
                        val serialized = PacketSerializer.serialize(packet)
                        sessions.values.forEach { session ->
                            try {
                                session.send(serialized)
                            } catch (e: Exception) {
                                println("Resend failed to ${session.deviceId}: ${e.message}")
                            }
                        }
                    }
                    if (pending.isNotEmpty()) {
                        println("Resent ${pending.size} pending packets to ${sessions.size} session(s)")
                    }
                }
            } catch (e: Exception) {
                println("PacketResender error: ${e.message}")
            }
            delay(5_000)
        }
    }
}

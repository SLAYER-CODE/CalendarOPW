package org.distributed.calendar.core.sync

import kotlinx.coroutines.delay

class PacketResender {

    suspend fun start() {

        while (true) {

            org.distributed.calendar.common.PendingPacketStore
                .getPending()
                .forEach {

                    println(
                        "Resending packet: ${it.packetId}"
                    )

                    // reenviar websocket
                }

            delay(5000)
        }
    }
}

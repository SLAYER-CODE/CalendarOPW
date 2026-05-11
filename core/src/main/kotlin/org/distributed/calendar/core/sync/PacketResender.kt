package org.distributed.calendar.core.sync

import kotlinx.coroutines.delay

class PacketResender {

    suspend fun start() {

        while (true) {

            PendingPacketStore
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

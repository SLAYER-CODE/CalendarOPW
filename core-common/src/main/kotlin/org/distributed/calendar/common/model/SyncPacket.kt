package org.distributed.calendar.common.model

import kotlinx.serialization.Serializable

@Serializable
data class SyncPacket(
    val packetId: String,
    val type: PacketType,
    val deviceId: String,
    val timestamp: Long,
    val payload: String
)

@Serializable
enum class PacketType {
    HEARTBEAT,
    EVENT_CREATE,
    EVENT_UPDATE,
    EVENT_DELETE,
    SYNC_REQUEST,
    SYNC_RESPONSE,
    ACK
}

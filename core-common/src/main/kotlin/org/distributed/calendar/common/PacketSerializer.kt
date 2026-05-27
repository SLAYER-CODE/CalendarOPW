package org.distributed.calendar.common

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.distributed.calendar.common.model.SyncPacket

object PacketSerializer {
  private val json = Json { ignoreUnknownKeys = true }

  fun serialize(packet: SyncPacket): String = json.encodeToString(packet)

  fun deserialize(data: String): SyncPacket = json.decodeFromString(data)
}

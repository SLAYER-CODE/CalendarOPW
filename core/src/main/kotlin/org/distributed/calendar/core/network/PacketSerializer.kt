package org.distributed.calendar.core.network

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.distributed.calendar.core.model.SyncPacket

object PacketSerializer {

  private val json = Json { ignoreUnknownKeys = true }

  fun serialize(packet: SyncPacket): String {
    return json.encodeToString(packet)
  }

  fun deserialize(data: String): SyncPacket {
    return json.decodeFromString(data)
  }
}

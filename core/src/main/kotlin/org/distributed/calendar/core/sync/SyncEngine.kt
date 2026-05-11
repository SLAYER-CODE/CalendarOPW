package org.distributed.calendar.core.sync

import io.ktor.websocket.Frame
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.distributed.calendar.core.model.*
import org.distributed.calendar.core.network.PacketSerializer
import org.distributed.calendar.core.network.SessionRegistry
import org.distributed.calendar.core.sync.PendingPacketStore

class SyncEngine {

  private val events = mutableMapOf<String, Event>()
  private val devices = mutableMapOf<String, Device>()

  private fun handleAck(packet: SyncPacket) {

    PendingPacketStore.acknowledge(packet.payload)

    println("Packet acknowledged")
  }

  fun applyPacket(packet: SyncPacket) {
    if (PacketDeduplicator.isDuplicate(packet.packetId)) {

      println("Duplicate packet ignored")

      return
    }
    when (packet.type) {

      PacketType.ACK -> handleAck(packet)
      PacketType.HEARTBEAT -> handleHeartbeat(packet)
      PacketType.EVENT_CREATE -> handleEventCreate(packet)
      PacketType.EVENT_UPDATE -> handleEventUpdate(packet)
      PacketType.EVENT_DELETE -> handleEventDelete(packet)
      PacketType.SYNC_REQUEST -> handleSyncRequest(packet)
      PacketType.SYNC_RESPONSE -> handleSyncResponse(packet)
    }
  }

  private fun handleHeartbeat(packet: SyncPacket) {
    val device = devices[packet.deviceId]

    if (device != null) {
      devices[packet.deviceId] = device.copy(lastSeen = packet.timestamp, isOnline = true)
    } else {
      devices[packet.deviceId] =
              Device(
                      deviceId = packet.deviceId,
                      name = packet.deviceId,
                      type = DeviceType.ANDROID,
                      lastSeen = packet.timestamp,
                      isOnline = true
              )
    }
  }

  private fun handleEventCreate(packet: SyncPacket) {

    val event = decodeEvent(packet.payload)

    val existing = events[event.id]

    if (existing == null || event.updatedAt > existing.updatedAt) {

      events[event.id] = event

      println("Event synced: ${event.title}")
    } else {

      println("Ignored older event update")
    }
    broadcast(packet)
  }

  private fun handleEventUpdate(packet: SyncPacket) {

    val event = decodeEvent(packet.payload)

    val existing = events[event.id]

    if (existing == null || event.updatedAt > existing.updatedAt) {

      events[event.id] = event
    }
    broadcast(packet)
  }

  private fun handleEventDelete(packet: SyncPacket) {

    val eventId = packet.payload

    events.remove(eventId)

    println("Event deleted: $eventId")
    broadcast(packet)
  }

  private fun handleSyncRequest(packet: SyncPacket) {
    // futuro: enviar estado completo
  }

  private fun handleSyncResponse(packet: SyncPacket) {
    // futuro: merge de estados
  }

  fun getEvents(): List<Event> = events.values.toList()

  fun getDevices(): List<Device> = devices.values.toList()

  private val json = Json { ignoreUnknownKeys = true }

  private fun decodeEvent(payload: String): Event {
    return json.decodeFromString<Event>(payload)
  }
  private fun broadcast(packet: SyncPacket) {

    val serialized = PacketSerializer.serialize(packet)

    runBlocking {
      SessionRegistry.getSessions().values.forEach { session ->
        session.send(Frame.Text(serialized))
      }
    }
  }
}

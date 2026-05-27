package org.distributed.calendar.core.sync

import kotlinx.serialization.json.Json
import org.distributed.calendar.common.model.*
import org.distributed.calendar.common.PacketSerializer
import org.distributed.calendar.core.network.SessionRegistry
import org.distributed.calendar.core.event.EventRepository

class SyncEngine(
    private val eventRepository: EventRepository? = null
) {

    private val events = mutableMapOf<String, Event>()
    private val devices = mutableMapOf<String, Device>()

    private fun handleAck(packet: SyncPacket) {
        org.distributed.calendar.common.PendingPacketStore.acknowledge(packet.payload)
        println("Packet acknowledged")
    }

    fun applyPacket(packet: SyncPacket) {
        if (org.distributed.calendar.common.PacketDeduplicator.isDuplicate(packet.packetId)) {
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
            devices[packet.deviceId] = Device(
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
            eventRepository?.let { repo ->
                kotlinx.coroutines.runBlocking { repo.insert(event) }
            }
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
            eventRepository?.let { repo ->
                kotlinx.coroutines.runBlocking { repo.insert(event) }
            }
        }
        broadcast(packet)
    }

    private fun handleEventDelete(packet: SyncPacket) {
        val eventId = packet.payload
        events.remove(eventId)
        eventRepository?.let { repo ->
            kotlinx.coroutines.runBlocking { repo.delete(eventId) }
        }
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
        SessionRegistry.getSessions().values.forEach { session ->
            kotlinx.coroutines.runBlocking {
                session.send(serialized)
            }
        }
    }
}

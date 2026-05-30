package org.distributed.calendar.core.sync

import java.util.UUID
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.distributed.calendar.common.DeviceManager
import org.distributed.calendar.common.PacketDeduplicator
import org.distributed.calendar.common.PacketSerializer
import org.distributed.calendar.common.PendingPacketStore
import org.distributed.calendar.common.model.*
import org.distributed.calendar.core.device.DeviceRegistry
import org.distributed.calendar.core.event.EventRepository
import org.distributed.calendar.core.network.SessionRegistry

class SyncEngine(
    private val eventRepository: EventRepository? = null
) {

    private val events = mutableMapOf<String, Event>()
    private val devices = mutableMapOf<String, Device>()
    private val changeListeners = mutableListOf<() -> Unit>()

    fun addChangeListener(listener: () -> Unit) {
        changeListeners.add(listener)
    }

    private fun notifyChange() {
        changeListeners.forEach { it() }
    }

    fun applyPacket(packet: SyncPacket) {
        if (PacketDeduplicator.isDuplicate(packet.packetId)) {
            println("Duplicate packet ignored: ${packet.packetId}")
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

    fun createEvent(title: String, description: String, duration: Long) {
        val now = System.currentTimeMillis()
        val event = Event(
            id = "evt_$now",
            title = title,
            description = description,
            timestamp = now,
            duration = duration,
            priority = 0,
            updatedAt = now,
            sourceDeviceId = DeviceManager.deviceId
        )
        val packet = SyncPacket(
            packetId = UUID.randomUUID().toString(),
            type = PacketType.EVENT_CREATE,
            deviceId = DeviceManager.deviceId,
            timestamp = now,
            payload = json.encodeToString(Event.serializer(), event)
        )
        applyPacket(packet)
    }

    fun getEvents(): List<Event> = events.values.toList()

    fun getDevices(): List<Device> = devices.values.toList()

    // ─── Packet handlers ────────────────────────────────────────────────────

    private fun handleAck(packet: SyncPacket) {
        PendingPacketStore.acknowledge(packet.payload)
        println("Packet acknowledged: ${packet.payload}")
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
        DeviceRegistry.heartbeat(packet.deviceId)
        notifyChange()
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
            println("Ignored older event update: ${event.id}")
        }
        broadcast(packet)
        notifyChange()
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
        notifyChange()
    }

    private fun handleEventDelete(packet: SyncPacket) {
        val eventId = packet.payload
        events.remove(eventId)
        eventRepository?.let { repo ->
            kotlinx.coroutines.runBlocking { repo.delete(eventId) }
        }
        println("Event deleted: $eventId")
        broadcast(packet)
        notifyChange()
    }

    private fun handleSyncRequest(packet: SyncPacket) {
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
        DeviceRegistry.heartbeat(packet.deviceId)

        val allEvents = events.values.toList()
        val payload = json.encodeToString(allEvents)
        val response = SyncPacket(
            packetId = UUID.randomUUID().toString(),
            type = PacketType.SYNC_RESPONSE,
            deviceId = DeviceManager.deviceId,
            timestamp = System.currentTimeMillis(),
            payload = payload
        )
        val session = SessionRegistry.getSession(packet.deviceId)
        if (session != null) {
            kotlinx.coroutines.runBlocking {
                session.send(PacketSerializer.serialize(response))
            }
            println("Sent SYNC_RESPONSE with ${allEvents.size} events to ${packet.deviceId}")
        }
        notifyChange()
    }

    private fun handleSyncResponse(packet: SyncPacket) {
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
        DeviceRegistry.heartbeat(packet.deviceId)

        val remoteEvents: List<Event> = json.decodeFromString(packet.payload)
        var merged = 0
        for (event in remoteEvents) {
            val existing = events[event.id]
            if (existing == null || event.updatedAt > existing.updatedAt) {
                events[event.id] = event
                eventRepository?.let { repo ->
                    kotlinx.coroutines.runBlocking { repo.insert(event) }
                }
                merged++
            }
        }
        if (merged > 0) {
            println("Merged $merged events from SYNC_RESPONSE")
        }
        notifyChange()
    }

    // ─── Broadcast with fallback to pending queue ───────────────────────────

    private fun broadcast(packet: SyncPacket) {
        val sessions = SessionRegistry.getSessions()
        if (sessions.isEmpty()) {
            PendingPacketStore.add(packet)
            println("No connected peers — packet queued: ${packet.packetId}")
            return
        }
        val serialized = PacketSerializer.serialize(packet)
        sessions.values.forEach { session ->
            kotlinx.coroutines.runBlocking {
                try {
                    session.send(serialized)
                } catch (e: Exception) {
                    println("Failed to send to ${session.deviceId}: ${e.message}")
                }
            }
        }
    }

    // ─── JSON helpers ───────────────────────────────────────────────────────

    private val json = Json { ignoreUnknownKeys = true }

    private fun decodeEvent(payload: String): Event {
        return json.decodeFromString<Event>(payload)
    }
}

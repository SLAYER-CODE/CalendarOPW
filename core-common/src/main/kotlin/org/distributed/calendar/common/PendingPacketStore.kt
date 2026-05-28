package org.distributed.calendar.common

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.distributed.calendar.common.model.SyncPacket
import java.io.File
import java.util.concurrent.ConcurrentHashMap

object PendingPacketStore {

    private val pending = ConcurrentHashMap<String, SyncPacket>()
    private val json = Json { ignoreUnknownKeys = true }
    private var persistDir: File? = null

    fun setPersistDir(dir: File) {
        persistDir = dir
        if (!dir.exists()) dir.mkdirs()
        dir.listFiles()?.forEach { file ->
            try {
                if (file.name.endsWith(".json")) {
                    val text = file.readText()
                    val packet = json.decodeFromString<SyncPacket>(text)
                    pending[packet.packetId] = packet
                    println("Loaded pending packet: ${packet.packetId}")
                }
            } catch (e: Exception) {
                println("Failed to load pending packet file ${file.name}: ${e.message}")
            }
        }
    }

    fun add(packet: SyncPacket) {
        pending[packet.packetId] = packet
        persistDir?.let { dir ->
            try {
                val file = File(dir, "${packet.packetId}.json")
                file.writeText(json.encodeToString(packet))
            } catch (e: Exception) {
                println("Failed to persist pending packet: ${e.message}")
            }
        }
    }

    fun acknowledge(packetId: String) {
        pending.remove(packetId)
        persistDir?.let { dir ->
            try {
                val file = File(dir, "${packetId}.json")
                if (file.exists()) file.delete()
            } catch (e: Exception) {
                println("Failed to delete persisted packet: ${e.message}")
            }
        }
    }

    fun getPending(): List<SyncPacket> = pending.values.toList()

    fun clear() {
        pending.clear()
        persistDir?.let { dir ->
            dir.listFiles()?.forEach { it.delete() }
        }
    }
}

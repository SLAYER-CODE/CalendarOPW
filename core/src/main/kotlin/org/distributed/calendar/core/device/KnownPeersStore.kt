package org.distributed.calendar.core.device

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.distributed.calendar.common.model.Device
import java.io.File

@Serializable
private data class KnownPeersData(
    val version: Int = 1,
    val devices: List<Device>
)

class KnownPeersStore(private val file: File) {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    fun load(): List<Device> {
        if (!file.exists()) return emptyList()
        return try {
            val data = json.decodeFromString<KnownPeersData>(file.readText())
            data.devices.map { it.copy(isOnline = false) }
        } catch (e: Exception) {
            println("Failed to load known peers: ${e.message}")
            emptyList()
        }
    }

    fun save(device: Device) {
        val all = load().toMutableList()
        all.removeAll { it.deviceId == device.deviceId }
        all.add(device.copy(isOnline = false))
        val data = KnownPeersData(devices = all)
        try {
            file.writeText(json.encodeToString(data))
        } catch (e: Exception) {
            println("Failed to save known peer: ${e.message}")
        }
    }
}

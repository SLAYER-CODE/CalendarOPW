package org.distributed.calendar.core.device

import java.util.concurrent.ConcurrentHashMap

object DeviceRegistry {

    private val devices = ConcurrentHashMap<String, Long>()
    private val ONLINE_WINDOW_MS = 15_000L

    fun heartbeat(deviceId: String) {
        devices[deviceId] = System.currentTimeMillis()
    }

    fun isOnline(deviceId: String): Boolean {
        val lastSeen = devices[deviceId] ?: return false
        return System.currentTimeMillis() - lastSeen < ONLINE_WINDOW_MS
    }

    fun getOnlineDevices(): List<String> = devices.filter { isOnline(it.key) }.keys.toList()

    fun cleanup() {
        val cutoff = System.currentTimeMillis() - ONLINE_WINDOW_MS * 2
        devices.entries.removeIf { it.value < cutoff }
    }

    fun remove(deviceId: String) {
        devices.remove(deviceId)
    }
}

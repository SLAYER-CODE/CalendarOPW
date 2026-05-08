package org.distributed.calendar.core.device

import java.util.concurrent.ConcurrentHashMap

object DeviceRegistry {

    private val devices = ConcurrentHashMap<String, Long>()

    fun heartbeat(deviceId: String) {

        devices[deviceId] = System.currentTimeMillis()
    }

    fun isOnline(deviceId: String): Boolean {

        val lastSeen = devices[deviceId] ?: return false

        return System.currentTimeMillis() - lastSeen < 15000
    }

    fun getOnlineDevices(): List<String> {

        return devices
            .filter { isOnline(it.key) }
            .keys
            .toList()
    }
}

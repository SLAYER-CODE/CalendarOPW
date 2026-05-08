package org.distributed.calendar.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Device(
    val deviceId: String,
    val name: String,
    val type: DeviceType,
    val lastSeen: Long,
    val isOnline: Boolean
)

@Serializable
enum class DeviceType {
    ANDROID,
    LINUX,
    DESKTOP
}

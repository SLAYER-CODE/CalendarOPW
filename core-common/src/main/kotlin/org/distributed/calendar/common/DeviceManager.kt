package org.distributed.calendar.common

import java.io.File
import java.util.UUID
import kotlin.jvm.Volatile

/**
 * Abstraction for obtaining a stable device id. By default a file-backed provider is used
 * (suitable for JVM/server). Android should set DeviceManager.provider to an Android-backed
 * implementation early in app startup (for example from a Service or Application).
 */
interface DeviceIdProvider {
    val deviceId: String
}

class DefaultFileDeviceIdProvider(private val filePath: String = "device.id") : DeviceIdProvider {
    // lazy-load the id
    private val id: String by lazy {
        val file = File(filePath)
        if (file.exists()) {
            file.readText()
        } else {
            val id = UUID.randomUUID().toString()
            file.writeText(id)
            id
        }
    }

    override val deviceId: String
        get() = id
}

object DeviceManager {
    @Volatile
    var provider: DeviceIdProvider = DefaultFileDeviceIdProvider()

    val deviceId: String
        get() = provider.deviceId
}

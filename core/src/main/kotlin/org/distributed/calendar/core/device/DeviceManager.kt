package org.distributed.calendar.core.device

import java.io.File
import java.util.UUID

object DeviceManager {

    private const val DEVICE_FILE = "device.id"

    val deviceId: String by lazy {

        val file = File(DEVICE_FILE)

        if (file.exists()) {

            file.readText()

        } else {

            val id = UUID.randomUUID().toString()

            file.writeText(id)

            id
        }
    }
}

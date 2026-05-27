package org.distributed.calendar.android

import android.content.Context
import org.distributed.calendar.common.DeviceIdProvider
import java.io.File
import java.util.UUID

class DeviceIdProviderAndroid(private val context: Context) : DeviceIdProvider {

    private val fileName = "device.id"

    private val id: String by lazy {
        val file = File(context.filesDir, fileName)
        if (file.exists()) {
            file.readText()
        } else {
            val uuid = UUID.randomUUID().toString()
            file.writeText(uuid)
            uuid
        }
    }

    override val deviceId: String
        get() = id
}

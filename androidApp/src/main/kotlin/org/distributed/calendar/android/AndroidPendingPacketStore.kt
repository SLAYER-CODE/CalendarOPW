package org.distributed.calendar.android

import android.content.Context
import org.distributed.calendar.common.PendingPacketStore
import org.distributed.calendar.common.PacketSerializer
import org.distributed.calendar.common.model.SyncPacket
import java.io.File

class AndroidPendingPacketStore(private val context: Context) {

    private val dirName = "pending_packets"

    private val dir: File
        get() = File(context.filesDir, dirName).also { if (!it.exists()) it.mkdirs() }

    // Load persisted packets into in-memory store
    fun initLoad() {
        dir.listFiles()?.forEach { file ->
            try {
                val text = file.readText()
                val packet = org.distributed.calendar.common.PacketSerializer.deserialize(text)
                PendingPacketStore.add(packet)
            } catch (_: Exception) {
                // ignore malformed
            }
        }
    }

    fun add(packet: SyncPacket) {
        try {
            val serialized = org.distributed.calendar.common.PacketSerializer.serialize(packet)
            val out = File(dir, "${packet.packetId}.json")
            out.writeText(serialized)
            PendingPacketStore.add(packet)
        } catch (e: Exception) {
            // best-effort; if persist fails, still add to memory to attempt send in this session
            PendingPacketStore.add(packet)
        }
    }

    fun acknowledge(packetId: String) {
        try {
            val file = File(dir, "${packetId}.json")
            if (file.exists()) file.delete()
        } catch (_: Exception) {
        }

        PendingPacketStore.acknowledge(packetId)
    }

    fun getPending(): List<SyncPacket> = PendingPacketStore.getPending()
}

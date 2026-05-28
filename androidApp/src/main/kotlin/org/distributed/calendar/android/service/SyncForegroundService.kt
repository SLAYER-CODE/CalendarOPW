package org.distributed.calendar.android.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.distributed.calendar.android.db.AndroidDatabaseFactory
import org.distributed.calendar.android.db.AndroidEventRepository
import org.distributed.calendar.android.network.AndroidDiscoveryListener
import org.distributed.calendar.android.network.AndroidWebSocketClient
import org.distributed.calendar.android.network.AndroidWebSocketServer
import org.distributed.calendar.android.network.AndroidDiscoveryBroadcaster
import org.distributed.calendar.android.DeviceIdProviderAndroid
import org.distributed.calendar.common.DeviceManager
import org.distributed.calendar.common.PacketDeduplicator
import org.distributed.calendar.common.PendingPacketStore
import org.distributed.calendar.core.device.DeviceRegistry
import org.distributed.calendar.core.sync.PacketResender
import org.distributed.calendar.core.sync.SyncEngine
import java.io.File

class SyncForegroundService : Service() {

    companion object {
        var syncEngine: SyncEngine? = null
    }

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        val notification = Notification.Builder(this, "sync_service")
            .setContentTitle("Distributed Calendar")
            .setContentText("Synchronization active")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .build()

        startForeground(1, notification)

        scope.launch {
            try {
                val provider = DeviceIdProviderAndroid(
                    this@SyncForegroundService.applicationContext
                )
                DeviceManager.provider = provider

                val persistDir = File(this@SyncForegroundService.filesDir, "pending_packets")
                PendingPacketStore.setPersistDir(persistDir)

                val driver = AndroidDatabaseFactory(this@SyncForegroundService.applicationContext).createDriver()
                val eventRepo = AndroidEventRepository(driver)
                val engine = SyncEngine(eventRepo)
                syncEngine = engine

                // Start P2P server with shared engine
                AndroidWebSocketServer(engine).start(8080)

                // Start discovery broadcaster
                AndroidDiscoveryBroadcaster().startBroadcast()

                // PacketResender safety net (Android client already has its own resend)
                scope.launch { PacketResender().start() }

                // Periodic cleanup
                scope.launch {
                    while (true) {
                        delay(60_000)
                        PacketDeduplicator.cleanup()
                        DeviceRegistry.cleanup()
                    }
                }

                // Search for peers and connect (retry on timeout)
                while (true) {
                    println("Searching for peers...")
                    val serverIp = AndroidDiscoveryListener().listen()
                    if (serverIp != null) {
                        println("Peer found: $serverIp")
                        val client = AndroidWebSocketClient(this@SyncForegroundService.applicationContext)
                        client.connect(serverIp, 8080)
                        break
                    }
                    delay(5_000)
                }
            } catch (e: Exception) {
                println("Sync service error (non-fatal): ${e.message}")
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "sync_service",
                "Sync Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}

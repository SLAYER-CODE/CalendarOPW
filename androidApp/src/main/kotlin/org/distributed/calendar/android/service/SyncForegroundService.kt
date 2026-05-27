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
import kotlinx.coroutines.launch
import org.distributed.calendar.android.network.AndroidDiscoveryListener
import org.distributed.calendar.android.network.AndroidWebSocketClient
import org.distributed.calendar.android.network.AndroidWebSocketServer
import org.distributed.calendar.android.network.AndroidDiscoveryBroadcaster
import org.distributed.calendar.common.DeviceManager

class SyncForegroundService : Service() {

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
                val provider = org.distributed.calendar.android.DeviceIdProviderAndroid(
                    this@SyncForegroundService.applicationContext
                )
                DeviceManager.provider = provider

                // Start P2P server
                AndroidWebSocketServer().start(8080)

                // Start discovery broadcaster
                AndroidDiscoveryBroadcaster().startBroadcast()

                // Search for peers and connect
                println("Searching for peers...")
                val serverIp = AndroidDiscoveryListener().listen()
                if (serverIp != null) {
                    println("Peer found: $serverIp")
                    val client = AndroidWebSocketClient(this@SyncForegroundService.applicationContext)
                    client.connect(serverIp, 8080)
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

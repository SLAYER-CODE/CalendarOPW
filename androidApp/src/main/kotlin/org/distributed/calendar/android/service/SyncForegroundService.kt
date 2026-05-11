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
import org.distributed.calendar.core.network.WebSocketClient
import org.distributed.calendar.core.network.discovery.DiscoveryListener

class SyncForegroundService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        val notification =
            Notification.Builder(this, "sync_service")
                .setContentTitle("Distributed Calendar")
                .setContentText("Synchronization active")
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .build()

        startForeground(1, notification)

        scope.launch {

            println("Searching for server...")

            val serverIp =
                DiscoveryListener()
                    .startListening()

            println("Server found: $serverIp")

            val client =
                WebSocketClient(serverIp)

            client.connect()
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel =
                NotificationChannel(
                    "sync_service",
                    "Sync Service",
                    NotificationManager.IMPORTANCE_LOW
                )

            val manager =
                getSystemService(
                    NotificationManager::class.java
                )

            manager.createNotificationChannel(channel)
        }
    }
}

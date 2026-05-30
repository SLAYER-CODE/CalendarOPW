package org.distributed.calendar.android.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.distributed.calendar.android.DeviceIdProviderAndroid
import org.distributed.calendar.android.MainActivity
import org.distributed.calendar.android.R
import org.distributed.calendar.android.db.AndroidDatabaseFactory
import org.distributed.calendar.android.db.AndroidEventRepository
import org.distributed.calendar.android.network.AndroidDiscoveryBroadcaster
import org.distributed.calendar.android.network.AndroidDiscoveryListener
import org.distributed.calendar.android.network.AndroidWebSocketClient
import org.distributed.calendar.android.network.AndroidWebSocketServer
import org.distributed.calendar.common.DeviceManager
import org.distributed.calendar.common.NetworkUtils
import org.distributed.calendar.common.PacketDeduplicator
import org.distributed.calendar.common.PendingPacketStore
import org.distributed.calendar.common.TcpPeerScanner
import org.distributed.calendar.common.model.Event
import org.distributed.calendar.core.device.DeviceRegistry
import org.distributed.calendar.core.device.KnownPeersStore
import org.distributed.calendar.core.sync.PacketResender
import org.distributed.calendar.core.sync.SyncEngine
import java.io.File

class SyncForegroundService : Service() {

    companion object {
        var syncEngine: SyncEngine? = null
        private const val NOTIFY_SYNC = 1
        private const val NOTIFY_EVENT = 2
        private const val CHAN_SYNC = "sync_service"
        private const val CHAN_EVENT = "calendar_events"
        private const val PREFS_FILE = "event_tracking"
        private const val PREFS_KNOWN = "known_event_ids"
    }

    private val scope = CoroutineScope(Dispatchers.IO)
    private var initialLoadComplete = false

    override fun onCreate() {
        super.onCreate()

        createChannels()

        try {
            startForeground(NOTIFY_SYNC, buildSyncNotification(false))
        } catch (e: Exception) {
            println("startForeground failed: ${e.message}")
        }

        scope.launch {
            try {
                val provider = DeviceIdProviderAndroid(
                    this@SyncForegroundService.applicationContext
                )
                DeviceManager.provider = provider

                NetworkUtils.printLocalAddresses()

                val persistDir = File(this@SyncForegroundService.filesDir, "pending_packets")
                PendingPacketStore.setPersistDir(persistDir)

                val driver = AndroidDatabaseFactory(this@SyncForegroundService.applicationContext)
                    .createDriver()
                val eventRepo = AndroidEventRepository(driver)
                val engine = SyncEngine(eventRepo)
                syncEngine = engine

                val knownPeersFile = File(
                    this@SyncForegroundService.filesDir, "known_peers.json"
                )
                engine.knownPeersStore = KnownPeersStore(knownPeersFile)
                engine.loadKnownPeers()

                val prefs = this@SyncForegroundService
                    .getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
                val knownIds = prefs.getStringSet(PREFS_KNOWN, mutableSetOf())
                    ?.toMutableSet() ?: mutableSetOf()
                engine.getEvents().forEach { knownIds.add(it.id) }
                prefs.edit().putStringSet(PREFS_KNOWN, knownIds).apply()
                initialLoadComplete = true

                engine.addChangeListener {
                    if (initialLoadComplete) {
                        val current = engine.getEvents()
                        val localId = DeviceManager.deviceId
                        val saved = this@SyncForegroundService
                            .getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
                        val oldIds = saved.getStringSet(PREFS_KNOWN, mutableSetOf())
                            ?: mutableSetOf()
                        val newRemote = current.filter { e ->
                            e.id !in oldIds && e.sourceDeviceId != localId
                        }
                        if (newRemote.isNotEmpty()) {
                            val updated = oldIds.toMutableSet()
                            newRemote.forEach { updated.add(it.id) }
                            saved.edit().putStringSet(PREFS_KNOWN, updated).apply()
                            showEventNotification(newRemote, engine)
                        }
                    }
                }

                scope.launch { AndroidWebSocketServer(engine).start(8080) }
                scope.launch { AndroidDiscoveryBroadcaster().startBroadcast() }
                scope.launch { PacketResender().start() }

                scope.launch {
                    while (true) {
                        delay(5_000)
                        PacketDeduplicator.cleanup()
                        DeviceRegistry.cleanup()
                        updateSyncNotification(DeviceRegistry.getOnlineDevices().isNotEmpty())
                    }
                }

                while (true) {
                    println("Searching for peers...")
                    var serverIp = AndroidDiscoveryListener().listen()
                    if (serverIp == null) {
                        println("UDP discovery timed out — trying TCP scan...")
                        serverIp = TcpPeerScanner.scan(8080)
                    }
                    if (serverIp != null) {
                        val localIPs = NetworkUtils.getAllLocalIPv4Addresses()
                        if (serverIp in localIPs) {
                            println("Ignoring self-IP: $serverIp")
                            delay(5_000)
                            continue
                        }
                        println("Peer found: $serverIp")
                        updateSyncNotification(true)
                        val client = AndroidWebSocketClient(
                            this@SyncForegroundService.applicationContext,
                            engine
                        )
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

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(NotificationManager::class.java)
            mgr.createNotificationChannel(NotificationChannel(
                CHAN_SYNC, "Sync Service", NotificationManager.IMPORTANCE_LOW
            ))
            val eventChan = NotificationChannel(
                CHAN_EVENT, "Calendar Events", NotificationManager.IMPORTANCE_HIGH
            )
            eventChan.enableVibration(true)
            eventChan.setShowBadge(true)
            mgr.createNotificationChannel(eventChan)
        }
    }

    private fun buildSyncNotification(connected: Boolean): Notification {
        val text = if (connected) "Connected — synchronizing"
            else "Synchronizing..."
        return Notification.Builder(this, CHAN_SYNC)
            .setContentTitle("Distributed Calendar")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_notify)
            .setOngoing(true)
            .build()
    }

    private fun updateSyncNotification(connected: Boolean) {
        startForeground(NOTIFY_SYNC, buildSyncNotification(connected))
    }

    private fun showEventNotification(events: List<Event>, engine: SyncEngine) {
        val first = events.first()
        val deviceName = engine.getDevices()
            .find { it.deviceId == first.sourceDeviceId }
            ?.name ?: first.sourceDeviceId.take(8)

        val title = if (events.size == 1) deviceName
            else "$deviceName + ${events.size - 1} more"

        val text = if (events.size == 1)
            "${first.title}${if (first.description.isNullOrBlank()) "" else ": ${first.description}"}"
            else events.joinToString(", ") { it.title }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pi = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = Notification.Builder(this, CHAN_EVENT)
            .setSmallIcon(R.drawable.ic_notify)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setCategory(Notification.CATEGORY_MESSAGE)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setStyle(Notification.BigTextStyle().bigText(text))
            .build()

        getSystemService(NotificationManager::class.java).notify(NOTIFY_EVENT, notif)
    }
}

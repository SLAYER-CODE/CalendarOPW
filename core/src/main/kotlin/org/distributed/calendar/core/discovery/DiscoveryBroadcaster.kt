package org.distributed.calendar.core.discovery

interface DiscoveryBroadcaster {
    suspend fun startBroadcast()
    fun stop()
}

package org.distributed.calendar.core.discovery

interface DiscoveryListener {
    suspend fun listen(): String?
}

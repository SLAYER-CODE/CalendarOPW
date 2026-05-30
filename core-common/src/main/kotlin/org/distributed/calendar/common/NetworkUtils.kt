package org.distributed.calendar.common

import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface

object NetworkUtils {
    fun getLocalIPv4Addresses(): Set<String> {
        return NetworkInterface.getNetworkInterfaces().asSequence()
            .flatMap { it.inetAddresses.asSequence() }
            .filter { !it.isLoopbackAddress && it is Inet4Address }
            .map { it.hostAddress }
            .toSet()
    }

    fun getBroadcastAddresses(): Set<InetAddress> {
        val result = mutableSetOf<InetAddress>()
        try {
            NetworkInterface.getNetworkInterfaces().asSequence()
                .flatMap { it.interfaceAddresses.asSequence() }
                .filter { it.address is Inet4Address && it.broadcast != null }
                .forEach { result.add(it.broadcast) }
        } catch (_: Exception) {}
        return result
    }

    fun printLocalAddresses() {
        val addrs = getLocalIPv4Addresses()
        println("Local IP addresses: ${addrs.joinToString(", ")}")
        val broadcasts = getBroadcastAddresses()
            .map { it.hostAddress }
            .joinToString(", ")
        println("Subnet broadcast addresses: $broadcasts")
        if (addrs.isEmpty()) {
            println("WARNING: No non-loopback IPv4 addresses found!")
            println("  discovery will NOT work — check network connectivity")
        }
    }
}

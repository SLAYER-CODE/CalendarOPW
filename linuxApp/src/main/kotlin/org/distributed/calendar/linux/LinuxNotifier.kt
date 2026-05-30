package org.distributed.calendar.linux

object LinuxNotifier {

    fun searchStarted() {
        notify("Searching for peers on the network...")
    }

    fun peerFound(ip: String) {
        notify("Connected to $ip")
    }

    fun eventReceived(title: String, fromDevice: String) {
        notify("$fromDevice", title)
    }

    fun synchronizing() {
        notify("Synchronization active")
    }

    private fun notify(summary: String, body: String = "") {
        try {
            val cmd = mutableListOf("notify-send", "-a", "Distributed Calendar", summary)
            if (body.isNotBlank()) cmd.add(body)
            ProcessBuilder(cmd).start()
        } catch (_: Exception) {
            // notify-send not available — silently ignore
        }
    }
}

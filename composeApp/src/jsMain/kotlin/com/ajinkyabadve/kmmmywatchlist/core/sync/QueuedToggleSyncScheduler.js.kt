package com.ajinkyabadve.kmmmywatchlist.core.sync

// Web never queues offline toggles (no local database) - nothing to send.
actual object QueuedToggleSyncScheduler {
    actual fun schedule() = Unit
}

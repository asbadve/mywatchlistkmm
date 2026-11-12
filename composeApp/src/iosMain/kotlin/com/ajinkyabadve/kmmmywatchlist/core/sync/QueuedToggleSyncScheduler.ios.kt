package com.ajinkyabadve.kmmmywatchlist.core.sync

// No network-constrained background job here - see QueuedToggleSyncScheduler's kdoc.
actual object QueuedToggleSyncScheduler {
    actual fun schedule() = sendQueuedTogglesNow()
}

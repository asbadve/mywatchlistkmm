package com.ajinkyabadve.kmmmywatchlist.features.account.repository

/** `trackedMediaOutbox.desiredState` values - see that table's comment in `MyDatabase.sq`. */
internal object TrackedMediaOutboxConstant {
    const val DESIRED_ADD = 1L
    const val DESIRED_REMOVE = 0L

    fun desiredStateOf(add: Boolean): Long = if (add) DESIRED_ADD else DESIRED_REMOVE
}

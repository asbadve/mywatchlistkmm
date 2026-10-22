package com.ajinkyabadve.kmmmywatchlist.features.backup.model

/** What a restore actually added - lower than [BackupSummary]'s counts when some items were
 *  already on this device (restore merges, it never overwrites). */
data class RestoreOutcome(
    val peopleAdded: Int,
    val collectionsAdded: Int,
    val remindersAdded: Int,
    val settingsApplied: Int,
)

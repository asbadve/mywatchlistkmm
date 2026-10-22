package com.ajinkyabadve.kmmmywatchlist.features.backup.model

/** How many of each thing a backup file holds - shown in the restore confirmation. */
data class BackupSummary(
    val people: Int,
    val collections: Int,
    val reminders: Int,
    val settings: Int,
)

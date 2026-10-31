package com.ajinkyabadve.kmmmywatchlist.features.backup.model

import kotlinx.serialization.Serializable

@Serializable
data class BackupReminderPreference(
    val enabled: Boolean,
    val minutesOfDay: Int,
)

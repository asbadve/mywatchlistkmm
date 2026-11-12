package com.ajinkyabadve.kmmmywatchlist.features.backup.model

import kotlinx.serialization.Serializable

@Serializable
data class BackupPerson(
    val id: Long,
    val name: String,
    val profilePath: String? = null,
    val addedAt: Long,
)

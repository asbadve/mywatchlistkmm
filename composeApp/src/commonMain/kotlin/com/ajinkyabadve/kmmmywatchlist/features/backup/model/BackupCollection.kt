package com.ajinkyabadve.kmmmywatchlist.features.backup.model

import kotlinx.serialization.Serializable

@Serializable
data class BackupCollection(
    val id: Long,
    val name: String,
    val posterPath: String? = null,
    val addedAt: Long,
)

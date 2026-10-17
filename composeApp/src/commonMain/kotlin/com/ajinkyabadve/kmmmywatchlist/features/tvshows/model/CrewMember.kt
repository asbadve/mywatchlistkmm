package com.ajinkyabadve.kmmmywatchlist.features.tvshows.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Episode-level crew entries carry job/department info (Director, Writer, ...) that the shared
// CastMember shape (built around character/ordering) doesn't have.
@Serializable
data class CrewMember(
    val id: Long = -1,
    val name: String = "",
    @SerialName("original_name") val originalName: String = "",
    val job: String = "",
    val department: String = "",
    @SerialName("credit_id") val creditId: String? = null,
    @SerialName("known_for_department") val knownForDepartment: String? = null,
    @SerialName("profile_path") val profilePath: String? = null,
)

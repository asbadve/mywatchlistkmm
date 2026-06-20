package com.ajinkyabadve.kmmmywatchlist.features.person.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PersonPageResult(
    @SerialName("page") var page: Int,
    @SerialName("results") var list: List<Person>?,
    @SerialName("total_results") var totalResults: Int?,
    @SerialName("total_pages") var totalPages: Int?,
)

@Serializable
data class Person(
    var id: Int = -1,
    var adult: Boolean = false,
    var gender: Int = 0,
    @SerialName("known_for_department") var knownForDepartment: String = "",
    var name: String = "",
    var popularity: Double = 0.0,
    @SerialName("profile_path") var profilePath: String? = "",
)

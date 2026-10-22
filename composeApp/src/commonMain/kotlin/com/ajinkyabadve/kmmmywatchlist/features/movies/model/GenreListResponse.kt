package com.ajinkyabadve.kmmmywatchlist.features.movies.model

import com.ajinkyabadve.kmmmywatchlist.features.movies.model.Genre
import kotlinx.serialization.Serializable

@Serializable
internal data class GenreListResponse(
    val genres: List<Genre> = emptyList(),
)

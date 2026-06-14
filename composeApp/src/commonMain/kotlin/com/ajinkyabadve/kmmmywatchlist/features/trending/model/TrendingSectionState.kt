package com.ajinkyabadve.kmmmywatchlist.features.trending.model

import com.ajinkyabadve.kmmmywatchlist.features.movies.model.Movie
import kotlinx.serialization.ExperimentalSerializationApi

data class TrendingSectionState
    @OptIn(ExperimentalSerializationApi::class)
    constructor(
        val title: String,
        val mediaType: String,
        val chipList: List<String>,
        val selectedChipIndex: Int,
        val isScreenLoading: Boolean,
        val isLoading: Boolean,
        val mediaList: List<Movie>,
        val errorMessage: String? = null,
    )

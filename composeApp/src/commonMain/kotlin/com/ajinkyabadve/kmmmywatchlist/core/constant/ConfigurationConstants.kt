package com.ajinkyabadve.kmmmywatchlist.core.constant

import com.ajinkyabadve.kmmmywatchlist.core.model.ImagesConfig

object ConfigurationConstants {
    const val KEY_CONFIG = "tmdb_config_json"
    const val KEY_TIMESTAMP = "tmdb_config_timestamp"
    const val DAY_IN_MILLIS = 24 * 60 * 60 * 1000L

    val defaultImagesConfig =
        ImagesConfig(
            base_url = "http://image.tmdb.org/t/p/",
            secure_base_url = "https://image.tmdb.org/t/p/",
            backdrop_sizes = listOf("w300", "w780", "w1280", "original"),
            logo_sizes = listOf("w45", "w92", "w154", "w185", "w300", "w500", "original"),
            poster_sizes = listOf("w92", "w154", "w185", "w342", "w500", "w780", "original"),
            profile_sizes = listOf("w45", "w185", "h632", "original"),
            still_sizes = listOf("w92", "w185", "w300", "original"),
        )
}

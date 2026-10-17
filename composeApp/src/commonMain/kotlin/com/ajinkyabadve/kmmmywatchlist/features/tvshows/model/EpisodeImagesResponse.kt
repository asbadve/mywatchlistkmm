package com.ajinkyabadve.kmmmywatchlist.features.tvshows.model

import com.ajinkyabadve.kmmmywatchlist.features.movies.model.BackdropImage
import kotlinx.serialization.Serializable

// TMDB's /tv/{id}/season/{season_number}/episode/{episode_number} images sub-resource returns a
// single "stills" bucket (no backdrops/posters split), unlike the movie/tv/season-level ImagesResponse.
@Serializable
data class EpisodeImagesResponse(
    val stills: List<BackdropImage> = emptyList(),
)

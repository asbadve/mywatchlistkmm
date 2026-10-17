package com.ajinkyabadve.kmmmywatchlist.features.tvshows.model

import com.ajinkyabadve.kmmmywatchlist.features.movies.model.Credits
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.ImagesResponse
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.VideoResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Note: TMDB's /tv/{id}/season/{season_number} sub-resource only supports credits, images,
// external_ids and videos as append_to_response values. content_ratings/keywords/recommendations/similar
// are ignored by TMDB for this endpoint (no error, just absent from the response), so they aren't modeled here.
@Serializable
data class TvSeasonDetail(
    val id: Long = -1,
    @SerialName("season_number") val seasonNumber: Int = 0,
    val name: String = "",
    val overview: String = "",
    @SerialName("air_date") val airDate: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    val episodes: List<Episode> = emptyList(),
    val credits: Credits? = null,
    val videos: VideoResponse? = null,
    val images: ImagesResponse? = null,
    @SerialName("external_ids") val externalIds: ExternalIds? = null,
)

package com.ajinkyabadve.kmmmywatchlist.features.tvshows.model

import com.ajinkyabadve.kmmmywatchlist.features.movies.model.Credits
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.Genre
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.ImagesResponse
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.VideoResponse
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.WatchProvidersResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TvDetail(
    val id: Long = -1,
    @SerialName("name") val title: String = "",
    val overview: String = "",
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("first_air_date") val firstAirDate: String = "",
    @SerialName("last_air_date") val lastAirDate: String? = null,
    // ISO 3166-1 codes of the show's home country (e.g. ["GB"]) - checked live 2026-10-17.
    @SerialName("origin_country") val originCountry: List<String> = emptyList(),
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    @SerialName("number_of_seasons") val numberOfSeasons: Int? = null,
    @SerialName("number_of_episodes") val numberOfEpisodes: Int? = null,
    @SerialName("episode_run_time") val episodeRunTime: List<Int>? = null,
    val genres: List<Genre>? = null,
    @SerialName("original_language") val originalLanguage: String = "",
    val status: String? = null,
    val tagline: String? = null,
    @SerialName("in_production") val inProduction: Boolean? = null,
    val networks: List<Network>? = null,
    @SerialName("created_by") val createdBy: List<CreatedBy>? = null,
    val seasons: List<SeasonSummary>? = null,
    @SerialName("next_episode_to_air") val nextEpisodeToAir: Episode? = null,
    @SerialName("last_episode_to_air") val lastEpisodeToAir: Episode? = null,
    val credits: Credits? = null,
    val videos: VideoResponse? = null,
    val recommendations: TvPageResult? = null,
    val similar: TvPageResult? = null,
    val keywords: TvKeywordResponse? = null,
    val images: ImagesResponse? = null,
    @SerialName("content_ratings") val contentRatings: ContentRatingsResponse? = null,
    // Same shape as the movie side, so one hero can resolve "where does this stream" for both.
    @SerialName("watch/providers") val watchProviders: WatchProvidersResponse? = null,
    @SerialName("external_ids") val externalIds: ExternalIds? = null,
)

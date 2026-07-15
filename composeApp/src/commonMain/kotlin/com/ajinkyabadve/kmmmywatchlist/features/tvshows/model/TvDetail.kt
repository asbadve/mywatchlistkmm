package com.ajinkyabadve.kmmmywatchlist.features.tvshows.model

import com.ajinkyabadve.kmmmywatchlist.features.movies.model.Credits
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.Genre
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.ImagesResponse
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.Keyword
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.VideoResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Network(
    val id: Int = -1,
    val name: String = "",
    @SerialName("logo_path") val logoPath: String? = null,
    @SerialName("origin_country") val originCountry: String = "",
)

@Serializable
data class CreatedBy(
    val id: Int = -1,
    val name: String = "",
    @SerialName("profile_path") val profilePath: String? = null,
)

@Serializable
data class SeasonSummary(
    val id: Long = -1,
    val name: String = "",
    @SerialName("season_number") val seasonNumber: Int = 0,
    @SerialName("episode_count") val episodeCount: Int = 0,
    @SerialName("air_date") val airDate: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    val overview: String = "",
    @SerialName("vote_average") val voteAverage: Double = 0.0,
)

@Serializable
data class Episode(
    val id: Long = -1,
    val name: String = "",
    val overview: String = "",
    @SerialName("air_date") val airDate: String? = null,
    @SerialName("episode_number") val episodeNumber: Int = 0,
    @SerialName("season_number") val seasonNumber: Int = 0,
    @SerialName("still_path") val stillPath: String? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    val runtime: Int? = null,
)

@Serializable
data class TvKeywordResponse(
    @SerialName("results") val keywords: List<Keyword> = emptyList(),
)

@Serializable
data class ContentRating(
    @SerialName("iso_3166_1") val iso3166: String = "",
    val rating: String = "",
)

@Serializable
data class ContentRatingsResponse(
    val results: List<ContentRating> = emptyList(),
)

@Serializable
data class ExternalIds(
    @SerialName("imdb_id") val imdbId: String? = null,
    @SerialName("facebook_id") val facebookId: String? = null,
    @SerialName("instagram_id") val instagramId: String? = null,
    @SerialName("twitter_id") val twitterId: String? = null,
    @SerialName("tvdb_id") val tvdbId: Long? = null,
)

@Serializable
data class TvDetail(
    val id: Long = -1,
    @SerialName("name") val title: String = "",
    val overview: String = "",
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("first_air_date") val firstAirDate: String = "",
    @SerialName("last_air_date") val lastAirDate: String? = null,
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
    @SerialName("external_ids") val externalIds: ExternalIds? = null,
)

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

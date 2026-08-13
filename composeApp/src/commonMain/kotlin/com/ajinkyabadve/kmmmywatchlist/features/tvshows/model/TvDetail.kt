package com.ajinkyabadve.kmmmywatchlist.features.tvshows.model

import com.ajinkyabadve.kmmmywatchlist.features.movies.model.BackdropImage
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.CastMember
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.Credits
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.Genre
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.ImagesResponse
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.Keyword
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.VideoResponse
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.WatchProvidersResponse
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
    @SerialName("wikidata_id") val wikidataId: String? = null,
    @SerialName("freebase_mid") val freebaseMid: String? = null,
    @SerialName("freebase_id") val freebaseId: String? = null,
    @SerialName("tvrage_id") val tvrageId: Long? = null,
    // Only returned for the person namespace; always null for movie/tv/season/episode.
    @SerialName("tiktok_id") val tiktokId: String? = null,
    @SerialName("youtube_id") val youtubeId: String? = null,
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
    // Same shape as the movie side, so one hero can resolve "where does this stream" for both.
    @SerialName("watch/providers") val watchProviders: WatchProvidersResponse? = null,
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

// TMDB's /tv/{id}/season/{season_number}/episode/{episode_number} images sub-resource returns a
// single "stills" bucket (no backdrops/posters split), unlike the movie/tv/season-level ImagesResponse.
@Serializable
data class EpisodeImagesResponse(
    val stills: List<BackdropImage> = emptyList(),
)

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

// TMDB's episode-level credits include a "guest_stars" bucket alongside cast/crew, unlike the
// movie/tv/season-level Credits which only expose cast.
@Serializable
data class EpisodeCredits(
    val cast: List<CastMember> = emptyList(),
    val crew: List<CrewMember> = emptyList(),
    @SerialName("guest_stars") val guestStars: List<CastMember> = emptyList(),
)

@Serializable
data class TranslationData(
    val name: String = "",
    val overview: String = "",
)

@Serializable
data class Translation(
    @SerialName("iso_3166_1") val iso3166: String = "",
    @SerialName("iso_639_1") val iso639: String = "",
    val name: String = "",
    @SerialName("english_name") val englishName: String = "",
    val data: TranslationData = TranslationData(),
)

@Serializable
data class TranslationsResponse(
    val translations: List<Translation> = emptyList(),
)

// Note: TMDB's episode sub-resource supports credits, external_ids, images, translations and
// videos as append_to_response values (per the episode-details OpenAPI definition; account_states
// additionally works but needs a user session). The base payload also carries its own top-level
// crew/guest_stars buckets with the same content as the appended credits.
@Serializable
data class EpisodeDetail(
    val id: Long = -1,
    val name: String = "",
    val overview: String = "",
    @SerialName("air_date") val airDate: String? = null,
    @SerialName("episode_number") val episodeNumber: Int = 0,
    @SerialName("season_number") val seasonNumber: Int = 0,
    @SerialName("episode_type") val episodeType: String? = null,
    @SerialName("production_code") val productionCode: String? = null,
    @SerialName("still_path") val stillPath: String? = null,
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    @SerialName("vote_count") val voteCount: Int = 0,
    val runtime: Int? = null,
    val crew: List<CrewMember> = emptyList(),
    val credits: EpisodeCredits? = null,
    val videos: VideoResponse? = null,
    val images: EpisodeImagesResponse? = null,
    @SerialName("external_ids") val externalIds: ExternalIds? = null,
    val translations: TranslationsResponse? = null,
) {
    // The top-level and credits-appended crew lists are the same data; prefer whichever arrived.
    val allCrew: List<CrewMember>
        get() = crew.ifEmpty { credits?.crew ?: emptyList() }
}

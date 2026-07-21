package com.ajinkyabadve.kmmmywatchlist.features.movies.model

import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.CrewMember
import com.ajinkyabadve.kmmmywatchlist.features.tvshows.model.ExternalIds
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Genre(
    val id: Int = -1,
    val name: String = ""
)

@Serializable
data class CastMember(
    val id: Int = -1,
    val name: String = "",
    @SerialName("profile_path") val profilePath: String? = null,
    val character: String = "",
    // Billing position within a movie's cast list; lower is more prominent.
    val order: Int = 999,
)

@Serializable
data class Credits(
    val cast: List<CastMember> = emptyList(),
    val crew: List<CrewMember> = emptyList(),
)

@Serializable
data class SpokenLanguage(
    @SerialName("english_name") val englishName: String = "",
    @SerialName("iso_639_1") val iso639: String = "",
    val name: String = "",
)

@Serializable
data class ProductionCompany(
    val id: Int = -1,
    val name: String = "",
    @SerialName("logo_path") val logoPath: String? = null,
    @SerialName("origin_country") val originCountry: String = "",
)

@Serializable
data class ProductionCountry(
    @SerialName("iso_3166_1") val iso3166: String = "",
    val name: String = "",
)

@Serializable
data class CollectionInfo(
    val id: Long = -1,
    val name: String = "",
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
)

@Serializable
data class WatchProvider(
    @SerialName("provider_id") val providerId: Int = -1,
    @SerialName("provider_name") val providerName: String = "",
    @SerialName("logo_path") val logoPath: String? = null,
    @SerialName("display_priority") val displayPriority: Int = 0,
)

// One region's providers from the watch/providers append. The link goes to TMDB's watch page,
// which carries the JustWatch attribution TMDB requires for this data.
@Serializable
data class RegionWatchProviders(
    val link: String = "",
    val flatrate: List<WatchProvider> = emptyList(),
    val rent: List<WatchProvider> = emptyList(),
    val buy: List<WatchProvider> = emptyList(),
    val free: List<WatchProvider> = emptyList(),
    val ads: List<WatchProvider> = emptyList(),
)

@Serializable
data class WatchProvidersResponse(
    val results: Map<String, RegionWatchProviders> = emptyMap(),
)

@Serializable
data class VideoResult(
    val id: String = "",
    val key: String = "",
    val name: String = "",
    val site: String = "",
    val type: String = "",
    val official: Boolean = false
)

@Serializable
data class VideoResponse(
    val results: List<VideoResult> = emptyList()
)

@Serializable
data class Review(
    val author: String = "",
    val content: String = "",
    val id: String = "",
    val url: String = ""
)

@Serializable
data class ReviewResponse(
    val results: List<Review> = emptyList()
)

@Serializable
data class Keyword(
    val id: Int = -1,
    val name: String = ""
)

@Serializable
data class KeywordResponse(
    val keywords: List<Keyword> = emptyList()
)

@Serializable
data class BackdropImage(
    @SerialName("file_path") val filePath: String,
    val width: Int = 0,
    val height: Int = 0
)

@Serializable
data class ImagesResponse(
    val backdrops: List<BackdropImage> = emptyList(),
    val posters: List<BackdropImage> = emptyList()
)

@Serializable
data class TranslationData(
    val title: String = "",
    val overview: String = ""
)

@Serializable
data class Translation(
    @SerialName("iso_3166_1") val iso3166: String = "",
    @SerialName("iso_639_1") val iso639: String = "",
    val name: String = "",
    @SerialName("english_name") val englishName: String = "",
    val data: TranslationData? = null
)

@Serializable
data class TranslationsResponse(
    val translations: List<Translation> = emptyList()
)

@Serializable
data class ReleaseDateItem(
    val certification: String = "",
    val note: String = "",
    @SerialName("release_date") val releaseDate: String = "",
    val type: Int = 0
)

@Serializable
data class ReleaseDatesResult(
    @SerialName("iso_3166_1") val iso3166: String = "",
    @SerialName("release_dates") val releaseDates: List<ReleaseDateItem> = emptyList()
)

@Serializable
data class ReleaseDatesResponse(
    val results: List<ReleaseDatesResult> = emptyList()
)

@Serializable
data class MovieDetail(
    val id: Long = -1,
    val title: String = "",
    @SerialName("original_title") val originalTitle: String? = null,
    val overview: String = "",
    val tagline: String? = null,
    val status: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("release_date") val releaseDate: String = "",
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    @SerialName("vote_count") val voteCount: Int = 0,
    val popularity: Double = 0.0,
    val runtime: Int? = null,
    val budget: Long? = null,
    val revenue: Long? = null,
    val homepage: String? = null,
    val genres: List<Genre>? = null,
    @SerialName("original_language") val originalLanguage: String = "",
    @SerialName("origin_country") val originCountry: List<String> = emptyList(),
    @SerialName("spoken_languages") val spokenLanguages: List<SpokenLanguage> = emptyList(),
    @SerialName("production_companies") val productionCompanies: List<ProductionCompany> = emptyList(),
    @SerialName("production_countries") val productionCountries: List<ProductionCountry> = emptyList(),
    @SerialName("belongs_to_collection") val belongsToCollection: CollectionInfo? = null,
    @SerialName("external_ids") val externalIds: ExternalIds? = null,
    @SerialName("watch/providers") val watchProviders: WatchProvidersResponse? = null,
    val credits: Credits? = null,
    val videos: VideoResponse? = null,
    val recommendations: MoviePageResult? = null,
    val similar: MoviePageResult? = null,
    val reviews: ReviewResponse? = null,
    val keywords: KeywordResponse? = null,
    val images: ImagesResponse? = null,
    val translations: TranslationsResponse? = null,
    @SerialName("release_dates") val releaseDates: ReleaseDatesResponse? = null
)




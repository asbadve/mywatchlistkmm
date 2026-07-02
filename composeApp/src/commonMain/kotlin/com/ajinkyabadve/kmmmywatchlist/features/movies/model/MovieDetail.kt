package com.ajinkyabadve.kmmmywatchlist.features.movies.model

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
    val character: String = ""
)

@Serializable
data class Credits(
    val cast: List<CastMember> = emptyList()
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
    val overview: String = "",
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("release_date") val releaseDate: String = "",
    @SerialName("vote_average") val voteAverage: Double = 0.0,
    val runtime: Int? = null,
    val budget: Long? = null,
    val genres: List<Genre>? = null,
    @SerialName("original_language") val originalLanguage: String = "",
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




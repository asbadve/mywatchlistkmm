package com.ajinkyabadve.kmmmywatchlist.features.tvshows.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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

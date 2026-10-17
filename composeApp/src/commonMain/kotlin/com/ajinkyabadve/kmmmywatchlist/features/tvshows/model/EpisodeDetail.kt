package com.ajinkyabadve.kmmmywatchlist.features.tvshows.model

import com.ajinkyabadve.kmmmywatchlist.features.movies.model.VideoResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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

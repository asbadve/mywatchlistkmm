package com.ajinkyabadve.kmmmywatchlist.features.tvshows.model

import com.ajinkyabadve.kmmmywatchlist.features.movies.model.CastMember
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// TMDB's episode-level credits include a "guest_stars" bucket alongside cast/crew, unlike the
// movie/tv/season-level Credits which only expose cast.
@Serializable
data class EpisodeCredits(
    val cast: List<CastMember> = emptyList(),
    val crew: List<CrewMember> = emptyList(),
    @SerialName("guest_stars") val guestStars: List<CastMember> = emptyList(),
)

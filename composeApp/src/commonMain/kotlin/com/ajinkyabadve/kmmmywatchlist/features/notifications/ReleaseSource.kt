package com.ajinkyabadve.kmmmywatchlist.features.notifications

/** Which kind of date a reminder fires on. [storageValue] is persisted in
 *  `releaseReminder.releaseSource`, so it must never change once shipped. */
enum class ReleaseSourceKind(
    val storageValue: String,
) {
    /** A regional theatrical release (TMDB release type 3, or 2 as a fallback). */
    THEATRICAL("theatrical"),

    /** A regional digital release (TMDB release type 4). */
    DIGITAL("digital"),

    /** The movie's primary `release_date`, used when no regional date exists. */
    PRIMARY("primary"),

    /** A show's `first_air_date`. */
    FIRST_AIR("first_air"),

    /** One episode's `air_date`. */
    EPISODE_AIR("episode_air"),
}

/** Which date won for a reminder, and in which region if it was a regional one - shown in the
 *  notification body so a date the user disagrees with is at least explicable. */
data class ReleaseSource(
    val kind: ReleaseSourceKind,
    val regionCode: String? = null,
) {
    fun encode(): String = if (regionCode == null) kind.storageValue else kind.storageValue + SEPARATOR + regionCode

    companion object {
        private const val SEPARATOR = ":"

        /** Parses a stored value; unknown or missing values read as [ReleaseSourceKind.PRIMARY]. */
        fun decode(value: String?): ReleaseSource {
            val parts = value?.split(SEPARATOR, limit = 2).orEmpty()
            val kind = ReleaseSourceKind.entries.firstOrNull { it.storageValue == parts.firstOrNull() } ?: ReleaseSourceKind.PRIMARY
            return ReleaseSource(kind = kind, regionCode = parts.getOrNull(1))
        }
    }
}

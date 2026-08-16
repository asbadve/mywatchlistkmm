package com.ajinkyabadve.kmmmywatchlist.features.movies.screen.detail

import com.ajinkyabadve.kmmmywatchlist.core.constant.RegionConstant
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.MovieDetail
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.RegionWatchProviders
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.WatchProvider
import com.ajinkyabadve.kmmmywatchlist.features.movies.model.WatchProvidersResponse

private object MovieHeroFactsConstant {
    const val MINUTES_PER_HOUR = 60
}

/**
 * Where a title can actually be watched, resolved down to the single best option.
 *
 * The hero's primary button promises one destination, so the ranking matters: a subscription a
 * viewer may already hold beats free-with-ads, which beats being asked to pay. Rent and buy come
 * last because they are the only options that cost money at the point of tapping.
 */
data class HeroWatchOption(
    val provider: WatchProvider,
    /** TMDB's watch page for the title, which carries the required JustWatch attribution. */
    val link: String,
    val allProviders: List<WatchProvider>,
)

/**
 * Picks the region the same way the "Where to watch" section does - the viewer's selected region,
 * falling back to their chosen fallback region and then to any region with data - so the hero
 * button and that section never disagree about which service they are talking about.
 */
fun MovieDetail.heroWatchOption(
    regionCode: String,
    fallbackRegionCode: String,
): HeroWatchOption? = watchProviders.heroWatchOption(regionCode, fallbackRegionCode)

/**
 * Picks which region's watch-provider bucket to show: the caller's selected region, falling back
 * to their chosen fallback region, then to any region with data - shared by the hero button and
 * the "Where to watch" section so they never disagree about which service they are talking about.
 */
fun WatchProvidersResponse?.resolveRegion(
    regionCode: String,
    fallbackRegionCode: String,
): RegionWatchProviders? {
    val regions = this?.results.orEmpty()
    if (regions.isEmpty()) return null
    return regions[regionCode] ?: regions[fallbackRegionCode] ?: regions.values.firstOrNull()
}

/** Shared by the movie and TV heroes, which resolve "where does this stream" identically. */
fun WatchProvidersResponse?.heroWatchOption(
    regionCode: String,
    fallbackRegionCode: String,
): HeroWatchOption? {
    val region = resolveRegion(regionCode, fallbackRegionCode) ?: return null

    val ranked =
        listOf(region.flatrate, region.free, region.ads, region.rent, region.buy)
            .firstOrNull { it.isNotEmpty() }
            ?.sortedBy { it.displayPriority }
            ?: return null

    return HeroWatchOption(
        provider = ranked.first(),
        link = region.link,
        allProviders = ranked,
    )
}

/** The age rating TMDB publishes for the US, which is the one release-dates bucket always present. */
fun MovieDetail.usCertification(): String? =
    releaseDates
        ?.results
        ?.firstOrNull { it.iso3166 == RegionConstant.US }
        ?.releaseDates
        ?.firstOrNull { it.certification.isNotEmpty() }
        ?.certification

/** Runtime split into whole hours and remaining minutes; null when TMDB has no runtime. */
fun MovieDetail.runtimeHoursAndMinutes(): Pair<Int, Int>? {
    val total = runtime?.takeIf { it > 0 } ?: return null
    return total / MovieHeroFactsConstant.MINUTES_PER_HOUR to total % MovieHeroFactsConstant.MINUTES_PER_HOUR
}

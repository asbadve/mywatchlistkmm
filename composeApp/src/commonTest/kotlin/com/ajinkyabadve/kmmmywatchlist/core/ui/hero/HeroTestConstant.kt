package com.ajinkyabadve.kmmmywatchlist.core.ui.hero

/**
 * Fixture values the movie and TV hero tests both assert on.
 *
 * The two heroes are one design with different facts in it, so their tests assert on the same
 * rendered labels. Keeping one copy is what stops a typo in either file producing a test that
 * passes while asserting the wrong text - and it lives beside [HeroConstant], the production object
 * these tests exercise, rather than in either feature package.
 */
object HeroTestConstant {
    /** Value of `Res.string.hero_play_trailer`, rendered as a label and as a content description. */
    const val PLAY_TRAILER = "Play trailer"

    /** Leading text of `Res.string.hero_watch_on`, matched on its own to assert no button exists. */
    const val WATCH_ON_PREFIX = "Watch on"

    const val PROVIDER_NETFLIX = "Netflix"

    /** What the primary button reads when [PROVIDER_NETFLIX] is the resolved option. */
    const val WATCH_ON_NETFLIX = "$WATCH_ON_PREFIX $PROVIDER_NETFLIX"

    /**
     * TMDB's watch page for the fixture title - what the primary button must open. One per media
     * type because the path differs, and the movie facts and hero tests both assert on the movie one.
     */
    const val MOVIE_WATCH_PAGE_LINK = "https://www.themoviedb.org/movie/1/watch"
    const val TV_WATCH_PAGE_LINK = "https://www.themoviedb.org/tv/1/watch"

    const val TRAILER_KEY = "trailerkey"

    /** The URL a trailer tap must open - the key assembled into a watchable YouTube link. */
    const val TRAILER_URL = "https://www.youtube.com/watch?v=$TRAILER_KEY"

    const val YOUTUBE_SITE = "YouTube"
    const val TRAILER_TYPE = "Trailer"

    /** Prefixes `Res.string.hero_rating`; its absence is how the tests assert "no facts shown". */
    const val RATING_STAR = "★"

    /** Value of [HeroConstant.META_SEPARATOR] trimmed to the character the facts row joins on. */
    const val FACT_SEPARATOR = "·"
}

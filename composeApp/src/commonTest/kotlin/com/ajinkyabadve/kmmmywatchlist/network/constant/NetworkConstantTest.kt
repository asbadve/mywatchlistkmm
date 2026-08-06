package com.ajinkyabadve.kmmmywatchlist.network.constant

import kotlin.test.Test
import kotlin.test.assertEquals

class NetworkConstantTest {
    /**
     * Regression test: the API host used to be `api.themoviedb.org`, which some ISPs (observed on
     * Reliance Jio, 2026-08-06) DNS-hijack at the zone level - they answer with a blackhole address
     * of their own, so every API call fails to connect and the app renders its layouts with every
     * card empty. `api.tmdb.org` is TMDB's alias for the same CloudFront distribution and is not on
     * those blocklists. Reverting this silently breaks the app on affected networks only, which is
     * exactly the kind of failure that does not show up in CI.
     */
    @Test
    fun testHostUsesTheUnblockedTmdbAlias() {
        assertEquals("api.tmdb.org", NetworkConstant.HOST)
    }
}

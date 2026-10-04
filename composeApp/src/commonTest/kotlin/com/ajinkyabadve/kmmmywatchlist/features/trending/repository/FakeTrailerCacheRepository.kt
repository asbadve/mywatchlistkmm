package com.ajinkyabadve.kmmmywatchlist.features.trending.repository

import com.ajinkyabadve.kmmmywatchlist.features.trending.model.Trailer
import com.ajinkyabadve.kmmmywatchlist.features.trending.model.TrailerSource

/** In-memory [TrailerCacheRepository]: every stored source counts as fresh, and puts are recorded.
 *  Keeps view-model tests off the real on-device settings store. */
class FakeTrailerCacheRepository : TrailerCacheRepository {
    val stored = mutableMapOf<TrailerSource, List<Trailer>>()
    val putCalls = mutableListOf<TrailerSource>()

    override fun get(source: TrailerSource): List<Trailer>? = stored[source]

    override fun put(
        source: TrailerSource,
        trailers: List<Trailer>,
    ) {
        putCalls.add(source)
        stored[source] = trailers
    }
}

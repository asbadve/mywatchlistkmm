package com.ajinkyabadve.kmmmywatchlist.features.movies.repository

import com.ajinkyabadve.kmmmywatchlist.features.movies.model.DiscoverFilters

class FakeDiscoverFilterRepository(
    private var movieFilters: DiscoverFilters = DiscoverFilters(),
    private var tvFilters: DiscoverFilters = DiscoverFilters(),
) : DiscoverFilterRepository {
    val setMovieFiltersCalls = mutableListOf<DiscoverFilters>()
    val setTvFiltersCalls = mutableListOf<DiscoverFilters>()

    override fun getSelectedMovieFilters(): DiscoverFilters = movieFilters

    override fun setSelectedMovieFilters(filters: DiscoverFilters) {
        setMovieFiltersCalls.add(filters)
        movieFilters = filters
    }

    override fun getSelectedTvFilters(): DiscoverFilters = tvFilters

    override fun setSelectedTvFilters(filters: DiscoverFilters) {
        setTvFiltersCalls.add(filters)
        tvFilters = filters
    }
}

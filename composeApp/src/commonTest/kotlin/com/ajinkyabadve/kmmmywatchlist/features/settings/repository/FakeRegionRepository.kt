package com.ajinkyabadve.kmmmywatchlist.features.settings.repository

import com.ajinkyabadve.kmmmywatchlist.features.settings.model.WatchProviderRegion

class FakeRegionRepository(
    var availableRegions: List<WatchProviderRegion> = emptyList(),
    private var selectedRegion: String = "US",
    private var fallbackRegion: String = "US",
) : RegionRepository {
    val setRegionCalls = mutableListOf<String>()
    val setFallbackRegionCalls = mutableListOf<String>()

    override suspend fun getAvailableRegions(): List<WatchProviderRegion> = availableRegions

    override fun getSelectedRegion(): String = selectedRegion

    override fun setSelectedRegion(regionCode: String) {
        setRegionCalls.add(regionCode)
        selectedRegion = regionCode
    }

    override fun getFallbackRegion(): String = fallbackRegion

    override fun setFallbackRegion(regionCode: String) {
        setFallbackRegionCalls.add(regionCode)
        fallbackRegion = regionCode
    }
}

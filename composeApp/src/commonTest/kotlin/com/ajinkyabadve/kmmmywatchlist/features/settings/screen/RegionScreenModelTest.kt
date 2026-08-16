package com.ajinkyabadve.kmmmywatchlist.features.settings.screen

import com.ajinkyabadve.kmmmywatchlist.features.settings.model.WatchProviderRegion
import com.ajinkyabadve.kmmmywatchlist.features.settings.repository.FakeRegionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

private object RegionScreenModelTestConstant {
    val UNITED_STATES = WatchProviderRegion(iso3166 = "US", englishName = "United States", nativeName = "United States")
    val INDIA = WatchProviderRegion(iso3166 = "IN", englishName = "India", nativeName = "India")
    val FRANCE = WatchProviderRegion(iso3166 = "FR", englishName = "France", nativeName = "France")
}

@OptIn(ExperimentalCoroutinesApi::class)
class RegionScreenModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialLoadPopulatesRegionsSortedByEnglishName() =
        runTest(testDispatcher) {
            val fakeRepository =
                FakeRegionRepository(
                    availableRegions =
                        listOf(
                            RegionScreenModelTestConstant.UNITED_STATES,
                            RegionScreenModelTestConstant.FRANCE,
                            RegionScreenModelTestConstant.INDIA,
                        ),
                )

            val viewModel = RegionScreenModel(fakeRepository)

            assertEquals(RegionLoadState.Loaded, viewModel.loadState)
            assertEquals(
                listOf("France", "India", "United States"),
                viewModel.filteredRegions.map { it.englishName },
            )
        }

    @Test
    fun testSearchQueryFiltersByEnglishNameOrCode() =
        runTest(testDispatcher) {
            val fakeRepository =
                FakeRegionRepository(
                    availableRegions =
                        listOf(RegionScreenModelTestConstant.UNITED_STATES, RegionScreenModelTestConstant.INDIA),
                )
            val viewModel = RegionScreenModel(fakeRepository)

            viewModel.onSearchQueryChanged("ind")

            assertEquals(listOf("IN"), viewModel.filteredRegions.map { it.iso3166 })
        }
}

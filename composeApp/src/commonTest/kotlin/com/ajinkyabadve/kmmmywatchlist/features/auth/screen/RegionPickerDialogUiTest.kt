package com.ajinkyabadve.kmmmywatchlist.features.auth.screen

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import com.ajinkyabadve.kmmmywatchlist.features.settings.model.WatchProviderRegion
import com.ajinkyabadve.kmmmywatchlist.features.settings.repository.FakeRegionRepository
import com.ajinkyabadve.kmmmywatchlist.features.settings.screen.RegionScreenModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private object RegionPickerDialogUiTestConstant {
    const val TEST_TITLE = "Select region"
    val UNITED_STATES = WatchProviderRegion(iso3166 = "US", englishName = "United States", nativeName = "United States")
    val INDIA = WatchProviderRegion(iso3166 = "IN", englishName = "India", nativeName = "India")
    val FRANCE = WatchProviderRegion(iso3166 = "FR", englishName = "France", nativeName = "France")
}

@OptIn(ExperimentalTestApi::class)
class RegionPickerDialogUiTest {
    @Test
    fun testRendersAllAvailableRegionsSortedByEnglishName() =
        runComposeUiTest {
            val fakeRepository =
                FakeRegionRepository(
                    availableRegions =
                        listOf(
                            RegionPickerDialogUiTestConstant.UNITED_STATES,
                            RegionPickerDialogUiTestConstant.FRANCE,
                            RegionPickerDialogUiTestConstant.INDIA,
                        ),
                )
            setContent {
                RegionPickerDialog(
                    title = RegionPickerDialogUiTestConstant.TEST_TITLE,
                    selectedRegionCode = "US",
                    onRegionSelected = {},
                    onDismiss = {},
                    regionScreenModel = RegionScreenModel(fakeRepository),
                )
            }

            onNodeWithText("United States", substring = true).assertExists()
            onNodeWithText("France", substring = true).assertExists()
            onNodeWithText("India", substring = true).assertExists()
        }

    @Test
    fun testSearchFiltersTheVisibleRegions() =
        runComposeUiTest {
            val fakeRepository =
                FakeRegionRepository(
                    availableRegions =
                        listOf(RegionPickerDialogUiTestConstant.UNITED_STATES, RegionPickerDialogUiTestConstant.INDIA),
                )
            setContent {
                RegionPickerDialog(
                    title = RegionPickerDialogUiTestConstant.TEST_TITLE,
                    selectedRegionCode = "US",
                    onRegionSelected = {},
                    onDismiss = {},
                    regionScreenModel = RegionScreenModel(fakeRepository),
                )
            }

            onNodeWithText("Search regions").performTextInput("Ind")

            onNodeWithText("India", substring = true).assertExists()
            onAllNodesWithText("United States", substring = true).assertCountEquals(0)
        }

    @Test
    fun testTappingARegionInvokesCallbackAndDismisses() =
        runComposeUiTest {
            val fakeRepository =
                FakeRegionRepository(
                    availableRegions = listOf(RegionPickerDialogUiTestConstant.UNITED_STATES, RegionPickerDialogUiTestConstant.INDIA),
                )
            var dismissed = false
            var pickedRegion: String? = null
            setContent {
                RegionPickerDialog(
                    title = RegionPickerDialogUiTestConstant.TEST_TITLE,
                    selectedRegionCode = "US",
                    onRegionSelected = { pickedRegion = it },
                    onDismiss = { dismissed = true },
                    regionScreenModel = RegionScreenModel(fakeRepository),
                )
            }

            onNodeWithText("India", substring = true).performClick()

            assertTrue(dismissed)
            assertEquals("IN", pickedRegion)
        }
}

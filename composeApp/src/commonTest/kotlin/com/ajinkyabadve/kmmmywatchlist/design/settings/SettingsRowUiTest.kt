package com.ajinkyabadve.kmmmywatchlist.design.settings

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class SettingsRowUiTest {
    @Test
    fun testSettingsRowRendersLabelDescriptionAndValue() =
        runComposeUiTest {
            setContent {
                SettingsRow(label = LABEL, description = DESCRIPTION, value = VALUE, onClick = {})
            }

            onNodeWithText(LABEL).assertExists()
            onNodeWithText(DESCRIPTION).assertExists()
            onNodeWithText(VALUE).assertExists()
        }

    @Test
    fun testSettingsRowWithoutOptionalTextsShowsOnlyTheLabel() =
        runComposeUiTest {
            setContent {
                SettingsRow(label = LABEL, onClick = {})
            }

            onNodeWithText(LABEL).assertExists()
            onNodeWithText(DESCRIPTION).assertDoesNotExist()
            onNodeWithText(VALUE).assertDoesNotExist()
        }

    @Test
    fun testTappingSettingsRowInvokesOnClick() =
        runComposeUiTest {
            var clicks = 0
            setContent {
                SettingsRow(label = LABEL, onClick = { clicks++ })
            }

            onNodeWithText(LABEL).performClick()

            assertEquals(1, clicks)
        }

    @Test
    fun testSwitchRowShowsItsCheckedState() =
        runComposeUiTest {
            setContent {
                SettingsSwitchRow(label = LABEL, description = DESCRIPTION, checked = true, onCheckedChange = {})
            }

            onNodeWithText(DESCRIPTION).assertExists()
            onNode(isToggleable()).assertIsOn()
        }

    @Test
    fun testTappingAnywhereOnTheSwitchRowReportsTheNewState() =
        runComposeUiTest {
            val reported = mutableListOf<Boolean>()
            setContent {
                SettingsSwitchRow(label = LABEL, checked = false, onCheckedChange = { reported.add(it) })
            }

            onNode(isToggleable()).assertIsOff()
            // The label, not the switch - the whole row is the toggle target.
            onNodeWithText(LABEL).performClick()

            assertEquals(listOf(true), reported)
        }

    private companion object {
        const val LABEL = "Restricted Mode"
        const val DESCRIPTION = "Hide adult titles"
        const val VALUE = "US"
    }
}

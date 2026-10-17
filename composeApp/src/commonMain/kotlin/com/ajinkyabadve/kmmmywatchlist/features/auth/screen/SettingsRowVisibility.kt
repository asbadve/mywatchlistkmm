package com.ajinkyabadve.kmmmywatchlist.features.auth.screen

/**
 * Which optional groups of [SettingsList] rows appear - each depends on the platform or build
 * type, so [AccountScreenModel] decides and the list only renders.
 */
data class SettingsRowVisibility(
    val releaseReminders: Boolean,
    val backup: Boolean,
    val debug: Boolean,
)

package com.ajinkyabadve.kmmmywatchlist.features.settings.repository

import com.ajinkyabadve.kmmmywatchlist.core.constant.TrendingAutoScrollConstant
import com.ajinkyabadve.kmmmywatchlist.createSettings
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

interface TrendingAutoScrollRepository {
    fun isTrendingAutoScrollEnabled(): Boolean

    fun setTrendingAutoScrollEnabled(enabled: Boolean)

    val isTrendingAutoScrollEnabledFlow: StateFlow<Boolean>
}

/**
 * Persists the trending-carousel autoscroll switch and exposes it as a [StateFlow], so an open
 * Trending tab reacts the moment the Account screen (or a backup restore) changes it.
 *
 * Platform equivalent: multiplatform-settings' `ObservableSettings` listeners (or its `-coroutines`
 * artifact's flow extensions). Not used because the web target's `StorageSettings` (1.0.0, the
 * version here) implements plain `Settings`, not `ObservableSettings`, so it can't be relied on in
 * common code. Instead one [MutableStateFlow] is shared per [Settings] instance - every
 * [createSettings] call returns the same store, so all repository instances see the same flow.
 *
 * Only writes made through [setTrendingAutoScrollEnabled] reach the flow: anything that changes the
 * setting (e.g. `BackupRepositoryImpl.restore`) must go through this repository, never write
 * [TrendingAutoScrollConstant.KEY_TRENDING_AUTO_SCROLL_ENABLED] directly.
 */
class TrendingAutoScrollRepositoryImpl(
    private val settings: Settings = createSettings(),
) : TrendingAutoScrollRepository {
    private val flow = getOrCreateFlow(settings, isTrendingAutoScrollEnabled())
    override val isTrendingAutoScrollEnabledFlow: StateFlow<Boolean> = flow.asStateFlow()

    override fun isTrendingAutoScrollEnabled(): Boolean =
        settings.getBoolean(
            TrendingAutoScrollConstant.KEY_TRENDING_AUTO_SCROLL_ENABLED,
            TrendingAutoScrollConstant.DEFAULT_TRENDING_AUTO_SCROLL_ENABLED,
        )

    override fun setTrendingAutoScrollEnabled(enabled: Boolean) {
        settings.putBoolean(TrendingAutoScrollConstant.KEY_TRENDING_AUTO_SCROLL_ENABLED, enabled)
        notifyChanged(settings, enabled)
    }

    private companion object {
        private val flowsBySettings = MutableStateFlow<Map<Settings, MutableStateFlow<Boolean>>>(emptyMap())

        private fun getOrCreateFlow(
            settings: Settings,
            initial: Boolean,
        ): MutableStateFlow<Boolean> {
            flowsBySettings.value[settings]?.let { return it }
            val newFlow = MutableStateFlow(initial)
            flowsBySettings.update { current ->
                if (current.containsKey(settings)) current else current + (settings to newFlow)
            }
            return flowsBySettings.value[settings] ?: newFlow
        }

        private fun notifyChanged(
            settings: Settings,
            enabled: Boolean,
        ) {
            flowsBySettings.value[settings]?.value = enabled
        }
    }
}

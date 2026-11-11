package com.ajinkyabadve.kmmmywatchlist.features.settings.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeTrendingAutoScrollRepository(
    initialEnabled: Boolean = true,
) : TrendingAutoScrollRepository {
    var enabled: Boolean = initialEnabled
        private set
    val setEnabledCalls = mutableListOf<Boolean>()

    private val flow = MutableStateFlow(initialEnabled)
    override val isTrendingAutoScrollEnabledFlow: StateFlow<Boolean> = flow.asStateFlow()

    override fun isTrendingAutoScrollEnabled(): Boolean = enabled

    override fun setTrendingAutoScrollEnabled(enabled: Boolean) {
        this.enabled = enabled
        setEnabledCalls.add(enabled)
        flow.value = enabled
    }
}

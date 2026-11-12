package com.ajinkyabadve.kmmmywatchlist.features.settings.repository

import com.ajinkyabadve.kmmmywatchlist.core.constant.TrendingAutoScrollConstant
import com.ajinkyabadve.kmmmywatchlist.features.auth.repository.FakeSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TrendingAutoScrollRepositoryImplTest {
    @Test
    fun testDefaultIsOn() {
        val settings = FakeSettings()
        val repository = TrendingAutoScrollRepositoryImpl(settings)

        assertTrue(repository.isTrendingAutoScrollEnabled())
        assertTrue(repository.isTrendingAutoScrollEnabledFlow.value)
    }

    @Test
    fun testSetTrendingAutoScrollEnabledPersistsAndUpdatesFlow() {
        val settings = FakeSettings()
        val repository = TrendingAutoScrollRepositoryImpl(settings)

        repository.setTrendingAutoScrollEnabled(false)

        assertFalse(repository.isTrendingAutoScrollEnabled())
        assertFalse(repository.isTrendingAutoScrollEnabledFlow.value)
        assertEquals(false, settings.getBooleanOrNull(TrendingAutoScrollConstant.KEY_TRENDING_AUTO_SCROLL_ENABLED))

        repository.setTrendingAutoScrollEnabled(true)

        assertTrue(repository.isTrendingAutoScrollEnabled())
        assertTrue(repository.isTrendingAutoScrollEnabledFlow.value)
        assertEquals(true, settings.getBooleanOrNull(TrendingAutoScrollConstant.KEY_TRENDING_AUTO_SCROLL_ENABLED))
    }

    @Test
    fun testMultipleInstancesStayInSyncViaFlow() {
        val settings = FakeSettings()
        val repo1 = TrendingAutoScrollRepositoryImpl(settings)
        val repo2 = TrendingAutoScrollRepositoryImpl(settings)

        repo1.setTrendingAutoScrollEnabled(false)

        assertFalse(repo2.isTrendingAutoScrollEnabledFlow.value)
    }

    @Test
    fun testDistinctSettingsInstancesDoNotPolluteEachOther() {
        val settings1 = FakeSettings()
        val settings2 = FakeSettings()
        val repo1 = TrendingAutoScrollRepositoryImpl(settings1)
        val repo2 = TrendingAutoScrollRepositoryImpl(settings2)

        repo1.setTrendingAutoScrollEnabled(false)

        assertFalse(repo1.isTrendingAutoScrollEnabledFlow.value)
        assertTrue(repo2.isTrendingAutoScrollEnabledFlow.value)
    }
}

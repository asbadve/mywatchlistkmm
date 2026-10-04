package com.ajinkyabadve.kmmmywatchlist.features.notifications.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

/** In-memory [ReleaseReminderRepository] that records writes, same shape as
 *  `FakeFavoriteCollectionRepository`. */
class FakeReleaseReminderRepository(
    initialPreference: ReminderPreference =
        ReminderPreference(enabled = ReleaseReminderConstant.DEFAULT_ENABLED, time = ReleaseReminderConstant.DEFAULT_TIME),
) : ReleaseReminderRepository {
    private val reminders = MutableStateFlow<Map<ReminderKey, ReleaseReminder>>(emptyMap())
    private val preferenceState = MutableStateFlow(initialPreference)

    val setReminderCalls = mutableListOf<Pair<ReminderKey, Boolean>>()
    val updateReleaseDateCalls = mutableListOf<Pair<ReminderKey, LocalDate>>()
    val setPreferenceCalls = mutableListOf<ReminderPreference>()
    var deleteAllCallCount = 0
        private set

    fun seed(vararg reminder: ReleaseReminder) {
        reminders.value = reminders.value + reminder.associateBy { it.key }
    }

    override fun observeHasReminder(key: ReminderKey): Flow<Boolean> = reminders.map { key in it }

    override fun observeReminderKeys(): Flow<Set<ReminderKey>> = reminders.map { it.keys }

    override suspend fun setReminder(
        reminder: ReleaseReminder,
        enabled: Boolean,
    ) {
        setReminderCalls.add(reminder.key to enabled)
        reminders.value = if (enabled) reminders.value + (reminder.key to reminder) else reminders.value - reminder.key
    }

    override suspend fun allReminders(): List<ReleaseReminder> = reminders.value.values.sortedBy { it.releaseDate }

    override suspend fun restore(reminders: List<ReleaseReminder>): Int {
        val added = reminders.filter { it.key !in this.reminders.value }
        this.reminders.value = this.reminders.value + added.associateBy { it.key }
        return added.size
    }

    override suspend fun updateReleaseDate(
        key: ReminderKey,
        releaseDate: LocalDate,
        releaseSource: String?,
    ) {
        updateReleaseDateCalls.add(key to releaseDate)
        val existing = reminders.value[key] ?: return
        reminders.value = reminders.value + (key to existing.copy(releaseDate = releaseDate, releaseSource = releaseSource))
    }

    override suspend fun deleteExpired(today: LocalDate) {
        val cutoff = today.minus(DatePeriod(days = ReleaseReminderConstant.EXPIRY_DAYS))
        reminders.value = reminders.value.filterValues { it.releaseDate >= cutoff }
    }

    override suspend fun deleteAll() {
        deleteAllCallCount++
        reminders.value = emptyMap()
    }

    override fun observePreference(): Flow<ReminderPreference> = preferenceState

    override suspend fun preference(): ReminderPreference = preferenceState.value

    override suspend fun setPreference(preference: ReminderPreference) {
        setPreferenceCalls.add(preference)
        preferenceState.value = preference
    }
}

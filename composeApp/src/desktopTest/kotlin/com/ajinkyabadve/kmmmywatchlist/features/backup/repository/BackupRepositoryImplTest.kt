package com.ajinkyabadve.kmmmywatchlist.features.backup.repository

import com.ajinkyabadve.kmmmywatchlist.core.constant.MediaTypeConstant
import com.ajinkyabadve.kmmmywatchlist.core.constant.RegionConstant
import com.ajinkyabadve.kmmmywatchlist.core.constant.RestrictedModeConstant
import com.ajinkyabadve.kmmmywatchlist.db.MyDatabase
import com.ajinkyabadve.kmmmywatchlist.db.createTestDatabase
import com.ajinkyabadve.kmmmywatchlist.features.auth.repository.AuthConstant
import com.ajinkyabadve.kmmmywatchlist.features.auth.repository.FakeSettings
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.FavoriteCollectionRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.features.notifications.repository.ReleaseReminder
import com.ajinkyabadve.kmmmywatchlist.features.notifications.repository.ReleaseReminderRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.features.notifications.repository.ReminderKey
import com.ajinkyabadve.kmmmywatchlist.features.notifications.repository.ReminderPreference
import com.ajinkyabadve.kmmmywatchlist.features.person.repository.FavoritePersonRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Real SQLite (in-memory) behind every repository, so the merge rules are checked against the
 *  actual `INSERT OR IGNORE` queries, not a fake's imitation of them. */
class BackupRepositoryImplTest {
    private class Device(
        val database: MyDatabase,
        val settings: FakeSettings,
        clock: Long,
    ) {
        val people = FavoritePersonRepositoryImpl(databaseProvider = { database }, now = { clock })
        val collections = FavoriteCollectionRepositoryImpl(databaseProvider = { database }, now = { clock })
        val reminders = ReleaseReminderRepositoryImpl(databaseProvider = { database }, now = { clock })
        val backup =
            BackupRepositoryImpl(
                favoritePersonRepository = people,
                favoriteCollectionRepository = collections,
                releaseReminderRepository = reminders,
                settings = settings,
                now = { clock },
            )
    }

    private suspend fun device(clock: Long = NOW) = Device(createTestDatabase(), FakeSettings(), clock)

    private suspend fun Device.seedEverything() {
        // Followed one at a time, so addedAt decides the order the Person tab shows.
        Device(database, settings, OLDER).people.setFavorite(OLDER_PERSON_ID, OLDER_PERSON_NAME, null, favorite = true)
        Device(database, settings, NEWER).people.setFavorite(NEWER_PERSON_ID, NEWER_PERSON_NAME, null, favorite = true)
        collections.setFavorite(COLLECTION_ID, COLLECTION_NAME, null, favorite = true)
        reminders.setReminder(movieReminder(), enabled = true)
        reminders.setPreference(ReminderPreference(enabled = true, time = REMINDER_TIME))
        settings.putString(RegionConstant.KEY_SELECTED_REGION, REGION)
        settings.putBoolean(RestrictedModeConstant.KEY_RESTRICTED_MODE_ENABLED, false)
    }

    private fun movieReminder() =
        ReleaseReminder(
            key = ReminderKey(REMINDER_MOVIE_ID, MediaTypeConstant.MOVIE),
            title = REMINDER_TITLE,
            posterPath = null,
            releaseDate = RELEASE_DATE,
            releaseSource = null,
        )

    private fun BackupRepository.decodeValid(json: String) = assertIs<BackupDecodeResult.Valid>(decode(json))

    @Test
    fun testExportThenRestore_carriesEverythingAndKeepsTheFollowOrder() =
        runTest {
            val oldPhone = device()
            oldPhone.seedEverything()
            val newPhone = device()

            val decoded = newPhone.backup.decodeValid(oldPhone.backup.exportToJson())
            val outcome = newPhone.backup.restore(decoded.envelope)

            assertEquals(RestoreOutcome(peopleAdded = 2, collectionsAdded = 1, remindersAdded = 1, settingsApplied = 3), outcome)
            assertEquals(
                listOf(NEWER_PERSON_NAME, OLDER_PERSON_NAME),
                newPhone.people
                    .observeFavoritePeople()
                    .first()
                    .map { it.name },
            )
            assertEquals(
                listOf(COLLECTION_NAME),
                newPhone.collections
                    .observeFavoriteCollections()
                    .first()
                    .map { it.name },
            )
            assertEquals(listOf(movieReminder()), newPhone.reminders.allReminders())
            assertEquals(REMINDER_TIME, newPhone.reminders.preference().time)
            assertEquals(REGION, newPhone.settings.getStringOrNull(RegionConstant.KEY_SELECTED_REGION))
            assertEquals(false, newPhone.settings.getBooleanOrNull(RestrictedModeConstant.KEY_RESTRICTED_MODE_ENABLED))
        }

    @Test
    fun testRestore_mergesAndNeverOverwritesWhatIsAlreadyOnTheDevice() =
        runTest {
            val oldPhone = device()
            oldPhone.seedEverything()
            val newPhone = device()
            newPhone.people.setFavorite(OLDER_PERSON_ID, LOCAL_NAME, null, favorite = true)

            val outcome = newPhone.backup.restore(newPhone.backup.decodeValid(oldPhone.backup.exportToJson()).envelope)

            assertEquals(1, outcome.peopleAdded)
            val names =
                newPhone.people
                    .observeFavoritePeople()
                    .first()
                    .map { it.name }
            assertTrue(LOCAL_NAME in names)
            assertFalse(OLDER_PERSON_NAME in names)
        }

    @Test
    fun testRestoredFollowsStartUnpolled_soTheNextPollFiresNothing() =
        runTest {
            val oldPhone = device()
            oldPhone.seedEverything()
            oldPhone.people.updateLastKnownCreditIds(OLDER_PERSON_ID, POLL_STATE)
            val newPhone = device()

            newPhone.backup.restore(newPhone.backup.decodeValid(oldPhone.backup.exportToJson()).envelope)

            assertTrue(newPhone.people.favoritePeopleForPolling().all { it.lastKnownCreditIds == null })
        }

    @Test
    fun testExport_neverContainsTheTmdbSession() =
        runTest {
            val phone = device()
            phone.seedEverything()
            phone.settings.putString(AuthConstant.KEY_SESSION_ID, SESSION_ID)
            phone.settings.putString(AuthConstant.KEY_USERNAME, USERNAME)

            val json = phone.backup.exportToJson()

            assertFalse(json.contains(AUTH_KEY_PREFIX))
            assertFalse(json.contains(SESSION_ID))
            assertFalse(json.contains(USERNAME))
        }

    @Test
    fun testRestore_onlyWritesSettingsTheFileHas() =
        runTest {
            val newPhone = device()
            newPhone.settings.putBoolean(RestrictedModeConstant.KEY_RESTRICTED_MODE_ENABLED, true)
            val emptyBackup = device().backup.exportToJson()

            newPhone.backup.restore(newPhone.backup.decodeValid(emptyBackup).envelope)

            assertEquals(true, newPhone.settings.getBooleanOrNull(RestrictedModeConstant.KEY_RESTRICTED_MODE_ENABLED))
            assertNull(newPhone.settings.getStringOrNull(RegionConstant.KEY_SELECTED_REGION))
        }

    @Test
    fun testDecode_aNewerFormatIsRefusedNotHalfRestored() =
        runTest {
            val result = device().backup.decode(FUTURE_FORMAT_JSON)

            assertEquals(BackupDecodeResult.UnsupportedFormat(found = FUTURE_FORMAT, supported = BackupConstant.FORMAT), result)
        }

    @Test
    fun testDecode_garbageAndInvalidFieldsAreMalformed() =
        runTest {
            val backup = device().backup

            listOf(NOT_JSON, JSON_ARRAY, MISSING_FORMAT_JSON, BLANK_NAME_JSON, BAD_REGION_JSON, BAD_REMINDER_DATE_JSON).forEach { json ->
                assertEquals(BackupDecodeResult.Malformed, backup.decode(json), json)
            }
        }

    @Test
    fun testDecode_unknownFieldsFromANewerBuildAreIgnored() =
        runTest {
            val decoded = device().backup.decodeValid(UNKNOWN_FIELDS_JSON)

            assertEquals(1, decoded.summary.people)
        }

    private companion object {
        const val NOW = 3_000L
        const val OLDER = 1_000L
        const val NEWER = 2_000L
        const val OLDER_PERSON_ID = 10L
        const val OLDER_PERSON_NAME = "Rebecca Ferguson"
        const val NEWER_PERSON_ID = 11L
        const val NEWER_PERSON_NAME = "Gary Oldman"
        const val LOCAL_NAME = "Already followed here"
        const val COLLECTION_ID = 20L
        const val COLLECTION_NAME = "Mission: Impossible Collection"
        const val REMINDER_MOVIE_ID = 30L
        const val REMINDER_TITLE = "Other Mommy"
        val RELEASE_DATE = LocalDate(2099, 10, 9)
        val REMINDER_TIME = LocalTime(18, 30)
        const val REGION = "DE"
        const val POLL_STATE = "1,2,3"
        const val SESSION_ID = "secret-session-value"
        const val USERNAME = "jane_doe"
        const val AUTH_KEY_PREFIX = "auth_"
        const val FUTURE_FORMAT = 99
        const val FUTURE_FORMAT_JSON = """{"format":99,"exportedAt":0,"people":"a shape this build can't read"}"""
        const val NOT_JSON = "this is not a backup"
        const val JSON_ARRAY = "[1, 2, 3]"
        const val MISSING_FORMAT_JSON = """{"exportedAt":0}"""
        const val BLANK_NAME_JSON = """{"format":1,"exportedAt":0,"people":[{"id":1,"name":" ","addedAt":0}]}"""
        const val BAD_REGION_JSON = """{"format":1,"exportedAt":0,"settings":{"selectedRegion":"Germany"}}"""
        const val BAD_REMINDER_DATE_JSON =
            """{"format":1,"exportedAt":0,"reminders":[{"mediaId":1,"mediaType":"movie","title":"T","releaseDate":"soon"}]}"""
        const val UNKNOWN_FIELDS_JSON =
            """{"format":1,"exportedAt":0,"newThing":true,"people":[{"id":1,"name":"P","addedAt":0,"nickname":"x"}]}"""
    }
}

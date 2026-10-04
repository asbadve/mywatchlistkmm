package com.ajinkyabadve.kmmmywatchlist.features.backup.screen

import com.ajinkyabadve.kmmmywatchlist.features.backup.repository.BackupSummary
import com.ajinkyabadve.kmmmywatchlist.features.backup.repository.RestoreOutcome
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
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class BackupScreenModelTest {
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private suspend fun backupWithOnePersonAndCollection(): String {
        val oldPhone = BackupTestFixture()
        oldPhone.people.seedFavorite(BackupTestConstant.PERSON_ID, BackupTestConstant.PERSON_NAME)
        oldPhone.collections.seedFavorite(BackupTestConstant.COLLECTION_ID, BackupTestConstant.COLLECTION_NAME)
        return oldPhone.repository.exportToJson()
    }

    @Test
    fun testSuggestedFileName_isDated() {
        assertEquals(BackupTestConstant.FILE_NAME, BackupTestFixture().screenModel.suggestedFileName())
    }

    @Test
    fun testCancellingThePicker_returnsToTheChoiceWithoutAnError() {
        val screenModel = BackupTestFixture().screenModel
        screenModel.open()

        screenModel.onFilePicked(null)

        assertEquals(BackupUiState.Choosing, screenModel.uiState.value)
    }

    @Test
    fun testANonBackupFile_isReportedAndNothingIsRestored() {
        val newPhone = BackupTestFixture()

        newPhone.screenModel.onFilePicked(BackupTestConstant.NOT_A_BACKUP)

        assertEquals(BackupUiState.Malformed, newPhone.screenModel.uiState.value)
    }

    @Test
    fun testAValidFile_asksFirstThenRestoresAndReschedules() =
        runTest {
            val newPhone = BackupTestFixture()

            newPhone.screenModel.onFilePicked(backupWithOnePersonAndCollection())
            val confirm = assertIs<BackupUiState.ConfirmRestore>(newPhone.screenModel.uiState.value)
            assertEquals(BackupSummary(people = 1, collections = 1, reminders = 0, settings = 1), confirm.summary)

            newPhone.screenModel.confirmRestore()

            assertEquals(
                BackupUiState.Restored(RestoreOutcome(peopleAdded = 1, collectionsAdded = 1, remindersAdded = 0, settingsApplied = 1)),
                newPhone.screenModel.uiState.value,
            )
            assertEquals(listOf(BackupTestConstant.PERSON_ID), newPhone.people.favoritePeopleForPolling().map { it.id })
        }

    @Test
    fun testExport_reportsWhetherTheFileWasSaved() =
        runTest {
            val screenModel = BackupTestFixture().screenModel

            screenModel.buildExport()
            screenModel.onExportFinished(saved = false)

            assertEquals(BackupUiState.ExportFailed, screenModel.uiState.value)
        }
}

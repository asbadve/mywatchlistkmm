package com.ajinkyabadve.kmmmywatchlist.features.backup.screen

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import com.ajinkyabadve.kmmmywatchlist.core.file.FakeBackupFileLauncher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class BackupRestoreDialogsUiTest {
    private fun exportedBackup(): String {
        val oldPhone = BackupTestFixture()
        oldPhone.people.seedFavorite(BackupTestConstant.PERSON_ID, BackupTestConstant.PERSON_NAME)
        var json = ""
        runTest { json = oldPhone.repository.exportToJson() }
        return json
    }

    @Test
    fun testRestoreFlow_confirmsWithCountsThenShowsTheResult() {
        val backup = exportedBackup()
        runComposeUiTest {
            val newPhone = BackupTestFixture()
            var restoredCallbacks = 0
            setContent {
                BackupRestoreDialogs(
                    screenModel = newPhone.screenModel,
                    fileLauncher = FakeBackupFileLauncher(openResult = backup),
                    onRestored = { restoredCallbacks++ },
                )
            }
            newPhone.screenModel.open()

            onNodeWithText(RESTORE_FROM_FILE).performClick()
            waitUntil { hasNode(CONFIRM_MESSAGE) }
            onNodeWithText(RESTORE).performClick()

            waitUntil { hasNode(RESULT_MESSAGE) }
            assertEquals(1, restoredCallbacks)
        }
    }

    @Test
    fun testANonBackupFile_showsTheErrorAndChangesNothing() =
        runComposeUiTest {
            val newPhone = BackupTestFixture()
            setContent {
                BackupRestoreDialogs(
                    screenModel = newPhone.screenModel,
                    fileLauncher = FakeBackupFileLauncher(openResult = BackupTestConstant.NOT_A_BACKUP),
                    onRestored = {},
                )
            }
            newPhone.screenModel.open()

            onNodeWithText(RESTORE_FROM_FILE).performClick()

            waitUntil { hasNode(MALFORMED_MESSAGE) }
        }

    @Test
    fun testExport_savesADatedFileAndConfirms() =
        runComposeUiTest {
            val phone = BackupTestFixture()
            val launcher = FakeBackupFileLauncher()
            setContent { BackupRestoreDialogs(screenModel = phone.screenModel, fileLauncher = launcher, onRestored = {}) }
            phone.screenModel.open()

            onNodeWithText(EXPORT_BACKUP).performClick()

            waitUntil { hasNode(EXPORT_SUCCESS) }
            assertEquals(BackupTestConstant.FILE_NAME, launcher.saved.single().first)
            assertTrue(
                launcher.saved
                    .single()
                    .second
                    .contains(FORMAT_FIELD),
            )
        }

    private fun ComposeUiTest.hasNode(text: String): Boolean =
        onAllNodes(hasText(text, substring = true)).fetchSemanticsNodes().isNotEmpty()

    private companion object {
        const val RESTORE_FROM_FILE = "Restore from file"
        const val EXPORT_BACKUP = "Export backup"
        const val RESTORE = "Restore"
        const val CONFIRM_MESSAGE = "People: 1\nCollections: 0\nRelease reminders: 0\nSettings: 1"
        const val RESULT_MESSAGE = "People added: 1"
        const val MALFORMED_MESSAGE = "This file isn't a MyWatchList backup"
        const val EXPORT_SUCCESS = "Backup saved."
        const val FORMAT_FIELD = "\"format\""
    }
}

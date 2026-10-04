package com.ajinkyabadve.kmmmywatchlist.core.file

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mywatchlist.composeapp.generated.resources.Res
import mywatchlist.composeapp.generated.resources.backup_export_action
import mywatchlist.composeapp.generated.resources.backup_restore_action
import org.jetbrains.compose.resources.getString
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.io.IOException

private class DesktopBackupFileLauncher : BackupFileLauncher {
    // Called from the composition's coroutine scope, i.e. the Swing event thread AWT dialogs need;
    // a modal FileDialog runs its own event loop, so the window keeps painting meanwhile.
    override suspend fun save(
        suggestedFileName: String,
        content: String,
    ): Boolean {
        val dialog = FileDialog(null as Frame?, getString(Res.string.backup_export_action), FileDialog.SAVE)
        dialog.file = suggestedFileName
        dialog.isVisible = true
        val directory = dialog.directory ?: return false
        val name = dialog.file ?: return false
        return withContext(Dispatchers.IO) {
            try {
                File(directory, name).writeText(content)
                true
            } catch (e: IOException) {
                false
            } catch (e: SecurityException) {
                false
            }
        }
    }

    override suspend fun open(): String? {
        val dialog = FileDialog(null as Frame?, getString(Res.string.backup_restore_action), FileDialog.LOAD)
        dialog.isVisible = true
        val directory = dialog.directory ?: return null
        val name = dialog.file ?: return null
        return withContext(Dispatchers.IO) {
            try {
                File(directory, name).readText()
            } catch (e: IOException) {
                null
            } catch (e: SecurityException) {
                null
            }
        }
    }
}

@Composable
actual fun rememberBackupFileLauncher(): BackupFileLauncher = remember { DesktopBackupFileLauncher() }

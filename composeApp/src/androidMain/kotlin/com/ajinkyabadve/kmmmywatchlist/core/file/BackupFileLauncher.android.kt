package com.ajinkyabadve.kmmmywatchlist.core.file

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.ajinkyabadve.kmmmywatchlist.features.backup.repository.BackupConstant
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.coroutines.resume

private class AndroidBackupFileLauncher(
    private val context: Context,
    private val launchCreate: (String, CancellableContinuation<Uri?>) -> Unit,
    private val launchOpen: (CancellableContinuation<Uri?>) -> Unit,
) : BackupFileLauncher {
    override suspend fun save(
        suggestedFileName: String,
        content: String,
    ): Boolean {
        val uri = suspendCancellableCoroutine<Uri?> { continuation -> launchCreate(suggestedFileName, continuation) } ?: return false
        return withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { it.write(content.toByteArray()) } != null
            } catch (e: IOException) {
                false
            } catch (e: SecurityException) {
                false
            }
        }
    }

    override suspend fun open(): String? {
        val uri = suspendCancellableCoroutine<Uri?> { continuation -> launchOpen(continuation) } ?: return null
        return withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
            } catch (e: IOException) {
                null
            } catch (e: SecurityException) {
                null
            }
        }
    }
}

@Composable
actual fun rememberBackupFileLauncher(): BackupFileLauncher {
    val context = LocalContext.current
    var pendingCreate by remember { mutableStateOf<CancellableContinuation<Uri?>?>(null) }
    var pendingOpen by remember { mutableStateOf<CancellableContinuation<Uri?>?>(null) }
    val createLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(BackupConstant.MIME_TYPE)) { uri ->
            pendingCreate?.resume(uri)
            pendingCreate = null
        }
    val openLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            pendingOpen?.resume(uri)
            pendingOpen = null
        }
    return remember(context, createLauncher, openLauncher) {
        AndroidBackupFileLauncher(
            context = context.applicationContext,
            launchCreate = { fileName, continuation ->
                pendingCreate = continuation
                createLauncher.launch(fileName)
            },
            launchOpen = { continuation ->
                pendingOpen = continuation
                // Some file providers label a .json file as octet-stream or text/plain, so a strict
                // application/json filter would grey out the user's own backup.
                openLauncher.launch(AndroidBackupFileConstant.OPEN_MIME_TYPES)
            },
        )
    }
}

private object AndroidBackupFileConstant {
    val OPEN_MIME_TYPES = arrayOf(BackupConstant.MIME_TYPE, "text/plain", "application/octet-stream")
}

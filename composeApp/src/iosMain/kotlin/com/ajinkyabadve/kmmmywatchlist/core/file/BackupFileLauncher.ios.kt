package com.ajinkyabadve.kmmmywatchlist.core.file

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.stringWithContentsOfURL
import platform.Foundation.writeToURL
import platform.UIKit.UIApplication
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIViewController
import platform.UniformTypeIdentifiers.UTTypeJSON
import platform.darwin.NSObject
import kotlin.coroutines.resume

/** Resumes once with the picked URL, or null on cancel. Held in a field while the picker is up -
 *  UIKit keeps only a weak reference to its delegate. */
private class PickerDelegate(
    private val onResult: (NSURL?) -> Unit,
) : NSObject(),
    UIDocumentPickerDelegateProtocol {
    override fun documentPicker(
        controller: UIDocumentPickerViewController,
        didPickDocumentsAtURLs: List<*>,
    ) {
        onResult(didPickDocumentsAtURLs.firstOrNull() as? NSURL)
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        onResult(null)
    }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private class IosBackupFileLauncher : BackupFileLauncher {
    private var activeDelegate: PickerDelegate? = null

    override suspend fun save(
        suggestedFileName: String,
        content: String,
    ): Boolean {
        // The exporting picker copies an existing file, so write it to tmp first.
        val tempUrl = NSURL.fileURLWithPath(NSTemporaryDirectory() + suggestedFileName)
        val written =
            NSString
                .create(
                    string = content,
                ).writeToURL(tempUrl, atomically = true, encoding = NSUTF8StringEncoding, error = null)
        if (!written) return false
        val picker = UIDocumentPickerViewController(forExportingURLs = listOf(tempUrl), asCopy = true)
        return present(picker) != null
    }

    override suspend fun open(): String? {
        // asCopy = true copies the file into the app's sandbox, so no security-scoped access is needed.
        val picker = UIDocumentPickerViewController(forOpeningContentTypes = listOf(UTTypeJSON), asCopy = true)
        val url = present(picker) ?: return null
        return NSString.stringWithContentsOfURL(url, encoding = NSUTF8StringEncoding, error = null)
    }

    private suspend fun present(picker: UIDocumentPickerViewController): NSURL? =
        suspendCancellableCoroutine { continuation ->
            val delegate =
                PickerDelegate { url ->
                    activeDelegate = null
                    if (continuation.isActive) continuation.resume(url)
                }
            activeDelegate = delegate
            picker.delegate = delegate
            val presenter = topViewController()
            if (presenter == null) {
                activeDelegate = null
                continuation.resume(null)
            } else {
                presenter.presentViewController(picker, animated = true, completion = null)
            }
        }

    private fun topViewController(): UIViewController? {
        var controller = UIApplication.sharedApplication.keyWindow?.rootViewController
        while (controller?.presentedViewController != null) controller = controller.presentedViewController
        return controller
    }
}

@Composable
actual fun rememberBackupFileLauncher(): BackupFileLauncher = remember { IosBackupFileLauncher() }

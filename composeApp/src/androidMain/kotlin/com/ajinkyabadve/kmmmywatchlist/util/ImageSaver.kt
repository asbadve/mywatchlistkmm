package com.ajinkyabadve.kmmmywatchlist.util

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.ajinkyabadve.kmmmywatchlist.AndroidApp
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

private object ImageSaverConstant {
    const val TAG = "ImageSaver"
    const val APP_FOLDER = "MyWatchList"
    const val MIME_TYPE_JPEG = "image/jpeg"
    const val PATH_SEPARATOR = "/"
    const val PENDING = 1
    const val NOT_PENDING = 0
}

actual class ImageSaver actual constructor() {
    actual suspend fun saveImage(
        bytes: ByteArray,
        fileName: String,
    ): String? =
        withContext(Dispatchers.IO) {
            try {
                val context = AndroidApp.instance
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    saveToPublicPictures(context, bytes, fileName)
                } else {
                    saveViaMediaStore(context, bytes, fileName)
                }
            } catch (e: IOException) {
                Napier.e("Saving image failed", e, tag = ImageSaverConstant.TAG)
                null
            } catch (e: SecurityException) {
                Napier.e("Saving image not permitted", e, tag = ImageSaverConstant.TAG)
                null
            } catch (e: IllegalStateException) {
                Napier.e("Saving image failed", e, tag = ImageSaverConstant.TAG)
                null
            }
        }

    /**
     * Android 9 and older: write into the public Pictures folder and ask the media scanner to index
     * it (`MediaScannerConnection.scanFile`, which replaced the deprecated
     * `ACTION_MEDIA_SCANNER_SCAN_FILE` broadcast). Falls back to the app's own Pictures folder when
     * the public one can't be written.
     */
    private fun saveToPublicPictures(
        context: Context,
        bytes: ByteArray,
        fileName: String,
    ): String? {
        val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        if (!directory.exists()) directory.mkdirs()
        val file = File(directory, fileName)
        return try {
            file.writeBytes(bytes)
            MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), null, null)
            Environment.DIRECTORY_PICTURES + ImageSaverConstant.PATH_SEPARATOR + fileName
        } catch (e: IOException) {
            saveToAppPictures(context, bytes, fileName)
        } catch (e: SecurityException) {
            saveToAppPictures(context, bytes, fileName)
        }
    }

    private fun saveToAppPictures(
        context: Context,
        bytes: ByteArray,
        fileName: String,
    ): String? {
        val appFile = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), fileName)
        return try {
            appFile.writeBytes(bytes)
            appRelativePath(fileName)
        } catch (e: IOException) {
            null
        } catch (e: SecurityException) {
            null
        }
    }

    /** Android 10+: scoped storage - insert through MediaStore, marked pending until written. */
    private fun saveViaMediaStore(
        context: Context,
        bytes: ByteArray,
        fileName: String,
    ): String? {
        val contentResolver = context.contentResolver
        val contentValues =
            ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, ImageSaverConstant.MIME_TYPE_JPEG)
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + ImageSaverConstant.PATH_SEPARATOR + ImageSaverConstant.APP_FOLDER,
                )
                put(MediaStore.MediaColumns.IS_PENDING, ImageSaverConstant.PENDING)
            }
        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues) ?: return null
        contentResolver.openOutputStream(uri)?.use { outputStream -> outputStream.write(bytes) }
        contentValues.clear()
        contentValues.put(MediaStore.MediaColumns.IS_PENDING, ImageSaverConstant.NOT_PENDING)
        contentResolver.update(uri, contentValues, null, null)
        return appRelativePath(fileName)
    }

    /** "Pictures/MyWatchList/<file>" - what the caller shows the user as the save location. */
    private fun appRelativePath(fileName: String): String =
        listOf(Environment.DIRECTORY_PICTURES, ImageSaverConstant.APP_FOLDER, fileName).joinToString(ImageSaverConstant.PATH_SEPARATOR)
}

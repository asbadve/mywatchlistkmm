package com.ajinkyabadve.kmmmywatchlist.util

import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.ajinkyabadve.kmmmywatchlist.AndroidApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual class ImageSaver actual constructor() {
    actual suspend fun saveImage(
        bytes: ByteArray,
        fileName: String,
    ): String? =
        withContext(Dispatchers.IO) {
            try {
                val context = AndroidApp.instance
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                    if (!directory.exists()) directory.mkdirs()
                    val file = java.io.File(directory, fileName)
                    try {
                        file.writeBytes(bytes)
                        val mediaScanIntent = android.content.Intent(android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                        mediaScanIntent.data = android.net.Uri.fromFile(file)
                        context.sendBroadcast(mediaScanIntent)
                        "Pictures/$fileName"
                    } catch (e: java.io.IOException) {
                        val appDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                        val appFile = java.io.File(appDir, fileName)
                        try {
                            appFile.writeBytes(bytes)
                            "Pictures/MyWatchList/$fileName"
                        } catch (ex: java.io.IOException) {
                            null
                        }
                    } catch (e: SecurityException) {
                        val appDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                        val appFile = java.io.File(appDir, fileName)
                        try {
                            appFile.writeBytes(bytes)
                            "Pictures/MyWatchList/$fileName"
                        } catch (ex: SecurityException) {
                            null
                        }
                    }
                } else {
                    val contentResolver = context.contentResolver
                    val contentValues =
                        ContentValues().apply {
                            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/MyWatchList")
                            put(MediaStore.MediaColumns.IS_PENDING, 1)
                        }

                    val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    if (uri != null) {
                        contentResolver.openOutputStream(uri)?.use { outputStream ->
                            outputStream.write(bytes)
                        }
                        contentValues.clear()
                        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        contentResolver.update(uri, contentValues, null, null)
                        "Pictures/MyWatchList/$fileName"
                    } else {
                        null
                    }
                }
            } catch (e: java.io.IOException) {
                e.printStackTrace()
                null
            } catch (e: SecurityException) {
                e.printStackTrace()
                null
            } catch (e: IllegalStateException) {
                e.printStackTrace()
                null
            }
        }
}

package com.ajinkyabadve.kmmmywatchlist.util

import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.ajinkyabadve.kmmmywatchlist.AndroidApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual class ImageSaver actual constructor() {
    actual suspend fun saveImage(bytes: ByteArray, fileName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val context = AndroidApp.INSTANCE
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                val directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                if (!directory.exists()) directory.mkdirs()
                val file = java.io.File(directory, fileName)
                try {
                    file.writeBytes(bytes)
                    val mediaScanIntent = android.content.Intent(android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                    mediaScanIntent.data = android.net.Uri.fromFile(file)
                    context.sendBroadcast(mediaScanIntent)
                    true
                } catch (e: java.io.IOException) {
                    val appDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                    val appFile = java.io.File(appDir, fileName)
                    try {
                        appFile.writeBytes(bytes)
                        true
                    } catch (ex: java.io.IOException) {
                        false
                    }
                } catch (e: SecurityException) {
                    val appDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                    val appFile = java.io.File(appDir, fileName)
                    try {
                        appFile.writeBytes(bytes)
                        true
                    } catch (ex: SecurityException) {
                        false
                    }
                }
            } else {
                val contentResolver = context.contentResolver
                val contentValues = ContentValues().apply {
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
                    true
                } else {
                    false
                }
            }
        } catch (e: java.io.IOException) {
            e.printStackTrace()
            false
        } catch (e: SecurityException) {
            e.printStackTrace()
            false
        } catch (e: IllegalStateException) {
            e.printStackTrace()
            false
        }
    }
}

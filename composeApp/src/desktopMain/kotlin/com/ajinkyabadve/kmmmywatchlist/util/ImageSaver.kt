package com.ajinkyabadve.kmmmywatchlist.util

import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual class ImageSaver actual constructor() {
    actual suspend fun saveImage(bytes: ByteArray, fileName: String): String? = withContext(Dispatchers.IO) {
        try {
            val userHome = System.getProperty("user.home")
            val downloadsDir = File(userHome, "Downloads")
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            val destinationFile = File(downloadsDir, fileName)
            destinationFile.writeBytes(bytes)
            "Downloads/$fileName"
        } catch (e: java.io.IOException) {
            e.printStackTrace()
            null
        } catch (e: SecurityException) {
            e.printStackTrace()
            null
        }
    }
}

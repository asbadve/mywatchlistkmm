package com.ajinkyabadve.kmmmywatchlist.util

expect class ImageSaver() {
    suspend fun saveImage(bytes: ByteArray, fileName: String): String?
}

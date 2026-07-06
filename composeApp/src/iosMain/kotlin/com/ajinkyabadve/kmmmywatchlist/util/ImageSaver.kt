package com.ajinkyabadve.kmmmywatchlist.util

import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIImage
import platform.UIKit.UIImageWriteToSavedPhotosAlbum
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned

actual class ImageSaver actual constructor() {
    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    actual suspend fun saveImage(bytes: ByteArray, fileName: String): Boolean {
        return try {
            if (bytes.isEmpty()) return false
            val nsData = bytes.usePinned { pinned ->
                NSData.create(
                    bytes = pinned.addressOf(0),
                    length = bytes.size.toULong()
                )
            }
            val uiImage = UIImage.imageWithData(nsData)
            if (uiImage != null) {
                UIImageWriteToSavedPhotosAlbum(uiImage, null, null, null)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

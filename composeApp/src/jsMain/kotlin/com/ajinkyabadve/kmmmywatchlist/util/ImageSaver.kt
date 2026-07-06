package com.ajinkyabadve.kmmmywatchlist.util

import org.khronos.webgl.Int8Array
import org.w3c.dom.HTMLAnchorElement
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag
import kotlinx.browser.document

actual class ImageSaver actual constructor() {
    actual suspend fun saveImage(bytes: ByteArray, fileName: String): String? {
        return try {
            val bytesTyped = Int8Array(bytes.size)
            for (i in bytes.indices) {
                bytesTyped[i] = bytes[i]
            }
            val blob = Blob(arrayOf(bytesTyped), BlobPropertyBag(type = "image/jpeg"))
            val url = createObjectURL(blob)
            val link = document.createElement("a") as HTMLAnchorElement
            link.href = url
            link.download = fileName
            document.body?.appendChild(link)
            link.click()
            document.body?.removeChild(link)
            revokeObjectURL(url)
            "Downloads"
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun createObjectURL(blob: Blob): String {
        return js("URL.createObjectURL(blob)") as String
    }

    private fun revokeObjectURL(url: String) {
        js("URL.revokeObjectURL(url)")
    }
}

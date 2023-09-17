package com.ajinkyabadve.kmmmywatchlist

import com.ajinkyabadve.kmmmywatchlist.util.commonConfig
import com.seiko.imageloader.ImageLoader
import com.seiko.imageloader.component.setupDefaultComponents
import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js
import io.ktor.http.HttpHeaders
import kotlinx.atomicfu.TraceBase.None.append
import kotlinx.browser.window
import okio.FileSystem
import okio.fakefilesystem.FakeFileSystem

internal actual fun openUrl(url: String?) {
    url?.let { window.open(it) }
}

internal actual fun generateImageLoader(): ImageLoader {
    return ImageLoader {
        commonConfig()
        components {
            setupDefaultComponents(
                httpClient = {
                    HttpClient(Js.create()).config {
                        append(HttpHeaders.AccessControlAllowOrigin, "*")
                    }
                },
            )
        }
        interceptor {
//            headers {
// //                header("Access-Control-Allow-Origin: http://localhost:4200");
// //                header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
// //                header("Access-Control-Allow-Headers: Content-Type, Authorization");
//                append(HttpHeaders.AccessControlAllowOrigin, "*")
//                append(HttpHeaders.AccessControlAllowMethods, "GET, POST, PUT")
//                append(HttpHeaders.AccessControlMaxAge, "1800")
//                append(HttpHeaders.AccessControlAllowHeaders, "Content-Type")
//            }
            memoryCacheConfig {
                maxSizeBytes(32 * 1024 * 1024) // 32MB
            }
            diskCacheConfig(FakeFileSystem().apply { emulateUnix() }) {
                directory(FileSystem.SYSTEM_TEMPORARY_DIRECTORY)
                maxSizeBytes(256L * 1024 * 1024) // 256MB
            }
        }
    }
}

package com.ajinkyabadve.kmmmywatchlist.imageloader

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import com.seiko.imageloader.ImageLoader
import com.seiko.imageloader.ImageLoaderConfigBuilder
import com.seiko.imageloader.component.setupDefaultComponents
import com.seiko.imageloader.intercept.Interceptor
import com.seiko.imageloader.model.ImageResult
import com.seiko.imageloader.model.NullRequestData
import com.seiko.imageloader.util.LogPriority
import com.seiko.imageloader.util.Logger
import io.github.aakira.napier.LogLevel
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js
import io.ktor.http.HttpHeaders
import kotlinx.atomicfu.TraceBase
import okio.FileSystem
import okio.fakefilesystem.FakeFileSystem


internal actual fun generateImageLoader(): ImageLoader {
    return ImageLoader {
        commonConfig()
        components {
            setupDefaultComponents(
                httpClient = {
                    HttpClient(Js.create()).config {
                        TraceBase.None.append(HttpHeaders.AccessControlAllowOrigin, "*")
                    }
                },
            )
        }
        interceptor {
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


fun ImageLoaderConfigBuilder.commonConfig() {
    logger = object : Logger {
        override fun log(
            priority: LogPriority,
            tag: String,
            data: Any?,
            throwable: Throwable?,
            message: String,
        ) {
            Napier.log(
                priority = when (priority) {
                    LogPriority.VERBOSE -> LogLevel.VERBOSE
                    LogPriority.DEBUG -> LogLevel.DEBUG
                    LogPriority.INFO -> LogLevel.INFO
                    LogPriority.WARN -> LogLevel.WARNING
                    LogPriority.ERROR -> LogLevel.ERROR
                    LogPriority.ASSERT -> LogLevel.ASSERT
                },
                tag = tag,
                throwable = throwable,
                message = buildString {
                    if (data != null) {
                        append("[image data] ")
                        append(data.toString().take(100))
                        append('\n')
                    }
                    append("[message] ")
                    append(message)
                },
            )
        }

        override fun isLoggable(priority: LogPriority) = priority >= LogPriority.DEBUG
    }
//    components {
//        add(MokoResourceFetcher.Factory())
//    }
    interceptor {
//        addInterceptor(BlurInterceptor())
        addInterceptor(NullDataInterceptor)
    }
}

/**
 * return empty painter if request is null or empty
 */
object NullDataInterceptor : Interceptor {

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val data = chain.request.data
        if (data === NullRequestData || data is String && data.isEmpty()) {
            return ImageResult.Painter(
                painter = EmptyPainter,
            )
        }
        return chain.proceed(chain.request)
    }

    private object EmptyPainter : Painter() {
        override val intrinsicSize: Size get() = Size.Unspecified
        override fun DrawScope.onDraw() {}
    }
}

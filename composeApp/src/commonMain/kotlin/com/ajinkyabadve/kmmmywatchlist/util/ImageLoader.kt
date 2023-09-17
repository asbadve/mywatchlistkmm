package com.ajinkyabadve.kmmmywatchlist.util

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import com.seiko.imageloader.ImageLoaderConfigBuilder
import com.seiko.imageloader.intercept.Interceptor
import com.seiko.imageloader.model.ImageResult
import com.seiko.imageloader.model.NullRequestData
import com.seiko.imageloader.util.LogPriority
import com.seiko.imageloader.util.Logger
import io.github.aakira.napier.LogLevel
import io.github.aakira.napier.Napier

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

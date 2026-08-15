package com.ajinkyabadve.kmmmywatchlist

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toComposeRect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.window.layout.WindowMetricsCalculator
import com.ajinkyabadve.kmmmywatchlist.core.WindowSize
import com.ajinkyabadve.kmmmywatchlist.core.logging.initLogging

class AndroidApp : Application() {
    companion object {
        lateinit var instance: AndroidApp
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        // Debuggable builds only - release APKs should not spend cycles formatting HTTP traffic
        // into logcat. Read off the manifest flag the build type already sets, so there is no
        // separate switch to remember to flip.
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            initLogging()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3WindowSizeClassApi::class)
class AppActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        com.ajinkyabadve.kmmmywatchlist.core.auth.AndroidAuthCallbackHandler
            .handleIntent(intent)
        setContent {
            App(calculateWindowSizeClass(this))
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        com.ajinkyabadve.kmmmywatchlist.core.auth.AndroidAuthCallbackHandler
            .handleIntent(intent)
    }
}

@Composable
private fun Activity.rememberWindowSize(): WindowSize {
    val configuration = LocalConfiguration.current
    val windowMetrics =
        remember(configuration) {
            WindowMetricsCalculator
                .getOrCreate()
                .computeCurrentWindowMetrics(this)
        }
    val windowDpSize =
        with(LocalDensity.current) {
            windowMetrics.bounds
                .toComposeRect()
                .size
                .toDpSize()
        }
    return WindowSize.basedOnWidth(windowDpSize.width)
}

internal actual fun openUrl(url: String?) {
    val uri = url?.let { Uri.parse(it) } ?: return
    val intent =
        Intent().apply {
            action = Intent.ACTION_VIEW
            data = uri
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    AndroidApp.instance.startActivity(intent)
}

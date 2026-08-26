package com.ajinkyabadve.kmmmywatchlist

import android.app.Activity
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.Build
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
import androidx.core.content.getSystemService
import androidx.window.layout.WindowMetricsCalculator
import com.ajinkyabadve.kmmmywatchlist.core.WindowSize
import com.ajinkyabadve.kmmmywatchlist.core.logging.initLogging
import com.ajinkyabadve.kmmmywatchlist.core.notification.AndroidNotificationConstant
import com.ajinkyabadve.kmmmywatchlist.core.notification.EpisodeNotificationTarget
import com.ajinkyabadve.kmmmywatchlist.core.notification.PendingEpisodeNotificationTarget

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
        createEpisodeNotificationChannel()
    }

    // Must exist before LocalNotifier.post() ever notifies against it - channels are one-time
    // setup, safe to recreate on every launch (createNotificationChannel is a no-op if unchanged).
    private fun createEpisodeNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel =
            NotificationChannel(
                AndroidNotificationConstant.CHANNEL_ID,
                getString(R.string.notification_channel_episodes_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = getString(R.string.notification_channel_episodes_description)
            }
        getSystemService<NotificationManager>()?.createNotificationChannel(channel)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3WindowSizeClassApi::class)
class AppActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        com.ajinkyabadve.kmmmywatchlist.core.auth.AndroidAuthCallbackHandler
            .handleIntent(intent)
        handleNotificationIntent(intent)
        setContent {
            App(calculateWindowSizeClass(this))
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        com.ajinkyabadve.kmmmywatchlist.core.auth.AndroidAuthCallbackHandler
            .handleIntent(intent)
        handleNotificationIntent(intent)
    }

    // Mirrors AndroidAuthCallbackHandler.handleIntent()'s shape, but for a tapped episode
    // notification's PendingIntent extras (see LocalNotifier.post, androidMain) instead of an
    // OAuth deep link - launchMode="singleInstance" means this Activity is reused via onNewIntent
    // rather than recreated, so both call sites matter (cold launch vs. already-running).
    private fun handleNotificationIntent(intent: Intent?) {
        if (intent == null || !intent.hasExtra(AndroidNotificationConstant.EXTRA_TV_SHOW_ID)) return
        val tvShowId = intent.getLongExtra(AndroidNotificationConstant.EXTRA_TV_SHOW_ID, -1L)
        val seasonNumber = intent.getIntExtra(AndroidNotificationConstant.EXTRA_SEASON_NUMBER, -1)
        val episodeNumber = intent.getIntExtra(AndroidNotificationConstant.EXTRA_EPISODE_NUMBER, -1)
        if (tvShowId < 0 || seasonNumber < 0 || episodeNumber < 0) return
        PendingEpisodeNotificationTarget.set(EpisodeNotificationTarget(tvShowId, seasonNumber, episodeNumber))
    }

    override fun onResume() {
        super.onResume()
        // onNewIntent() (and its handleIntent() call) always runs before onResume() when the
        // Custom Tab redirects back via deep link, so a still-pending callback here means the
        // user returned without one - i.e. they denied or dismissed the TMDB auth page.
        com.ajinkyabadve.kmmmywatchlist.core.auth.AndroidAuthCallbackHandler
            .handleResume()
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

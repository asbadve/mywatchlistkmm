@file:Suppress("ktlint:standard:filename")

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.ui.window.ComposeUIViewController
import com.ajinkyabadve.kmmmywatchlist.App
import com.ajinkyabadve.kmmmywatchlist.core.logging.initLogging
import com.ajinkyabadve.kmmmywatchlist.core.notification.NotificationScheduler
import com.ajinkyabadve.kmmmywatchlist.core.notification.NotificationTapDelegate
import platform.UIKit.UIViewController

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Suppress("ktlint:standard:function-naming")
fun MainViewController(): UIViewController {
    initLogging()
    NotificationTapDelegate.register()
    NotificationScheduler.ensureRegisteredAtLaunch()
    return ComposeUIViewController(configure = { enforceStrictPlistSanityCheck = false }) {
        App(calculateWindowSizeClass())
    }
}

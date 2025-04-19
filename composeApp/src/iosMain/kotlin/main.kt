@file:Suppress("ktlint:standard:filename")

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.ui.window.ComposeUIViewController
import com.ajinkyabadve.kmmmywatchlist.App
import platform.UIKit.UIViewController

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Suppress("ktlint:standard:function-naming")
fun MainViewController(): UIViewController {
    return ComposeUIViewController(configure = { enforceStrictPlistSanityCheck = false }) {
        App(calculateWindowSizeClass())
    }
}

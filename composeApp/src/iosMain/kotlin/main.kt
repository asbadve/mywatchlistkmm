@file:Suppress("ktlint:standard:filename")

import androidx.compose.ui.window.ComposeUIViewController
import com.ajinkyabadve.kmmmywatchlist.App
import platform.UIKit.UIViewController

@Suppress("ktlint:standard:function-naming")
fun MainViewController(): UIViewController {
    return ComposeUIViewController(configure = { enforceStrictPlistSanityCheck = false }) {
        App()
    }
}

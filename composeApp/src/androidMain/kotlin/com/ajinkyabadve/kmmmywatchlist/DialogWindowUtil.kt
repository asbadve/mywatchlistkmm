package com.ajinkyabadve.kmmmywatchlist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import android.view.Window
import android.view.ViewGroup
import android.graphics.drawable.ColorDrawable
import android.graphics.Color
import android.view.WindowManager
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.os.Build

@Composable
actual fun ConfigureDialogWindow() {
    val view = LocalView.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val config = context.resources.configuration
    val isLargeScreen = config.smallestScreenWidthDp >= 600

    DisposableEffect(view) {
        var window: Window? = null
        
        // 1. Try resolving Dialog via view's context wrappers
        var ctx = view.context
        while (ctx is android.content.ContextWrapper) {
            if (ctx is android.app.Dialog) {
                window = ctx.window
                break
            }
            ctx = ctx.baseContext
        }

        // 2. Try casting view directly
        if (window == null && view is DialogWindowProvider) {
            window = view.window
        }
        
        // 3. Walk up parents to find DialogWindowProvider or window reference
        if (window == null) {
            var parent = view.parent
            while (parent != null) {
                if (parent is DialogWindowProvider) {
                    window = parent.window
                    break
                }
                // Try via reflection as fallback
                try {
                    val getWindowMethod = parent::class.java.getMethod("getWindow")
                    window = getWindowMethod.invoke(parent) as? Window
                    if (window != null) break
                } catch (e: NoSuchMethodException) {
                    // Method not found, proceed walking up
                } catch (e: SecurityException) {
                    // Security restriction, proceed walking up
                } catch (e: IllegalAccessException) {
                    // Method access restriction, proceed walking up
                } catch (e: java.lang.reflect.InvocationTargetException) {
                    // Method invocation failed, proceed walking up
                } catch (e: NullPointerException) {
                    // Null pointer, proceed walking up
                }
                parent = parent.parent
            }
        }

        window?.let { w ->
            try {
                w.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS or WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
                w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                w.setBackgroundDrawable(ColorDrawable(Color.BLACK))
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    w.attributes?.let { lp ->
                        lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                        w.attributes = lp
                    }
                }

                // Avoid FLAG_LAYOUT_NO_LIMITS on foldables and tablets to prevent clipping/stretching
                if (!isLargeScreen) {
                    w.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
                }
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    w.insetsController?.hide(
                        WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars()
                    )
                    w.insetsController?.systemBarsBehavior =
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                } else {
                    @Suppress("DEPRECATION")
                    w.decorView.systemUiVisibility = (
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    )
                }
            } catch (e: IllegalArgumentException) {
                // Invalid layout params or background
            } catch (e: NullPointerException) {
                // Insets controller or decorView null pointer
            } catch (e: IllegalStateException) {
                // Window state not matching
            } catch (e: UnsupportedOperationException) {
                // Operation not supported on this device version
            }
        }
        onDispose {}
    }
}

actual fun getDialogProperties(
    dismissOnBackPress: Boolean,
    dismissOnClickOutside: Boolean
): androidx.compose.ui.window.DialogProperties {
    return androidx.compose.ui.window.DialogProperties(
        usePlatformDefaultWidth = false,
        dismissOnBackPress = dismissOnBackPress,
        dismissOnClickOutside = dismissOnClickOutside
    )
}

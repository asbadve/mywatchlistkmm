package com.ajinkyabadve.kmmmywatchlist.features.backup.transfer

import android.os.Build
import android.provider.Settings
import com.ajinkyabadve.kmmmywatchlist.AndroidApp

/** The name the user gave the phone (Settings > About phone > Device name, e.g. "Ajinkya's Pixel"),
 *  which needs no permission; the model ("Pixel 8") when it isn't set. */
actual fun transferDeviceName(): String =
    Settings.Global.getString(AndroidApp.instance.contentResolver, Settings.Global.DEVICE_NAME)?.takeIf { it.isNotBlank() }
        ?: Build.MODEL

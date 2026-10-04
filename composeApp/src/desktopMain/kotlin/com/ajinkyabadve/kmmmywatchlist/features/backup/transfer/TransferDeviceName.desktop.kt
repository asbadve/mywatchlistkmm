package com.ajinkyabadve.kmmmywatchlist.features.backup.transfer

import java.net.InetAddress
import java.net.UnknownHostException

private const val OS_NAME_PROPERTY = "os.name"

actual fun transferDeviceName(): String =
    try {
        InetAddress.getLocalHost().hostName
    } catch (e: UnknownHostException) {
        System.getProperty(OS_NAME_PROPERTY)
    }

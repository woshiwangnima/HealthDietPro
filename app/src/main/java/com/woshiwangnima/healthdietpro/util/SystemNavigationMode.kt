package com.woshiwangnima.healthdietpro.util

import android.content.Context
import android.provider.Settings

enum class SystemNavigationMode {
    ThreeButton,
    TwoButton,
    Gestural,
    Unknown,
}

fun Context.systemNavigationMode(): SystemNavigationMode {
    val resourceId = resources.getIdentifier(
        "config_navBarInteractionMode",
        "integer",
        "android",
    )
    val mode = if (resourceId > 0) {
        runCatching { resources.getInteger(resourceId) }.getOrDefault(-1)
    } else {
        runCatching {
            Settings.Secure.getInt(contentResolver, "navigation_mode")
        }.getOrDefault(-1)
    }
    return when (mode) {
        0 -> SystemNavigationMode.ThreeButton
        1 -> SystemNavigationMode.TwoButton
        2 -> SystemNavigationMode.Gestural
        else -> SystemNavigationMode.Unknown
    }
}

fun SystemNavigationMode.needsNavigationBarInset(): Boolean =
    this == SystemNavigationMode.ThreeButton || this == SystemNavigationMode.TwoButton

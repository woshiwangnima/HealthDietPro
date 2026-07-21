package com.woshiwangnima.healthdietpro.model.archive

import android.content.Context

internal fun appVersion(context: Context): String = normalizedAppVersion(
    context.packageManager.getPackageInfo(context.packageName, 0).versionName,
)

internal fun normalizedAppVersion(rawVersion: String?): String {
    val match = VERSION_PATTERN.matchEntire(rawVersion.orEmpty()) ?: return "unknown"
    val major = match.groupValues[1]
    val minor = match.groupValues[2].ifEmpty { "0" }
    val patch = match.groupValues[3].ifEmpty { "0" }
    return "$major.$minor.$patch${match.groupValues[4]}"
}

private val VERSION_PATTERN = Regex("^(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?(.*)$")

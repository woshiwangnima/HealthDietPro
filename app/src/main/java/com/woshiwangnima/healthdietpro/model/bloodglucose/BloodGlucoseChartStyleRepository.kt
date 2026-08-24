package com.woshiwangnima.healthdietpro.model.bloodglucose

import android.content.Context
import com.woshiwangnima.healthdietpro.model.prefs.UserPrefs
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
internal data class BloodGlucoseChartStylePrefs(
    val primary: BloodGlucoseSeriesStylePrefs = BloodGlucoseSeriesStylePrefs(
        colorArgb = 0xFF1976D2,
        lineStyle = "Spline",
    ),
    val delayed: BloodGlucoseSeriesStylePrefs = BloodGlucoseSeriesStylePrefs(
        colorArgb = 0xFFF57C00,
        lineStyle = "Spline",
        linePattern = "Dotted",
        pointShape = "Cross",
    ),
    val bars: Map<String, BloodGlucoseBarStylePrefs> = defaultBloodGlucoseBarStyles(),
)

@Serializable
internal data class BloodGlucoseSeriesStylePrefs(
    val colorArgb: Long,
    val alpha: Float = 1f,
    val visible: Boolean = true,
    val lineStyle: String = "Linear",
    val linePattern: String = "Solid",
    val pointShape: String = "Circle",
    val pointFill: String = "Filled",
)

@Serializable
internal data class BloodGlucoseBarStylePrefs(
    val colorArgb: Long,
    val mainAlpha: Float = 0.5f,
    val impactAlpha: Float = 0.3f,
    val visible: Boolean = true,
)

internal class BloodGlucoseChartStyleRepository private constructor(context: Context) {
    private val preferences = UserPrefs.current(context.applicationContext)

    fun load(): BloodGlucoseChartStylePrefs {
        val encoded = preferences.getString(STORAGE_KEY, "")
        return encoded.takeIf(String::isNotEmpty)
            ?.let(::decodeBloodGlucoseChartStyle)
            ?: BloodGlucoseChartStylePrefs()
    }

    fun save(style: BloodGlucoseChartStylePrefs) {
        preferences.putString(STORAGE_KEY, encodeBloodGlucoseChartStyle(style))
    }

    companion object {
        private const val STORAGE_KEY = "blood_glucose_chart_style_v1"
        fun fromContext(context: Context): BloodGlucoseChartStyleRepository =
            BloodGlucoseChartStyleRepository(context.applicationContext)
    }
}

private val bloodGlucoseChartStyleJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
}

internal fun encodeBloodGlucoseChartStyle(style: BloodGlucoseChartStylePrefs): String =
    bloodGlucoseChartStyleJson.encodeToString(style.sanitized())

internal fun decodeBloodGlucoseChartStyle(encoded: String): BloodGlucoseChartStylePrefs? =
    runCatching { bloodGlucoseChartStyleJson.decodeFromString<BloodGlucoseChartStylePrefs>(encoded).sanitized() }.getOrNull()

private fun BloodGlucoseChartStylePrefs.sanitized(): BloodGlucoseChartStylePrefs {
    val defaults = defaultBloodGlucoseBarStyles()
    return copy(
        primary = primary.sanitized(),
        delayed = delayed.sanitized(),
        bars = defaults.mapValues { (kind, default) -> (bars[kind] ?: default).sanitized() },
    )
}

private fun BloodGlucoseSeriesStylePrefs.sanitized(): BloodGlucoseSeriesStylePrefs =
    copy(alpha = alpha.coerceIn(0f, 1f))

private fun BloodGlucoseBarStylePrefs.sanitized(): BloodGlucoseBarStylePrefs =
    copy(mainAlpha = mainAlpha.coerceIn(0f, 1f), impactAlpha = impactAlpha.coerceIn(0f, 1f))

private fun defaultBloodGlucoseBarStyles(): Map<String, BloodGlucoseBarStylePrefs> = mapOf(
    "Medication" to BloodGlucoseBarStylePrefs(colorArgb = 0xFFE53935),
    "Diet" to BloodGlucoseBarStylePrefs(colorArgb = 0xFFF57C00),
    "Exercise" to BloodGlucoseBarStylePrefs(colorArgb = 0xFF43A047),
    "Sleep" to BloodGlucoseBarStylePrefs(colorArgb = 0xFF7E57C2),
)

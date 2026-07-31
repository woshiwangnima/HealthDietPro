package com.woshiwangnima.healthdietpro.model.bloodglucose

import android.content.Context
import com.woshiwangnima.healthdietpro.model.prefs.UserPrefs

internal enum class BloodGlucoseAlertMode { Sound, Vibration, SoundAndVibration }

internal data class BloodGlucoseReminderSettings(
    val highEnabled: Boolean = false,
    val highThresholdMmolPerL: Float = 10.0f,
    val lowEnabled: Boolean = false,
    val lowThresholdMmolPerL: Float = 3.9f,
    val emergencyLowEnabled: Boolean = false,
    val emergencyLowThresholdMmolPerL: Float = 3.0f,
    val risingEnabled: Boolean = false,
    val risingMode: BloodGlucoseAlertMode = BloodGlucoseAlertMode.SoundAndVibration,
    val risingReminderIntervalSeconds: Int = 900,
    val risingAlertDurationSeconds: Int = 2,
    val fallingEnabled: Boolean = false,
    val fallingMode: BloodGlucoseAlertMode = BloodGlucoseAlertMode.SoundAndVibration,
    val fallingReminderIntervalSeconds: Int = 900,
    val fallingAlertDurationSeconds: Int = 2,
)

internal enum class BloodGlucoseAlertKind {
    High,
    Low,
    EmergencyLow,
    RisingFast,
    FallingFast,
}

internal data class BloodGlucoseAlert(
    val kind: BloodGlucoseAlertKind,
    val mode: BloodGlucoseAlertMode,
    val durationSeconds: Int,
)

internal class BloodGlucoseReminderRepository private constructor(context: Context) {
    private val userPrefs = UserPrefs.current(context)

    fun load(): BloodGlucoseReminderSettings = BloodGlucoseReminderSettings(
        highEnabled = userPrefs.getBoolean(KEY_HIGH_ENABLED, false),
        highThresholdMmolPerL = userPrefs.getFloat(KEY_HIGH_THRESHOLD, 10.0f),
        lowEnabled = userPrefs.getBoolean(KEY_LOW_ENABLED, false),
        lowThresholdMmolPerL = userPrefs.getFloat(KEY_LOW_THRESHOLD, 3.9f),
        emergencyLowEnabled = userPrefs.getBoolean(KEY_EMERGENCY_LOW_ENABLED, false),
        emergencyLowThresholdMmolPerL = userPrefs.getFloat(KEY_EMERGENCY_LOW_THRESHOLD, 3.0f),
        risingEnabled = userPrefs.getBoolean(KEY_RISING_ENABLED, false),
        risingMode = loadMode(KEY_RISING_MODE),
        risingReminderIntervalSeconds = userPrefs.getInt(KEY_RISING_INTERVAL, 900),
        risingAlertDurationSeconds = userPrefs.getInt(KEY_RISING_DURATION, 2),
        fallingEnabled = userPrefs.getBoolean(KEY_FALLING_ENABLED, false),
        fallingMode = loadMode(KEY_FALLING_MODE),
        fallingReminderIntervalSeconds = userPrefs.getInt(KEY_FALLING_INTERVAL, 900),
        fallingAlertDurationSeconds = userPrefs.getInt(KEY_FALLING_DURATION, 2),
    )

    fun save(settings: BloodGlucoseReminderSettings) {
        userPrefs.putBoolean(KEY_HIGH_ENABLED, settings.highEnabled)
        userPrefs.putFloat(KEY_HIGH_THRESHOLD, settings.highThresholdMmolPerL)
        userPrefs.putBoolean(KEY_LOW_ENABLED, settings.lowEnabled)
        userPrefs.putFloat(KEY_LOW_THRESHOLD, settings.lowThresholdMmolPerL)
        userPrefs.putBoolean(KEY_EMERGENCY_LOW_ENABLED, settings.emergencyLowEnabled)
        userPrefs.putFloat(KEY_EMERGENCY_LOW_THRESHOLD, settings.emergencyLowThresholdMmolPerL)
        userPrefs.putBoolean(KEY_RISING_ENABLED, settings.risingEnabled)
        userPrefs.putString(KEY_RISING_MODE, settings.risingMode.name)
        userPrefs.putInt(KEY_RISING_INTERVAL, settings.risingReminderIntervalSeconds)
        userPrefs.putInt(KEY_RISING_DURATION, settings.risingAlertDurationSeconds)
        userPrefs.putBoolean(KEY_FALLING_ENABLED, settings.fallingEnabled)
        userPrefs.putString(KEY_FALLING_MODE, settings.fallingMode.name)
        userPrefs.putInt(KEY_FALLING_INTERVAL, settings.fallingReminderIntervalSeconds)
        userPrefs.putInt(KEY_FALLING_DURATION, settings.fallingAlertDurationSeconds)
    }

    fun lastAlertAt(kind: BloodGlucoseAlertKind): Long = userPrefs.getLong("blood_glucose_alert_last_${kind.name}", 0L)

    fun saveLastAlertAt(kind: BloodGlucoseAlertKind, timestamp: Long) = userPrefs.putLong("blood_glucose_alert_last_${kind.name}", timestamp)

    private fun loadMode(key: String): BloodGlucoseAlertMode =
        userPrefs.getString(key, BloodGlucoseAlertMode.SoundAndVibration.name)
            .let { saved -> BloodGlucoseAlertMode.entries.find { it.name == saved } }
            ?: BloodGlucoseAlertMode.SoundAndVibration

    companion object {
        private const val KEY_HIGH_ENABLED = "blood_glucose_alert_high_enabled"
        private const val KEY_HIGH_THRESHOLD = "blood_glucose_alert_high_threshold_mmol_l"
        private const val KEY_LOW_ENABLED = "blood_glucose_alert_low_enabled"
        private const val KEY_LOW_THRESHOLD = "blood_glucose_alert_low_threshold_mmol_l"
        private const val KEY_EMERGENCY_LOW_ENABLED = "blood_glucose_alert_emergency_low_enabled"
        private const val KEY_EMERGENCY_LOW_THRESHOLD = "blood_glucose_alert_emergency_low_threshold_mmol_l"
        private const val KEY_RISING_ENABLED = "blood_glucose_alert_rising_enabled"
        private const val KEY_RISING_MODE = "blood_glucose_alert_rising_mode"
        private const val KEY_RISING_INTERVAL = "blood_glucose_alert_rising_interval_minutes"
        private const val KEY_RISING_DURATION = "blood_glucose_alert_rising_duration_seconds"
        private const val KEY_FALLING_ENABLED = "blood_glucose_alert_falling_enabled"
        private const val KEY_FALLING_MODE = "blood_glucose_alert_falling_mode"
        private const val KEY_FALLING_INTERVAL = "blood_glucose_alert_falling_interval_minutes"
        private const val KEY_FALLING_DURATION = "blood_glucose_alert_falling_duration_seconds"

        fun fromContext(context: Context) = BloodGlucoseReminderRepository(context.applicationContext)
    }
}

internal fun evaluateBloodGlucoseAlerts(
    record: BloodGlucoseRecord,
    records: List<BloodGlucoseRecord>,
    settings: BloodGlucoseReminderSettings,
): List<BloodGlucoseAlert> {
    val alerts = buildList {
        if (settings.highEnabled && record.valueMmolPerL >= settings.highThresholdMmolPerL) add(BloodGlucoseAlert(BloodGlucoseAlertKind.High, BloodGlucoseAlertMode.SoundAndVibration, 2))
        if (settings.lowEnabled && record.valueMmolPerL <= settings.lowThresholdMmolPerL) add(BloodGlucoseAlert(BloodGlucoseAlertKind.Low, BloodGlucoseAlertMode.SoundAndVibration, 2))
        if (settings.emergencyLowEnabled && record.valueMmolPerL <= settings.emergencyLowThresholdMmolPerL) add(BloodGlucoseAlert(BloodGlucoseAlertKind.EmergencyLow, BloodGlucoseAlertMode.SoundAndVibration, 2))
        val previous = records.filter { it.timestamp < record.timestamp }.maxByOrNull { it.timestamp }
        previous?.let {
            val elapsedMinutes = (record.timestamp - it.timestamp) / 60_000f
            if (elapsedMinutes > 0f) {
                val changePerFifteenMinutes = (record.valueMmolPerL - it.valueMmolPerL) * 15f / elapsedMinutes
                if (settings.risingEnabled && changePerFifteenMinutes >= 1.5f) add(BloodGlucoseAlert(BloodGlucoseAlertKind.RisingFast, settings.risingMode, settings.risingAlertDurationSeconds))
                if (settings.fallingEnabled && changePerFifteenMinutes <= -1.5f) add(BloodGlucoseAlert(BloodGlucoseAlertKind.FallingFast, settings.fallingMode, settings.fallingAlertDurationSeconds))
            }
        }
    }
    return alerts
}

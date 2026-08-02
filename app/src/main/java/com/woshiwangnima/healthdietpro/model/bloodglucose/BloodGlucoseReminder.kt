package com.woshiwangnima.healthdietpro.model.bloodglucose

import android.content.Context
import kotlinx.serialization.Serializable

@Serializable
internal enum class BloodGlucoseAlertMode { Sound, Vibration, SoundAndVibration }

@Serializable
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

@Serializable
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
    private val archive = BloodGlucoseArchiveStore.current(context)

    fun load(): BloodGlucoseReminderSettings = archive.load().reminder

    fun save(settings: BloodGlucoseReminderSettings) {
        archive.update { it.copy(reminder = settings) }
    }

    fun lastAlertAt(kind: BloodGlucoseAlertKind): Long = archive.load().lastAlertAt[kind] ?: 0L

    fun saveLastAlertAt(kind: BloodGlucoseAlertKind, timestamp: Long) {
        archive.update { it.copy(lastAlertAt = it.lastAlertAt + (kind to timestamp)) }
    }

    companion object {
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

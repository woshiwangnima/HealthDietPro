package com.woshiwangnima.healthdietpro.model.bloodglucose

import kotlinx.serialization.Serializable

@Serializable
internal data class BloodGlucoseRecord(
    val id: String,
    val timestamp: Long,
    val valueMmolPerL: Double,
    val timingAnchor: BloodGlucoseTimingAnchor? = null,
    val relativeMinutes: Int? = null,
    val note: String = "",
)

@Serializable
internal enum class BloodGlucoseTimingAnchor {
    BREAKFAST,
    LUNCH,
    DINNER,
    WAKE_UP,
    BEDTIME,
}

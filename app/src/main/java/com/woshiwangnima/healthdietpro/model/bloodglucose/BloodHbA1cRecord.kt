package com.woshiwangnima.healthdietpro.model.bloodglucose

import kotlinx.serialization.Serializable

@Serializable
internal data class BloodHbA1cRecord(
    val id: String,
    val timestamp: Long,
    val valueHbA1c: Double,
    val timingAnchor: BloodGlucoseTimingAnchor? = null,
    val relativeMinutes: Int? = null,
    val note: String = "",
    val sourceId: String? = null,
)

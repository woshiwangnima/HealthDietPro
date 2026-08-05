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
    val sourceId: String? = null,
)

@Serializable
internal data class BloodGlucoseSource(
    val id: String,
    val note: String,
)

@Serializable
internal enum class BloodGlucoseTimingAnchor {
    BREAKFAST,
    LUNCH,
    DINNER,
    WAKE_UP,
    BEDTIME,
}

/** Future meal and sleep modules provide the closest earlier anchor through this contract. */
internal data class BloodGlucoseTimingDefaults(
    val meal: BloodGlucoseTimingAnchor? = null,
    val sleep: BloodGlucoseTimingAnchor? = null,
) {
    fun preferredAnchor(): BloodGlucoseTimingAnchor? = meal ?: sleep
}

internal fun reorderBloodGlucoseSources(
    sources: List<BloodGlucoseSource>,
    orderedIds: List<String>,
): List<BloodGlucoseSource> {
    val currentIds = sources.map(BloodGlucoseSource::id)
    require(orderedIds.size == currentIds.size) { "Incomplete blood glucose source order" }
    require(orderedIds.distinct().size == orderedIds.size) { "Duplicate blood glucose source id" }
    require(orderedIds.toSet() == currentIds.toSet()) { "Unknown blood glucose source id" }
    val byId = sources.associateBy(BloodGlucoseSource::id)
    return orderedIds.map { requireNotNull(byId[it]) }
}

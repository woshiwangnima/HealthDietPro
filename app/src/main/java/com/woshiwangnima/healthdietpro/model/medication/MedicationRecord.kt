package com.woshiwangnima.healthdietpro.model.medication

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class MedicationRecord(
    val id: String,
    val timestamp: Long,
    val medicationName: String,
    val doseValue: Float,
    val doseUnit: String,
    val specValue: Float,
    val specUnitCategory: String,
    val specUnitId: String,
    val method: String,
    val feelings: List<String> = emptyList(),
    val feelingNote: String = "",
    val recordPhotoPaths: List<String> = emptyList(),
    /** Null only for records that could not be associated with a catalog medicine. */
    val medicationId: String? = null,
    val manufacturer: String = "",
    val medicationImagePaths: List<String> = emptyList(),
    val frequency: MedicationFrequency = MedicationFrequency(),
    val intakeRules: List<MedicationIntakeRule> = emptyList(),
    val purposes: List<String> = emptyList(),
    val sideEffectWarning: String = "",
    val lotNumber: String = "",
    val expiresAt: Long? = null,
)

@Serializable
data class MedicationCatalogItem(
    val id: String,
    val name: String,
    val specValue: Float = 0f,
    val specUnitCategory: String = "",
    val specUnitId: String = "",
    val manufacturer: String = "",
    val defaultMethod: String = "",
    val defaultDoseValue: Float = 0f,
    val defaultDoseUnit: String = "",
    val imagePaths: List<String> = emptyList(),
    val frequency: MedicationFrequency = MedicationFrequency(),
    val intakeRules: List<MedicationIntakeRule> = emptyList(),
    val packageQuantity: Float = 0f,
    val packageUnit: String = "",
    val packageDescription: String = "",
    val lotNumber: String = "",
    val expiresAt: Long? = null,
    val indicationTags: List<String> = emptyList(),
    val sideEffectWarning: String = "",
    val archived: Boolean = false,
)

@Serializable
data class MedicationFrequency(
    val type: MedicationFrequencyType = MedicationFrequencyType.SCHEDULED,
    val interval: Int = 1,
    val unit: MedicationFrequencyUnit = MedicationFrequencyUnit.DAY,
    val times: Int = 1,
)

@Serializable
enum class MedicationFrequencyType {
    @SerialName("scheduled") SCHEDULED,
    @SerialName("as_needed") AS_NEEDED,
}

@Serializable
enum class MedicationFrequencyUnit {
    @SerialName("min") MINUTE,
    @SerialName("h") HOUR,
    @SerialName("d") DAY,
}

@Serializable
data class MedicationIntakeRule(
    val anchor: MedicationTimingAnchor,
    val offsetMinutes: Int = 0,
)

@Serializable
enum class MedicationTimingAnchor {
    @SerialName("breakfast") BREAKFAST,
    @SerialName("lunch") LUNCH,
    @SerialName("dinner") DINNER,
    @SerialName("wake_up") WAKE_UP,
    @SerialName("bedtime") BEDTIME,
}

internal fun MedicationFrequency.defaultIntakeRules(): List<MedicationIntakeRule> {
    if (type != MedicationFrequencyType.SCHEDULED || unit != MedicationFrequencyUnit.DAY || interval != 1) return emptyList()
    return listOf(
        MedicationIntakeRule(MedicationTimingAnchor.BREAKFAST),
        MedicationIntakeRule(MedicationTimingAnchor.LUNCH),
        MedicationIntakeRule(MedicationTimingAnchor.DINNER),
    ).take(times.coerceIn(0, 3))
}

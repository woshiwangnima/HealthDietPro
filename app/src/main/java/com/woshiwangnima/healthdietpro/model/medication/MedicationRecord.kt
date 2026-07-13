package com.woshiwangnima.healthdietpro.model.medication

import kotlinx.serialization.Serializable

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
    val photoPath: String? = null,
    /** Null only for records that could not be associated with a catalog medicine. */
    val medicationId: String? = null,
    val manufacturer: String = "",
    val medicationImagePath: String? = null,
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
    val imagePath: String? = null,
    val archived: Boolean = false,
)

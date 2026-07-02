package com.woshiwangnima.healthdietpro.model.unit

import kotlinx.serialization.Serializable

@Serializable
data class UnitCategory(
    val id: String,
    val categoryCn: String,
    val categoryEn: String,
    val baseUnit: String,
    val units: List<UnitDef>
)

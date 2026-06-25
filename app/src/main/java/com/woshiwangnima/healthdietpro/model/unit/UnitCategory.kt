package com.woshiwangnima.healthdietpro.model.unit

data class UnitCategory(
    val category: String,
    val categoryCn: String,
    val baseUnit: String,
    val units: List<UnitDef>
)

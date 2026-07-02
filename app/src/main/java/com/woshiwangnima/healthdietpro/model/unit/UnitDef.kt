package com.woshiwangnima.healthdietpro.model.unit

import kotlinx.serialization.Serializable

@Serializable
data class UnitDef(
    val id: String,
    val symbolCn: String,
    val symbolEn: String,
    val toBase: Float,
    val hidden: Boolean = false
)

package com.woshiwangnima.healthdietpro.model.unit

import kotlinx.serialization.Serializable

/** Stable reference to a unit from a category in the read-only unit catalog. */
@Serializable
data class UnitReference(
    val categoryId: String,
    val unitId: String,
)

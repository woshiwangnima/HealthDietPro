package com.woshiwangnima.healthdietpro.model.disease

import kotlinx.serialization.Serializable

@Serializable
data class NutrientRecommendation(
    val nutrientId: String,
    val recommendation: DietaryRecommendation
)

package com.woshiwangnima.healthdietpro.model.disease

data class Disease(
    val id: String,
    val name: String,
    val gender: List<String> = listOf("MALE", "FEMALE"),
    val prevalence: Map<String, Float>,
    val nutrientRecommendations: List<NutrientRecommendation>
)

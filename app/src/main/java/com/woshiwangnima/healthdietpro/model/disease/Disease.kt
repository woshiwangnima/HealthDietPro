package com.woshiwangnima.healthdietpro.model.disease

import java.util.Locale

data class Disease(
    val id: String,
    val i18n: Map<String, DiseaseI18n> = emptyMap(),
    val gender: List<String> = listOf("MALE", "FEMALE"),
    val prevalence: Map<String, Float>,
    val nutrientRecommendations: List<NutrientRecommendation>
) {
    fun displayName(locale: Locale = Locale.getDefault()): String {
        val language = locale.language.lowercase(Locale.ROOT)
        return i18n[language]?.label?.takeIf { it.isNotBlank() }
            ?: i18n["zh"]?.label?.takeIf { it.isNotBlank() }
            ?: i18n["en"]?.label?.takeIf { it.isNotBlank() }
            ?: id
    }
}

data class DiseaseI18n(
    val label: String,
    val description: String = "",
)

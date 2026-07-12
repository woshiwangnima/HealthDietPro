package com.woshiwangnima.healthdietpro.model.disease

import com.woshiwangnima.healthdietpro.model.i18n.localizedI18nValue
import java.util.Locale

data class Disease(
    val id: String,
    val i18n: Map<String, DiseaseI18n> = emptyMap(),
    val gender: List<String> = listOf("MALE", "FEMALE"),
    val prevalence: Map<String, Float>,
    val nutrientRecommendations: List<NutrientRecommendation>
) {
    fun displayName(locale: Locale = Locale.getDefault()): String {
        return localizedI18nValue(i18n, locale) { it.label } ?: id
    }
}

data class DiseaseI18n(
    val label: String,
    val description: String = "",
)

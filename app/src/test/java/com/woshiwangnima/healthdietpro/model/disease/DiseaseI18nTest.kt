package com.woshiwangnima.healthdietpro.model.disease

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class DiseaseI18nTest {

    @Test fun displayNameUsesRequestedLanguageLabel() {
        val disease = Disease(
            id = "hypertension",
            i18n = mapOf(
                "zh" to DiseaseI18n(label = "高血压"),
                "en" to DiseaseI18n(label = "Hypertension"),
            ),
            prevalence = emptyMap(),
            nutrientRecommendations = emptyList(),
        )

        assertEquals("高血压", disease.displayName(Locale.SIMPLIFIED_CHINESE))
        assertEquals("Hypertension", disease.displayName(Locale.ENGLISH))
    }

    @Test fun displayNameFallsBackToChineseThenId() {
        val withChinese = Disease(
            id = "hypertension",
            i18n = mapOf("zh" to DiseaseI18n(label = "高血压")),
            prevalence = emptyMap(),
            nutrientRecommendations = emptyList(),
        )
        val withoutLabels = Disease(
            id = "unknown_disease",
            i18n = emptyMap(),
            prevalence = emptyMap(),
            nutrientRecommendations = emptyList(),
        )

        assertEquals("高血压", withChinese.displayName(Locale.ENGLISH))
        assertEquals("unknown_disease", withoutLabels.displayName(Locale.ENGLISH))
    }
}

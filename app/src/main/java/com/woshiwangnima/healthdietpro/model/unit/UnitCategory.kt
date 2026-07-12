package com.woshiwangnima.healthdietpro.model.unit

import com.woshiwangnima.healthdietpro.model.i18n.localizedI18nValue
import kotlinx.serialization.Serializable
import java.util.Locale

@Serializable
data class UnitCategory(
    val id: String,
    val i18n: Map<String, UnitI18n> = emptyMap(),
    val baseUnit: String,
    val units: List<UnitDef>
) {
    fun displayName(locale: Locale = Locale.getDefault()): String =
        localizedI18nValue(i18n, locale) { it.label } ?: id
}

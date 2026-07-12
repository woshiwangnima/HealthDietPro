package com.woshiwangnima.healthdietpro.model.unit

import com.woshiwangnima.healthdietpro.model.i18n.localizedI18nValue
import kotlinx.serialization.Serializable
import java.util.Locale

@Serializable
data class UnitDef(
    val id: String,
    val i18n: Map<String, UnitI18n> = emptyMap(),
    val toBase: Float,
    val hidden: Boolean = false
) {
    fun symbol(locale: Locale = Locale.getDefault()): String =
        localizedI18nValue(i18n, locale) { it.symbol } ?: id
}

@Serializable
data class UnitI18n(
    val label: String = "",
    val symbol: String = "",
)

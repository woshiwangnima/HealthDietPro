package com.woshiwangnima.healthdietpro.model.i18n

import java.util.Locale

internal fun <T> localizedI18nValue(
    i18n: Map<String, T>,
    locale: Locale = Locale.getDefault(),
    valueOf: (T) -> String?,
): String? {
    val language = locale.language.lowercase(Locale.ROOT)
    return sequenceOf(language, "zh", "en")
        .distinct()
        .mapNotNull { key -> i18n[key]?.let(valueOf)?.takeIf { it.isNotBlank() } }
        .firstOrNull()
}

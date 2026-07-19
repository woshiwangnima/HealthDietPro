package com.woshiwangnima.healthdietpro.model.medication

import com.woshiwangnima.healthdietpro.model.unit.UnitRepository
import java.text.NumberFormat
import java.util.Locale

internal fun MedicationCatalogItem.formatSpecification(
    unitRepository: UnitRepository?,
    locale: Locale,
): String = formatMedicationAmount(specValue, specUnitCategory, specUnitId, unitRepository, locale)

internal fun MedicationCatalogItem.formatDefaultDose(locale: Locale): String =
    formatMedicationAmount(defaultDoseValue, "", defaultDoseUnit, null, locale)

internal fun MedicationCatalogItem.formatSelectionDetails(
    unitRepository: UnitRepository?,
    locale: Locale,
    separator: String,
): String = listOf(
    formatSpecification(unitRepository, locale),
    formatDefaultDose(locale),
    frequency.format(locale),
    intakeRules.format(locale),
    manufacturer,
).filter(String::isNotBlank).joinToString(separator)

internal fun MedicationRecord.formatDose(locale: Locale): String =
    formatMedicationAmount(doseValue, "", doseUnit, null, locale)

private fun formatMedicationAmount(
    value: Float,
    categoryId: String,
    unitId: String,
    unitRepository: UnitRepository?,
    locale: Locale,
): String {
    if (value <= 0f && unitId.isBlank()) return ""
    val symbol = unitRepository?.getUnit(categoryId, unitId)?.symbol(locale) ?: unitId
    val number = NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 2 }.format(value)
    return listOf(number, symbol).filter(String::isNotBlank).joinToString(" ")
}

internal fun MedicationFrequency.format(locale: Locale): String {
    if (type == MedicationFrequencyType.AS_NEEDED) return if (locale.language == "zh") "必要时" else "As needed"
    val unitName = when (unit) {
        MedicationFrequencyUnit.MINUTE -> if (locale.language == "zh") "分钟" else if (interval == 1) "minute" else "minutes"
        MedicationFrequencyUnit.HOUR -> if (locale.language == "zh") "小时" else if (interval == 1) "hour" else "hours"
        MedicationFrequencyUnit.DAY -> if (locale.language == "zh") "天" else if (interval == 1) "day" else "days"
    }
    return if (locale.language == "zh") "每${interval}${unitName}${times}次" else "Every $interval $unitName, $times time${if (times == 1) "" else "s"}"
}

internal fun List<MedicationIntakeRule>.format(locale: Locale): String = joinToString(", ") { rule ->
    val anchorName = when (rule.anchor) {
        MedicationTimingAnchor.BREAKFAST -> if (locale.language == "zh") "早餐" else "Breakfast"
        MedicationTimingAnchor.LUNCH -> if (locale.language == "zh") "午餐" else "Lunch"
        MedicationTimingAnchor.DINNER -> if (locale.language == "zh") "晚餐" else "Dinner"
        MedicationTimingAnchor.WAKE_UP -> if (locale.language == "zh") "睡起" else "After waking"
        MedicationTimingAnchor.BEDTIME -> if (locale.language == "zh") "睡前" else "Before bed"
    }
    when {
        rule.offsetMinutes == 0 && rule.anchor in setOf(MedicationTimingAnchor.BREAKFAST, MedicationTimingAnchor.LUNCH, MedicationTimingAnchor.DINNER) -> if (locale.language == "zh") "${anchorName}随餐" else "With $anchorName"
        rule.offsetMinutes == 0 -> anchorName
        locale.language == "zh" -> "$anchorName${if (rule.offsetMinutes > 0) "后" else "前"}${NumberFormat.getNumberInstance(locale).format(kotlin.math.abs(rule.offsetMinutes) / 60.0)}小时"
        else -> "$anchorName ${if (rule.offsetMinutes > 0) "after" else "before"} ${NumberFormat.getNumberInstance(locale).format(kotlin.math.abs(rule.offsetMinutes) / 60.0)} hours"
    }
}

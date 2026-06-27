package com.woshiwangnima.healthdietpro.model.chart

import android.content.Context
import com.google.gson.Gson
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.model.unit.UnitRepository

class ChartTimeConfigRepository(
    private val context: Context,
    private val unitRepo: UnitRepository
) {
    private var config: ChartTimeConfig? = null

    fun load(): ChartTimeConfig {
        config?.let { return it }
        val json = context.assets.open("chart_time_config.json")
            .bufferedReader().use { it.readText() }
        val parsed = Gson().fromJson(json, ChartTimeConfig::class.java)

        val intervalOptions = parsed.intervalOptions
        val sortedRules = parsed.autoIntervalRules.map { rule ->
            rule.copy(
                availableIntervalIds = rule.availableIntervalIds.sortedBy { id ->
                    intervalOptions.find { it.id == id }?.quantity?.toMillis(unitRepo) ?: 0L
                }
            )
        }
        val sorted = parsed.copy(autoIntervalRules = sortedRules)
        config = sorted
        return sorted
    }

    fun resolveMs(quantity: Quantity?): Long {
        return quantity?.toMillis(unitRepo) ?: 0L
    }

    fun getDisplayName(option: TimeRangeOptionDef): String {
        option.quantity?.let { return it.getDisplayName(unitRepo) }
        return when (option.id) {
            "all" -> context.getString(R.string.chart_time_range_all)
            else -> option.id
        }
    }

    fun getDisplayName(option: IntervalOptionDef): String {
        option.quantity?.let { return it.getDisplayName(unitRepo) }
        return when (option.id) {
            "auto" -> context.getString(R.string.chart_interval_auto)
            else -> option.id
        }
    }

    fun getAvailableIntervalOptions(visibleRangeMs: Long): List<IntervalOptionDef> {
        val config = load()
        val rule = findMatchingRule(visibleRangeMs)
        val ids = rule.availableIntervalIds
        if (ids.isEmpty()) return config.intervalOptions
        return ids.mapNotNull { id -> config.intervalOptions.find { it.id == id } }
    }

    fun findIntervalOptionByMs(ms: Long): IntervalOptionDef? {
        return load().intervalOptions.find { resolveMs(it.quantity) == ms }
    }

    fun computeAutoIntervalMs(visibleRangeMs: Long): Long {
        val config = load()
        val rule = findMatchingRule(visibleRangeMs)
        val option = config.intervalOptions.find { it.id == rule.autoIntervalId }
        return resolveMs(option?.quantity)
    }

    fun getFormatForIntervalMs(ms: Long): String? {
        return findIntervalOptionByMs(ms)?.format
    }

    private fun findMatchingRule(visibleRangeMs: Long): AutoIntervalRuleDef {
        val rules = load().autoIntervalRules
        for (rule in rules) {
            val thresholdMs = resolveMs(rule.maxVisible)
            if (visibleRangeMs <= thresholdMs) {
                return rule
            }
        }
        return rules.last()
    }
}

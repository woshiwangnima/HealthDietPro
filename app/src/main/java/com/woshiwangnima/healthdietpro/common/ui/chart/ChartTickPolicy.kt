package com.woshiwangnima.healthdietpro.common.ui.chart

import com.woshiwangnima.healthdietpro.model.chart.ChartTimeConfig
import com.woshiwangnima.healthdietpro.model.unit.UnitRepository
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

data class ChartTickOption(
    val id: String,
    val interval: Double,
    val label: String,
)

data class ChartTickResolution(
    val selectedId: String,
    val interval: Double,
    val options: List<ChartTickOption>,
)

interface ChartTickPolicy {
    fun resolve(visibleRange: Double, selectedId: String?): ChartTickResolution
}

object NumericChartTickPolicy : ChartTickPolicy {
    override fun resolve(visibleRange: Double, selectedId: String?): ChartTickResolution {
        val range = visibleRange.takeIf { it.isFinite() && it > 0.0 } ?: 1.0
        val base = niceStep(range / 5.0)
        val options = listOf(base / 5.0, base / 2.0, base, base * 2.0, base * 5.0)
            .filter { it.isFinite() && it > 0.0 }
            .distinct()
            .map { interval ->
                ChartTickOption(
                    id = "numeric:${interval.toStableId()}",
                    interval = interval,
                    label = interval.formatNumber(),
                )
            }
        val selected = options.firstOrNull { it.id == selectedId }
            ?: options.firstOrNull { it.interval == base }
            ?: options.first()
        return ChartTickResolution(selected.id, selected.interval, options)
    }
}

class TimeChartTickPolicy private constructor(
    private val rules: List<TimeTickRule>,
) : ChartTickPolicy {
    override fun resolve(visibleRange: Double, selectedId: String?): ChartTickResolution {
        val rangeMs = visibleRange.toLong().coerceAtLeast(1L)
        val rule = rules.firstOrNull { rangeMs <= it.maxVisibleMs } ?: rules.last()
        val selected = rule.options.firstOrNull { it.id == selectedId }
            ?: rule.options.firstOrNull { it.id == rule.autoOptionId }
            ?: rule.options.first()
        return ChartTickResolution(selected.id, selected.interval, rule.options)
    }

    internal fun calendarTicks(min: Double, max: Double, optionId: String?): List<Double>? {
        val interval = rules.asSequence()
            .flatMap { it.options.asSequence() }
            .firstOrNull { it.id == optionId }
            ?: return null
        val months = when (interval.id) {
            "time:1m" -> 1L
            "time:3m" -> 3L
            "time:1y" -> 12L
            else -> return null
        }
        val zone = ZoneId.systemDefault()
        var cursor = Instant.ofEpochMilli(min.toLong())
            .atZone(zone)
            .withDayOfMonth(1)
            .truncatedTo(ChronoUnit.DAYS)
            .withMonth(((Instant.ofEpochMilli(min.toLong()).atZone(zone).monthValue - 1) / months.toInt()) * months.toInt() + 1)
        while (cursor.toInstant().toEpochMilli() > min) cursor = cursor.minusMonths(months)
        return buildList {
            while (cursor.toInstant().toEpochMilli() <= max && size < 1000) {
                val value = cursor.toInstant().toEpochMilli().toDouble()
                if (value >= min) add(value)
                cursor = cursor.plusMonths(months)
            }
        }
    }

    companion object {
        fun fromConfig(config: ChartTimeConfig, unitRepository: UnitRepository): TimeChartTickPolicy {
            val optionsById = config.intervalOptions.mapNotNull { option ->
                val quantity = option.quantity ?: return@mapNotNull null
                val intervalMs = quantity.toMillis(unitRepository).toDouble()
                if (intervalMs <= 0.0) return@mapNotNull null
                option.id to ChartTickOption(
                    id = "time:${option.id}",
                    interval = intervalMs,
                    label = quantity.getDisplayName(unitRepository),
                )
            }.toMap()
            val rules = config.autoIntervalRules.mapNotNull { rule ->
                val options = rule.availableIntervalIds.mapNotNull { id -> optionsById[id] }
                if (options.isEmpty()) return@mapNotNull null
                TimeTickRule(
                    maxVisibleMs = rule.maxVisible.toMillis(unitRepository),
                    autoOptionId = "time:${rule.autoIntervalId}",
                    options = options,
                )
            }
            require(rules.isNotEmpty()) { "Time tick configuration must contain at least one rule" }
            return TimeChartTickPolicy(rules)
        }
    }
}

private data class TimeTickRule(
    val maxVisibleMs: Long,
    val autoOptionId: String,
    val options: List<ChartTickOption>,
)

private fun niceStep(rawStep: Double): Double {
    val magnitude = 10.0.pow(floor(log10(rawStep.coerceAtLeast(Double.MIN_VALUE))))
    return when (val normalized = rawStep / magnitude) {
        in 0.0..1.0 -> magnitude
        in 1.0..2.0 -> 2.0 * magnitude
        in 2.0..5.0 -> 5.0 * magnitude
        else -> 10.0 * magnitude
    }
}

private fun Double.toStableId(): String = "%.12g".format(this)

private fun Double.formatNumber(): String = when {
    this >= 1.0 && this % 1.0 == 0.0 -> toLong().toString()
    else -> "%.2f".format(this).trimEnd('0').trimEnd('.')
}

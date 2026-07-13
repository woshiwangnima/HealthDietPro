package com.woshiwangnima.healthdietpro.common.ui.chart

import com.woshiwangnima.healthdietpro.model.chart.AutoIntervalRuleDef
import com.woshiwangnima.healthdietpro.model.chart.ChartTimeConfig
import com.woshiwangnima.healthdietpro.model.chart.IntervalOptionDef
import com.woshiwangnima.healthdietpro.model.chart.Quantity
import com.woshiwangnima.healthdietpro.model.chart.TimeRangeOptionDef
import com.woshiwangnima.healthdietpro.model.unit.UnitRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartTickPolicyTest {

    @Test
    fun numericPolicyExposesReadableDiscreteSteps() {
        val resolution = NumericChartTickPolicy.resolve(4.8, null)

        assertEquals(1.0, resolution.interval, 0.0)
        assertTrue(resolution.options.map { it.interval }.containsAll(listOf(0.2, 0.5, 1.0, 2.0, 5.0)))
    }

    @Test
    fun timePolicyUsesRangeBucketAndFallsBackFromInvalidSelection() {
        val policy = TimeChartTickPolicy.fromConfig(testConfig(), UnitRepository.fromAsset("src/main/assets/units.json"))

        val dayResolution = policy.resolve(24.0 * HOUR_MS, "time:1min")
        assertEquals("time:2h", dayResolution.selectedId)
        assertEquals(listOf("time:1h", "time:2h", "time:6h"), dayResolution.options.map { it.id })

        val weekResolution = policy.resolve(7.0 * DAY_MS, "time:2h")
        assertEquals("time:1d", weekResolution.selectedId)
    }

    @Test
    fun timePolicyKeepsValidFixedSelectionWithinRangeBucket() {
        val policy = TimeChartTickPolicy.fromConfig(testConfig(), UnitRepository.fromAsset("src/main/assets/units.json"))

        val resolution = policy.resolve(24.0 * HOUR_MS, "time:6h")

        assertEquals("time:6h", resolution.selectedId)
        assertEquals(6.0 * HOUR_MS, resolution.interval, 0.0)
    }

    private fun testConfig() = ChartTimeConfig(
        timeRangeOptions = emptyList<TimeRangeOptionDef>(),
        intervalOptions = listOf(
            interval("1min", 1, "min"),
            interval("1h", 1, "h"),
            interval("2h", 2, "h"),
            interval("6h", 6, "h"),
            interval("1d", 1, "d"),
        ),
        autoIntervalRules = listOf(
            rule(1, "d", "2h", listOf("1h", "2h", "6h")),
            rule(1, "week", "1d", listOf("6h", "1d")),
        ),
    )

    private fun interval(id: String, value: Int, unit: String) =
        IntervalOptionDef(id, Quantity(value.toDouble(), unit), null)

    private fun rule(max: Int, unit: String, autoId: String, options: List<String>) =
        AutoIntervalRuleDef(Quantity(max.toDouble(), unit), autoId, options)

    private companion object {
        const val HOUR_MS = 3_600_000.0
        const val DAY_MS = 86_400_000.0
    }
}

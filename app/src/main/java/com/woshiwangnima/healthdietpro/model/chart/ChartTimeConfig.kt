package com.woshiwangnima.healthdietpro.model.chart

data class ChartTimeConfig(
    val timeRangeOptions: List<TimeRangeOptionDef>,
    val intervalOptions: List<IntervalOptionDef>,
    val autoIntervalRules: List<AutoIntervalRuleDef>
)

data class TimeRangeOptionDef(
    val id: String,
    val quantity: Quantity?
)

data class IntervalOptionDef(
    val id: String,
    val quantity: Quantity?,
    val format: String?
)

data class AutoIntervalRuleDef(
    val maxVisible: Quantity,
    val autoIntervalId: String,
    val availableIntervalIds: List<String>
)

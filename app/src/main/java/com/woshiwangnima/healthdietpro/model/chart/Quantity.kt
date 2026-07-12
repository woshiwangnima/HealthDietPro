package com.woshiwangnima.healthdietpro.model.chart

import com.woshiwangnima.healthdietpro.model.unit.UnitRepository

data class Quantity(
    val value: Double,
    val unit: String
) {
    fun toMillis(unitRepo: UnitRepository): Long {
        val unitDef = unitRepo.getUnit("time", unit) ?: return 0L
        return (value * unitDef.toBase * 1000).toLong()
    }

    fun getDisplayName(unitRepo: UnitRepository): String {
        val unitDef = unitRepo.getUnit("time", unit)
        val symbol = unitDef?.symbol() ?: unit
        if (value == value.toLong().toDouble()) {
            return "${value.toLong()}$symbol"
        }
        return "$value$symbol"
    }
}

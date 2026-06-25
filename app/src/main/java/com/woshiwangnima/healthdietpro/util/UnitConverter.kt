package com.woshiwangnima.healthdietpro.util

import android.content.Context
import com.woshiwangnima.healthdietpro.model.unit.UnitRepository

object UnitConverter {
    private var repository: UnitRepository? = null

    fun init(context: Context) {
        repository = UnitRepository(context)
        repository?.getCategories()
    }

    fun getRepository(): UnitRepository? = repository

    fun toBase(category: String, value: Float, fromUnitId: String): Float {
        val repo = repository ?: return value
        val unit = repo.getUnit(category, fromUnitId) ?: return value
        return value * unit.toBase
    }

    fun fromBase(category: String, baseValue: Float, toUnitId: String): Float {
        val repo = repository ?: return baseValue
        val unit = repo.getUnit(category, toUnitId) ?: return baseValue
        return baseValue / unit.toBase
    }

    fun formatWithUnit(category: String, baseValue: Float, unitId: String, locale: String = "zh"): String {
        val repo = repository ?: return "$baseValue"
        val unit = repo.getUnit(category, unitId) ?: return "$baseValue"
        val converted = fromBase(category, baseValue, unitId)
        val symbol = if (locale == "zh") unit.symbolCn else unit.symbolEn
        return if (unitId == "ft") {
            formatHeightFtIn(baseValue)
        } else {
            "%.1f %s".format(converted, symbol)
        }
    }

    fun formatHeightFtIn(baseValueCm: Float): String {
        val totalInches = (baseValueCm / 2.54).toInt()
        val ft = totalInches / 12
        val inc = totalInches % 12
        return "%d'%d\"".format(ft, inc)
    }

    fun parseHeightFtIn(input: String): Float {
        val regex = Regex("""(\d+)'(\d+)""")
        val match = regex.find(input.trim())
        return if (match != null) {
            val ft = match.groupValues[1].toFloat()
            val inc = match.groupValues[2].toFloat()
            ft * 30.48f + inc * 2.54f
        } else {
            input.toFloatOrNull() ?: 0f
        }
    }
}

package com.woshiwangnima.healthdietpro.model.food

import com.woshiwangnima.healthdietpro.common.range.RangeBand
import com.woshiwangnima.healthdietpro.common.range.findRangeBand

internal enum class GlycemicClassification { Low, Medium, High }

private val glycemicIndexBands = listOf(
    RangeBand(max = 55.0, maxInclusive = true, value = GlycemicClassification.Low),
    RangeBand(min = 55.0, minInclusive = false, max = 69.0, maxInclusive = true, value = GlycemicClassification.Medium),
    RangeBand(min = 69.0, minInclusive = false, value = GlycemicClassification.High),
)

private val glycemicLoadBands = listOf(
    RangeBand(max = 10.0, maxInclusive = true, value = GlycemicClassification.Low),
    RangeBand(min = 10.0, minInclusive = false, max = 19.0, maxInclusive = true, value = GlycemicClassification.Medium),
    RangeBand(min = 19.0, minInclusive = false, value = GlycemicClassification.High),
)

internal fun classifyGlycemicIndex(value: Double): GlycemicClassification? =
    value.findRangeBand(glycemicIndexBands)?.value

internal fun classifyGlycemicLoad(value: Double): GlycemicClassification? =
    value.findRangeBand(glycemicLoadBands)?.value

internal fun glycemicIndexClassificationBands(): List<RangeBand<Double, GlycemicClassification>> = glycemicIndexBands

internal fun glycemicLoadClassificationBands(): List<RangeBand<Double, GlycemicClassification>> = glycemicLoadBands

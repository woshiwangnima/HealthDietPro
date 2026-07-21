package com.woshiwangnima.healthdietpro.model.food

import com.woshiwangnima.healthdietpro.common.range.NumericRangeBand
import com.woshiwangnima.healthdietpro.common.range.findRangeBand

internal enum class GlycemicClassification { Low, Medium, High }

private val glycemicIndexBands = listOf(
    NumericRangeBand(max = 55.0, maxInclusive = true, value = GlycemicClassification.Low),
    NumericRangeBand(min = 55.0, minInclusive = false, max = 69.0, maxInclusive = true, value = GlycemicClassification.Medium),
    NumericRangeBand(min = 69.0, minInclusive = false, value = GlycemicClassification.High),
)

private val glycemicLoadBands = listOf(
    NumericRangeBand(max = 10.0, maxInclusive = true, value = GlycemicClassification.Low),
    NumericRangeBand(min = 10.0, minInclusive = false, max = 19.0, maxInclusive = true, value = GlycemicClassification.Medium),
    NumericRangeBand(min = 19.0, minInclusive = false, value = GlycemicClassification.High),
)

internal fun classifyGlycemicIndex(value: Double): GlycemicClassification? =
    value.findRangeBand(glycemicIndexBands)?.value

internal fun classifyGlycemicLoad(value: Double): GlycemicClassification? =
    value.findRangeBand(glycemicLoadBands)?.value

internal fun glycemicIndexClassificationBands(): List<NumericRangeBand<GlycemicClassification>> = glycemicIndexBands

internal fun glycemicLoadClassificationBands(): List<NumericRangeBand<GlycemicClassification>> = glycemicLoadBands

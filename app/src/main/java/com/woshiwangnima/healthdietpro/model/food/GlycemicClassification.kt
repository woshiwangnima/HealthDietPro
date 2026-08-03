package com.woshiwangnima.healthdietpro.model.food

import com.woshiwangnima.healthdietpro.common.range.RangeBand
import com.woshiwangnima.healthdietpro.common.range.findRangeBand

internal enum class GlycemicClassification { Low, Medium, High }

/** Combined GI/GL display score. GI and GL use the established low/medium/high cutoffs below. */
internal data class GlycemicLevel(val level: Int, val classification: GlycemicClassification) {
    val fillPercent: Float get() = level / 5f
}

internal fun glycemicLevel(gi: Double?, gl: Double?): GlycemicLevel? {
    val giClass = gi?.let(::classifyGlycemicIndex)
    val glClass = gl?.let(::classifyGlycemicLoad)
    if (giClass == null && glClass == null) return null
    val scores = listOfNotNull(giClass, glClass).map { when (it) {
        GlycemicClassification.Low -> 1
        GlycemicClassification.Medium -> 3
        GlycemicClassification.High -> 5
    } }
    val score = (scores.average().toInt()).coerceIn(1, 5)
    return GlycemicLevel(score, when {
        score <= 2 -> GlycemicClassification.Low
        score <= 3 -> GlycemicClassification.Medium
        else -> GlycemicClassification.High
    })
}

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

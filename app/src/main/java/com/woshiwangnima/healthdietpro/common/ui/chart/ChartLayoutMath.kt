package com.woshiwangnima.healthdietpro.common.ui.chart

import kotlin.math.abs

internal object ChartLayoutMath {

    fun selectNonOverlappingLabelIndexes(
        centers: List<Float>,
        sizes: List<Float>,
        gap: Float,
    ): List<Int> {
        require(centers.size == sizes.size)
        if (centers.isEmpty()) return emptyList()
        val result = mutableListOf<Int>()
        var lastEnd = Float.NEGATIVE_INFINITY
        centers.indices.sortedBy { centers[it] }.forEach { index ->
            val start = centers[index] - sizes[index] / 2f
            val end = centers[index] + sizes[index] / 2f
            if (start >= lastEnd + gap || result.isEmpty()) {
                result += index
                lastEnd = end
            }
        }
        return result
    }

    fun panDelta(
        dragPixels: Float,
        trackPixels: Float,
        fullRange: Double,
        visibleRange: Double,
        positiveDirection: Double,
    ): Double {
        if (trackPixels <= 0f || fullRange <= visibleRange) return 0.0
        val movablePercent = (100.0 - visibleRange / fullRange * 100.0).coerceAtLeast(0.0)
        return dragPixels / trackPixels * movablePercent / 100.0 * fullRange * positiveDirection
    }

    fun percentStep(fullRange: Double, percent: Double = 0.5): Double =
        fullRange * percent / 100.0

    fun nearlyEqual(first: Double, second: Double): Boolean = abs(first - second) < 0.000001
}

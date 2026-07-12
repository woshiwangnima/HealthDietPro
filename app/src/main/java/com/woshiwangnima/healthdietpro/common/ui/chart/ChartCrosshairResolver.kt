package com.woshiwangnima.healthdietpro.common.ui.chart

import kotlin.math.abs

internal object ChartCrosshairResolver {

    fun resolve(
        points: List<ComposeChartPoint>,
        lineStyle: ComposeChartLineStyle,
        basis: ComposeChartCrosshairBasis,
        rawX: Double,
        rawY: Double,
    ): ComposeChartPoint? {
        val sorted = points.sortedBy { it.x }
        if (sorted.size < 2) return null
        return when (basis) {
            ComposeChartCrosshairBasis.PerpendicularToXAxis -> pointOnSeriesByX(sorted, rawX, lineStyle)
            ComposeChartCrosshairBasis.PerpendicularToYAxis -> pointOnSeriesByY(sorted, rawY, rawX, lineStyle)
        }
    }

    private fun pointOnSeriesByX(
        points: List<ComposeChartPoint>,
        x: Double,
        lineStyle: ComposeChartLineStyle,
    ): ComposeChartPoint? {
        if (x < points.first().x || x > points.last().x) return null
        return ComposeChartPoint(x = x, y = interpolateY(points, x, lineStyle))
    }

    private fun pointOnSeriesByY(
        points: List<ComposeChartPoint>,
        y: Double,
        preferredX: Double,
        lineStyle: ComposeChartLineStyle,
    ): ComposeChartPoint? {
        val candidateXs = buildList {
            points.zipWithNext { start, end ->
                val segmentStart = start.x
                val segmentEnd = end.x
                when (lineStyle) {
                    ComposeChartLineStyle.SteppedFront -> {
                        if (approximatelyEqual(start.y, y)) add(preferredX.coerceIn(segmentStart, segmentEnd))
                        if (y in closedRange(start.y, end.y)) add(segmentEnd)
                    }
                    ComposeChartLineStyle.SteppedBack -> {
                        if (approximatelyEqual(end.y, y)) add(preferredX.coerceIn(segmentStart, segmentEnd))
                        if (y in closedRange(start.y, end.y)) add(segmentStart)
                    }
                    else -> addAll(findRoots(points, lineStyle, segmentStart, segmentEnd, y))
                }
            }
        }
        val x = candidateXs.minByOrNull { abs(it - preferredX) } ?: return null
        return ComposeChartPoint(x = x, y = interpolateY(points, x, lineStyle))
    }

    private fun findRoots(
        points: List<ComposeChartPoint>,
        lineStyle: ComposeChartLineStyle,
        startX: Double,
        endX: Double,
        targetY: Double,
    ): List<Double> {
        val startValue = interpolateY(points, startX, lineStyle) - targetY
        val endValue = interpolateY(points, endX, lineStyle) - targetY
        if (approximatelyEqual(startValue, 0.0)) return listOf(startX)
        if (approximatelyEqual(endValue, 0.0)) return listOf(endX)
        if (startValue * endValue > 0.0) return emptyList()

        var low = startX
        var high = endX
        var lowValue = startValue
        repeat(48) {
            val middle = (low + high) / 2.0
            val middleValue = interpolateY(points, middle, lineStyle) - targetY
            if (approximatelyEqual(middleValue, 0.0)) return listOf(middle)
            if (lowValue * middleValue <= 0.0) {
                high = middle
            } else {
                low = middle
                lowValue = middleValue
            }
        }
        return listOf((low + high) / 2.0)
    }

    private fun interpolateY(
        points: List<ComposeChartPoint>,
        x: Double,
        lineStyle: ComposeChartLineStyle,
    ): Double {
        val nextIndex = points.indexOfFirst { it.x >= x }.let { if (it == -1) points.lastIndex else it }
        val currentIndex = (nextIndex - 1).coerceAtLeast(0)
        val current = points[currentIndex]
        val next = points[nextIndex.coerceAtLeast(1)]
        val span = (next.x - current.x).takeIf { abs(it) > EPSILON } ?: return current.y
        val t = ((x - current.x) / span).coerceIn(0.0, 1.0)
        return when (lineStyle) {
            ComposeChartLineStyle.Linear -> current.y + (next.y - current.y) * t
            ComposeChartLineStyle.SteppedFront -> current.y
            ComposeChartLineStyle.SteppedBack -> next.y
            ComposeChartLineStyle.Bezier,
            ComposeChartLineStyle.Spline,
            ComposeChartLineStyle.CatmullRom,
            ComposeChartLineStyle.Monotone -> interpolateSmoothY(points, x, lineStyle)
        }
    }

    private fun interpolateSmoothY(
        points: List<ComposeChartPoint>,
        x: Double,
        lineStyle: ComposeChartLineStyle,
    ): Double {
        val nextIndex = points.indexOfFirst { it.x >= x }.let { if (it == -1) points.lastIndex else it }
        val currentIndex = (nextIndex - 1).coerceAtLeast(0)
        val current = points[currentIndex]
        val next = points[nextIndex.coerceAtLeast(1)]
        val span = (next.x - current.x).takeIf { abs(it) > EPSILON } ?: return current.y
        val t = ((x - current.x) / span).coerceIn(0.0, 1.0)
        val y0 = points.getOrNull(currentIndex - 1)?.y ?: current.y
        val y1 = current.y
        val y2 = next.y
        val y3 = points.getOrNull(nextIndex + 1)?.y ?: next.y
        val value = when (lineStyle) {
            ComposeChartLineStyle.Bezier,
            ComposeChartLineStyle.Spline,
            ComposeChartLineStyle.Monotone -> {
                val eased = t * t * (3.0 - 2.0 * t)
                y1 + (y2 - y1) * eased
            }
            ComposeChartLineStyle.CatmullRom -> catmullRom(y0, y1, y2, y3, t)
            else -> y1 + (y2 - y1) * t
        }
        return if (lineStyle == ComposeChartLineStyle.Monotone || lineStyle == ComposeChartLineStyle.CatmullRom) {
            value.coerceIn(minOf(y1, y2), maxOf(y1, y2))
        } else {
            value
        }
    }

    private fun catmullRom(p0: Double, p1: Double, p2: Double, p3: Double, t: Double): Double {
        val t2 = t * t
        val t3 = t2 * t
        return 0.5 * ((2.0 * p1) + (-p0 + p2) * t + (2.0 * p0 - 5.0 * p1 + 4.0 * p2 - p3) * t2 + (-p0 + 3.0 * p1 - 3.0 * p2 + p3) * t3)
    }

    private fun approximatelyEqual(first: Double, second: Double): Boolean = abs(first - second) <= EPSILON

    private fun closedRange(first: Double, second: Double): ClosedFloatingPointRange<Double> =
        minOf(first, second)..maxOf(first, second)

    private const val EPSILON = 0.000001
}

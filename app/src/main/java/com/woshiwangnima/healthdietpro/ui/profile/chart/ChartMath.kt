package com.woshiwangnima.healthdietpro.ui.profile.chart

import kotlin.math.*

object ChartMath {

    data class ChartEntry(val x: Float, val y: Float)

    fun toChartEntries(points: List<com.woshiwangnima.healthdietpro.model.profile.DataPoint>, baseTs: Long): List<ChartEntry> =
        points.map { ChartEntry((it.timestamp - baseTs).toFloat(), it.value) }

    fun findSegmentIndex(entries: List<ChartEntry>, x: Float): Int {
        if (entries.size < 2) return 0
        for (i in 0 until entries.size - 1) {
            if (x >= entries[i].x && x <= entries[i + 1].x) return i
        }
        return (entries.size - 2).coerceAtLeast(0)
    }

    fun interpolateLinear(entries: List<ChartEntry>, x: Float): Float {
        if (entries.size < 2) return entries.firstOrNull()?.y ?: 0f
        val i = findSegmentIndex(entries, x)
        val a = entries[i]
        val b = entries[i + 1]
        val span = b.x - a.x
        val t = if (span == 0f) 0f else (x - a.x) / span
        return a.y + t * (b.y - a.y)
    }

    fun interpolateSteppedFront(entries: List<ChartEntry>, x: Float): Float {
        if (entries.size < 2) return entries.firstOrNull()?.y ?: 0f
        val i = findSegmentIndex(entries, x)
        return entries[i].y
    }

    fun interpolateSteppedBack(entries: List<ChartEntry>, x: Float): Float {
        if (entries.size < 2) return entries.firstOrNull()?.y ?: 0f
        val i = findSegmentIndex(entries, x)
        return entries[i + 1].y
    }

    fun interpolateBezier(entries: List<ChartEntry>, x: Float): Float {
        val i = findSegmentIndex(entries, x)
        if (i < 0 || i + 1 >= entries.size) return interpolateLinear(entries, x)
        val p0 = entries[i]
        val p3 = entries[i + 1]
        val dx = p3.x - p0.x
        if (dx == 0f) return interpolateLinear(entries, x)
        val dy = p3.y - p0.y
        val cIntensity = 0.2f
        val prevY = if (i > 0) entries[i - 1].y else p0.y - dy
        val nextY = if (i + 2 < entries.size) entries[i + 2].y else p3.y + dy
        val cp1y = p0.y + dy * cIntensity + (p3.y - prevY) * cIntensity * 0.5f
        val cp2y = p3.y - dy * cIntensity - (nextY - p0.y) * cIntensity * 0.5f
        val t = ((x - p0.x) / dx).coerceIn(0f, 1f)
        val ti = 1f - t
        return ti * ti * ti * p0.y + 3f * ti * ti * t * cp1y + 3f * ti * t * t * cp2y + t * t * t * p3.y
    }

    fun interpolateCatmullRom(entries: List<ChartEntry>, x: Float): Float {
        val n = entries.size
        if (n < 2) return entries.firstOrNull()?.y ?: 0f
        val i = findSegmentIndex(entries, x)
        if (i < 0 || i + 1 >= n) return interpolateLinear(entries, x)

        val p0 = if (i > 0) entries[i - 1] else entries[i]
        val p1 = entries[i]
        val p2 = entries[i + 1]
        val p3 = if (i + 2 < n) entries[i + 2] else entries[i + 1]

        val span = p2.x - p1.x
        val t = if (span == 0f) 0f else ((x - p1.x) / span).coerceIn(0f, 1f)
        val t2 = t * t
        val t3 = t2 * t
        return 0.5f * (
            (2f * p1.y) +
            (-p0.y + p2.y) * t +
            (2f * p0.y - 5f * p1.y + 4f * p2.y - p3.y) * t2 +
            (-p0.y + 3f * p1.y - 3f * p2.y + p3.y) * t3
        )
    }

    fun interpolateMonotone(entries: List<ChartEntry>, x: Float): Float {
        val n = entries.size
        if (n < 2) return entries.firstOrNull()?.y ?: 0f
        val i = findSegmentIndex(entries, x)
        if (i < 0 || i + 1 >= n) return interpolateLinear(entries, x)

        val d = FloatArray(n - 1)
        for (k in 0 until n - 1) {
            val dx = entries[k + 1].x - entries[k].x
            d[k] = if (dx == 0f) 0f else (entries[k + 1].y - entries[k].y) / dx
        }

        val m = FloatArray(n)
        for (k in 1 until n - 1) {
            val dk = d[k - 1]
            val dk1 = d[k]
            if (dk * dk1 <= 0f) {
                m[k] = 0f
            } else {
                val h0 = entries[k].x - entries[k - 1].x
                val h1 = entries[k + 1].x - entries[k].x
                val w1 = 2f * h1 + h0
                val w2 = h1 + 2f * h0
                val raw = (w1 + w2) / (w1 / dk + w2 / dk1)
                m[k] = min(abs(raw), 3f * min(abs(dk), abs(dk1))) * sign(raw)
            }
        }
        m[0] = d[0]
        m[n - 1] = d[n - 2]

        val h = entries[i + 1].x - entries[i].x
        if (h == 0f) return entries[i].y
        val t = ((x - entries[i].x) / h).coerceIn(0f, 1f)
        val t2 = t * t
        val t3 = t2 * t
        val h00 = 2f * t3 - 3f * t2 + 1f
        val h10 = (t3 - 2f * t2 + t) * h
        val h01 = -2f * t3 + 3f * t2
        val h11 = (t3 - t2) * h
        return h00 * entries[i].y + h10 * m[i] + h01 * entries[i + 1].y + h11 * m[i + 1]
    }

    fun interpolateSpline(entries: List<ChartEntry>, x: Float): Float {
        val i = findSegmentIndex(entries, x)
        if (i < 0 || i + 1 >= entries.size) return interpolateLinear(entries, x)
        val n = entries.size
        if (n < 3) return interpolateBezier(entries, x)

        val h = FloatArray(n - 1) { k -> entries[k + 1].x - entries[k].x }
        val alpha = FloatArray(n - 1)
        for (k in 1 until n - 1) {
            val hk = h[k]
            val hk1 = h[k - 1]
            if (hk + hk1 == 0f) {
                alpha[k] = 0f
            } else {
                alpha[k] = (3f / hk) * (entries[k + 1].y - entries[k].y) -
                           (3f / hk1) * (entries[k].y - entries[k - 1].y)
            }
        }
        val c = FloatArray(n)
        val l = FloatArray(n) { 1f }
        val mu = FloatArray(n)
        val z = FloatArray(n)
        for (k in 1 until n - 1) {
            val hk = h[k]
            val hk1 = h[k - 1]
            l[k] = 2f * (entries[k + 1].x - entries[k - 1].x) - hk1 * mu[k - 1]
            if (l[k] == 0f) l[k] = 1f
            mu[k] = hk / l[k]
            z[k] = (alpha[k] - hk1 * z[k - 1]) / l[k]
        }
        for (j in n - 2 downTo 0) {
            c[j] = z[j] - mu[j] * c[j + 1]
        }

        val p0 = entries[i]
        val p1 = entries[i + 1]
        val hi = h[i]
        if (hi == 0f) return p0.y
        val dx = x - p0.x
        val a = p0.y
        val b = (p1.y - p0.y) / hi - hi * (2f * c[i] + c[i + 1]) / 3f
        val cc = c[i]
        val d = (c[i + 1] - c[i]) / (3f * hi)
        return a + b * dx + cc * dx * dx + d * dx * dx * dx
    }

    fun interpolateY(entries: List<ChartEntry>, x: Float, style: LineStyle): Float = when (style) {
        LineStyle.LINEAR -> interpolateLinear(entries, x)
        LineStyle.BEZIER -> interpolateBezier(entries, x)
        LineStyle.SPLINE -> interpolateSpline(entries, x)
        LineStyle.CATMULL_ROM -> interpolateCatmullRom(entries, x)
        LineStyle.MONOTONE -> interpolateMonotone(entries, x)
        LineStyle.STEPPED_FRONT -> interpolateSteppedFront(entries, x)
        LineStyle.STEPPED_BACK -> interpolateSteppedBack(entries, x)
    }

    fun niceScale(minV: Float, maxV: Float, maxTicks: Int = 5): Pair<Float, Float> {
        if (minV == maxV) return (minV - 1f) to (maxV + 1f)
        val range = niceNum(maxV - minV, false)
        val step = niceNum(range / (maxTicks - 1), true)
        val niceMin = floor(minV / step) * step
        val niceMax = ceil(maxV / step) * step
        return niceMin to niceMax
    }

    private fun niceNum(range: Float, round: Boolean): Float {
        val exp = floor(log10(range.toDouble())).toFloat()
        val fraction = range / 10f.pow(exp)
        val nice = if (round) {
            when {
                fraction <= 1.5f -> 1f
                fraction <= 3f -> 2f
                fraction <= 7f -> 5f
                else -> 10f
            }
        } else {
            when {
                fraction <= 1f -> 1f
                fraction <= 2f -> 2f
                fraction <= 5f -> 5f
                else -> 10f
            }
        }
        return nice * 10f.pow(exp)
    }

    fun computeLabelInterval(visibleRangeMs: Long): Long {
        val h = 3_600_000L
        val d = 24 * h
        val w = 7 * d
        val m = 30 * d
        return when {
            visibleRangeMs <= 1 * h -> 5 * 60_000L
            visibleRangeMs <= 6 * h -> 30 * 60_000L
            visibleRangeMs <= 1 * d -> 2 * h
            visibleRangeMs <= 1 * w -> d
            visibleRangeMs <= 1 * m -> 3 * d
            visibleRangeMs <= 6 * m -> w
            visibleRangeMs <= 365 * d -> m
            else -> 3 * m
        }
    }
}

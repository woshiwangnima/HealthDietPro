package com.woshiwangnima.healthdietpro.ui.profile.chart

import com.github.mikephil.charting.data.Entry

object ChartSegmentMath {

    fun findSegmentIndex(entries: List<Entry>, x: Float): Int {
        if (entries.size < 2) return 0
        for (i in 0 until entries.size - 1) {
            if (x >= entries[i].x && x <= entries[i + 1].x) return i
        }
        return (entries.size - 2).coerceAtLeast(0)
    }

    fun interpolateLinear(entries: List<Entry>, x: Float): Float {
        if (entries.size < 2) return entries.firstOrNull()?.y ?: 0f
        val i = findSegmentIndex(entries, x)
        val a = entries[i]
        val b = entries[i + 1]
        val span = b.x - a.x
        val t = if (span == 0f) 0f else (x - a.x) / span
        return a.y + t * (b.y - a.y)
    }

    fun interpolateStepped(entries: List<Entry>, x: Float): Float {
        if (entries.size < 2) return entries.firstOrNull()?.y ?: 0f
        val i = findSegmentIndex(entries, x)
        return entries[i + 1].y
    }
}

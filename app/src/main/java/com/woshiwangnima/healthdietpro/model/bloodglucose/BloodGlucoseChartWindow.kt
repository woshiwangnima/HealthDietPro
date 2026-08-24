package com.woshiwangnima.healthdietpro.model.bloodglucose

import com.woshiwangnima.healthdietpro.common.range.Range
import com.woshiwangnima.healthdietpro.common.range.RangeBand
import com.woshiwangnima.healthdietpro.common.range.findRangeBand
import kotlin.math.roundToLong

internal enum class BloodGlucoseChartWindow(val durationMillis: Long, val tickMillis: Long) {
    Hours3(10_800_000L, 1_800_000L),
    Hours6(21_600_000L, 3_600_000L),
    Hours12(43_200_000L, 7_200_000L),
    Hours24(86_400_000L, 14_400_000L),
}

internal data class BloodGlucoseChartSlice(
    val scoped: List<BloodGlucoseRecord>,
    val primary: List<BloodGlucoseRecord>,
    val delayed: List<BloodGlucoseRecord>,
    val windowStart: Long,
    val windowEnd: Long,
    val historicalMaximum: Double,
)

internal enum class GlucoseTimeRangeBand { HIGH, IN_RANGE, LOW }

internal data class GlucoseTimeRangeDistribution(
    val highMillis: Long = 0L,
    val inRangeMillis: Long = 0L,
    val lowMillis: Long = 0L,
) {
    val coveredMillis: Long get() = highMillis + inRangeMillis + lowMillis

    fun millisFor(band: GlucoseTimeRangeBand): Long = when (band) {
        GlucoseTimeRangeBand.HIGH -> highMillis
        GlucoseTimeRangeBand.IN_RANGE -> inRangeMillis
        GlucoseTimeRangeBand.LOW -> lowMillis
    }
}

/** Reference thresholds for the percentage of observed time in each glucose band. */
internal val glucoseTimeReferenceRanges = listOf(
    RangeBand(max = 20f, maxInclusive = false, value = GlucoseTimeRangeBand.HIGH),
    RangeBand(min = 70f, minInclusive = false, value = GlucoseTimeRangeBand.IN_RANGE),
    RangeBand(max = 4f, maxInclusive = false, value = GlucoseTimeRangeBand.LOW),
)

/** Maps absolute glucose change rate (mmol/L/min) to the trend indicator magnitude. */
internal val bloodGlucoseTrendRateRanges = listOf(
    RangeBand(min = 0.0, minInclusive = true, max = 0.05, maxInclusive = true, value = 0),
    RangeBand(min = 0.05, minInclusive = false, max = 0.09, maxInclusive = true, value = 1),
    RangeBand(min = 0.09, minInclusive = false, max = 0.13, maxInclusive = true, value = 2),
    RangeBand(min = 0.13, minInclusive = false, max = 0.17, maxInclusive = true, value = 3),
    RangeBand(min = 0.17, minInclusive = false, max = null, value = 4),
)

/** Maps the latest pair's per-minute rate of change to -4..4; zero represents stable glucose. */
internal fun bloodGlucoseParticleLevel(
    previous: BloodGlucoseRecord,
    latest: BloodGlucoseRecord,
): Int {
    val elapsedMinutes = (latest.timestamp - previous.timestamp) / 60_000.0
    if (elapsedMinutes <= 0.0) return 0
    val ratePerMinute = (latest.valueMmolPerL - previous.valueMmolPerL) / elapsedMinutes
    val magnitude = kotlin.math.abs(ratePerMinute).findRangeBand(bloodGlucoseTrendRateRanges)?.value ?: 4
    return if (ratePerMinute >= 0.0) magnitude else -magnitude
}

/**
 * Splits each adjacent observed glucose segment at target-range boundaries. Time outside
 * adjacent observations is deliberately excluded because its glucose state is unknown.
 */
internal fun calculateGlucoseTimeRangeDistribution(
    records: List<BloodGlucoseRecord>,
    targetRange: Range<Float>,
): GlucoseTimeRangeDistribution {
    val ordered = records.distinctBy(BloodGlucoseRecord::timestamp).sortedBy(BloodGlucoseRecord::timestamp)
    var highMillis = 0L
    var inRangeMillis = 0L
    var lowMillis = 0L
    ordered.zipWithNext().forEach { (start, end) ->
        val totalMillis = (end.timestamp - start.timestamp).coerceAtLeast(0L)
        if (totalMillis == 0L) return@forEach
        val boundaries = buildList {
            targetRange.min?.let { add(it.toDouble()) }
            targetRange.max?.let { add(it.toDouble()) }
        }
        val fractions = buildList {
            add(0.0)
            boundaries.forEach { boundary ->
                segmentCrossingFraction(start.valueMmolPerL, end.valueMmolPerL, boundary)?.let(::add)
            }
            add(1.0)
        }.distinct().sorted()
        var allocatedMillis = 0L
        fractions.zipWithNext().forEachIndexed { index, (from, to) ->
            val durationMillis = if (index == fractions.lastIndex - 1) {
                totalMillis - allocatedMillis
            } else {
                (totalMillis * (to - from)).roundToLong()
            }
            allocatedMillis += durationMillis
            when (classifyGlucoseValue(start.valueMmolPerL + (end.valueMmolPerL - start.valueMmolPerL) * ((from + to) / 2.0), targetRange)) {
                GlucoseTimeRangeBand.HIGH -> highMillis += durationMillis
                GlucoseTimeRangeBand.IN_RANGE -> inRangeMillis += durationMillis
                GlucoseTimeRangeBand.LOW -> lowMillis += durationMillis
            }
        }
    }
    return GlucoseTimeRangeDistribution(highMillis, inRangeMillis, lowMillis)
}

private fun segmentCrossingFraction(start: Double, end: Double, boundary: Double): Double? {
    val change = end - start
    if (change == 0.0) return null
    return ((boundary - start) / change).takeIf { it > 0.0 && it < 1.0 }
}

private fun classifyGlucoseValue(value: Double, targetRange: Range<Float>): GlucoseTimeRangeBand = when {
    targetRange.contains(value.toFloat()) -> GlucoseTimeRangeBand.IN_RANGE
    targetRange.min?.let { value < it || (!targetRange.minInclusive && value <= it) } == true -> GlucoseTimeRangeBand.LOW
    else -> GlucoseTimeRangeBand.HIGH
}

/** Immutable timestamp index used by the rolling blood-glucose chart. */
internal class BloodGlucoseChartIndex(records: List<BloodGlucoseRecord>) {
    private val ordered = records.sortedBy(BloodGlucoseRecord::timestamp)

    val historicalMaximum: Double = ordered.maxOfOrNull(BloodGlucoseRecord::valueMmolPerL)?.coerceAtLeast(1.0) ?: 1.0

    fun slice(start: Long, end: Long): List<BloodGlucoseRecord> {
        if (start > end || ordered.isEmpty()) return emptyList()
        val from = ordered.lowerBound(start)
        val until = ordered.upperBound(end)
        return ordered.subList(from, until)
    }
}

internal fun BloodGlucoseChartIndex.scopedSlice(
    scopeStart: Long,
    scopeEnd: Long,
    requestedWindowEnd: Long,
    window: BloodGlucoseChartWindow,
): BloodGlucoseChartSlice {
    val scoped = slice(scopeStart, scopeEnd)
    val earliest = scoped.firstOrNull()?.timestamp ?: scopeStart
    // The chart can continue through the end of the selected time range even
    // when no glucose record exists at the right edge.
    val end = requestedWindowEnd.coerceIn(earliest, scopeEnd)
    val start = end - window.durationMillis
    return BloodGlucoseChartSlice(
        scoped = scoped,
        primary = slice(maxOf(scopeStart, start), minOf(scopeEnd, end)),
        delayed = slice(maxOf(scopeStart, start - window.durationMillis), minOf(scopeEnd, start - 1)),
        windowStart = start,
        windowEnd = end,
        historicalMaximum = historicalMaximum,
    )
}

private fun List<BloodGlucoseRecord>.lowerBound(timestamp: Long): Int {
    val result = binarySearchBy(timestamp) { it.timestamp }
    return if (result < 0) -result - 1 else result
}

private fun List<BloodGlucoseRecord>.upperBound(timestamp: Long): Int {
    var index = lowerBound(timestamp)
    while (index < size && this[index].timestamp == timestamp) index++
    return index
}

internal fun buildBloodGlucoseChartSlice(
    records: List<BloodGlucoseRecord>,
    scopeStart: Long,
    scopeEnd: Long,
    windowEnd: Long,
    window: BloodGlucoseChartWindow,
): BloodGlucoseChartSlice {
    return BloodGlucoseChartIndex(records).scopedSlice(scopeStart, scopeEnd, windowEnd, window)
}

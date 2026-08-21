package com.woshiwangnima.healthdietpro.model.bloodglucose

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
    val latest = scoped.lastOrNull()?.timestamp ?: scopeEnd
    val earliest = scoped.firstOrNull()?.timestamp ?: scopeStart
    val end = requestedWindowEnd.coerceIn(earliest, latest)
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

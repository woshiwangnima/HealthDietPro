package com.woshiwangnima.healthdietpro.common.range

internal data class NumericRangeBand<T>(
    val min: Double? = null,
    val minInclusive: Boolean = true,
    val max: Double? = null,
    val maxInclusive: Boolean = false,
    val value: T,
) {
    fun contains(number: Double): Boolean {
        val meetsMinimum = min == null || if (minInclusive) number >= min else number > min
        val meetsMaximum = max == null || if (maxInclusive) number <= max else number < max
        return meetsMinimum && meetsMaximum
    }
}

internal fun <T> Double.findRangeBand(bands: List<NumericRangeBand<T>>): NumericRangeBand<T>? =
    bands.firstOrNull { it.contains(this) }

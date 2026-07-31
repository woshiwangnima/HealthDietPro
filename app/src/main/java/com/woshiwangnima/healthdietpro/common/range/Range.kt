package com.woshiwangnima.healthdietpro.common.range

import com.woshiwangnima.healthdietpro.model.unit.UnitReference
import kotlinx.serialization.Serializable

/** A comparable range with optional infinite endpoints and independently configurable bounds. */
abstract class Range<T : Comparable<T>> {
    abstract val min: T?
    abstract val minInclusive: Boolean
    abstract val max: T?
    abstract val maxInclusive: Boolean

    fun contains(value: T): Boolean {
        val lower = min
        val upper = max
        val meetsMinimum = lower == null || if (minInclusive) value >= lower else value > lower
        val meetsMaximum = upper == null || if (maxInclusive) value <= upper else value < upper
        return meetsMinimum && meetsMaximum
    }

    protected fun validateEndpoints() {
        val lower = min
        val upper = max
        require(lower == null || upper == null || lower <= upper) { "Range minimum must not exceed maximum" }
    }
}

/** A [Range] whose endpoints share one quantity category and unit. */
@Serializable
data class UnitRange<T : Comparable<T>>(
    override val min: T? = null,
    override val minInclusive: Boolean = true,
    override val max: T? = null,
    override val maxInclusive: Boolean = true,
    val unitCategory: String,
    val unitId: String,
) : Range<T>() {
    init {
        validateEndpoints()
    }
}

/** A [Range] that allows each finite endpoint to specify its own unit. */
@Serializable
data class EndpointUnitRange<T : Comparable<T>>(
    override val min: T? = null,
    override val minInclusive: Boolean = true,
    val minUnit: UnitReference? = null,
    override val max: T? = null,
    override val maxInclusive: Boolean = true,
    val maxUnit: UnitReference? = null,
) : Range<T>() {
    init {
        validateEndpoints()
        require(min == null || minUnit != null) { "A finite minimum requires a unit" }
        require(max == null || maxUnit != null) { "A finite maximum requires a unit" }
    }
}

/** Associates a [Range] with an application-defined value. */
data class RangeBand<T : Comparable<T>, V>(
    override val min: T? = null,
    override val minInclusive: Boolean = true,
    override val max: T? = null,
    override val maxInclusive: Boolean = false,
    val value: V,
) : Range<T>() {
    init {
        validateEndpoints()
    }
}

fun <T : Comparable<T>, V> T.findRangeBand(bands: List<RangeBand<T, V>>): RangeBand<T, V>? =
    bands.firstOrNull { it.contains(this) }

enum class CriterionOperator { All, Any }

fun interface Criterion<T> {
    fun matches(value: T): Boolean
}

class RangeCriterion<T, V : Comparable<V>>(
    private val range: Range<V>,
    private val valueOf: (T) -> V,
) : Criterion<T> {
    override fun matches(value: T): Boolean = range.contains(valueOf(value))
}

fun <T> matchesCriteria(
    value: T,
    operator: CriterionOperator,
    criteria: List<Criterion<T>>,
): Boolean = when (operator) {
    CriterionOperator.All -> criteria.all { it.matches(value) }
    CriterionOperator.Any -> criteria.any { it.matches(value) }
}

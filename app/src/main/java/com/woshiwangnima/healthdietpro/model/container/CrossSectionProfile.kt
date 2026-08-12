package com.woshiwangnima.healthdietpro.model.container

import kotlin.math.min
import kotlin.math.sqrt

/** Position of a zero-area cross-section along the container height. */
internal enum class ZeroAreaPosition { BOTTOM, TOP, MIDDLE }

/** Physical-consistency check result for a cross-section profile. */
internal sealed interface CrossSectionValidation {

    /** Area decreases with height — possibly a kettle, requires user confirmation. */
    data class AreaDecreasing(
        val lowerHeight: Double,
        val upperHeight: Double,
        val lowerArea: Double,
        val upperArea: Double,
    ) : CrossSectionValidation

    /** Zero area at a middle height — physically implausible. */
    data class ZeroAreaAtMiddle(val heightCm: Double) : CrossSectionValidation
}

/**
 * A container's cross-section profile: key transition points plus the container's total height.
 *
 * Key-point semantics: the user enters cross-sections only at meaningful transition points
 * (转折处). The container height is supplied separately.
 *
 * Volume integration:
 * - single point → uniform prism: area × height
 * - similar adjacent shapes → straight-walled frustum: H/3 × (A1 + A2 + √(A1·A2))
 * - dissimilar/transition adjacent shapes → linear (trapezoid) interpolation
 * - below the first point and above the last point → parallel extension (constant area)
 *
 * **不变量**：所有长度（[totalHeightCm]、[CrossSection.heightCm]、形状线性尺寸）均以基准
 * 长度单位 cm 存储。显示单位由 [lengthUnitId] 提示，UI 边界经
 * `UnitConverter.toBase / fromBase` 换算；[lengthUnitId] 不可信为值的实际单位。
 *
 * Immutable. Construction validates ordering, bounds and completeness.
 */
internal data class CrossSectionProfile(
    val points: List<CrossSection>,
    val totalHeightCm: Double,
    val lengthUnitId: String = "cm",
) {
    init {
        require(points.isNotEmpty()) { "At least 1 cross-section point required" }
        require(totalHeightCm > 0.0) { "totalHeightCm must be > 0, got $totalHeightCm" }
        val heights = points.map { it.heightCm }
        require(heights == heights.sorted()) { "Points must be sorted by heightCm" }
        require(heights.distinct().size == heights.size) { "Duplicate heightCm not allowed" }
        require(heights.all { it in 0.0..totalHeightCm }) {
            "Point height must be within [0, totalHeightCm], got $heights"
        }
    }

    /** Integrates volume over [0, heightCm]. Returns cm³ (= ml). */
    fun volumeUpTo(heightCm: Double): Double {
        if (heightCm <= 0.0 || points.isEmpty()) return 0.0
        val clamped = heightCm.coerceIn(0.0, totalHeightCm)
        var volumeCm3 = 0.0
        val first = points.first()

        if (clamped <= first.heightCm) {
            return clamped * first.shape.area
        }
        volumeCm3 += first.heightCm * first.shape.area

        if (points.size == 1) {
            return volumeCm3 + (clamped - first.heightCm) * first.shape.area
        }

        for (i in 0 until points.size - 1) {
            val lower = points[i]
            val upper = points[i + 1]
            if (clamped <= lower.heightCm) break
            val segEnd = min(clamped, upper.heightCm)
            val coveredHeight = segEnd - lower.heightCm
            if (coveredHeight <= 0.0) continue
            volumeCm3 += segmentVolume(
                lowerShape = lower.shape,
                upperShape = upper.shape,
                coveredHeight = coveredHeight,
                fullHeight = upper.heightCm - lower.heightCm,
            )
        }

        val last = points.last()
        if (clamped > last.heightCm) {
            volumeCm3 += (clamped - last.heightCm) * last.shape.area
        }
        return volumeCm3
    }

    /**
     * Volume of a segment between two key points, covered up to [coveredHeight] of [fullHeight].
     *
     * - Similar shapes → straight-walled frustum (area varies with the square of the linear
     *   scale); lower area 0 collapses to a cone.
     * - Dissimilar shapes → linear area interpolation (trapezoid).
     */
    private fun segmentVolume(
        lowerShape: CrossSectionShape,
        upperShape: CrossSectionShape,
        coveredHeight: Double,
        fullHeight: Double,
    ): Double {
        if (fullHeight <= 0.0) return 0.0
        val u = (coveredHeight / fullHeight).coerceIn(0.0, 1.0)
        val a1 = lowerShape.area
        val a2 = upperShape.area
        if (!lowerShape.similarTo(upperShape)) {
            val aAt = a1 + (a2 - a1) * u
            return coveredHeight * (a1 + aAt) / 2.0
        }
        if (a1 <= 0.0) {
            return a2 * fullHeight * u * u * u / 3.0
        }
        val k = sqrt(a2 / a1)
        if (k == 1.0) return coveredHeight * a1
        val s = 1.0 + (k - 1.0) * u
        return a1 * fullHeight * (s * s * s - 1.0) / (3.0 * (k - 1.0))
    }

    /** Total volume in ml (1 cm³ = 1 ml). */
    fun totalVolumeMl(): Double = volumeUpTo(totalHeightCm)

    /** Cross-sectional area (cm²) at an arbitrary height (quadratic for similar, linear otherwise, constant beyond). */
    fun areaAt(heightCm: Double): Double {
        val clamped = heightCm.coerceIn(0.0, totalHeightCm)
        if (points.size == 1) return points[0].shape.area
        val first = points.first()
        val last = points.last()
        if (clamped <= first.heightCm) return first.shape.area
        if (clamped >= last.heightCm) return last.shape.area
        for (i in 0 until points.size - 1) {
            val lower = points[i]
            val upper = points[i + 1]
            if (clamped in lower.heightCm..upper.heightCm) {
                val u = (clamped - lower.heightCm) / (upper.heightCm - lower.heightCm)
                val a1 = lower.shape.area
                val a2 = upper.shape.area
                if (lower.shape.similarTo(upper.shape)) {
                    if (a1 <= 0.0) return a2 * u * u
                    val k = sqrt(a2 / a1)
                    val s = 1.0 + (k - 1.0) * u
                    return a1 * s * s
                }
                return a1 + (a2 - a1) * u
            }
        }
        return last.shape.area
    }

    /** Validation result list. Empty = valid. */
    fun validate(): List<CrossSectionValidation> = buildList {
        for (point in points) {
            if (point.shape.area <= 0.0) {
                val position = if (point.heightCm == 0.0) ZeroAreaPosition.BOTTOM
                else if (point.heightCm == totalHeightCm) ZeroAreaPosition.TOP
                else ZeroAreaPosition.MIDDLE
                if (position == ZeroAreaPosition.MIDDLE) {
                    add(CrossSectionValidation.ZeroAreaAtMiddle(point.heightCm))
                }
            }
        }
        for (i in 0 until points.size - 1) {
            val lower = points[i]
            val upper = points[i + 1]
            if (upper.shape.area < lower.shape.area) {
                add(
                    CrossSectionValidation.AreaDecreasing(
                        lowerHeight = lower.heightCm,
                        upperHeight = upper.heightCm,
                        lowerArea = lower.shape.area,
                        upperArea = upper.shape.area,
                    ),
                )
                break
            }
        }
    }
}

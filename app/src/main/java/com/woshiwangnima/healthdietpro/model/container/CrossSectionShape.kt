package com.woshiwangnima.healthdietpro.model.container

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sqrt

/** Horizontal cross-section shape family. */
internal enum class ShapeKind { CIRCLE, SQUARE, RECTANGLE, IRREGULAR }

/**
 * Horizontal cross-section shape domain interface.
 *
 * Each shape stores its own geometry parameters (user input); [area] and [perimeter] are
 * derived. All linear dimensions are stored in the base length unit (cm); irregular area is
 * stored in cm². Domain types are not @Serializable; serialization goes through the
 * [CrossSectionShapeDto] boundary.
 */
internal sealed interface CrossSectionShape {

    /** Cross-sectional area in cm². */
    val area: Double

    /** Perimeter in cm. Optional, reserved for advanced estimates. */
    val perimeter: Double?

    /** Shape family identifier used by the DTO kind field and the UI switcher. */
    val kind: ShapeKind

    /** Parameter summary for UI display (e.g. "⌀ 8.0 cm", "6.0 × 4.0 cm"). */
    fun paramSummary(): String

    /**
     * Whether this and [other] can be lofted as similar straight-walled solids (frustum/cone).
     * For similar shapes the cross-section area varies quadratically with height; for dissimilar
     * shapes it is approximated linearly.
     */
    fun similarTo(other: CrossSectionShape): Boolean
}

/** Circular cross-section. Parameter: diameter (cm, base). */
internal data class CircleShape(
    val diameterCm: Double,
) : CrossSectionShape {
    override val kind: ShapeKind get() = ShapeKind.CIRCLE
    override val area: Double get() = PI * (diameterCm / 2.0) * (diameterCm / 2.0)
    override val perimeter: Double get() = PI * diameterCm
    override fun paramSummary(): String = "⌀ %.1f cm".format(diameterCm)
    override fun similarTo(other: CrossSectionShape): Boolean = other is CircleShape
}

/** Square cross-section. Parameter: side length (cm, base). */
internal data class SquareShape(
    val sideLengthCm: Double,
) : CrossSectionShape {
    override val kind: ShapeKind get() = ShapeKind.SQUARE
    override val area: Double get() = sideLengthCm * sideLengthCm
    override val perimeter: Double get() = 4.0 * sideLengthCm
    override fun paramSummary(): String = "%.1f × %.1f cm".format(sideLengthCm, sideLengthCm)
    override fun similarTo(other: CrossSectionShape): Boolean = other is SquareShape
}

/** Rectangle cross-section. Parameters: length (cm) × width (cm), base. */
internal data class RectangleShape(
    val lengthCm: Double,
    val widthCm: Double,
) : CrossSectionShape {
    override val kind: ShapeKind get() = ShapeKind.RECTANGLE
    override val area: Double get() = lengthCm * widthCm
    override val perimeter: Double get() = 2.0 * (lengthCm + widthCm)
    override fun paramSummary(): String = "%.1f × %.1f cm".format(lengthCm, widthCm)
    override fun similarTo(other: CrossSectionShape): Boolean =
        other is RectangleShape &&
            widthCm > 0.0 && other.widthCm > 0.0 &&
            abs(lengthCm / widthCm - other.lengthCm / other.widthCm) < 1e-6
}

/**
 * Irregular cross-section. Parameters: user-entered area (cm²) and optional perimeter (cm).
 * Area has no dedicated unit category in the unit system; it is stored in the cm² base and
 * documented as such. Never treated as similar (similarity cannot be verified from area alone).
 */
internal data class IrregularShape(
    override val area: Double,
    override val perimeter: Double? = null,
) : CrossSectionShape {
    override val kind: ShapeKind get() = ShapeKind.IRREGULAR
    override fun paramSummary(): String = "S = %.1f cm²".format(area)
    override fun similarTo(other: CrossSectionShape): Boolean = false
}

/** Equivalent diameter in cm for a given area (cm²), for side-view width mapping. */
internal fun CrossSectionShape.equivalentDiameterCm(): Double = 2.0 * sqrt(area / PI)

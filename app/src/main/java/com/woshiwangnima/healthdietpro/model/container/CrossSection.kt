package com.woshiwangnima.healthdietpro.model.container

/**
 * A cross-section of a container at a given height.
 *
 * Height is stored in the base length unit (cm) measured from the bottom. Key-point
 * semantics: the user only enters cross-sections at meaningful transition points; below
 * the first point and above the last point the profile extends in parallel (constant area).
 *
 * **不变量**：[heightCm] 始终为基准长度单位（cm）。显示单位经
 * `UnitConverter.toBase / fromBase` 换算，见 [CrossSectionProfile.lengthUnitId]。
 *
 * @param heightCm height from the bottom in cm (0.0 = bottom)
 * @param shape the cross-section shape at this height
 */
internal data class CrossSection(
    val heightCm: Double,
    val shape: CrossSectionShape,
) {
    init {
        require(heightCm >= 0.0) { "heightCm must be >= 0, got $heightCm" }
    }
}

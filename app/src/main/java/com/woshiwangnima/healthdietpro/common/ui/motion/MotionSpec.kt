package com.woshiwangnima.healthdietpro.common.ui.motion

import androidx.compose.animation.core.Spring
import androidx.compose.runtime.Immutable

@Immutable
internal data class MotionSpec(
    val durationMillis: Int,
    val delayMillis: Int = 0,
    val easing: EasingCurve = EasingCurve.CubicInOut,
    val springDampingRatio: Float? = null,
    val springStiffness: Float? = null,
) {
    val usesSpring: Boolean
        get() = springDampingRatio != null && springStiffness != null
}

internal fun springMotionSpec(
    dampingRatio: Float = Spring.DampingRatioNoBouncy,
    stiffness: Float = Spring.StiffnessMediumLow,
): MotionSpec =
    MotionSpec(
        durationMillis = 0,
        springDampingRatio = dampingRatio,
        springStiffness = stiffness,
    )

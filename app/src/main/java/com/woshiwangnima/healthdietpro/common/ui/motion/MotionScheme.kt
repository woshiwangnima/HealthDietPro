package com.woshiwangnima.healthdietpro.common.ui.motion

import androidx.compose.runtime.Immutable

@Immutable
internal data class MotionScheme(
    val immediate: MotionSpec,
    val fastEffects: MotionSpec,
    val standardEffects: MotionSpec,
    val emphasizedEffects: MotionSpec,
    val spatialMovement: MotionSpec,
    val expressiveMovement: MotionSpec,
)

internal fun motionScheme(policy: MotionPolicy = MotionPolicy.Standard): MotionScheme =
    when (policy) {
        MotionPolicy.Standard -> MotionScheme(
            immediate = MotionSpec(durationMillis = 0, easing = EasingCurve.Linear),
            fastEffects = MotionSpec(durationMillis = 120, easing = EasingCurve.SineInOut),
            standardEffects = MotionSpec(durationMillis = 180, easing = EasingCurve.CubicInOut),
            emphasizedEffects = MotionSpec(durationMillis = 240, easing = EasingCurve.CubicInOut),
            spatialMovement = springMotionSpec(),
            expressiveMovement = MotionSpec(durationMillis = 260, easing = EasingCurve.BackOut),
        )

        MotionPolicy.Reduced -> MotionScheme(
            immediate = MotionSpec(durationMillis = 0, easing = EasingCurve.Linear),
            fastEffects = MotionSpec(durationMillis = 80, easing = EasingCurve.CubicOut),
            standardEffects = MotionSpec(durationMillis = 100, easing = EasingCurve.CubicOut),
            emphasizedEffects = MotionSpec(durationMillis = 120, easing = EasingCurve.CubicOut),
            spatialMovement = MotionSpec(durationMillis = 0, easing = EasingCurve.Linear),
            expressiveMovement = MotionSpec(durationMillis = 100, easing = EasingCurve.CubicOut),
        )
    }

internal fun MotionSpec.reduced(policy: MotionPolicy): MotionSpec =
    if (policy == MotionPolicy.Reduced) {
        when {
            durationMillis == 0 -> this
            easing.isExpressive() || usesSpring -> MotionSpec(durationMillis = 100, easing = EasingCurve.CubicOut)
            else -> copy(durationMillis = durationMillis.coerceAtMost(120), easing = EasingCurve.CubicOut)
        }
    } else {
        this
    }

private fun EasingCurve.isExpressive(): Boolean =
    name.startsWith("Back") || name.startsWith("Elastic") || name.startsWith("Bounce")

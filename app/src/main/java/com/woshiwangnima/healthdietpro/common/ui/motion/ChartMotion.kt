package com.woshiwangnima.healthdietpro.common.ui.motion

internal data class ChartMotion(
    val pathReveal: MotionSpec,
    val selection: MotionSpec,
    val background: MotionSpec,
)

internal fun chartMotion(scheme: MotionScheme): ChartMotion =
    ChartMotion(
        pathReveal = scheme.emphasizedEffects.copy(easing = EasingCurve.CubicInOut),
        selection = scheme.fastEffects.copy(easing = EasingCurve.CubicOut),
        background = scheme.fastEffects.copy(easing = EasingCurve.SineInOut),
    )

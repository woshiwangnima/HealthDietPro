package com.woshiwangnima.healthdietpro.common.ui.motion

internal data class ButtonMotion(
    val press: MotionSpec,
    val release: MotionSpec,
    val expressiveFeedback: MotionSpec,
)

internal fun buttonMotion(scheme: MotionScheme): ButtonMotion =
    ButtonMotion(
        press = scheme.fastEffects.copy(easing = EasingCurve.CubicInOut),
        release = scheme.fastEffects.copy(easing = EasingCurve.CubicOut),
        expressiveFeedback = scheme.expressiveMovement.copy(easing = EasingCurve.BackOut),
    )

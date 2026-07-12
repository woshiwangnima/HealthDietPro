package com.woshiwangnima.healthdietpro.common.ui.motion

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FloatSpringSpec
import androidx.compose.animation.core.tween

internal fun MotionSpec.asFloatAnimationSpec(): AnimationSpec<Float> =
    if (usesSpring) {
        FloatSpringSpec(
            dampingRatio = springDampingRatio ?: 1f,
            stiffness = springStiffness ?: 1f,
        )
    } else {
        tween(
            durationMillis = durationMillis,
            delayMillis = delayMillis,
            easing = easing,
        )
    }

package com.woshiwangnima.healthdietpro.common.ui.motion

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

internal fun easingTransform(
    curve: EasingCurve,
    fraction: Float,
): Float {
    val t = fraction.coerceIn(0f, 1f)
    return when (curve) {
        EasingCurve.Linear -> t

        EasingCurve.SineIn -> (1f - cos((t * PI) / 2f)).toFloat()
        EasingCurve.SineOut -> sin((t * PI) / 2f).toFloat()
        EasingCurve.SineInOut -> (-(cos(PI * t) - 1f) / 2f).toFloat()

        EasingCurve.QuadIn -> t * t
        EasingCurve.QuadOut -> 1f - (1f - t) * (1f - t)
        EasingCurve.QuadInOut -> if (t < 0.5f) 2f * t * t else 1f - (-2f * t + 2f).pow(2) / 2f

        EasingCurve.CubicIn -> t * t * t
        EasingCurve.CubicOut -> 1f - (1f - t).pow(3)
        EasingCurve.CubicInOut -> if (t < 0.5f) 4f * t * t * t else 1f - (-2f * t + 2f).pow(3) / 2f

        EasingCurve.QuartIn -> t.pow(4)
        EasingCurve.QuartOut -> 1f - (1f - t).pow(4)
        EasingCurve.QuartInOut -> if (t < 0.5f) 8f * t.pow(4) else 1f - (-2f * t + 2f).pow(4) / 2f

        EasingCurve.QuintIn -> t.pow(5)
        EasingCurve.QuintOut -> 1f - (1f - t).pow(5)
        EasingCurve.QuintInOut -> if (t < 0.5f) 16f * t.pow(5) else 1f - (-2f * t + 2f).pow(5) / 2f

        EasingCurve.ExpoIn -> if (t == 0f) 0f else 2f.pow(10f * t - 10f)
        EasingCurve.ExpoOut -> if (t == 1f) 1f else 1f - 2f.pow(-10f * t)
        EasingCurve.ExpoInOut -> when {
            t == 0f -> 0f
            t == 1f -> 1f
            t < 0.5f -> 2f.pow(20f * t - 10f) / 2f
            else -> (2f - 2f.pow(-20f * t + 10f)) / 2f
        }

        EasingCurve.CircIn -> 1f - sqrt(1f - t * t)
        EasingCurve.CircOut -> sqrt(1f - (t - 1f).pow(2))
        EasingCurve.CircInOut -> if (t < 0.5f) {
            (1f - sqrt(1f - (2f * t).pow(2))) / 2f
        } else {
            (sqrt(1f - (-2f * t + 2f).pow(2)) + 1f) / 2f
        }

        EasingCurve.BackIn -> backIn(t)
        EasingCurve.BackOut -> backOut(t)
        EasingCurve.BackInOut -> backInOut(t)

        EasingCurve.ElasticIn -> elasticIn(t)
        EasingCurve.ElasticOut -> elasticOut(t)
        EasingCurve.ElasticInOut -> elasticInOut(t)

        EasingCurve.BounceIn -> 1f - bounceOut(1f - t)
        EasingCurve.BounceOut -> bounceOut(t)
        EasingCurve.BounceInOut -> if (t < 0.5f) {
            (1f - bounceOut(1f - 2f * t)) / 2f
        } else {
            (1f + bounceOut(2f * t - 1f)) / 2f
        }
    }
}

private fun backIn(t: Float): Float {
    val c1 = 1.70158f
    val c3 = c1 + 1f
    return c3 * t * t * t - c1 * t * t
}

private fun backOut(t: Float): Float {
    val c1 = 1.70158f
    val c3 = c1 + 1f
    return 1f + c3 * (t - 1f).pow(3) + c1 * (t - 1f).pow(2)
}

private fun backInOut(t: Float): Float {
    val c1 = 1.70158f
    val c2 = c1 * 1.525f
    return if (t < 0.5f) {
        ((2f * t).pow(2) * ((c2 + 1f) * 2f * t - c2)) / 2f
    } else {
        ((2f * t - 2f).pow(2) * ((c2 + 1f) * (2f * t - 2f) + c2) + 2f) / 2f
    }
}

private fun elasticIn(t: Float): Float {
    if (t == 0f || t == 1f) return t
    val c4 = (2f * PI / 3f).toFloat()
    return -(2f.pow(10f * t - 10f) * sin((t * 10f - 10.75f) * c4))
}

private fun elasticOut(t: Float): Float {
    if (t == 0f || t == 1f) return t
    val c4 = (2f * PI / 3f).toFloat()
    return 2f.pow(-10f * t) * sin((t * 10f - 0.75f) * c4) + 1f
}

private fun elasticInOut(t: Float): Float {
    if (t == 0f || t == 1f) return t
    val c5 = (2f * PI / 4.5f).toFloat()
    return if (t < 0.5f) {
        -(2f.pow(20f * t - 10f) * sin((20f * t - 11.125f) * c5)) / 2f
    } else {
        (2f.pow(-20f * t + 10f) * sin((20f * t - 11.125f) * c5)) / 2f + 1f
    }
}

private fun bounceOut(t: Float): Float {
    val n1 = 7.5625f
    val d1 = 2.75f
    return when {
        t < 1f / d1 -> n1 * t * t
        t < 2f / d1 -> n1 * (t - 1.5f / d1).pow(2) + 0.75f
        t < 2.5f / d1 -> n1 * (t - 2.25f / d1).pow(2) + 0.9375f
        else -> n1 * (t - 2.625f / d1).pow(2) + 0.984375f
    }
}

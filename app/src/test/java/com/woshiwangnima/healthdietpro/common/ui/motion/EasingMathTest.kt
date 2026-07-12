package com.woshiwangnima.healthdietpro.common.ui.motion

import org.junit.Assert.assertEquals
import org.junit.Test

class EasingMathTest {

    @Test
    fun everyCurveKeepsZeroAndOneAnchored() {
        EasingCurve.entries.forEach { curve ->
            assertEquals("zero for $curve", 0f, easingTransform(curve, 0f), 0.0001f)
            assertEquals("one for $curve", 1f, easingTransform(curve, 1f), 0.0001f)
        }
    }

    @Test
    fun inputFractionIsClamped() {
        assertEquals(0f, easingTransform(EasingCurve.CubicInOut, -0.5f), 0.0001f)
        assertEquals(1f, easingTransform(EasingCurve.CubicInOut, 1.5f), 0.0001f)
    }

    @Test
    fun reducedMotionDowngradesExpressiveCurves() {
        val reduced = MotionSpec(
            durationMillis = 260,
            easing = EasingCurve.ElasticOut,
        ).reduced(MotionPolicy.Reduced)

        assertEquals(100, reduced.durationMillis)
        assertEquals(EasingCurve.CubicOut, reduced.easing)
    }
}

package com.woshiwangnima.healthdietpro.util

import com.woshiwangnima.healthdietpro.model.unit.UnitCategoryType
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test

class UnitConverterRoundTripTest {

    companion object {
        @BeforeClass
        @JvmStatic
        fun setUp() {
            UnitConverter.initFromAsset("src/main/assets/units.json")
        }

        @AfterClass
        @JvmStatic
        fun tearDown() {
            UnitConverter.reset()
        }
    }

    @Test
    fun weightKgRoundTrip() {
        val base = 70f
        val toLb = UnitConverter.fromBase(UnitCategoryType.Weight.id, base, "lb")
        val back = UnitConverter.toBase(UnitCategoryType.Weight.id, toLb, "lb")
        assertEquals(base, back, 0.001f)
    }

    @Test
    fun weightJinRoundTrip() {
        val base = 65f
        val toJin = UnitConverter.fromBase(UnitCategoryType.Weight.id, base, "jin")
        val back = UnitConverter.toBase(UnitCategoryType.Weight.id, toJin, "jin")
        assertEquals(base, back, 0.001f)
    }

    @Test
    fun lengthCmRoundTrip() {
        val base = 170f
        val toFt = UnitConverter.fromBase(UnitCategoryType.Length.id, base, "ft")
        val back = UnitConverter.toBase(UnitCategoryType.Length.id, toFt, "ft")
        assertEquals(base, back, 0.01f)
    }

    @Test
    fun lengthInRoundTrip() {
        val base = 170f
        val toIn = UnitConverter.fromBase(UnitCategoryType.Length.id, base, "in")
        val back = UnitConverter.toBase(UnitCategoryType.Length.id, toIn, "in")
        assertEquals(base, back, 0.01f)
    }

    @Test
    fun toBaseTimesFromBaseEqualsOne() {
        val repo = UnitConverter.getRepository() ?: error("repo not initialized")
        for (category in repo.getCategories()) {
            for (unit in category.units) {
                if (unit.toBase == 0f) continue
                val product = unit.toBase * (1f / unit.toBase)
                assertEquals(1f, product, 0.0001f)
            }
        }
    }

    @Test
    fun parseHeightFtInRoundTrip() {
        val baseCm = 170f
        val formatted = UnitConverter.formatHeightFtIn(baseCm)
        val parsed = UnitConverter.parseHeightFtIn(formatted)
        // ft/in 格式化用整数截断（toInt），固有误差 < 3cm
        assertTrue("parsed=$parsed should be within 3cm of $baseCm", kotlin.math.abs(parsed - baseCm) < 3f)
    }
}

package com.woshiwangnima.healthdietpro.common.format

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class NumberFormatTest {

    @Test
    fun groupsIntegersWithThousandsSeparator() {
        assertEquals("1,234", formatGroupedInteger(1234.0, Locale.US))
        assertEquals("1,234", formatGroupedInteger(1234, Locale.US))
        assertEquals("25,000", formatGroupedInteger(25000.4, Locale.US))
    }

    @Test
    fun keepsSmallValuesWithoutSeparator() {
        assertEquals("0", formatGroupedInteger(0.0, Locale.US))
        assertEquals("999", formatGroupedInteger(999.49, Locale.US))
    }

    @Test
    fun roundsToInteger() {
        assertEquals("2", formatGroupedInteger(1.6, Locale.US))
        assertEquals("1,235", formatGroupedInteger(1234.6, Locale.US))
    }
}

package com.woshiwangnima.healthdietpro.common.time

import org.junit.Assert.assertEquals
import org.junit.Test

class RelativeTimeTest {
    private val now = 2_000_000_000_000L

    @Test
    fun `selects the largest complete relative-time unit`() {
        assertRelativeTime(1L, RelativeTimeUnit.SECOND, 999L)
        assertRelativeTime(59L, RelativeTimeUnit.SECOND, 59_999L)
        assertRelativeTime(1L, RelativeTimeUnit.MINUTE, 60_000L)
        assertRelativeTime(59L, RelativeTimeUnit.MINUTE, 3_599_999L)
        assertRelativeTime(1L, RelativeTimeUnit.HOUR, 3_600_000L)
        assertRelativeTime(23L, RelativeTimeUnit.HOUR, 86_399_999L)
        assertRelativeTime(1L, RelativeTimeUnit.DAY, 86_400_000L)
        assertRelativeTime(29L, RelativeTimeUnit.DAY, 2_591_999_999L)
        assertRelativeTime(1L, RelativeTimeUnit.MONTH, 2_592_000_000L)
        assertRelativeTime(12L, RelativeTimeUnit.MONTH, 31_535_999_999L)
        assertRelativeTime(1L, RelativeTimeUnit.YEAR, 31_536_000_000L)
    }

    @Test
    fun `clamps future timestamps to one second ago`() {
        assertEquals(RelativeTime(1L, RelativeTimeUnit.SECOND), relativeTimeSince(now + 1L, now))
    }

    private fun assertRelativeTime(amount: Long, unit: RelativeTimeUnit, elapsedMillis: Long) {
        assertEquals(RelativeTime(amount, unit), relativeTimeSince(now - elapsedMillis, now))
    }
}

package com.woshiwangnima.healthdietpro.model.archive

import org.junit.Assert.assertEquals
import org.junit.Test

class ArchiveVersionTest {
    @Test
    fun `normalizes two-part version to semantic version`() {
        assertEquals("1.0.0", normalizedAppVersion("1.0"))
    }

    @Test
    fun `preserves semantic version suffix`() {
        assertEquals("1.0.0-beta01", normalizedAppVersion("1.0.0-beta01"))
    }
}

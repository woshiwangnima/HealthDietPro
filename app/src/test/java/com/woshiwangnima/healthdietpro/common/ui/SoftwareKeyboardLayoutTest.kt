package com.woshiwangnima.healthdietpro.common.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SoftwareKeyboardLayoutTest {
    @Test
    fun lettersFollowTheRequestedCase() {
        val lowercase = softwareKeyboardLayout(SoftwareKeyboardMode.LETTERS, uppercase = false)
        val uppercase = softwareKeyboardLayout(SoftwareKeyboardMode.LETTERS, uppercase = true)

        assertEquals("q", lowercase.rows.first().first())
        assertEquals("Q", uppercase.rows.first().first())
    }

    @Test
    fun numberAndSymbolLayoutsExposeTheirExpectedKeys() {
        val numberKeys = softwareKeyboardLayout(SoftwareKeyboardMode.NUMBERS, uppercase = false).rows.flatten()
        val symbolKeys = softwareKeyboardLayout(SoftwareKeyboardMode.SYMBOLS, uppercase = false).rows.flatten()

        assertTrue("0" in numberKeys)
        assertTrue("@" in numberKeys)
        assertTrue("#" in symbolKeys)
        assertTrue("\\" in symbolKeys)
    }
}

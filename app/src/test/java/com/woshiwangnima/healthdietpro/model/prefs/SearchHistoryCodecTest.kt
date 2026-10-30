package com.woshiwangnima.healthdietpro.model.prefs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SearchHistoryCodecTest {
    @Test
    fun `serializes histories as a JSON string list without losing special characters`() {
        val entries = listOf("apple, banana", "a\u001Fb", "\"quoted\"")

        assertEquals(entries, deserializeSearchHistory(serializeSearchHistory(entries)))
    }

    @Test
    fun `deserializes legacy separator-delimited histories`() {
        assertEquals(listOf("height", "weight"), deserializeSearchHistory("height\u001Fweight"))
    }

    @Test
    fun `returns an empty list for malformed archived history`() {
        assertEquals(emptyList<String>(), deserializeSearchHistory("not a history"))
    }

    @Test
    fun `pushes the newest term to the top of the stack`() {
        assertEquals(listOf("sleep", "weight"), pushSearchHistory(listOf("weight"), "sleep"))
    }

    @Test
    fun `caps the stack at ten entries by dropping the oldest`() {
        val existing = (1..10).map { "term$it" }

        val history = pushSearchHistory(existing, "newest")

        assertEquals(10, history.size)
        assertEquals("newest", history.first())
        assertEquals("term9", history.last())
        assertFalse(history.contains("term10"))
    }

    @Test
    fun `repeating a term moves it to the top instead of duplicating it`() {
        val history = pushSearchHistory(listOf("Height", "weight"), "height")

        assertEquals(listOf("height", "weight"), history)
    }

    @Test
    fun `blank terms keep the stack untouched`() {
        assertEquals(listOf("weight"), pushSearchHistory(listOf("weight"), "   "))
    }

    @Test
    fun `custom limit is honoured`() {
        assertEquals(listOf("c", "b"), pushSearchHistory(listOf("b", "a"), "c", limit = 2))
    }
}

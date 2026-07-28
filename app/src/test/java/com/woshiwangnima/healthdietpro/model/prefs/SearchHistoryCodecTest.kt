package com.woshiwangnima.healthdietpro.model.prefs

import org.junit.Assert.assertEquals
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
}

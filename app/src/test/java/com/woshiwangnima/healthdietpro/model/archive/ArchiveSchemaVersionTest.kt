package com.woshiwangnima.healthdietpro.model.archive

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ArchiveSchemaVersionTest {
    @Test
    fun `current version is the user archive envelope`() {
        assertEquals(ArchiveSchemaVersion.UserArchiveEnvelope, ArchiveSchemaVersion.Current)
        assertEquals(ArchiveSchemaVersion(1, 4, 0), ArchiveSchemaVersion.Current)
    }

    @Test
    fun `comparison is major then minor then patch`() {
        assertEquals(0, ArchiveSchemaVersion(1, 2, 3).compareTo(ArchiveSchemaVersion(1, 2, 3)))
        assertEquals(1, ArchiveSchemaVersion(2, 0, 0).compareTo(ArchiveSchemaVersion(1, 9, 9)))
        assertEquals(1, ArchiveSchemaVersion(1, 3, 0).compareTo(ArchiveSchemaVersion(1, 2, 9)))
        assertEquals(1, ArchiveSchemaVersion(1, 2, 4).compareTo(ArchiveSchemaVersion(1, 2, 3)))
        assertEquals(-1, ArchiveSchemaVersion(1, 0, 0).compareTo(ArchiveSchemaVersion(1, 4, 0)))
    }

    @Test
    fun `negative components are rejected`() {
        assertThrows(IllegalArgumentException::class.java) { ArchiveSchemaVersion(-1, 0, 0) }
    }

    @Test
    fun `version round-trips through json serializer`() {
        val json = Json
        val encoded = json.encodeToString<ArchiveSchemaVersion>(ArchiveSchemaVersion(1, 4, 0))
        assertEquals("""{"major":1,"minor":4,"patch":0}""", encoded)
        assertEquals(ArchiveSchemaVersion(1, 4, 0), json.decodeFromString<ArchiveSchemaVersion>(encoded))
    }

    @Test
    fun `normalized app version parses partial and suffixed versions`() {
        assertEquals("1.2.3", normalizedAppVersion("1.2.3"))
        assertEquals("1.2.0", normalizedAppVersion("1.2"))
        assertEquals("1.0.0", normalizedAppVersion("1"))
        assertEquals("1.2.3-rc1", normalizedAppVersion("1.2.3-rc1"))
        assertEquals("unknown", normalizedAppVersion(null))
        assertEquals("unknown", normalizedAppVersion("alpha"))
    }
}
package com.woshiwangnima.healthdietpro.model.archive

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class ArchiveSchemaVersionTest {
    @Test
    fun `unversioned archive migrates to current version`() {
        assertEquals(
            ArchiveSchemaVersion.Current,
            migrateArchiveSchemaVersion(null),
        )
    }

    @Test
    fun `legacy integer archive version migrates to current version`() {
        assertEquals(
            ArchiveSchemaVersion.Current,
            migrateArchiveSchemaVersion(archiveSchemaVersionFromLegacy(3)),
        )
    }

    @Test
    fun `plain JSON archive without version field migrates to current version`() {
        val archive = Json.decodeFromString<PlainUserArchive>(
            """{"appVersion":"test","exportedAt":"2026-07-21T00:00:00Z","sourceUserId":"user","profile":{},"preferences":{}}""",
        )

        assertEquals(
            ArchiveSchemaVersion.Current,
            migrateArchiveSchemaVersion(archive.formatVersion),
        )
    }
}

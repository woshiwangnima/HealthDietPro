package com.woshiwangnima.healthdietpro.model.archive

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UserArchiveSnapshotTest {

    @Test
    fun `rebuilds the on-disk folder layout from a flat archive`() {
        val snapshot = buildUserArchiveSnapshot(archive())

        val names = snapshot.root.children.map { it.name }
        assertEquals(listOf("attachments", "body_metrics.json", "profile.json", "user_preferences.json"), names)
        assertEquals(3, snapshot.folderCount)
        assertEquals(5, snapshot.fileCount)
    }

    @Test
    fun `groups attachments by their nested folder`() {
        val snapshot = buildUserArchiveSnapshot(archive())

        val attachments = snapshot.root.children.first { it.name == ARCHIVE_ATTACHMENTS_FOLDER }
        assertEquals(listOf("avatar", "foods"), attachments.children.map { it.name })
        val foods = attachments.children.first { it.name == "foods" }
        assertEquals(ArchiveNodeKind.IMAGE, foods.children.single().kind)
        assertEquals("attachments/foods/x.jpg", foods.children.single().path)
    }

    @Test
    fun `keeps attachment byte size equal to the decoded bytes`() {
        val snapshot = buildUserArchiveSnapshot(archive())

        val avatar = snapshot.files.first { it.name == "a.png" }
        // "QUJD" 解码为 3 字节；预览不能为了量体积把上兆图片真的解一遍。
        assertEquals(3L, avatar.byteSize)
        assertEquals(7L, snapshot.attachmentsBytes)
    }

    @Test
    fun `counts records from the domain payload`() {
        val snapshot = buildUserArchiveSnapshot(archive())

        val bodyMetrics = snapshot.files.first { it.name == "body_metrics.json" }
        assertEquals(2, bodyMetrics.recordCount)
        assertNotNull(bodyMetrics.text)
        assertTrue(bodyMetrics.text!!.contains("heightRecords"))
    }

    @Test
    fun `counts preferences as records`() {
        val snapshot = buildUserArchiveSnapshot(archive())

        val preferences = snapshot.files.first { it.name == "user_preferences.json" }
        assertEquals(1, snapshot.preferenceCount)
        assertEquals(1, preferences.recordCount)
    }

    @Test
    fun `reports the same version as compatible`() {
        val snapshot = buildUserArchiveSnapshot(archive())

        assertEquals(ArchiveCompatibility.CURRENT, snapshot.compatibility)
        assertEquals("1.4.0", snapshot.formatVersion.toString())
        assertEquals("小明", snapshot.userName)
        assertNotNull(snapshot.exportedAtMillis)
    }

    @Test
    fun `older archives stay importable so the migration chain can run`() {
        val snapshot = buildUserArchiveSnapshot(archive(version = "1.0.0"))

        assertEquals(ArchiveCompatibility.OLDER, snapshot.compatibility)
    }

    @Test
    fun `newer archives are flagged instead of parsed optimistically`() {
        val snapshot = buildUserArchiveSnapshot(archive(version = "9.9.9"))

        assertEquals(ArchiveCompatibility.NEWER, snapshot.compatibility)
    }

    @Test
    fun `missing optional domains do not invent files`() {
        val snapshot = buildUserArchiveSnapshot(
            """
            {
              "formatVersion": {"major": 1, "minor": 4, "patch": 0},
              "appVersion": "1.4.0",
              "exportedAt": "2026-11-02T06:30:00Z",
              "sourceUserId": "user-1",
              "profile": {"id": "user-1", "name": "小明"}
            }
            """.trimIndent(),
        )

        assertEquals(listOf("profile.json"), snapshot.root.children.map { it.name })
        assertEquals(1, snapshot.fileCount)
        assertEquals(0, snapshot.preferenceCount)
        assertNull(snapshot.files.single().recordCount)
    }

    @Test
    fun `raw text stays compact but still pretty prints on demand`() {
        val snapshot = buildUserArchiveSnapshot(archive())

        assertTrue(snapshot.rawJson.startsWith("{"))
        assertTrue(!snapshot.rawJson.contains("\n"))
        assertTrue(stableJsonString(snapshot.rawJson, prettyPrint = true).lines().size > 5)
    }

    private fun archive(version: String = "1.4.0"): String {
        val parts = version.split('.')
        return """
        {
          "formatVersion": {"major": ${parts[0]}, "minor": ${parts[1]}, "patch": ${parts[2]}},
          "appVersion": "1.4.0",
          "exportedAt": "2026-11-02T06:30:00Z",
          "sourceUserId": "user-1",
          "metadata": {"id": "user-1", "name": "小明", "gender": "MALE", "avatarFileName": "attachments/avatar/a.png"},
          "profile": {"id": "user-1", "name": "小明", "gender": "MALE", "avatarFileName": "attachments/avatar/a.png"},
          "bodyMetrics": {
            "domainId": "body_metrics",
            "schemaVersion": {"major": 1, "minor": 4, "patch": 0},
            "appVersion": "1.4.0",
            "updatedAtMillis": 1,
            "payload": {"heightRecords": [{"value": 1}, {"value": 2}], "weightRecords": []}
          },
          "userPreferences": {"theme": {"type": "STRING", "value": "dark"}},
          "attachments": [
            {"path": "attachments/avatar/a.png", "contentBase64": "QUJD"},
            {"path": "attachments/foods/x.jpg", "contentBase64": "QUJDRA=="}
          ]
        }
        """.trimIndent()
    }
}

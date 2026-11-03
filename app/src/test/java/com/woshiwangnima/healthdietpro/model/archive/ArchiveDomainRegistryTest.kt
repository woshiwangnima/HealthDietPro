package com.woshiwangnima.healthdietpro.model.archive

import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 注册表的护栏。
 *
 * 「以后加了新存档内容，怎么保证能进导出/导入/预览」这件事不能靠文档：
 * 这里是**编译后立刻会红**的那道网——[UserArchiveBundle] 的领域字段与 [ArchiveDomains]
 * 必须双向一一对应。加字段忘了登记、登记了却没有字段，都会让下面的测试失败。
 */
class ArchiveDomainRegistryTest {

    /** 档案里不是「领域」的字段。加这些要同时想清楚它们怎么落盘。 */
    private val nonDomainFields = setOf(
        "formatVersion",
        "appVersion",
        "exportedAt",
        "sourceUserId",
        "metadata",
        "profile",
        "userPreferences",
        "attachments",
    )

    @Test
    fun `every bundle domain field is registered in the registry`() {
        val bundleFields = UserArchiveBundle.serializer().descriptor.elementNames
            .filterNot { it in nonDomainFields }
            .map(::snakeCase)
            .toSet()
        val registered = ArchiveDomains.filter(ArchiveDomain::readsDisk).map(ArchiveDomain::id).toSet()

        assertEquals(registered, bundleFields)
    }

    @Test
    fun `every registered domain carries a real file name and a label`() {
        ArchiveDomains.forEach { domain ->
            assertTrue("空白 id", domain.id.isNotBlank())
            assertTrue("${domain.id} 的文件名不是 .json", domain.fileName.endsWith(".json"))
            assertNotEquals("${domain.id} 缺少名称资源", 0, domain.labelRes)
        }
        assertEquals(
            ArchiveDomains.map(ArchiveDomain::id).distinct().size,
            ArchiveDomains.size,
        )
        assertEquals(
            ArchiveDomains.map(ArchiveDomain::fileName).distinct().size,
            ArchiveDomains.size,
        )
    }

    @Test
    fun `attach then extract returns the same payload`() {
        ArchiveDomains.filter(ArchiveDomain::readsDisk).forEach { domain ->
            val payload = JsonObject(mapOf("domainId" to JsonPrimitive(domain.id)))
            val bundle = domain.attach(emptyBundle(), payload)

            assertEquals(domain.id, domain.extract(bundle)?.jsonObject?.get("domainId")?.jsonPrimitive?.content)
        }
    }

    @Test
    fun `profile and preferences are labelled but are not part of the domain map`() {
        val profile = ArchiveDomains.first { it.id == "profile" }
        val preferences = ArchiveDomains.first { it.id == "user_preferences" }

        assertTrue(!profile.readsDisk)
        assertTrue(!preferences.readsDisk)
        assertTrue(profile.fileName.endsWith(".json"))
        assertTrue(preferences.fileName.endsWith(".json"))
    }

    @Test
    fun `diet keeps its historical file name instead of following the domain id`() {
        // 领域 id 是 diet_records，磁盘上却一直是 diet.json；写成 diet_records.json 会让
        // DietArchiveStore 读不到，导入后「饮食记录」凭空消失。
        val diet = ArchiveDomains.first { it.id == "diet_records" }

        assertEquals("diet.json", diet.fileName)
    }

    @Test
    fun `preferences are converted to plain json for the archive tree`() {
        val preferences = ArchiveDomains.first { it.id == "user_preferences" }
        val bundle = emptyBundle().copy(
            userPreferences = mapOf(
                "theme" to UserArchivePreference(UserArchivePreferenceType.STRING, JsonPrimitive("dark")),
            ),
        )

        val element: JsonElement? = preferences.extract(bundle)
        assertEquals("STRING", element?.jsonObject?.get("theme")?.jsonObject?.get("type")?.jsonPrimitive?.content)
    }

    @Test
    fun `orphan scan flags unknown domains but ignores bookkeeping files`() {
        val orphans = unregisteredArchiveFileNames(
            listOf(
                "manifest.json",
                "water.json",
                "diet.json",
                "custom_things.json",
                "water.json.prev",
                "water.json.corrupt-1730000000000",
                "attachments",
            ),
        )

        // 只有真正没登记过的领域文件会被点名：清单、备份、目录都不是。
        assertEquals(listOf("custom_things.json"), orphans)
    }

    private fun emptyBundle(): UserArchiveBundle = UserArchiveBundle(
        formatVersion = ArchiveSchemaVersion.Current,
        appVersion = "test",
        exportedAt = "2026-11-02T00:00:00Z",
        sourceUserId = "user-1",
        profile = JsonObject(mapOf("id" to JsonPrimitive("user-1"))),
    )

    private fun snakeCase(field: String): String = buildString {
        field.forEach { character ->
            if (character.isUpperCase()) {
                append('_').append(character.lowercaseChar())
            } else {
                append(character)
            }
        }
    }
}

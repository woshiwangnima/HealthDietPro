package com.woshiwangnima.healthdietpro.model.archive

import java.io.File
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * 存档守卫的行为约定。
 *
 * 这几条对应真机上出过的事：版本升级后旧存档被判定非法，读取失败又被折叠成空档案，
 * 下一次写入就把历史覆盖掉。测试要保证的是「旧存档能读」和「读不出来也不丢字节」。
 */
class ArchiveFileGuardTest {
    @get:Rule
    val temp = TemporaryFolder()

    @Test
    fun `missing file reads as null and creates nothing`() {
        val file = File(temp.root, "sample.json")

        assertNull(json.decodeDomainSafely(file, DOMAIN, SamplePayload.serializer()))

        assertFalse(file.exists())
        assertEquals(emptyList<String>(), corruptSnapshots(file))
    }

    @Test
    fun `older schema version still reads`() {
        // 1.0.1 是历史存档写出的版本；升级到 1.4.0 后必须还能读出来。
        val file = File(temp.root, "sample.json")
        file.writeText(envelope(ArchiveSchemaVersion(1, 0, 1), SamplePayload("旧档", 7)))

        val payload = json.decodeDomainSafely(file, DOMAIN, SamplePayload.serializer())

        assertEquals(SamplePayload("旧档", 7), payload)
        assertEquals(emptyList<String>(), corruptSnapshots(file))
    }

    @Test
    fun `payload with unknown fields still reads`() {
        // 新版写盘、旧版读取时会出现当前代码不认识的字段。
        val file = File(temp.root, "sample.json")
        file.writeText(
            """{"domainId":"$DOMAIN",""" +
                """"schemaVersion":{"major":1,"minor":4,"patch":0},""" +
                """"appVersion":"1.0.0","updatedAtMillis":0,""" +
                """"payload":{"label":"带新字段","count":3,"futureField":{"nested":true}}}""",
        )

        val payload = json.decodeDomainSafely(file, DOMAIN, SamplePayload.serializer())

        assertEquals(SamplePayload("带新字段", 3), payload)
        assertEquals(emptyList<String>(), corruptSnapshots(file))
    }

    @Test
    fun `newer schema version is quarantined instead of silently dropped`() {
        // 装了更新的 debug 包又装回 release 时会走到这里：当前代码读不懂未来版本。
        val file = File(temp.root, "sample.json")
        val original = envelope(ArchiveSchemaVersion(9, 0, 0), SamplePayload("未来", 1))
        file.writeText(original)

        assertNull(json.decodeDomainSafely(file, DOMAIN, SamplePayload.serializer()))

        val snapshots = corruptSnapshots(file)
        assertEquals(1, snapshots.size)
        assertEquals(
            "原始字节必须原样保留，否则事后无从抢救",
            original,
            File(temp.root, snapshots.single()).readText(),
        )
        assertEquals("原文件本身不被改写", original, file.readText())
    }

    @Test
    fun `broken json is quarantined`() {
        val file = File(temp.root, "sample.json")
        file.writeText("""{"domainId":"sample","payload":""")

        assertNull(json.decodeDomainSafely(file, DOMAIN, SamplePayload.serializer()))

        assertEquals(1, corruptSnapshots(file).size)
    }

    @Test
    fun `wrong domain is quarantined`() {
        val file = File(temp.root, "sample.json")
        file.writeText(envelope(ArchiveSchemaVersion(1, 4, 0), SamplePayload()).replace(DOMAIN, "other"))

        assertNull(json.decodeDomainSafely(file, DOMAIN, SamplePayload.serializer()))

        assertEquals(1, corruptSnapshots(file).size)
    }

    @Test
    fun `prepare archive write keeps the previous version`() {
        val file = File(temp.root, "sample.json")
        file.writeText("第一版")

        val temporary = prepareArchiveWrite(file)

        assertEquals(file.parentFile, temporary.parentFile)
        assertTrue(temporary.name.endsWith(".tmp"))
        assertEquals("第一版", File(temp.root, "sample.json.prev").readText())
        assertEquals("写入前原文件保持不动", "第一版", file.readText())
    }

    @Test
    fun `prepare archive write tolerates a missing file`() {
        val file = File(temp.root, "fresh.json")

        val temporary = prepareArchiveWrite(file)

        assertTrue(temporary.parentFile?.isDirectory == true)
        assertFalse(File(temp.root, "fresh.json.prev").exists())
    }

    private fun corruptSnapshots(file: File): List<String> =
        file.parentFile?.listFiles()
            ?.map(File::getName)
            ?.filter { it.startsWith("${file.name}.corrupt-") }
            ?.sorted()
            .orEmpty()

    private fun envelope(version: ArchiveSchemaVersion, payload: SamplePayload): String =
        json.encodeToString(
            DomainEnvelope.serializer(SamplePayload.serializer()),
            DomainEnvelope(
                domainId = DOMAIN,
                schemaVersion = version,
                appVersion = "1.0.0",
                updatedAtMillis = 0L,
                payload = payload,
            ),
        )

    private companion object {
        const val DOMAIN = "sample"

        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; explicitNulls = false }
    }
}

@Serializable
private data class SamplePayload(
    val label: String = "",
    val count: Int = 0,
)

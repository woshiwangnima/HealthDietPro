package com.woshiwangnima.healthdietpro.migration

import com.woshiwangnima.healthdietpro.common.timer.TimerArchive
import com.woshiwangnima.healthdietpro.model.archive.ArchiveSchemaVersion
import com.woshiwangnima.healthdietpro.model.archive.DomainEnvelope
import com.woshiwangnima.healthdietpro.model.archive.decodeDomain
import com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucoseArchive
import com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucosePredictionArchive
import com.woshiwangnima.healthdietpro.model.bloodpressure.BloodPressureArchive
import com.woshiwangnima.healthdietpro.model.container.ContainerArchive
import com.woshiwangnima.healthdietpro.model.diet.DietArchive
import com.woshiwangnima.healthdietpro.model.disease.UserDiseaseArchive
import com.woshiwangnima.healthdietpro.model.food.UserCustomFoodArchive
import com.woshiwangnima.healthdietpro.model.medication.MedicationArchive
import com.woshiwangnima.healthdietpro.model.memo.MemoArchive
import com.woshiwangnima.healthdietpro.model.price.PriceArchive
import com.woshiwangnima.healthdietpro.model.sleep.SleepArchive
import com.woshiwangnima.healthdietpro.model.water.WaterArchive
import java.io.File
import java.time.Instant
import java.util.Base64
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * 一次性迁移工具：把实机上 1.4.0 时期的用户存档转成当前版本可直接导入的档案。
 *
 * 它跑在单元测试里，为的是直接复用应用自己的 DTO 与序列化配置：
 * 手工改 JSON 无法证明「当前代码真的能读懂这份数据」，而这里每一步都是真实解析。
 *
 * 输入 / 输出为固定绝对路径（迁移完即删除本文件，不留在仓库）。
 */
class LegacyArchiveMigratorTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; explicitNulls = false }

    private val legacyRoot = File(deviceDataDir, "副本1-原始备份")
    private val outputRoot = File(deviceDataDir, "副本2-迁移后")
    private val userId = "1784392406758"
    private val sourceDir = File(legacyRoot, "files/user_archives/$userId")

    @Test
    fun migrateToCurrentBundle() {
        // 留档目录被 .gitignore 忽略，换机器或清理后不存在——此时跳过，不让基线凭空多一条失败。
        assumeTrue("缺少设备留档目录: $sourceDir", sourceDir.isDirectory)
        outputRoot.mkdirs()
        val normalizedDir = File(outputRoot, "user_archives/$userId").apply { mkdirs() }

        // 1. 逐个领域：旧文件 → 当前 DTO → 新信封，同时记录指纹（叶子数）用于完整性比对。
        val migrated = linkedMapOf<String, JsonElement>()
        val report = StringBuilder()
        DOMAINS.forEach { (domainId, fileName) ->
            val file = File(sourceDir, fileName)
            if (!file.isFile) {
                report.appendLine("跳过 $domainId：文件不存在")
                return@forEach
            }
            val raw = file.readText(Charsets.UTF_8)
            val before = fingerprint(json.parseToJsonElement(raw).jsonObject.getValue("payload"))
            val element = reencode(domainId, raw)
            val after = fingerprint(element.jsonObject.getValue("payload"))
            assertEquals("$domainId 迁移后数据量发生变化", before, after)
            migrated[domainId] = element
            File(normalizedDir, fileName).writeText(pretty(element), Charsets.UTF_8)
            report.appendLine("$domainId：叶子 $before → $after，附件无关")
        }

        // 2. 旧版本没有的两个领域：补空档案（schemaVersion 取各自当前值）。
        listOf("notes", "prices").forEach { domainId ->
            if (migrated.containsKey(domainId)) return@forEach
            val element = emptyDomain(domainId)
            migrated[domainId] = element
            File(normalizedDir, "${if (domainId == "notes") "notes" else "prices"}.json")
                .writeText(pretty(element), Charsets.UTF_8)
            report.appendLine("$domainId：旧档无此领域，补空档案")
        }

        // 3. profile：存档层用 JsonObject 直通，原样带过去（去掉信封）。
        val profileEnvelope = json.parseToJsonElement(File(sourceDir, "profile.json").readText()).jsonObject
        val profilePayload = profileEnvelope.getValue("payload").jsonObject

        // 4. 偏好：磁盘是 {type, string/...}，档案是 {type, value}，显式转换。
        val preferences = migratePreferences(File(sourceDir, "user_preferences.json"))
        report.appendLine("user_preferences：${preferences.size} 项")

        // 5. 附件：整目录 Base64 内联，路径相对用户目录。
        val attachments = collectAttachments()
        report.appendLine("attachments：${attachments.size} 个文件，共 ${attachments.sumOf { it.bytes.size }} 字节（Base64 前）")

        // 6. 组装 bundle。
        val manifest = json.parseToJsonElement(File(sourceDir, "manifest.json").readText()).jsonObject
        val createdAt = manifest["createdAtMillis"]?.jsonPrimitive?.longOrNull ?: 0L
        val updatedAt = manifest["updatedAtMillis"]?.jsonPrimitive?.longOrNull ?: createdAt
        val bundle = buildJsonObject {
            put("formatVersion", versionJson(ArchiveSchemaVersion.Current))
            put("appVersion", "1.0.0")
            put("exportedAt", Instant.now().toString())
            put("sourceUserId", userId)
            put(
                "metadata",
                buildJsonObject {
                    put("id", userId)
                    put("name", profilePayload["name"]?.jsonPrimitive?.content ?: "")
                    put("gender", profilePayload["gender"]?.jsonPrimitive?.content ?: "")
                    put("avatarFileName", profilePayload["avatarFileName"]?.jsonPrimitive?.content ?: "")
                    put("createdAtMillis", createdAt)
                    put("lastActiveAtMillis", updatedAt)
                    put("updatedAtMillis", updatedAt)
                },
            )
            put("profile", profilePayload)
            migrated.forEach { (domainId, element) -> put(BUNDLE_FIELDS.getValue(domainId), element) }
            put("userPreferences", preferences)
            putJsonArray("attachments") {
                attachments.forEach { attachment ->
                    add(
                        buildJsonObject {
                            put("path", attachment.path)
                            put("contentBase64", Base64.getEncoder().encodeToString(attachment.bytes))
                        },
                    )
                }
            }
        }

        val outFile = File(outputRoot, "HealthDietPro-$userId-migrated.json")
        outFile.writeText(json.encodeToString(JsonObject.serializer(), bundle), Charsets.UTF_8)

        // 7. 回读自检：按导入端的解码方式再解一遍，逐领域落到 DTO。
        val reread = json.decodeFromString(
            com.woshiwangnima.healthdietpro.model.archive.UserArchiveBundle.serializer(),
            outFile.readText(Charsets.UTF_8),
        )
        assertEquals(userId, reread.sourceUserId)
        val rereadDomains = listOfNotNull(
            reread.bodyMetrics, reread.medications, reread.bloodGlucose, reread.bloodPressure,
            reread.water, reread.diseaseRecords, reread.customFoods, reread.sleep,
            reread.dietRecords, reread.containers, reread.timers, reread.prices,
            reread.notes, reread.bloodGlucosePredictions,
        )
        assertEquals("回读领域数量不符", migrated.size, rereadDomains.size)
        rereadDomains.forEach { element -> fingerprint(element) }
        assertEquals("偏好项数量不符", preferences.size, reread.userPreferences.size)

        report.appendLine("bundle：${outFile.length()} 字节")
        File(outputRoot, "迁移报告.txt").writeText(report.toString(), Charsets.UTF_8)
        println(report)
    }

    /** 用当前 DTO 解析旧信封，再按当前 schema 重新封装。 */
    private fun reencode(domainId: String, raw: String): JsonElement {
        val updatedAt = json.parseToJsonElement(raw).jsonObject["updatedAtMillis"]?.jsonPrimitive?.longOrNull ?: 0L
        return when (domainId) {
            // BodyMetricsDto 是文件私有类型，测试侧无法引用；其字段与旧数据一致，
            // 这里原样转封，导入端的 BodyMetricsRepository.validateJson 会真解析一遍。
            "body_metrics" -> envelope(
                domainId,
                updatedAt,
                json.parseToJsonElement(raw).jsonObject.getValue("payload").jsonObject,
                JsonObject.serializer(),
            )
            "medications" -> envelope(domainId, updatedAt, json.decodeDomain(raw, domainId, MedicationArchive.serializer()), MedicationArchive.serializer())
            "blood_glucose" -> envelope(domainId, updatedAt, json.decodeDomain(raw, domainId, BloodGlucoseArchive.serializer()), BloodGlucoseArchive.serializer())
            "blood_pressure" -> envelope(domainId, updatedAt, json.decodeDomain(raw, domainId, BloodPressureArchive.serializer()), BloodPressureArchive.serializer())
            "water" -> envelope(domainId, updatedAt, json.decodeDomain(raw, domainId, WaterArchive.serializer()), WaterArchive.serializer())
            "disease_records" -> envelope(domainId, updatedAt, json.decodeDomain(raw, domainId, UserDiseaseArchive.serializer()), UserDiseaseArchive.serializer())
            "custom_foods" -> envelope(domainId, updatedAt, json.decodeDomain(raw, domainId, UserCustomFoodArchive.serializer()), UserCustomFoodArchive.serializer())
            "sleep" -> envelope(domainId, updatedAt, json.decodeDomain(raw, domainId, SleepArchive.serializer()), SleepArchive.serializer())
            "diet_records" -> envelope(domainId, updatedAt, json.decodeDomain(raw, domainId, DietArchive.serializer()), DietArchive.serializer())
            "containers" -> envelope(domainId, updatedAt, json.decodeDomain(raw, domainId, ContainerArchive.serializer()), ContainerArchive.serializer())
            "timers" -> envelope(domainId, updatedAt, json.decodeDomain(raw, domainId, TimerArchive.serializer()), TimerArchive.serializer())
            "blood_glucose_predictions" -> envelope(domainId, updatedAt, json.decodeDomain(raw, domainId, BloodGlucosePredictionArchive.serializer()), BloodGlucosePredictionArchive.serializer())
            else -> error("未覆盖的领域: $domainId")
        }
    }

    private fun <T> envelope(domainId: String, updatedAt: Long, payload: T, serializer: KSerializer<T>): JsonElement =
        json.encodeToJsonElement(
            DomainEnvelope.serializer(serializer),
            DomainEnvelope(
                domainId = domainId,
                schemaVersion = ArchiveSchemaVersion.Current,
                appVersion = "1.0.0",
                updatedAtMillis = updatedAt,
                payload = payload,
            ),
        )

    private fun emptyDomain(domainId: String): JsonElement = when (domainId) {
        "notes" -> envelope(domainId, System.currentTimeMillis(), MemoArchive(), MemoArchive.serializer())
        "prices" -> envelope(domainId, System.currentTimeMillis(), PriceArchive(), PriceArchive.serializer())
        else -> error("未知领域 $domainId")
    }

    /** `{type, boolean|int|long|float|string|strings}` → `{type, value}`。 */
    private fun migratePreferences(file: File): JsonObject {
        if (!file.isFile) return JsonObject(emptyMap())
        val values = json.parseToJsonElement(file.readText())
            .jsonObject.getValue("payload").jsonObject.getValue("values").jsonObject
        return JsonObject(
            values.mapValues { (_, value) ->
                val objectValue = value.jsonObject
                val type = objectValue.getValue("type").jsonPrimitive.content
                val payload = objectValue[PREFERENCE_PAYLOAD_KEY.getValue(type)]
                    ?: error("偏好项缺少 $type 载荷")
                buildJsonObject {
                    put("type", type)
                    put("value", payload)
                }
            },
        )
    }

    private fun collectAttachments(): List<Attachment> {
        val root = File(sourceDir, "attachments")
        if (!root.isDirectory) return emptyList()
        return root.walkTopDown()
            .filter(File::isFile)
            .map { file -> Attachment(file.relativeTo(sourceDir).invariantSeparatorsPath, file.readBytes()) }
            .sortedBy(Attachment::path)
            .toList()
    }

    /**
     * 结构指纹：叶子节点总数 + 数组元素总数。
     *
     * 迁移只应改字段名与默认值，不应该凭空多出或少掉数据，所以这两个数必须守恒。
     */
    private fun fingerprint(element: JsonElement): Int = when (element) {
        is JsonObject -> element.size + element.values.sumOf(::fingerprint)
        is JsonArray -> element.size + element.sumOf(::fingerprint)
        JsonNull -> 0
        else -> 1
    }

    private fun versionJson(version: ArchiveSchemaVersion): JsonObject = buildJsonObject {
        put("major", version.major)
        put("minor", version.minor)
        put("patch", version.patch)
    }

    private fun pretty(element: JsonElement): String = json.encodeToString(JsonElement.serializer(), element)

    private data class Attachment(val path: String, val bytes: ByteArray)

    private companion object {
        /** `device-data/<厂商>-<型号>-<序列号>/migration-<日期>/`：本项目约定的设备数据留档位置。 */
        val deviceDataDir = File(
            generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
                .firstOrNull { File(it, "settings.gradle.kts").isFile }
                ?: error("找不到项目根目录"),
            "device-data/Xiaomi-24117RK2CC-d794c3f/migration-20261105",
        )

        /** 领域 id → 磁盘文件名（与 ArchiveDomains 一致）。 */
        val DOMAINS = linkedMapOf(
            "body_metrics" to "body_metrics.json",
            "medications" to "medications.json",
            "blood_glucose" to "blood_glucose.json",
            "blood_pressure" to "blood_pressure.json",
            "water" to "water.json",
            "disease_records" to "disease_records.json",
            "custom_foods" to "custom_foods.json",
            "sleep" to "sleep.json",
            "diet_records" to "diet.json",
            "containers" to "containers.json",
            "timers" to "timers.json",
            "blood_glucose_predictions" to "blood_glucose_predictions.json",
        )

        /** 领域 id → bundle 字段名（与 UserArchiveBundle 一致）。 */
        val BUNDLE_FIELDS = mapOf(
            "body_metrics" to "bodyMetrics",
            "medications" to "medications",
            "blood_glucose" to "bloodGlucose",
            "blood_pressure" to "bloodPressure",
            "water" to "water",
            "disease_records" to "diseaseRecords",
            "custom_foods" to "customFoods",
            "sleep" to "sleep",
            "diet_records" to "dietRecords",
            "containers" to "containers",
            "timers" to "timers",
            "prices" to "prices",
            "notes" to "notes",
            "blood_glucose_predictions" to "bloodGlucosePredictions",
        )

        val PREFERENCE_PAYLOAD_KEY = mapOf(
            "BOOLEAN" to "boolean",
            "INT" to "int",
            "LONG" to "long",
            "FLOAT" to "float",
            "STRING" to "string",
            "STRING_SET" to "strings",
        )
    }
}

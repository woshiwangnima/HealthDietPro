package com.woshiwangnima.healthdietpro.model.archive

import android.content.Context
import android.util.Base64
import androidx.annotation.StringRes
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.timer.TimerArchive
import com.woshiwangnima.healthdietpro.common.timer.TimerArchiveStore
import com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucoseArchiveStore
import com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucosePredictionArchive
import com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucosePredictionArchiveStore
import com.woshiwangnima.healthdietpro.model.bloodpressure.BloodPressureArchiveStore
import com.woshiwangnima.healthdietpro.model.container.ContainerArchiveStore
import com.woshiwangnima.healthdietpro.model.diet.DietArchiveStore
import com.woshiwangnima.healthdietpro.model.disease.UserDiseaseArchiveStore
import com.woshiwangnima.healthdietpro.model.food.UserCustomFoodArchive
import com.woshiwangnima.healthdietpro.model.food.UserCustomFoodArchiveStore
import com.woshiwangnima.healthdietpro.model.medication.MedicationArchive
import com.woshiwangnima.healthdietpro.model.medication.MedicationArchiveStore
import com.woshiwangnima.healthdietpro.model.price.PriceArchiveStore
import com.woshiwangnima.healthdietpro.model.profile.BodyMetricsRepository
import com.woshiwangnima.healthdietpro.model.sleep.SleepArchiveStore
import com.woshiwangnima.healthdietpro.model.water.WaterArchiveStore
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * 用户档案里的一个领域。
 *
 * 这个文件是**唯一登记处**：导出要带哪些领域、导入前怎么校验、附件路径怎么改写、
 * 预览里叫什么名字、诊断按什么文件名查——全部由 [ArchiveDomains] 派生。
 * 以前这几件事分别写在四处手写清单里（导出的 `readDomainSnapshot`、导入的 `validateDomains`、
 * 预览的 `ArchiveDomainFiles`、诊断的 `archiveDiagnosticFiles`），谁新增领域都得记得同步四遍，
 * 结果 `diet.json` / `timers.json` / `containers.json` / `prices.json` /
 * `blood_glucose_predictions.json` 五个真实存在的领域就这么被漏在档案外面，没人发现。
 *
 * 新增领域只需要两步：
 * 1. 在 [UserArchiveBundle] 加一个可空字段（`ignoreUnknownKeys` 保证旧档案照常读）；
 * 2. 在 [ArchiveDomains] 加一条，把它接上 `extract` / `attach`。
 * 只做第一步不做第二步，`ArchiveDomainRegistryTest` 会直接红；两步都不做，
 * 档案状态卡会显示「N 个文件未纳入档案」（[unregisteredArchiveFiles]）。
 */
internal class ArchiveDomain(
    val id: String,
    val fileName: String,
    @param:StringRes val labelRes: Int,
    /** 是否以 `user_archives/<uid>/<fileName>` 独立文件落盘（profile 与偏好不是这个路径的来源）。 */
    val readsDisk: Boolean = true,
    /** 导出前的加工：自定义食物要把图片抽成附件，其它领域原样带走。 */
    val prepare: ((Context, String, JsonElement) -> ArchiveDomainPayload)? = null,
    /**
     * 读文件之前先把 store 读一遍。
     *
     * 有些领域的迁移链只在 `load()` 里跑（water / sleep / diet / containers），
     * 直接读原始文件可能拿到还没迁移的旧 payload；导出去别的版本会卡在导入校验上。
     */
    val warmUp: ((Context, String) -> Unit)? = null,
    /** 从档案里取出该领域，预览、导出、诊断共用。 */
    val extract: (UserArchiveBundle) -> JsonElement?,
    /** 导出时把该领域放回档案。 */
    val attach: (UserArchiveBundle, JsonElement?) -> UserArchiveBundle,
    /** 导入前校验。profile 由 [com.woshiwangnima.healthdietpro.model.profile.ProfilePrefs] 校验。 */
    val validate: (Context, String, JsonElement) -> Unit = { _, _, _ -> },
    /** 把档案里指向「导出设备那个用户」的路径改写成本机目标用户。 */
    val rewrite: ((Context, String, JsonElement) -> JsonElement)? = null,
) {
    /** 读本机某个用户的该领域文件；文件不存在时返回 null（档案里就不写这个领域）。 */
    fun read(context: Context, userId: String): ArchiveDomainPayload? {
        if (!readsDisk) return null
        runCatching { warmUp?.invoke(context, userId) }
        val file = File(userArchiveDirectory(context, userId), fileName).takeIf(File::isFile)
            ?: return null
        val element = runCatching {
            registryJson.parseToJsonElement(file.readText(Charsets.UTF_8))
        }.getOrNull() ?: return null
        return prepare?.invoke(context, userId, element) ?: ArchiveDomainPayload(element)
    }
}

/** 一份领域在档案里的载荷：JSON 本身 + 它为档案带来的附件。 */
internal data class ArchiveDomainPayload(
    val element: JsonElement,
    val attachments: List<UserArchiveAttachment> = emptyList(),
)

/**
 * 档案里全部领域，顺序即预览页的展示顺序。
 *
 * 加领域时**只改这里**；`ArchiveDomainRegistryTest` 会把这里和 [UserArchiveBundle] 的字段
 * 做双向比对，漏一边就编译后测试红。
 */
internal val ArchiveDomains: List<ArchiveDomain> = listOf(
    ArchiveDomain(
        id = "profile",
        fileName = "profile.json",
        labelRes = R.string.archive_domain_profile,
        readsDisk = false,
        extract = UserArchiveBundle::profile,
        attach = { bundle, element -> if (element == null) bundle else bundle.copy(profile = element) },
    ),
    ArchiveDomain(
        id = "body_metrics",
        fileName = "body_metrics.json",
        labelRes = R.string.archive_domain_body_metrics,
        extract = { it.bodyMetrics },
        attach = { bundle, element -> bundle.copy(bodyMetrics = element) },
        validate = { context, userId, element ->
            BodyMetricsRepository.forUser(context, userId).validateJson(element.toString())
        },
    ),
    ArchiveDomain(
        id = "medications",
        fileName = "medications.json",
        labelRes = R.string.archive_domain_medications,
        extract = { it.medications },
        attach = { bundle, element -> bundle.copy(medications = element) },
        validate = { context, userId, element ->
            MedicationArchiveStore.forUser(context, userId).validateJson(element.toString())
        },
        rewrite = ::rewriteMedicationAttachmentPaths,
    ),
    ArchiveDomain(
        id = "blood_glucose",
        fileName = "blood_glucose.json",
        labelRes = R.string.archive_domain_blood_glucose,
        extract = { it.bloodGlucose },
        attach = { bundle, element -> bundle.copy(bloodGlucose = element) },
        validate = { context, userId, element ->
            BloodGlucoseArchiveStore.forUser(context, userId).validateJson(element.toString())
        },
    ),
    ArchiveDomain(
        id = "blood_pressure",
        fileName = "blood_pressure.json",
        labelRes = R.string.archive_domain_blood_pressure,
        extract = { it.bloodPressure },
        attach = { bundle, element -> bundle.copy(bloodPressure = element) },
        validate = { context, userId, element ->
            BloodPressureArchiveStore.forUser(context, userId).validateJson(element.toString())
        },
    ),
    ArchiveDomain(
        id = "water",
        fileName = "water.json",
        labelRes = R.string.archive_domain_water,
        extract = { it.water },
        attach = { bundle, element -> bundle.copy(water = element) },
        validate = { context, userId, element ->
            WaterArchiveStore.forUser(context, userId).validateJson(element.toString())
        },
        warmUp = { context, userId -> WaterArchiveStore.forUser(context, userId).load() },
    ),
    ArchiveDomain(
        id = "disease_records",
        fileName = "disease_records.json",
        labelRes = R.string.archive_domain_disease_records,
        extract = { it.diseaseRecords },
        attach = { bundle, element -> bundle.copy(diseaseRecords = element) },
        validate = { context, userId, element ->
            UserDiseaseArchiveStore.forUser(context, userId).validateJson(element.toString())
        },
    ),
    ArchiveDomain(
        id = "custom_foods",
        fileName = "custom_foods.json",
        labelRes = R.string.archive_domain_custom_foods,
        prepare = ::exportCustomFoodImages,
        extract = { it.customFoods },
        attach = { bundle, element -> bundle.copy(customFoods = element) },
        validate = { context, userId, element ->
            UserCustomFoodArchiveStore.forUser(context, userId).validateJson(element.toString())
        },
        rewrite = ::rewriteCustomFoodImagePaths,
    ),
    ArchiveDomain(
        id = "sleep",
        fileName = "sleep.json",
        labelRes = R.string.archive_domain_sleep,
        extract = { it.sleep },
        attach = { bundle, element -> bundle.copy(sleep = element) },
        validate = { context, userId, element ->
            SleepArchiveStore.forUser(context, userId).validateJson(element.toString())
        },
        warmUp = { context, userId -> SleepArchiveStore.forUser(context, userId).load() },
    ),
    ArchiveDomain(
        id = "diet_records",
        fileName = "diet.json",
        labelRes = R.string.archive_domain_diet_records,
        extract = { it.dietRecords },
        attach = { bundle, element -> bundle.copy(dietRecords = element) },
        validate = { context, userId, element ->
            DietArchiveStore.forUser(context, userId).validateJson(element.toString())
        },
        warmUp = { context, userId -> DietArchiveStore.forUser(context, userId).load() },
    ),
    ArchiveDomain(
        id = "containers",
        fileName = "containers.json",
        labelRes = R.string.archive_domain_containers,
        extract = { it.containers },
        attach = { bundle, element -> bundle.copy(containers = element) },
        validate = { context, userId, element ->
            ContainerArchiveStore.forUser(context, userId).validateJson(element.toString())
        },
        warmUp = { context, userId -> ContainerArchiveStore.forUser(context, userId).load() },
    ),
    ArchiveDomain(
        id = "timers",
        fileName = "timers.json",
        labelRes = R.string.archive_domain_timers,
        extract = { it.timers },
        attach = { bundle, element -> bundle.copy(timers = element) },
        // 计时器没有自己的 validateJson：能按信封解出来就算合法。
        validate = { _, _, element ->
            registryJson.decodeDomain(element.toString(), "timers", TimerArchive.serializer())
            Unit
        },
    ),
    ArchiveDomain(
        id = "prices",
        fileName = "prices.json",
        labelRes = R.string.archive_domain_prices,
        extract = { it.prices },
        attach = { bundle, element -> bundle.copy(prices = element) },
        validate = { context, userId, element ->
            PriceArchiveStore.forUser(context, userId).validateJson(element.toString())
        },
    ),
    ArchiveDomain(
        id = "blood_glucose_predictions",
        fileName = "blood_glucose_predictions.json",
        labelRes = R.string.archive_domain_blood_glucose_predictions,
        extract = { it.bloodGlucosePredictions },
        attach = { bundle, element -> bundle.copy(bloodGlucosePredictions = element) },
        validate = { _, _, element ->
            registryJson.decodeDomain(
                element.toString(),
                "blood_glucose_predictions",
                BloodGlucosePredictionArchive.serializer(),
            )
            Unit
        },
    ),
    ArchiveDomain(
        id = "user_preferences",
        fileName = "user_preferences.json",
        labelRes = R.string.archive_domain_user_preferences,
        readsDisk = false,
        extract = { bundle ->
            bundle.userPreferences.takeIf { it.isNotEmpty() }?.let { preferences ->
                JsonObject(
                    preferences.mapValues { (_, preference) ->
                        JsonObject(
                            mapOf(
                                "type" to JsonPrimitive(preference.type.name),
                                "value" to preference.value,
                            ),
                        )
                    },
                )
            }
        },
        // 偏好走 UserPrefs.replaceAll 落盘，不进 domains 映射。
        attach = { bundle, _ -> bundle },
    ),
)

/** 档案里带附件的两个领域共用的路径前缀，与自定义食物图片在磁盘上的写法一致。 */
internal const val USER_IMAGE_PREFIX = "user:"

/**
 * `user_archives/<uid>/` 里存在、但注册表没认领的 `.json`。
 *
 * 这是「新领域忘了登记」的第二道网：单测抓的是「字段加了没登记」，
 * 这里抓的是「文件写了但根本没进档案」。非空时档案状态卡会明确提示，
 * 而不是让用户导出一份悄悄少数据的档案。
 */
internal fun unregisteredArchiveFiles(context: Context, userId: String): List<String> {
    if (userId.isBlank()) return emptyList()
    val directory = userArchiveDirectory(context, userId)
    if (!directory.isDirectory) return emptyList()
    return unregisteredArchiveFileNames(directory.listFiles().orEmpty().filter(File::isFile).map(File::getName))
}

/**
 * 纯函数版本，供单测直接喂文件名列表。
 *
 * 排除清单文件与守卫留下的备份（`.prev` / `.corrupt-*` 不以 `.json` 结尾，天然被排除）。
 */
internal fun unregisteredArchiveFileNames(fileNames: List<String>): List<String> {
    val known = ArchiveDomains.map(ArchiveDomain::fileName).toSet() + MANIFEST_FILE_NAME
    return fileNames.filter { it.endsWith(".json") && it !in known }.sorted()
}

/** 用户档案目录里的清单文件名，与 [writeUserArchiveManifest] 保持一致。 */
internal const val MANIFEST_FILE_NAME = "manifest.json"

/** 自定义食物：图片从 `files/` 里抽出来当附件，领域里改写成档案内的相对路径。 */
private fun exportCustomFoodImages(
    context: Context,
    userId: String,
    domain: JsonElement,
): ArchiveDomainPayload {
    val archive = runCatching {
        registryJson.decodeDomain(domain.toString(), "custom_foods", UserCustomFoodArchive.serializer())
    }.getOrElse { return ArchiveDomainPayload(domain) }
    val root = userArchiveDirectory(context, userId)
    val filesRoot = context.filesDir.canonicalFile
    val attachments = mutableListOf<UserArchiveAttachment>()
    val foods = archive.foods.map { food ->
        val image = food.image ?: return@map food
        val key = image.localKey.takeIf { it.startsWith(USER_IMAGE_PREFIX) } ?: return@map food
        val source = File(context.filesDir, key.removePrefix(USER_IMAGE_PREFIX)).canonicalFile
        if (!source.isFile || !source.path.startsWith(filesRoot.path + File.separator)) return@map food
        val exportedPath = "attachments/foods/${safeArchiveFileName(source.name)}"
        attachments += UserArchiveAttachment(
            exportedPath,
            Base64.encodeToString(source.readBytes(), Base64.NO_WRAP),
        )
        food.copy(
            image = image.copy(
                localKey = "$USER_IMAGE_PREFIX${root.relativeTo(context.filesDir).invariantSeparatorsPath}/$exportedPath",
            ),
        )
    }
    val element = registryJson.parseToJsonElement(
        registryJson.encodeDomain(
            context,
            "custom_foods",
            archive.copy(foods = foods),
            UserCustomFoodArchive.serializer(),
        ),
    )
    return ArchiveDomainPayload(element, attachments)
}

private fun rewriteCustomFoodImagePaths(
    context: Context,
    userId: String,
    domain: JsonElement,
): JsonElement {
    val archive = registryJson.decodeDomain(domain.toString(), "custom_foods", UserCustomFoodArchive.serializer())
    val rootPath = userArchiveDirectory(context, userId).relativeTo(context.filesDir).invariantSeparatorsPath
    val foods = archive.foods.map { food ->
        val image = food.image ?: return@map food
        val attachment = image.localKey.removePrefix(USER_IMAGE_PREFIX)
            .takeIf { image.localKey.startsWith(USER_IMAGE_PREFIX) && it.startsWith("user_archives/") }
            ?.substringAfter("/attachments/", missingDelimiterValue = "")
            ?.takeIf(String::isNotBlank)
            ?: return@map food
        food.copy(image = image.copy(localKey = "$USER_IMAGE_PREFIX$rootPath/attachments/$attachment"))
    }
    return registryJson.parseToJsonElement(
        registryJson.encodeDomain(context, "custom_foods", archive.copy(foods = foods), UserCustomFoodArchive.serializer()),
    )
}

private fun rewriteMedicationAttachmentPaths(
    context: Context,
    userId: String,
    domain: JsonElement,
): JsonElement {
    val archive = registryJson.decodeDomain(domain.toString(), "medications", MedicationArchive.serializer())
    val rootPath = userArchiveDirectory(context, userId).relativeTo(context.filesDir).invariantSeparatorsPath
    fun rewrite(path: String): String = path
        .substringAfter("/attachments/", missingDelimiterValue = "")
        .takeIf { path.startsWith("user_archives/") && it.startsWith("medications/") }
        ?.let { "$rootPath/attachments/$it" }
        ?: path
    val rewritten = archive.copy(
        catalog = archive.catalog.map { item -> item.copy(imagePaths = item.imagePaths.map(::rewrite)) },
        records = archive.records.map { record ->
            record.copy(
                medicationImagePaths = record.medicationImagePaths.map(::rewrite),
                recordPhotoPaths = record.recordPhotoPaths.map(::rewrite),
            )
        },
    )
    return registryJson.parseToJsonElement(
        registryJson.encodeDomain(context, "medications", rewritten, MedicationArchive.serializer()),
    )
}

internal fun safeArchiveFileName(fileName: String): String =
    fileName.substringAfterLast('/').replace(Regex("[^A-Za-z0-9._-]"), "_")

private val registryJson = Json { encodeDefaults = true; explicitNulls = false; ignoreUnknownKeys = true }

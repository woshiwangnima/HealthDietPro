package com.woshiwangnima.healthdietpro.model.archive

import android.content.Context
import android.util.Base64
import com.woshiwangnima.healthdietpro.model.prefs.UserPrefs
import com.woshiwangnima.healthdietpro.model.profile.ProfilePrefs
import com.woshiwangnima.healthdietpro.model.profile.UserProfile
import java.io.File
import java.time.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/** 导入落点：覆盖当前用户，还是新建一个用户（原数据完全不动）。 */
internal enum class ArchiveImportTarget { REPLACE_CURRENT_USER, NEW_USER }

/** 一次成功导入的结果，界面用它说明数据落到哪儿了。 */
internal data class ArchiveImportResult(
    val userId: String,
    val userName: String,
    val newUser: Boolean,
)

/** Coordinates plain-text import/export without allowing an archive to address another user. */
internal class PlainUserArchiveRepository(
    private val context: Context,
) {
    /** 领域清单来自 [ArchiveDomains]：加领域不用动这里。 */
    fun exportCurrentUser(): Result<String> = runCatching {
        val profile = ProfilePrefs.load(context).let { loaded ->
            loaded.takeIf { it.id.isNotBlank() } ?: ProfilePrefs.createDefaultIfEmpty(context)
        }
        val metadata = requireNotNull(ProfilePrefs.getUserMetadata(context, profile.id)) {
            "Current user metadata is missing"
        }
        val avatarReference = migrateAvatarReference(context, profile.id, profile.avatarFileName)
        val archiveProfile = profile.copy(avatarFileName = avatarReference)
        val preferences = UserPrefs.snapshot(context, profile.id)
            .mapValues { (_, value) -> encodePreference(value) ?: error("Unsupported preference value") }
        val attachments = mutableListOf<UserArchiveAttachment>()
        var bundle = UserArchiveBundle(
            formatVersion = ArchiveSchemaVersion.Current,
            appVersion = appVersion(context),
            exportedAt = Instant.now().toString(),
            sourceUserId = profile.id,
            metadata = UserArchiveMetadata(
                id = metadata.id,
                name = archiveProfile.name,
                gender = archiveProfile.gender.name,
                avatarFileName = archiveProfile.avatarFileName,
                createdAtMillis = metadata.createdAtMillis,
                lastActiveAtMillis = metadata.lastActiveAtMillis,
                updatedAtMillis = metadata.updatedAtMillis,
            ),
            profile = json.parseToJsonElement(ProfilePrefs.exportCurrentUserJson(context)).jsonObject.let { raw ->
                JsonObject(raw + ("avatarFileName" to JsonPrimitive(avatarReference)))
            },
            userPreferences = preferences,
        )
        ArchiveDomains.forEach { domain ->
            val payload = domain.read(context, profile.id) ?: return@forEach
            bundle = domain.attach(bundle, payload.element)
            attachments += payload.attachments
        }
        attachments += collectAttachments(profile.id)
        bundle = bundle.copy(attachments = attachments.distinctBy(UserArchiveAttachment::path).sortedBy(UserArchiveAttachment::path))
        stableUserArchiveJsonString(json.encodeToString(bundle))
    }

    /** 导出并同时产出预览快照：预览看到的内容与写进文件的内容是同一份字节。 */
    fun exportWithSnapshot(): Result<UserArchiveExport> = exportCurrentUser().map { raw ->
        UserArchiveExport(rawJson = raw, snapshot = buildUserArchiveSnapshot(raw))
    }

    fun currentIntegrity(): UserArchiveIntegrity =
        runCatching { inspectUserArchive(context, ProfilePrefs.getCurrentUserId(context)).integrity }
            .getOrDefault(UserArchiveIntegrity.MISSING)

    /** 本机档案目录里存在、但注册表没认领的领域文件。 */
    fun unregisteredFiles(): List<String> =
        unregisteredArchiveFiles(context, ProfilePrefs.getCurrentUserId(context))

    /**
     * 只读地解析并校验一份待导入档案，供导入前的预览使用。
     *
     * 走的是与真正导入完全相同的校验链（领域解码 + 信封版本 + 附件路径与引用），
     * 因此「预览通过、导入失败」只可能发生在落盘阶段，不会出现校验标准两套。
     */
    fun inspectArchive(rawArchive: String, byteSize: Long = rawArchive.toByteArray(Charsets.UTF_8).size.toLong()): Result<UserArchiveSnapshot> =
        runCatching {
            prepareImport(rawArchive)
            buildUserArchiveSnapshot(rawArchive, byteSize)
        }

    fun importIntoCurrentUser(rawArchive: String): Result<ArchiveImportResult> =
        importArchive(rawArchive, ArchiveImportTarget.REPLACE_CURRENT_USER)

    /**
     * 导入为一个新用户：不碰现有任何用户的数据，导入完成后切换到这个新用户。
     *
     * 与覆盖导入共用同一条校验链；区别只在落点 id 从「当前用户」换成新分配的 id，
     * 以及提交阶段多了「注册用户 + 建索引」这一步。
     */
    fun importAsNewUser(rawArchive: String): Result<ArchiveImportResult> =
        importArchive(rawArchive, ArchiveImportTarget.NEW_USER)

    private fun importArchive(rawArchive: String, target: ArchiveImportTarget): Result<ArchiveImportResult> = runCatching {
        val prepared = prepareImport(rawArchive)
        val newUser = target == ArchiveImportTarget.NEW_USER
        val targetUserId = if (newUser) ProfilePrefs.allocateUserId(context) else prepared.targetUserId
        val importedProfile = prepared.profile.copy(id = targetUserId)
        val preferences = prepared.preferences
        // 附件路径里的用户 id 必须换成本机目标用户，否则新用户会指向别人的目录。
        val domains = prepared.domains.mapValues { (domainId, element) ->
            val domain = ArchiveDomains.firstOrNull { it.id == domainId } ?: return@mapValues element
            domain.rewrite?.invoke(context, targetUserId, element) ?: element
        }
        validateAttachmentReferences(importedProfile.avatarFileName, domains, prepared.bundle.attachments)
        val priorProfile = if (newUser) null else ProfilePrefs.getProfile(context, targetUserId)
        val priorPreferences = if (newUser) emptyMap() else UserPrefs.snapshot(context, targetUserId)
        val directoryTransaction = stageUserDirectory(
            userId = targetUserId,
            domains = domains,
            attachments = prepared.bundle.attachments,
            mergeExisting = !newUser,
        )
        try {
            check(UserPrefs.replaceAll(context, targetUserId, preferences))
            if (newUser) {
                ProfilePrefs.registerArchiveUser(
                    context = context,
                    profile = importedProfile,
                    createdAtMillis = prepared.bundle.metadata?.createdAtMillis ?: 0L,
                    lastActiveAtMillis = prepared.bundle.metadata?.lastActiveAtMillis ?: 0L,
                )
            } else {
                ProfilePrefs.replaceCurrentUserFromArchive(context = context, profile = importedProfile)
            }
            directoryTransaction.commit()
        } catch (error: Throwable) {
            directoryTransaction.rollback()
            if (newUser) {
                ProfilePrefs.discardRegisteredUser(context, targetUserId)
            } else {
                UserPrefs.replaceAll(context, targetUserId, priorPreferences)
                priorProfile?.let { ProfilePrefs.replaceCurrentUserFromArchive(context, it) }
            }
            throw error
        }
        ArchiveImportResult(
            userId = targetUserId,
            userName = importedProfile.name,
            newUser = newUser,
        )
    }

    private fun prepareImport(rawArchive: String): PreparedImport {
        val bundle = decodeBundle(rawArchive)
        require(bundle.appVersion.isNotBlank())
        require(bundle.exportedAt.isNotBlank())
        require(bundle.sourceUserId.isNotBlank())
        require(bundle.profile is JsonObject)

        val importedProfile = ProfilePrefs.parseArchiveProfile(context, bundle.profile.toString())
            ?: error("Invalid profile")
        require(importedProfile.id == bundle.sourceUserId)
        validateBundleMetadata(bundle.metadata, importedProfile)
        val preferences = bundle.userPreferences.mapValues { (key, value) ->
            require(key.isNotBlank())
            decodePreference(value)
        }
        val targetUserId = ProfilePrefs.getCurrentUserId(context).ifEmpty { importedProfile.id }
        val domains = bundle.domains()
        validateDomains(targetUserId, domains)
        validateAttachments(bundle.attachments)
        return PreparedImport(
            bundle = bundle,
            profile = importedProfile,
            preferences = preferences,
            domains = domains,
            targetUserId = targetUserId,
        )
    }

    private fun decodeBundle(rawArchive: String): UserArchiveBundle {
        val root = json.parseToJsonElement(rawArchive).jsonObject
        require("bodyMetrics" in root || "userPreferences" in root || "attachments" in root) {
            "Unsupported legacy user archive"
        }
        return json.decodeFromString<UserArchiveBundle>(rawArchive).also { bundle ->
            // 与 decodeDomain 同一口径：旧档案由迁移链补齐，只有更高版本才拒绝。
            require(bundle.formatVersion <= ArchiveSchemaVersion.Current) {
                "Archive schema ${bundle.formatVersion} is newer than supported ${ArchiveSchemaVersion.Current}"
            }
        }
    }

    private fun validateBundleMetadata(metadata: UserArchiveMetadata?, profile: UserProfile) {
        if (metadata == null) return
        if (metadata.id.isBlank() && metadata.name.isBlank() && metadata.gender.isBlank() && metadata.avatarFileName.isBlank()) return
        require(metadata.id == profile.id) { "Archive metadata user id mismatch" }
        require(metadata.name == profile.name) { "Archive metadata name mismatch" }
        require(metadata.gender == profile.gender.name) { "Archive metadata gender mismatch" }
        require(metadata.avatarFileName == profile.avatarFileName) {
            "Archive metadata avatar mismatch"
        }
        val hasTimestamps = metadata.createdAtMillis != 0L || metadata.lastActiveAtMillis != 0L || metadata.updatedAtMillis != 0L
        if (hasTimestamps) {
            require(metadata.createdAtMillis > 0L) { "Archive metadata creation time is invalid" }
            require(metadata.lastActiveAtMillis >= metadata.createdAtMillis) { "Archive metadata activity time is invalid" }
            require(metadata.updatedAtMillis >= metadata.createdAtMillis) { "Archive metadata update time is invalid" }
        }
    }

    private fun collectAttachments(userId: String): List<UserArchiveAttachment> {
        val root = userDirectory(userId)
        val attachments = File(root, "attachments")
        if (!attachments.isDirectory) return emptyList()
        return attachments.walkTopDown()
            .filter(File::isFile)
            .map { file ->
                val path = file.relativeTo(root).invariantSeparatorsPath
                requireValidAttachmentPath(path)
                require(file.length() <= MAX_ATTACHMENT_BYTES)
                UserArchiveAttachment(path, Base64.encodeToString(file.readBytes(), Base64.NO_WRAP))
            }
            .sortedBy(UserArchiveAttachment::path)
            .toList()
    }

    private fun validateAttachmentReferences(
        avatarReference: String,
        domains: Map<String, JsonElement>,
        attachments: List<UserArchiveAttachment>,
    ) {
        val paths = attachments.map(UserArchiveAttachment::path).toSet()
        if (avatarReference.isNotBlank()) {
            require(avatarReference.startsWith("attachments/avatar/") && avatarReference in paths) {
                "Profile avatar is not included in archive attachments"
            }
        }
        domains["medications"]?.let { domain ->
            val archive = json.decodeDomain(domain.toString(), "medications", com.woshiwangnima.healthdietpro.model.medication.MedicationArchive.serializer())
            val referenced = archive.catalog.flatMap { it.imagePaths } + archive.records.flatMap {
                it.medicationImagePaths + it.recordPhotoPaths
            }
            referenced.forEach { path ->
                val attachment = path.substringAfter("/attachments/", missingDelimiterValue = "")
                require(attachment.startsWith("medications/") && "attachments/$attachment" in paths) {
                    "Medication attachment is not included in archive"
                }
            }
        }
        domains["custom_foods"]?.let { domain ->
            val archive = json.decodeDomain(domain.toString(), "custom_foods", com.woshiwangnima.healthdietpro.model.food.UserCustomFoodArchive.serializer())
            archive.foods.mapNotNull { it.image?.localKey }
                .filter { it.startsWith(USER_IMAGE_PREFIX) }
                .forEach { key ->
                    val attachment = key.substringAfter("/attachments/", missingDelimiterValue = "")
                    require(attachment.startsWith("foods/") && "attachments/$attachment" in paths) {
                        "Custom food image is not included in archive"
                    }
                }
        }
        // 便签语音：`voiceFile` 是裸文件名，导出侧的 prepare 已剔除失效块，
        // 所以这里可以严格比对——引用不到附件说明档案被改坏了。
        domains["notes"]?.let { domain ->
            val archive = json.decodeDomain(
                domain.toString(),
                "notes",
                com.woshiwangnima.healthdietpro.model.memo.MemoArchive.serializer(),
            )
            archive.notes.flatMap { it.blocks }
                .filter { it.kind == com.woshiwangnima.healthdietpro.model.memo.MemoBlockKind.VOICE }
                .forEach { block ->
                    require("attachments/notes/voice/${block.voiceFile}" in paths) {
                        "Memo voice file is not included in archive"
                    }
                }
        }
    }

    /**
     * 在临时目录里拼出完整的用户目录，再整体换上去。
     *
     * `mergeExisting` 为 false（导入为新用户）时不复制现有目录，也不留备份：
     * 目标用户本来就该是从零开始的一份数据。
     */
    private fun stageUserDirectory(
        userId: String,
        domains: Map<String, JsonElement>,
        attachments: List<UserArchiveAttachment>,
        mergeExisting: Boolean = true,
    ): UserDirectoryTransaction {
        val root = File(context.filesDir, "user_archives")
        root.mkdirs()
        val target = userDirectory(userId)
        val staging = File(root, ".import-${safeUserId(userId)}-${System.nanoTime()}")
        val backup = File(root, ".previous-${safeUserId(userId)}-${System.nanoTime()}")
        try {
            staging.mkdirs()
            if (mergeExisting && target.isDirectory) target.copyRecursively(staging, overwrite = true)
            // Attachments are a closed set in UserArchiveBundle. Retaining this directory would
            // resurrect files removed on the exporting device.
            File(staging, "attachments").deleteRecursively()
            domains.forEach { (domainId, content) ->
                // 文件名来自注册表：diet 领域的 id 是 diet_records，但磁盘上必须是 diet.json。
                val fileName = ArchiveDomains.firstOrNull { it.id == domainId }?.fileName ?: "$domainId.json"
                File(staging, fileName).writeText(content.toString(), Charsets.UTF_8)
            }
            attachments.forEach { attachment ->
                val output = File(staging, attachment.path)
                output.parentFile?.mkdirs()
                output.writeBytes(Base64.decode(attachment.contentBase64, Base64.NO_WRAP))
            }
            writeUserArchiveManifest(context, userId, staging)
            if (target.exists()) check(target.renameTo(backup)) { "Unable to stage current user archive" }
            check(staging.renameTo(target)) { "Unable to commit imported user archive" }
            return UserDirectoryTransaction(target, backup)
        } catch (error: Throwable) {
            if (!target.exists() && backup.exists()) backup.renameTo(target)
            throw error
        } finally {
            staging.deleteRecursively()
        }
    }

    private fun validateAttachments(attachments: List<UserArchiveAttachment>) {
        require(attachments.map(UserArchiveAttachment::path).distinct().size == attachments.size) {
            "Duplicate archive attachment path"
        }
        attachments.forEach { attachment ->
            requireValidAttachmentPath(attachment.path)
            val bytes = Base64.decode(attachment.contentBase64, Base64.NO_WRAP)
            require(bytes.isNotEmpty() && bytes.size <= MAX_ATTACHMENT_BYTES) { "Invalid archive attachment" }
        }
    }

    private fun requireValidAttachmentPath(path: String) {
        require(path.startsWith("attachments/")) { "Attachment must be inside attachments" }
        require(!path.contains("\\") && !path.contains("//") && !path.contains("..")) { "Unsafe attachment path" }
        require(!File(path).isAbsolute) { "Absolute attachment path" }
        require(path.split('/').all { it.isNotBlank() }) { "Invalid attachment path" }
    }

    private fun validateDomains(userId: String, domains: Map<String, JsonElement>) {
        domains.forEach { (domainId, content) ->
            val domain = ArchiveDomains.firstOrNull { it.id == domainId }
                ?: error("Unsupported user archive domain: $domainId")
            domain.validate(context, userId, content)
        }
    }

    private fun encodePreference(value: Any): UserArchivePreference? = when (value) {
        is Boolean -> UserArchivePreference(UserArchivePreferenceType.BOOLEAN, JsonPrimitive(value))
        is Int -> UserArchivePreference(UserArchivePreferenceType.INT, JsonPrimitive(value))
        is Long -> UserArchivePreference(UserArchivePreferenceType.LONG, JsonPrimitive(value))
        is Float -> value.takeIf { it.isFinite() }?.let { UserArchivePreference(UserArchivePreferenceType.FLOAT, JsonPrimitive(it)) }
        is String -> UserArchivePreference(UserArchivePreferenceType.STRING, JsonPrimitive(value))
        is Set<*> -> value.filterIsInstance<String>().takeIf { it.size == value.size }?.let { strings ->
            UserArchivePreference(UserArchivePreferenceType.STRING_SET, JsonArray(strings.sorted().map(::JsonPrimitive)))
        }
        else -> null
    }

    private fun decodePreference(preference: UserArchivePreference): Any = when (preference.type) {
        UserArchivePreferenceType.BOOLEAN -> preference.value.jsonPrimitive.booleanOrNull
        UserArchivePreferenceType.INT -> preference.value.jsonPrimitive.intOrNull
        UserArchivePreferenceType.LONG -> preference.value.jsonPrimitive.longOrNull
        UserArchivePreferenceType.FLOAT -> preference.value.jsonPrimitive.floatOrNull?.takeIf { it.isFinite() }
        UserArchivePreferenceType.STRING -> preference.value.jsonPrimitive.content
        UserArchivePreferenceType.STRING_SET -> preference.value.jsonArray.map { it.jsonPrimitive.content }
            .toSet().takeIf { it.size == preference.value.jsonArray.size }
    } ?: error("Invalid preference value")

    /** 档案里要落盘的领域（profile 与偏好另有落盘路径，不在这里）。 */
    private fun UserArchiveBundle.domains(): Map<String, JsonElement> = linkedMapOf<String, JsonElement>().apply {
        ArchiveDomains.filter(ArchiveDomain::readsDisk).forEach { domain ->
            domain.extract(this@domains)?.let { put(domain.id, it) }
        }
    }

    private fun userDirectory(userId: String) = File(context.filesDir, "user_archives/${safeUserId(userId)}")

    private companion object {
        const val MAX_ATTACHMENT_BYTES = 15 * 1024 * 1024
        val json = Json { encodeDefaults = true; explicitNulls = false; ignoreUnknownKeys = true }

        fun safeUserId(userId: String) = userId.replace(Regex("[^A-Za-z0-9_-]"), "_")
    }
}

/** 一次导出的全部产物：写盘用的字节 + 预览用的只读快照。 */
internal data class UserArchiveExport(
    val rawJson: String,
    val snapshot: UserArchiveSnapshot,
)

/** 校验通过、尚未落盘的导入数据。 */
private data class PreparedImport(
    val bundle: UserArchiveBundle,
    val profile: UserProfile,
    val preferences: Map<String, Any>,
    val domains: Map<String, JsonElement>,
    val targetUserId: String,
)

private class UserDirectoryTransaction(
    private val target: File,
    private val backup: File,
) {
    fun commit() {
        backup.deleteRecursively()
    }

    fun rollback() {
        target.deleteRecursively()
        if (backup.exists()) check(backup.renameTo(target)) { "Unable to restore user archive after import failure" }
    }
}

@Serializable
internal data class UserArchiveBundle(
    val formatVersion: ArchiveSchemaVersion,
    val appVersion: String,
    val exportedAt: String,
    val sourceUserId: String,
    val metadata: UserArchiveMetadata? = null,
    val profile: JsonElement,
    val bodyMetrics: JsonElement? = null,
    val medications: JsonElement? = null,
    val bloodGlucose: JsonElement? = null,
    val bloodPressure: JsonElement? = null,
    val water: JsonElement? = null,
    val diseaseRecords: JsonElement? = null,
    val customFoods: JsonElement? = null,
    val sleep: JsonElement? = null,
    // 以下五个领域 1.4.0 时没被带上（磁盘上一直有文件），1.4.1 起纳入档案。
    // 旧档案没有这些字段时解析为 null，`ignoreUnknownKeys` 也保证旧版本读到新字段不会炸。
    val dietRecords: JsonElement? = null,
    val containers: JsonElement? = null,
    val timers: JsonElement? = null,
    val prices: JsonElement? = null,
    val notes: JsonElement? = null,
    val bloodGlucosePredictions: JsonElement? = null,
    val userPreferences: Map<String, UserArchivePreference> = emptyMap(),
    val attachments: List<UserArchiveAttachment> = emptyList(),
)

@Serializable
internal data class UserArchiveMetadata(
    val id: String = "",
    val name: String = "",
    val gender: String = "",
    val avatarFileName: String = "",
    val createdAtMillis: Long = 0L,
    val lastActiveAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L,
)

@Serializable
internal data class UserArchiveAttachment(val path: String, val contentBase64: String)

@Serializable
internal data class UserArchivePreference(val type: UserArchivePreferenceType, val value: JsonElement)

@Serializable
internal enum class UserArchivePreferenceType { BOOLEAN, INT, LONG, FLOAT, STRING, STRING_SET }

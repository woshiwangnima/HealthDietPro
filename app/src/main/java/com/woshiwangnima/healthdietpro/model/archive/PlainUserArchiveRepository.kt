package com.woshiwangnima.healthdietpro.model.archive

import android.content.Context
import android.util.Base64
import com.woshiwangnima.healthdietpro.model.bloodglucose.BloodGlucoseArchiveStore
import com.woshiwangnima.healthdietpro.model.bloodpressure.BloodPressureArchiveStore
import com.woshiwangnima.healthdietpro.model.disease.UserDiseaseArchiveStore
import com.woshiwangnima.healthdietpro.model.food.UserCustomFoodArchiveStore
import com.woshiwangnima.healthdietpro.model.food.UserCustomFoodArchive
import com.woshiwangnima.healthdietpro.model.medication.MedicationArchiveStore
import com.woshiwangnima.healthdietpro.model.medication.MedicationArchive
import com.woshiwangnima.healthdietpro.model.water.WaterArchiveStore
import com.woshiwangnima.healthdietpro.model.prefs.UserPrefs
import com.woshiwangnima.healthdietpro.model.profile.BodyMetricsRepository
import com.woshiwangnima.healthdietpro.model.profile.ProfilePrefs
import com.woshiwangnima.healthdietpro.model.archive.migrateAvatarReference
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

/** Coordinates plain-text import/export without allowing an archive to address another user. */
internal class PlainUserArchiveRepository(
    private val context: Context,
) {
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
        val customFoods = readDomainSnapshot(profile.id, "custom_foods")
        val imageExport = exportCustomFoodImages(profile.id, customFoods)
        val attachments = (collectAttachments(profile.id) + imageExport.attachments).distinctBy(UserArchiveAttachment::path)
        val bundle = UserArchiveBundle(
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
            bodyMetrics = readDomainSnapshot(profile.id, "body_metrics"),
            medications = readDomainSnapshot(profile.id, "medications"),
            bloodGlucose = readDomainSnapshot(profile.id, "blood_glucose"),
            bloodPressure = readDomainSnapshot(profile.id, "blood_pressure"),
            water = readDomainSnapshot(profile.id, "water"),
            diseaseRecords = readDomainSnapshot(profile.id, "disease_records"),
            customFoods = imageExport.domain,
            sleep = readDomainSnapshot(profile.id, "sleep"),
            userPreferences = preferences,
            attachments = attachments,
        )
        stableUserArchiveJsonString(json.encodeToString(bundle))
    }

    fun importIntoCurrentUser(rawArchive: String): Result<Unit> = runCatching {
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
        val domains = bundle.domains().toMutableMap()
        rewriteMedicationAttachmentPaths(targetUserId, domains["medications"])
            ?.let { domains["medications"] = it }
        rewriteCustomFoodImagePaths(targetUserId, domains["custom_foods"])
            ?.let { domains["custom_foods"] = it }
        validateDomains(targetUserId, domains)
        validateAttachments(bundle.attachments)
        validateAttachmentReferences(importedProfile.avatarFileName, domains, bundle.attachments)
        val priorProfile = ProfilePrefs.getProfile(context, targetUserId)
        val priorPreferences = UserPrefs.snapshot(context, targetUserId)
        val directoryTransaction = stageUserDirectory(targetUserId, domains, bundle.attachments)
        try {
            check(UserPrefs.replaceAll(context, targetUserId, preferences))
            ProfilePrefs.replaceCurrentUserFromArchive(
                context = context,
                profile = importedProfile.copy(id = targetUserId),
            )
            directoryTransaction.commit()
        } catch (error: Throwable) {
            directoryTransaction.rollback()
            UserPrefs.replaceAll(context, targetUserId, priorPreferences)
            priorProfile?.let { ProfilePrefs.replaceCurrentUserFromArchive(context, it) }
            throw error
        }
    }

    private fun decodeBundle(rawArchive: String): UserArchiveBundle {
        val root = json.parseToJsonElement(rawArchive).jsonObject
        require("bodyMetrics" in root || "userPreferences" in root || "attachments" in root) {
            "Unsupported legacy user archive"
        }
        return json.decodeFromString<UserArchiveBundle>(rawArchive).also { bundle ->
            require(bundle.formatVersion == ArchiveSchemaVersion.Current) {
                "Unsupported archive schema: ${bundle.formatVersion}"
            }
        }
    }

    private fun validateBundleMetadata(metadata: UserArchiveMetadata?, profile: com.woshiwangnima.healthdietpro.model.profile.UserProfile) {
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

    private fun exportCustomFoodImages(
        userId: String,
        domain: JsonElement?,
    ): CustomFoodImageExport {
        if (domain == null) return CustomFoodImageExport(null, emptyList())
        val archive = runCatching {
            json.decodeDomain(domain.toString(), "custom_foods", UserCustomFoodArchive.serializer())
        }
            .getOrElse { return CustomFoodImageExport(domain, emptyList()) }
        val root = userArchiveDirectory(context, userId)
        val attachments = mutableListOf<UserArchiveAttachment>()
        val foods = archive.foods.map { food ->
            val image = food.image ?: return@map food
            val key = image.localKey.takeIf { it.startsWith(USER_IMAGE_PREFIX) } ?: return@map food
            val source = File(context.filesDir, key.removePrefix(USER_IMAGE_PREFIX)).canonicalFile
            val filesRoot = context.filesDir.canonicalFile
            if (!source.isFile || !source.path.startsWith(filesRoot.path + File.separator)) return@map food
            val exportedPath = "attachments/foods/${safeFileName(source.name)}"
            attachments += UserArchiveAttachment(exportedPath, Base64.encodeToString(source.readBytes(), Base64.NO_WRAP))
            food.copy(image = image.copy(localKey = "$USER_IMAGE_PREFIX${root.relativeTo(context.filesDir).invariantSeparatorsPath}/$exportedPath"))
        }
        return CustomFoodImageExport(
            json.parseToJsonElement(
                json.encodeDomain(context, "custom_foods", archive.copy(foods = foods), UserCustomFoodArchive.serializer()),
            ),
            attachments,
        )
    }

    private fun rewriteCustomFoodImagePaths(userId: String, domain: JsonElement?): JsonElement? {
        if (domain == null) return null
        val archive = json.decodeDomain(domain.toString(), "custom_foods", UserCustomFoodArchive.serializer())
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
        return json.parseToJsonElement(
            json.encodeDomain(context, "custom_foods", archive.copy(foods = foods), UserCustomFoodArchive.serializer()),
        )
    }

    private fun rewriteMedicationAttachmentPaths(userId: String, domain: JsonElement?): JsonElement? {
        if (domain == null) return null
        val archive = json.decodeDomain(domain.toString(), "medications", MedicationArchive.serializer())
        val rootPath = userArchiveDirectory(context, userId).relativeTo(context.filesDir).invariantSeparatorsPath
        fun rewrite(path: String): String = path
            .substringAfter("/attachments/", missingDelimiterValue = "")
            .takeIf { path.startsWith("user_archives/") && it.startsWith("medications/") }
            ?.let { "$rootPath/attachments/$it" }
            ?: path
        val rewritten = archive.copy(
            catalog = archive.catalog.map { item -> item.copy(imagePaths = item.imagePaths.map(::rewrite)) },
            records = archive.records.map { record -> record.copy(
                medicationImagePaths = record.medicationImagePaths.map(::rewrite),
                recordPhotoPaths = record.recordPhotoPaths.map(::rewrite),
            ) },
        )
        return json.parseToJsonElement(
            json.encodeDomain(context, "medications", rewritten, MedicationArchive.serializer()),
        )
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
            val archive = json.decodeDomain(domain.toString(), "medications", MedicationArchive.serializer())
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
            val archive = json.decodeDomain(domain.toString(), "custom_foods", UserCustomFoodArchive.serializer())
            archive.foods.mapNotNull { it.image?.localKey }
                .filter { it.startsWith(USER_IMAGE_PREFIX) }
                .forEach { key ->
                    val attachment = key.substringAfter("/attachments/", missingDelimiterValue = "")
                    require(attachment.startsWith("foods/") && "attachments/$attachment" in paths) {
                        "Custom food image is not included in archive"
                    }
                }
        }
    }

    private fun stageUserDirectory(
        userId: String,
        domains: Map<String, JsonElement>,
        attachments: List<UserArchiveAttachment>,
    ): UserDirectoryTransaction {
        val root = File(context.filesDir, "user_archives")
        root.mkdirs()
        val target = userDirectory(userId)
        val staging = File(root, ".import-${safeUserId(userId)}-${System.nanoTime()}")
        val backup = File(root, ".previous-${safeUserId(userId)}-${System.nanoTime()}")
        try {
            staging.mkdirs()
            if (target.isDirectory) target.copyRecursively(staging, overwrite = true)
            // Attachments are a closed set in UserArchiveBundle. Retaining this directory would
            // resurrect files removed on the exporting device.
            File(staging, "attachments").deleteRecursively()
            domains.forEach { (domainId, content) ->
                File(staging, "$domainId.json").writeText(content.toString(), Charsets.UTF_8)
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

    private fun readDomainSnapshot(userId: String, domainId: String): JsonElement? = runCatching {
        File(userDirectory(userId), "$domainId.json").takeIf(File::isFile)
            ?.readText(Charsets.UTF_8)?.let(json::parseToJsonElement)
    }.getOrNull()

    private fun validateDomains(userId: String, domains: Map<String, JsonElement>) {
        domains.forEach { (domainId, content) ->
            when (domainId) {
                "body_metrics" -> BodyMetricsRepository.forUser(context, userId).validateJson(content.toString())
                "medications" -> MedicationArchiveStore.forUser(context, userId).validateJson(content.toString())
                "blood_glucose" -> BloodGlucoseArchiveStore.forUser(context, userId).validateJson(content.toString())
                "blood_pressure" -> BloodPressureArchiveStore.forUser(context, userId).validateJson(content.toString())
                "water" -> WaterArchiveStore.forUser(context, userId).validateJson(content.toString())
                "disease_records" -> UserDiseaseArchiveStore.forUser(context, userId).validateJson(content.toString())
                "custom_foods" -> UserCustomFoodArchiveStore.forUser(context, userId).validateJson(content.toString())
                "sleep" -> com.woshiwangnima.healthdietpro.model.sleep.SleepArchiveStore.forUser(context, userId).validateJson(content.toString())
                else -> error("Unsupported user archive domain: $domainId")
            }
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

    private fun UserArchiveBundle.domains(): Map<String, JsonElement> = linkedMapOf<String, JsonElement>().apply {
        bodyMetrics?.let { put("body_metrics", it) }
        medications?.let { put("medications", it) }
        bloodGlucose?.let { put("blood_glucose", it) }
        bloodPressure?.let { put("blood_pressure", it) }
        water?.let { put("water", it) }
        diseaseRecords?.let { put("disease_records", it) }
        customFoods?.let { put("custom_foods", it) }
        sleep?.let { put("sleep", it) }
    }

    private fun userDirectory(userId: String) = File(context.filesDir, "user_archives/${safeUserId(userId)}")

    private companion object {
        const val MAX_ATTACHMENT_BYTES = 15 * 1024 * 1024
        const val USER_IMAGE_PREFIX = "user:"
        val json = Json { encodeDefaults = true; explicitNulls = false; ignoreUnknownKeys = false }

        fun safeUserId(userId: String) = userId.replace(Regex("[^A-Za-z0-9_-]"), "_")
        fun safeFileName(fileName: String) = fileName.substringAfterLast('/').replace(Regex("[^A-Za-z0-9._-]"), "_")
    }
}

private data class CustomFoodImageExport(
    val domain: JsonElement?,
    val attachments: List<UserArchiveAttachment>,
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

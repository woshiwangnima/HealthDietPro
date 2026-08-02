package com.woshiwangnima.healthdietpro.model.disease

import android.content.Context
import com.woshiwangnima.healthdietpro.model.archive.decodeDomain
import com.woshiwangnima.healthdietpro.model.archive.encodeDomain
import com.woshiwangnima.healthdietpro.model.archive.writeUserArchiveManifest
import com.woshiwangnima.healthdietpro.model.profile.ProfilePrefs
import java.io.File
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** User-owned disease records. Never reads or writes the static assets/diseases.json catalog. */
internal class UserDiseaseArchiveStore private constructor(
    private val context: Context,
    private val userId: String,
) {
    fun load(): UserDiseaseArchive = synchronized(lock) {
        readCurrent() ?: UserDiseaseArchive()
    }

    fun update(transform: (UserDiseaseArchive) -> UserDiseaseArchive): UserDiseaseArchive = synchronized(lock) {
        transform(load()).also(::write)
    }

    internal fun validateJson(raw: String) {
        synchronized(lock) { validate(json.decodeDomain(raw, DOMAIN_ID, UserDiseaseArchive.serializer())) }
    }

    internal fun replaceJson(raw: String) = synchronized(lock) {
        write(json.decodeDomain(raw, DOMAIN_ID, UserDiseaseArchive.serializer()))
    }

    private fun readCurrent(): UserDiseaseArchive? = read(file())

    private fun read(file: File): UserDiseaseArchive? = runCatching {
        if (!file.isFile) {
            null
        } else {
            val raw = file.readText(Charsets.UTF_8)
            json.decodeDomain(raw, DOMAIN_ID, UserDiseaseArchive.serializer())
        }
    }.getOrNull()

    private fun write(archive: UserDiseaseArchive) {
        validate(archive)
        val target = file()
        target.parentFile?.mkdirs()
        val temporary = File(target.parentFile, "${target.name}.tmp")
        temporary.writeText(json.encodeDomain(context, DOMAIN_ID, archive, UserDiseaseArchive.serializer()), Charsets.UTF_8)
        check(temporary.renameTo(target)) { "Unable to replace user disease archive" }
        writeUserArchiveManifest(context, userId)
        ProfilePrefs.noteUserActivity(context, userId)
    }

    private fun validate(archive: UserDiseaseArchive) {
        validateIds(archive.records.map(UserDiseaseRecord::id), "disease record")
        validateIds(archive.customDiseases.map(UserCustomDisease::id), "custom disease")
    }

    private fun validateIds(ids: List<String>, label: String) {
        require(ids.all { it.isNotBlank() }) { "$label id is blank" }
        require(ids.distinct().size == ids.size) { "Duplicate $label id" }
    }

    private fun file(): File = File(
        context.filesDir,
        "user_archives/${userId.replace(Regex("[^A-Za-z0-9_-]"), "_")}/$FILE_NAME",
    )

    companion object {
        private const val DOMAIN_ID = "disease_records"
        private const val FILE_NAME = "disease_records.json"
        private val lock = Any()
        private val json = Json { ignoreUnknownKeys = false; encodeDefaults = true; explicitNulls = false }

        fun current(context: Context) = UserDiseaseArchiveStore(context.applicationContext, ProfilePrefs.getCurrentUserId(context))
        fun forUser(context: Context, userId: String) = UserDiseaseArchiveStore(context.applicationContext, userId)
    }
}

@Serializable
internal data class UserDiseaseArchive(
    val schemaVersion: Int = 1,
    val records: List<UserDiseaseRecord> = emptyList(),
    val customDiseases: List<UserCustomDisease> = emptyList(),
)

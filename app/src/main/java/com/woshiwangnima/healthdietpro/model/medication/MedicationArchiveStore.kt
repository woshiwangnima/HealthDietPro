package com.woshiwangnima.healthdietpro.model.medication

import android.content.Context
import com.woshiwangnima.healthdietpro.model.archive.decodeDomain
import com.woshiwangnima.healthdietpro.model.archive.encodeDomain
import com.woshiwangnima.healthdietpro.model.archive.writeUserArchiveManifest
import com.woshiwangnima.healthdietpro.model.profile.ProfilePrefs
import java.io.File
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal class MedicationArchiveStore private constructor(
    private val context: Context,
    private val userId: String,
) {
    fun load(): MedicationArchive = synchronized(lock) {
        read() ?: if (archiveFile().isFile) {
            error("Unable to read medication archive for user $userId")
        } else {
            MedicationArchive()
        }
    }

    fun replace(archive: MedicationArchive) = save(archive)

    internal fun validateJson(raw: String) {
        synchronized(lock) { validateIds(json.decodeDomain(raw, DOMAIN_ID, MedicationArchive.serializer())) }
    }

    internal fun replaceJson(raw: String) = synchronized(lock) {
        save(json.decodeDomain(raw, DOMAIN_ID, MedicationArchive.serializer()))
    }

    fun save(archive: MedicationArchive) = synchronized(lock) {
        validateIds(archive)
        val file = archiveFile()
        file.parentFile?.mkdirs()
        val temporary = File(file.parentFile, "${file.name}.tmp")
        temporary.writeText(json.encodeDomain(context, DOMAIN_ID, archive, MedicationArchive.serializer()), Charsets.UTF_8)
        check(temporary.renameTo(file)) { "Unable to replace medication archive" }
        writeUserArchiveManifest(context, userId)
        ProfilePrefs.noteUserActivity(context, userId)
    }

    fun update(transform: (MedicationArchive) -> MedicationArchive): MedicationArchive =
        synchronized(lock) {
            transform(load()).also(::save)
        }

    private fun read(): MedicationArchive? = runCatching {
        val file = archiveFile()
        if (!file.isFile) {
            null
        } else {
            json.decodeDomain(file.readText(Charsets.UTF_8), DOMAIN_ID, MedicationArchive.serializer())
        }
    }.getOrNull()

    private fun archiveFile() = File(context.filesDir, "user_archives/${safeId(userId)}/medications.json")

    private fun safeId(value: String) = value.replace(Regex("[^A-Za-z0-9_-]"), "_")

    private fun validateIds(archive: MedicationArchive) {
        validateIds(archive.catalog.map(MedicationCatalogItem::id), "medication catalog")
        validateIds(archive.records.map(MedicationRecord::id), "medication record")
    }

    private fun validateIds(ids: List<String>, label: String) {
        require(ids.all { it.isNotBlank() }) { "$label id is blank" }
        require(ids.distinct().size == ids.size) { "Duplicate $label id" }
    }

    companion object {
        private const val DOMAIN_ID = "medications"
        private val lock = Any()
        private val json = Json { ignoreUnknownKeys = false; encodeDefaults = true; explicitNulls = false }

        fun current(context: Context) = MedicationArchiveStore(
            context.applicationContext,
            ProfilePrefs.getCurrentUserId(context),
        )

        fun forUser(context: Context, userId: String) = MedicationArchiveStore(context.applicationContext, userId)
    }
}

@Serializable
internal data class MedicationArchive(
    val schemaVersion: Int = 1,
    val catalog: List<MedicationCatalogItem> = emptyList(),
    val records: List<MedicationRecord> = emptyList(),
)

package com.woshiwangnima.healthdietpro.model.bloodglucose

import android.content.Context
import com.woshiwangnima.healthdietpro.model.archive.decodeDomain
import com.woshiwangnima.healthdietpro.model.archive.encodeDomain
import com.woshiwangnima.healthdietpro.model.archive.writeUserArchiveManifest
import com.woshiwangnima.healthdietpro.model.profile.ProfilePrefs
import java.io.File
import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal class BloodGlucoseArchiveStore private constructor(
    private val context: Context,
    private val userId: String,
) {
    fun load(): BloodGlucoseArchive = synchronized(lock) { read() ?: BloodGlucoseArchive() }

    fun update(transform: (BloodGlucoseArchive) -> BloodGlucoseArchive): BloodGlucoseArchive = synchronized(lock) {
        transform(read() ?: BloodGlucoseArchive()).also(::save)
    }

    fun replace(archive: BloodGlucoseArchive) = synchronized(lock) { save(archive) }

    internal fun validateJson(raw: String) {
        synchronized(lock) { validateRecordIds(json.decodeDomain(raw, DOMAIN_ID, BloodGlucoseArchive.serializer()).records) }
    }

    internal fun replaceJson(raw: String) = synchronized(lock) {
        save(json.decodeDomain(raw, DOMAIN_ID, BloodGlucoseArchive.serializer()))
    }

    private fun read(): BloodGlucoseArchive? = runCatching {
        val file = file()
        if (!file.isFile) {
            null
        } else {
            val raw = file.readText(Charsets.UTF_8)
            json.decodeDomain(raw, DOMAIN_ID, BloodGlucoseArchive.serializer())
        }
    }.getOrNull()

    private fun save(archive: BloodGlucoseArchive) {
        validateRecordIds(archive.records)
        val target = file()
        target.parentFile?.mkdirs()
        val temporary = File(target.parentFile, "${target.name}.tmp")
        temporary.writeText(json.encodeDomain(context, DOMAIN_ID, archive, BloodGlucoseArchive.serializer()), Charsets.UTF_8)
        check(temporary.renameTo(target)) { "Unable to replace blood glucose archive" }
        writeUserArchiveManifest(context, userId)
        ProfilePrefs.noteUserActivity(context, userId)
    }

    private fun file(): File = File(context.filesDir, "user_archives/${safeId(userId)}/blood_glucose.json")
    private fun safeId(value: String) = value.replace(Regex("[^A-Za-z0-9_-]"), "_")

    private fun validateRecordIds(records: List<BloodGlucoseRecord>) {
        val ids = records.map(BloodGlucoseRecord::id)
        require(ids.all { it.isNotBlank() }) { "Blood glucose record id is blank" }
        require(ids.distinct().size == ids.size) { "Duplicate blood glucose record id" }
    }

    private fun nextId(usedIds: MutableSet<String>): String {
        var id: String
        do id = UUID.randomUUID().toString() while (!usedIds.add(id))
        return id
    }

    companion object {
        private const val DOMAIN_ID = "blood_glucose"
        private val lock = Any()
        private val json = Json { ignoreUnknownKeys = false; encodeDefaults = true; explicitNulls = false }

        fun current(context: Context) = BloodGlucoseArchiveStore(context.applicationContext, ProfilePrefs.getCurrentUserId(context))
        fun forUser(context: Context, userId: String) = BloodGlucoseArchiveStore(context.applicationContext, userId)
    }
}

@Serializable
internal data class BloodGlucoseArchive(
    val schemaVersion: Int = 1,
    val records: List<BloodGlucoseRecord> = emptyList(),
    val diabetesType: BloodGlucoseDiabetesType = BloodGlucoseDiabetesType.Normal,
    val reminder: BloodGlucoseReminderSettings = BloodGlucoseReminderSettings(),
    val lastAlertAt: Map<BloodGlucoseAlertKind, Long> = emptyMap(),
)

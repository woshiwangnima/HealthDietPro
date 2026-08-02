package com.woshiwangnima.healthdietpro.model.bloodpressure

import android.content.Context
import com.woshiwangnima.healthdietpro.model.archive.decodeDomain
import com.woshiwangnima.healthdietpro.model.archive.encodeDomain
import com.woshiwangnima.healthdietpro.model.archive.writeUserArchiveManifest
import com.woshiwangnima.healthdietpro.model.profile.ProfilePrefs
import java.io.File
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal class BloodPressureArchiveStore private constructor(
    private val context: Context,
    private val userId: String,
) {
    fun load(): BloodPressureArchive = synchronized(lock) { read() ?: BloodPressureArchive() }

    fun save(records: List<BloodPressureRecord>) = synchronized(lock) {
        validate(records)
        write(BloodPressureArchive(records = records.sortedByDescending(BloodPressureRecord::timestamp)))
    }

    fun replace(archive: BloodPressureArchive) = synchronized(lock) {
        validate(archive.records)
        write(archive.copy(records = archive.records.sortedByDescending(BloodPressureRecord::timestamp)))
    }

    internal fun validateJson(raw: String) {
        synchronized(lock) { validate(json.decodeDomain(raw, DOMAIN_ID, BloodPressureArchive.serializer()).records) }
    }

    internal fun replaceJson(raw: String) = synchronized(lock) {
        replace(json.decodeDomain(raw, DOMAIN_ID, BloodPressureArchive.serializer()))
    }

    private fun read(): BloodPressureArchive? = runCatching {
        val target = file()
        if (!target.isFile) {
            null
        } else {
            val raw = target.readText(Charsets.UTF_8)
            json.decodeDomain(raw, DOMAIN_ID, BloodPressureArchive.serializer())
        }
    }.getOrNull()

    private fun write(archive: BloodPressureArchive) {
        val target = file()
        target.parentFile?.mkdirs()
        val temporary = File(target.parentFile, "${target.name}.tmp")
        temporary.writeText(json.encodeDomain(context, DOMAIN_ID, archive, BloodPressureArchive.serializer()), Charsets.UTF_8)
        check(temporary.renameTo(target)) { "Unable to replace blood pressure archive" }
        writeUserArchiveManifest(context, userId)
        ProfilePrefs.noteUserActivity(context, userId)
    }

    private fun validate(records: List<BloodPressureRecord>) {
        val ids = records.map(BloodPressureRecord::id)
        require(ids.all { it.isNotBlank() }) { "Blood pressure record id is blank" }
        require(ids.distinct().size == ids.size) { "Duplicate blood pressure record id" }
    }

    private fun file() = File(context.filesDir, "user_archives/${userId.replace(Regex("[^A-Za-z0-9_-]"), "_")}/blood_pressure.json")

    companion object {
        private const val DOMAIN_ID = "blood_pressure"
        private val lock = Any()
        private val json = Json { ignoreUnknownKeys = false; encodeDefaults = true; explicitNulls = false }
        fun current(context: Context) = BloodPressureArchiveStore(context.applicationContext, ProfilePrefs.getCurrentUserId(context))
        fun forUser(context: Context, userId: String) = BloodPressureArchiveStore(context.applicationContext, userId)
    }
}

@Serializable
internal data class BloodPressureArchive(
    val schemaVersion: Int = 1,
    val records: List<BloodPressureRecord> = emptyList(),
)

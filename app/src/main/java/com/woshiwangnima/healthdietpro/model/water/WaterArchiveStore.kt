package com.woshiwangnima.healthdietpro.model.water

import android.content.Context
import com.woshiwangnima.healthdietpro.model.archive.decodeDomain
import com.woshiwangnima.healthdietpro.model.archive.encodeDomain
import com.woshiwangnima.healthdietpro.model.archive.writeUserArchiveManifest
import com.woshiwangnima.healthdietpro.model.profile.ProfilePrefs
import java.io.File
import kotlinx.serialization.json.Json

internal class WaterArchiveStore private constructor(
    private val context: Context,
    private val userId: String,
) {
    fun load(): WaterArchive = synchronized(lock) { read() ?: WaterArchive() }

    fun save(archive: WaterArchive) = synchronized(lock) {
        validate(archive)
        val target = file()
        target.parentFile?.mkdirs()
        val temporary = File(target.parentFile, "${target.name}.tmp")
        temporary.writeText(
            json.encodeDomain(context, DOMAIN_ID, archive.copy(records = archive.records.sortedByDescending(WaterRecord::timestamp)), WaterArchive.serializer()),
            Charsets.UTF_8,
        )
        check(temporary.renameTo(target)) { "Unable to replace water archive" }
        writeUserArchiveManifest(context, userId)
        ProfilePrefs.noteUserActivity(context, userId)
    }

    fun update(transform: (WaterArchive) -> WaterArchive): WaterArchive = synchronized(lock) {
        transform(read() ?: WaterArchive()).also(::save)
    }

    internal fun validateJson(raw: String) {
        synchronized(lock) { validate(json.decodeDomain(raw, DOMAIN_ID, WaterArchive.serializer())) }
    }

    private fun read(): WaterArchive? = runCatching {
        file().takeIf(File::isFile)?.readText(Charsets.UTF_8)?.let {
            json.decodeDomain(it, DOMAIN_ID, WaterArchive.serializer())
        }
    }.getOrNull()?.let { archive ->
        val migrated = migrateWaterArchive(archive)
        // Persist the v1-to-v2 identity migration before callers can edit a preset.
        if (migrated != archive) save(migrated)
        migrated
    }

    private fun validate(archive: WaterArchive) {
        val recordIds = archive.records.map(WaterRecord::id)
        require(recordIds.all(String::isNotBlank) && recordIds.distinct().size == recordIds.size) { "Invalid water record ids" }
        require(archive.records.all { it.beverageId.isNotBlank() && it.beverageName.isNotBlank() && it.volumeMl > 0.0 }) { "Invalid water record" }
        require(archive.schemaVersion == WATER_ARCHIVE_SCHEMA_VERSION) { "Unsupported water archive schema" }
        val quickRecordIds = archive.quickRecords.map(WaterQuickRecord::id)
        require(quickRecordIds.all(String::isNotBlank) && quickRecordIds.distinct().size == quickRecordIds.size) { "Invalid water quick record ids" }
        require(archive.quickRecords.all { it.beverageId.isNotBlank() && it.volume > 0.0 }) { "Invalid water quick record" }
    }

    private fun file() = File(context.filesDir, "user_archives/${userId.replace(Regex("[^A-Za-z0-9_-]"), "_")}/water.json")

    companion object {
        private const val DOMAIN_ID = "water"
        private val lock = Any()
        private val json = Json { ignoreUnknownKeys = false; encodeDefaults = true; explicitNulls = false }
        fun current(context: Context) = WaterArchiveStore(context.applicationContext, ProfilePrefs.getCurrentUserId(context))
        fun forUser(context: Context, userId: String) = WaterArchiveStore(context.applicationContext, userId)
    }
}

package com.woshiwangnima.healthdietpro.model.sleep

import android.content.Context
import com.woshiwangnima.healthdietpro.model.archive.decodeDomain
import com.woshiwangnima.healthdietpro.model.archive.encodeDomain
import com.woshiwangnima.healthdietpro.model.archive.writeUserArchiveManifest
import com.woshiwangnima.healthdietpro.model.profile.ProfilePrefs
import java.io.File
import kotlinx.serialization.json.Json

internal class SleepArchiveStore private constructor(
    private val context: Context,
    private val userId: String,
) {
    fun load(): SleepArchive = synchronized(lock) { read() ?: SleepArchive() }

    fun save(archive: SleepArchive) = synchronized(lock) {
        validateSleepArchive(archive)
        val target = file()
        target.parentFile?.mkdirs()
        val temporary = File(target.parentFile, "${target.name}.tmp")
        temporary.writeText(
            json.encodeDomain(
                context,
                DOMAIN_ID,
                archive.copy(records = archive.records.sortedByDescending(SleepRecord::sleepStartAt)),
                SleepArchive.serializer(),
            ),
            Charsets.UTF_8,
        )
        check(temporary.renameTo(target)) { "Unable to replace sleep archive" }
        writeUserArchiveManifest(context, userId)
        ProfilePrefs.noteUserActivity(context, userId)
    }

    fun update(transform: (SleepArchive) -> SleepArchive): SleepArchive = synchronized(lock) {
        transform(read() ?: SleepArchive()).also(::save)
    }

    internal fun validateJson(raw: String) {
        synchronized(lock) { validateSleepArchive(json.decodeDomain(raw, DOMAIN_ID, SleepArchive.serializer())) }
    }

    private fun read(): SleepArchive? = runCatching {
        file().takeIf(File::isFile)?.readText(Charsets.UTF_8)?.let {
            json.decodeDomain(it, DOMAIN_ID, SleepArchive.serializer())
        }
    }.getOrNull()?.let { archive ->
        val migrated = migrateSleepArchive(archive)
        if (migrated != archive) save(migrated)
        migrated
    }

    private fun file() = File(context.filesDir, "user_archives/${userId.replace(Regex("[^A-Za-z0-9_-]"), "_")}/sleep.json")

    companion object {
        private const val DOMAIN_ID = "sleep"
        private val lock = Any()
        private val json = Json { ignoreUnknownKeys = false; encodeDefaults = true; explicitNulls = false }
        fun current(context: Context) = SleepArchiveStore(context.applicationContext, ProfilePrefs.getCurrentUserId(context))
        fun forUser(context: Context, userId: String) = SleepArchiveStore(context.applicationContext, userId)
    }
}
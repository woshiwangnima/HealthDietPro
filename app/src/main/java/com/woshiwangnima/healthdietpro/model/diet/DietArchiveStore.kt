package com.woshiwangnima.healthdietpro.model.diet

import android.content.Context
import com.woshiwangnima.healthdietpro.model.archive.decodeDomain
import com.woshiwangnima.healthdietpro.model.archive.encodeDomain
import com.woshiwangnima.healthdietpro.model.archive.writeUserArchiveManifest
import com.woshiwangnima.healthdietpro.model.profile.ProfilePrefs
import java.io.File
import kotlinx.serialization.json.Json

internal class DietArchiveStore private constructor(
    private val context: Context,
    private val userId: String,
) {
    fun load(): DietArchive = synchronized(lock) { read() ?: DietArchive() }

    fun save(archive: DietArchive) = synchronized(lock) {
        validateDietArchive(archive)
        val target = file()
        target.parentFile?.mkdirs()
        val temporary = File(target.parentFile, "${target.name}.tmp")
        temporary.writeText(
            json.encodeDomain(
                context,
                DOMAIN_ID,
                archive.copy(records = archive.records.sortedByDescending(DietRecord::mealStartAt)),
                DietArchive.serializer(),
            ),
            Charsets.UTF_8,
        )
        check(temporary.renameTo(target)) { "Unable to replace diet archive" }
        writeUserArchiveManifest(context, userId)
        ProfilePrefs.noteUserActivity(context, userId)
    }

    fun update(transform: (DietArchive) -> DietArchive): DietArchive = synchronized(lock) {
        transform(read() ?: DietArchive()).also(::save)
    }

    internal fun validateJson(raw: String) {
        synchronized(lock) { validateDietArchive(json.decodeDomain(raw, DOMAIN_ID, DietArchive.serializer())) }
    }

    private fun read(): DietArchive? = runCatching {
        file().takeIf(File::isFile)?.readText(Charsets.UTF_8)?.let {
            json.decodeDomain(it, DOMAIN_ID, DietArchive.serializer())
        }
    }.getOrNull()?.let { archive ->
        val migrated = migrateDietArchive(archive)
        if (migrated != archive) save(migrated)
        migrated
    }

    private fun file() = File(context.filesDir, "user_archives/${userId.replace(Regex("[^A-Za-z0-9_-]"), "_")}/diet.json")

    companion object {
        private const val DOMAIN_ID = "diet_records"
        private val lock = Any()
        private val json = Json { ignoreUnknownKeys = false; encodeDefaults = true; explicitNulls = false }
        fun current(context: Context) = DietArchiveStore(context.applicationContext, ProfilePrefs.getCurrentUserId(context))
        fun forUser(context: Context, userId: String) = DietArchiveStore(context.applicationContext, userId)
    }
}
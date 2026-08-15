package com.woshiwangnima.healthdietpro.model.container

import android.content.Context
import com.woshiwangnima.healthdietpro.model.archive.decodeDomain
import com.woshiwangnima.healthdietpro.model.archive.encodeDomain
import com.woshiwangnima.healthdietpro.model.archive.writeUserArchiveManifest
import com.woshiwangnima.healthdietpro.model.profile.ProfilePrefs
import java.io.File
import kotlinx.serialization.json.Json

internal class ContainerArchiveStore private constructor(
    private val context: Context,
    private val userId: String,
) {
    fun load(): ContainerArchive = synchronized(lock) { read() ?: ContainerArchive() }

    fun save(archive: ContainerArchive) = synchronized(lock) {
        validate(archive)
        val target = file()
        target.parentFile?.mkdirs()
        val temporary = File(target.parentFile, "${target.name}.tmp")
        temporary.writeText(
            json.encodeDomain(
                context,
                DOMAIN_ID,
                archive.copy(containers = archive.containers.sortedByDescending(ContainerRecord::updatedAtMillis)),
                ContainerArchive.serializer(),
            ),
            Charsets.UTF_8,
        )
        check(temporary.renameTo(target)) { "Unable to replace container archive" }
        writeUserArchiveManifest(context, userId)
        ProfilePrefs.noteUserActivity(context, userId)
    }

    fun update(transform: (ContainerArchive) -> ContainerArchive): ContainerArchive = synchronized(lock) {
        transform(read() ?: ContainerArchive()).also(::save)
    }

    internal fun validateJson(raw: String) {
        synchronized(lock) { validate(json.decodeDomain(raw, DOMAIN_ID, ContainerArchive.serializer())) }
    }

    private fun read(): ContainerArchive? = runCatching {
        file().takeIf(File::isFile)?.readText(Charsets.UTF_8)?.let {
            json.decodeDomain(it, DOMAIN_ID, ContainerArchive.serializer())
        }
    }.getOrNull()?.let { archive ->
        val migrated = migrateContainerArchive(archive)
        if (migrated != archive) save(migrated)
        migrated
    }

    private fun validate(archive: ContainerArchive) {
        validateContainerArchive(archive)
    }

    private fun file() = File(context.filesDir, "user_archives/${userId.replace(Regex("[^A-Za-z0-9_-]"), "_")}/containers.json")

    companion object {
        private const val DOMAIN_ID = "containers"
        private val lock = Any()
        private val json = Json { ignoreUnknownKeys = false; encodeDefaults = true; explicitNulls = false }
        fun current(context: Context) = ContainerArchiveStore(context.applicationContext, ProfilePrefs.getCurrentUserId(context))
        fun forUser(context: Context, userId: String) = ContainerArchiveStore(context.applicationContext, userId)
    }
}

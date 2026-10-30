package com.woshiwangnima.healthdietpro.model.price

import android.content.Context
import com.woshiwangnima.healthdietpro.model.archive.decodeDomain
import com.woshiwangnima.healthdietpro.model.archive.encodeDomain
import com.woshiwangnima.healthdietpro.model.archive.writeUserArchiveManifest
import com.woshiwangnima.healthdietpro.model.profile.ProfilePrefs
import java.io.File
import kotlinx.serialization.json.Json

/**
 * 物价归档存储：`user_archives/<userId>/prices.json`。
 * 与容器、饮水等记录一致，走 `encodeDomain/decodeDomain` 的信封格式，备份导出会自动带上。
 */
internal class PriceArchiveStore private constructor(
    private val context: Context,
    private val userId: String,
) {
    fun load(): PriceArchive = synchronized(lock) { read() ?: PriceArchive() }

    fun save(archive: PriceArchive) = synchronized(lock) {
        validatePriceArchive(archive)
        val target = file()
        target.parentFile?.mkdirs()
        val temporary = File(target.parentFile, "${target.name}.tmp")
        temporary.writeText(
            json.encodeDomain(
                context,
                DOMAIN_ID,
                archive.copy(records = archive.records.sortedByDescending(PriceRecord::recordedAt)),
                PriceArchive.serializer(),
            ),
            Charsets.UTF_8,
        )
        check(temporary.renameTo(target)) { "Unable to replace price archive" }
        writeUserArchiveManifest(context, userId)
        ProfilePrefs.noteUserActivity(context, userId)
    }

    fun update(transform: (PriceArchive) -> PriceArchive): PriceArchive = synchronized(lock) {
        transform(read() ?: PriceArchive()).also(::save)
    }

    internal fun validateJson(raw: String) {
        synchronized(lock) { validatePriceArchive(json.decodeDomain(raw, DOMAIN_ID, PriceArchive.serializer())) }
    }

    private fun read(): PriceArchive? = runCatching {
        file().takeIf(File::isFile)?.readText(Charsets.UTF_8)?.let {
            json.decodeDomain(it, DOMAIN_ID, PriceArchive.serializer())
        }
    }.getOrNull()

    private fun file() = File(
        context.filesDir,
        "user_archives/${userId.replace(Regex("[^A-Za-z0-9_-]"), "_")}/prices.json",
    )

    companion object {
        private const val DOMAIN_ID = "prices"
        private val lock = Any()
        private val json = Json { ignoreUnknownKeys = false; encodeDefaults = true; explicitNulls = false }
        fun current(context: Context) =
            PriceArchiveStore(context.applicationContext, ProfilePrefs.getCurrentUserId(context))
        fun forUser(context: Context, userId: String) =
            PriceArchiveStore(context.applicationContext, userId)
    }
}

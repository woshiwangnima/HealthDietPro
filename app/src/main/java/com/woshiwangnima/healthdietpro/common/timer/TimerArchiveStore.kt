package com.woshiwangnima.healthdietpro.common.timer

import android.content.Context
import com.woshiwangnima.healthdietpro.model.archive.decodeDomain
import com.woshiwangnima.healthdietpro.model.archive.decodeDomainSafely
import com.woshiwangnima.healthdietpro.model.archive.prepareArchiveWrite
import com.woshiwangnima.healthdietpro.model.archive.decodeDomainSafely
import com.woshiwangnima.healthdietpro.model.archive.encodeDomain
import com.woshiwangnima.healthdietpro.model.archive.writeUserArchiveManifest
import com.woshiwangnima.healthdietpro.model.profile.ProfilePrefs
import java.io.File
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** 计时器实例的用户级持久化（`user_archives/<uid>/timers.json`）。 */
internal class TimerArchiveStore private constructor(
    private val context: Context,
    private val userId: String,
) {
    fun load(): List<TimerInstance> = synchronized(lock) { read().orEmpty() }

    fun save(instances: List<TimerInstance>) = synchronized(lock) {
        val target = file()
        val temporary = prepareArchiveWrite(target)
        temporary.writeText(
            json.encodeDomain(context, DOMAIN_ID, TimerArchive(instances), TimerArchive.serializer()),
            Charsets.UTF_8,
        )
        check(temporary.renameTo(target)) { "Unable to replace timer archive" }
        writeUserArchiveManifest(context, userId)
        ProfilePrefs.noteUserActivity(context, userId)
    }

    fun update(transform: (List<TimerInstance>) -> List<TimerInstance>) = synchronized(lock) {
        transform(read().orEmpty()).also(::save)
    }

    private fun read(): List<TimerInstance>? =
        json.decodeDomainSafely(file(), DOMAIN_ID, TimerArchive.serializer())?.instances

    private fun file() = File(context.filesDir, "user_archives/${userId.replace(Regex("[^A-Za-z0-9_-]"), "_")}/timers.json")

    companion object {
        private const val DOMAIN_ID = "timers"
        private val lock = Any()
        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; explicitNulls = false }
        fun current(context: Context) = TimerArchiveStore(context.applicationContext, ProfilePrefs.getCurrentUserId(context))
    }
}

@Serializable
internal data class TimerArchive(val instances: List<TimerInstance> = emptyList())
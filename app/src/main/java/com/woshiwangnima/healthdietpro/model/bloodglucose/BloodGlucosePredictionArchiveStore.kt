package com.woshiwangnima.healthdietpro.model.bloodglucose

import android.content.Context
import com.woshiwangnima.healthdietpro.model.archive.decodeDomain
import com.woshiwangnima.healthdietpro.model.archive.encodeDomain
import com.woshiwangnima.healthdietpro.model.archive.writeUserArchiveManifest
import com.woshiwangnima.healthdietpro.model.profile.ProfilePrefs
import java.io.File
import kotlinx.serialization.json.Json

internal class BloodGlucosePredictionArchiveStore private constructor(
    private val context: Context,
    private val userId: String,
) {
    fun load(): BloodGlucosePredictionArchive = synchronized(lock) { read() ?: BloodGlucosePredictionArchive() }

    fun replaceOverlapping(result: BloodGlucosePredictionResult, generatedAt: Long) = synchronized(lock) {
        val current = read() ?: BloodGlucosePredictionArchive()
        val start = result.points.firstOrNull()?.timestamp ?: return
        val end = result.points.lastOrNull()?.timestamp ?: return
        save(current.copy(
            points = (current.points.filterNot { it.timestamp in start..end } + result.points).sortedBy(BloodGlucosePredictionPoint::timestamp),
            generatedAt = generatedAt,
            confidence = result.confidence,
        ))
    }

    private fun read(): BloodGlucosePredictionArchive? = runCatching {
        file().takeIf(File::isFile)?.readText(Charsets.UTF_8)?.let {
            json.decodeDomain(it, DOMAIN_ID, BloodGlucosePredictionArchive.serializer())
        }
    }.getOrNull()

    private fun save(archive: BloodGlucosePredictionArchive) {
        require(archive.points.all { it.timestamp > 0 && it.valueMmolPerL in 1.1..33.3 && it.unitId == "mmol_per_l" })
        require(archive.points.map(BloodGlucosePredictionPoint::timestamp).distinct().size == archive.points.size) {
            "Duplicate blood glucose prediction timestamps"
        }
        val target = file()
        target.parentFile?.mkdirs()
        val temporary = File(target.parentFile, "${target.name}.tmp")
        temporary.writeText(json.encodeDomain(context, DOMAIN_ID, archive, BloodGlucosePredictionArchive.serializer()), Charsets.UTF_8)
        check(temporary.renameTo(target)) { "Unable to replace blood glucose prediction archive" }
        writeUserArchiveManifest(context, userId)
        ProfilePrefs.noteUserActivity(context, userId)
    }

    private fun file() = File(context.filesDir, "user_archives/${userId.replace(Regex("[^A-Za-z0-9_-]"), "_")}/blood_glucose_predictions.json")

    companion object {
        private const val DOMAIN_ID = "blood_glucose_predictions"
        private val lock = Any()
        private val json = Json { ignoreUnknownKeys = false; encodeDefaults = true; explicitNulls = false }
        fun current(context: Context) = BloodGlucosePredictionArchiveStore(context.applicationContext, ProfilePrefs.getCurrentUserId(context))
    }
}

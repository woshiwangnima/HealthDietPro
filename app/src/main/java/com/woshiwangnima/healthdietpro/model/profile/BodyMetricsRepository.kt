package com.woshiwangnima.healthdietpro.model.profile

import android.content.Context
import com.woshiwangnima.healthdietpro.model.archive.decodeDomain
import com.woshiwangnima.healthdietpro.model.archive.encodeDomain
import com.woshiwangnima.healthdietpro.model.archive.writeUserArchiveManifest
import java.io.File
import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal class BodyMetricsRepository private constructor(
    private val context: Context,
    private val userId: String,
) {
    fun load(): BodyMetrics = synchronized(lock) {
        readStored()?.let(::normalize) ?: BodyMetrics()
    }

    fun replace(metrics: BodyMetrics) {
        synchronized(lock) { save(normalize(metrics)) }
    }

    internal fun validateJson(raw: String) {
        synchronized(lock) { normalize(json.decodeDomain(raw, DOMAIN_ID, BodyMetricsDto.serializer()).toDomain()) }
    }

    internal fun replaceJson(raw: String) {
        synchronized(lock) { save(normalize(json.decodeDomain(raw, DOMAIN_ID, BodyMetricsDto.serializer()).toDomain())) }
    }

    fun update(transform: (BodyMetrics) -> BodyMetrics): BodyMetrics = synchronized(lock) {
        normalize(transform(readStored()?.let(::normalize) ?: BodyMetrics())).also(::save)
    }

    private fun readStored(): BodyMetrics? = runCatching {
        val file = archiveFile()
        if (!file.isFile) return null
        val raw = file.readText(Charsets.UTF_8)
        json.decodeDomain(raw, DOMAIN_ID, BodyMetricsDto.serializer()).toDomain()
    }.getOrNull()

    private fun save(metrics: BodyMetrics) {
        val file = archiveFile()
        file.parentFile?.mkdirs()
        val temporary = File(file.parentFile, "${file.name}.tmp")
        temporary.writeText(json.encodeDomain(context, DOMAIN_ID, metrics.toDto(), BodyMetricsDto.serializer()), Charsets.UTF_8)
        check(temporary.renameTo(file)) { "Unable to replace body metrics archive: ${file.name}" }
        writeUserArchiveManifest(context, userId)
        ProfilePrefs.noteUserActivity(context, userId)
    }

    private fun normalize(metrics: BodyMetrics): BodyMetrics {
        val usedIds = mutableSetOf<String>()
        fun normalizeRecords(records: List<BodyRecord>): List<BodyRecord> = records.map { record ->
            val existingId = record.id.orEmpty()
            val id = if (existingId.isBlank()) nextId(usedIds) else {
                require(usedIds.add(existingId)) { "Duplicate body record id: $existingId" }
                existingId
            }
            record.copy(id = id)
        }
        return BodyMetrics(
            heightRecords = normalizeRecords(metrics.heightRecords),
            weightRecords = normalizeRecords(metrics.weightRecords),
            circumferenceRecords = metrics.circumferenceRecords.mapValues { (_, records) -> normalizeRecords(records) },
        )
    }

    private fun nextId(usedIds: MutableSet<String>): String {
        var id: String
        do id = UUID.randomUUID().toString() while (!usedIds.add(id))
        return id
    }

    private fun archiveFile(): File =
        File(context.filesDir, "user_archives/${safeId(userId)}/body_metrics.json")

    private fun safeId(value: String): String = value.replace(Regex("[^A-Za-z0-9_-]"), "_")

    companion object {
        private const val DOMAIN_ID = "body_metrics"
        private val json = Json { ignoreUnknownKeys = false; encodeDefaults = true; explicitNulls = false }
        private val lock = Any()

        fun current(context: Context): BodyMetricsRepository = forUser(context, ProfilePrefs.getCurrentUserId(context))

        fun forUser(context: Context, userId: String): BodyMetricsRepository =
            BodyMetricsRepository(context.applicationContext, userId)
    }
}

internal data class BodyMetrics(
    val heightRecords: List<BodyRecord> = emptyList(),
    val weightRecords: List<BodyRecord> = emptyList(),
    val circumferenceRecords: Map<String, List<BodyRecord>> = emptyMap(),
)

@Serializable
private data class BodyMetricsDto(
    val schemaVersion: Int = 1,
    val heightRecords: List<BodyRecordDto> = emptyList(),
    val weightRecords: List<BodyRecordDto> = emptyList(),
    val circumferenceRecords: Map<String, List<BodyRecordDto>> = emptyMap(),
)

@Serializable
private data class BodyRecordDto(
    val id: String = "",
    val date: String,
    val value: Float,
    val unit: String? = null,
    val recordedAtMillis: Long,
)

private fun BodyMetrics.toDto(): BodyMetricsDto = BodyMetricsDto(
    heightRecords = heightRecords.map(BodyRecord::toDto),
    weightRecords = weightRecords.map(BodyRecord::toDto),
    circumferenceRecords = circumferenceRecords.mapValues { (_, records) -> records.map(BodyRecord::toDto) },
)

private fun BodyMetricsDto.toDomain(): BodyMetrics = BodyMetrics(
    heightRecords = heightRecords.map(BodyRecordDto::toDomain),
    weightRecords = weightRecords.map(BodyRecordDto::toDomain),
    circumferenceRecords = circumferenceRecords.mapValues { (_, records) -> records.map(BodyRecordDto::toDomain) },
)

private fun BodyRecord.toDto(): BodyRecordDto = BodyRecordDto(id.orEmpty(), date, value, unit, recordedAtMillis)

private fun BodyRecordDto.toDomain(): BodyRecord = BodyRecord(date, value, unit, recordedAtMillis, id)

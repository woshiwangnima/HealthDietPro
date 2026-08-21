package com.woshiwangnima.healthdietpro.model.bloodglucose

import android.content.Context
import com.woshiwangnima.healthdietpro.model.profile.ProfilePrefs
import java.io.File
import java.time.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal class AgpPreviewImporter(private val context: Context) {
    private val pendingFile = File(context.filesDir, "agp_import/pending_agp_preview.json")

    private val format = "agp-vector-preview-v1"
    // Preview metadata such as the local PDF path is audit-only and not part of
    // the import contract; strict validation remains on all required fields.
    private val json = Json { ignoreUnknownKeys = true }

    fun loadSources(): List<BloodGlucoseSource> = BloodGlucoseRepository.fromContext(context).loadArchive().sources

    fun importPending(sourceId: String): Int {
        val userId = ProfilePrefs.getCurrentUserId(context)
        require(userId.isNotBlank()) { "No current user selected" }
        require(pendingFile.isFile) { "No pending AGP preview" }
        val raw = pendingFile.readText(Charsets.UTF_8)
        val preview = json.decodeFromString(AgpPreview.serializer(), raw)
        require(preview.format == format) { "Unsupported AGP preview format" }
        require(preview.timezone == "Asia/Shanghai") { "Unsupported AGP preview timezone" }
        require(preview.sampleIntervalMinutes == 5) { "Unsupported AGP sampling interval" }
        require(preview.readings.isNotEmpty()) { "AGP preview contains no readings" }
        require(loadSources().any { it.id == sourceId }) { "Selected blood glucose source no longer exists" }
        val importId = "agp_pdf_${raw.hashCode().toUInt().toString(16)}"
        val records = preview.readings.map { reading ->
            val timestamp = normalizeBloodGlucoseTimestamp(Instant.parse(reading.timestamp).toEpochMilli())
            require(reading.valueMmolPerL.isFinite() && reading.valueMmolPerL in 1.1..33.3) { "AGP value out of range" }
            BloodGlucoseRecord(
                id = "${importId}_${timestamp}",
                timestamp = timestamp,
                valueMmolPerL = reading.valueMmolPerL,
                sourceId = sourceId,
            )
        }
        require(records.map(BloodGlucoseRecord::id).distinct().size == records.size) { "Duplicate AGP timestamps" }
        val inserted = BloodGlucoseRepository.fromContext(context).importAgp(records, sourceId)
        pendingFile.delete()
        return inserted
    }

    fun writeResult(result: String) {
        val resultFile = File(context.filesDir, "agp_import/import_result.txt")
        resultFile.parentFile?.mkdirs()
        resultFile.writeText(result, Charsets.UTF_8)
    }
}

@Serializable
private data class AgpPreview(
    val format: String,
    val timezone: String,
    val sampleIntervalMinutes: Int,
    val days: List<AgpPreviewDay>,
    val readings: List<AgpPreviewReading>,
)

@Serializable
private data class AgpPreviewDay(val date: String)

@Serializable
private data class AgpPreviewReading(val timestamp: String, val valueMmolPerL: Double)

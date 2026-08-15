package com.woshiwangnima.healthdietpro.model.archive

import android.content.Context
import java.io.File
import java.security.MessageDigest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

internal data class UserArchiveDiagnostics(
    val userId: String,
    val archiveSchemaVersion: ArchiveSchemaVersion?,
    val integrity: UserArchiveIntegrity,
    val invalidDomainIds: List<String>,
    val domains: List<UserArchiveDomainDiagnostics>,
)

internal data class UserArchiveDomainDiagnostics(
    val domainId: String,
    val byteSize: Long,
    val recordCount: Int?,
    val checksumSha256: String,
    val schemaVersion: ArchiveSchemaVersion?,
    val error: String? = null,
)

/** Read-only diagnostics used by device-upgrade verification tooling. */
internal fun inspectUserArchiveDiagnostics(context: Context, userId: String): UserArchiveDiagnostics {
    val report = inspectUserArchive(context, userId)
    val manifest = readUserArchiveManifest(context, userId)
    val directory = userArchiveDirectory(context, userId)
    val domains = archiveDiagnosticFiles.mapNotNull { (domainId, fileName) ->
        val file = File(directory, fileName).takeIf(File::isFile) ?: return@mapNotNull null
        val manifestDomain = manifest?.domains?.get(domainId)
        UserArchiveDomainDiagnostics(
            domainId = domainId,
            byteSize = file.length(),
            recordCount = diagnosticRecordCount(file),
            checksumSha256 = sha256(file.readBytes()),
            schemaVersion = manifestDomain?.schemaVersion,
            error = when {
                domainId in report.invalidDomainIds -> "Invalid or unsupported domain envelope"
                manifestDomain == null -> "Missing manifest entry"
                manifestDomain.byteSize != file.length() -> "Manifest byte size mismatch"
                manifestDomain.checksumSha256 != sha256(file.readBytes()) -> "Manifest checksum mismatch"
                else -> null
            },
        )
    }
    return UserArchiveDiagnostics(
        userId = userId,
        archiveSchemaVersion = manifest?.archiveSchemaVersion,
        integrity = report.integrity,
        invalidDomainIds = report.invalidDomainIds,
        domains = domains,
    )
}

private fun diagnosticRecordCount(file: File): Int? = runCatching {
    val root = diagnosticJson.parseToJsonElement(file.readText(Charsets.UTF_8)).jsonObject
    val payload = root["payload"]?.jsonObject ?: root
    when {
        payload["records"] is kotlinx.serialization.json.JsonArray -> payload["records"]!!.jsonArray.size
        payload["foods"] is kotlinx.serialization.json.JsonArray -> payload["foods"]!!.jsonArray.size
        payload["heightRecords"] is kotlinx.serialization.json.JsonArray -> payload["heightRecords"]!!.jsonArray.size
        else -> null
    }
}.getOrNull()

private val archiveDiagnosticFiles = linkedMapOf(
    "profile" to "profile.json",
    "body_metrics" to "body_metrics.json",
    "medications" to "medications.json",
    "blood_glucose" to "blood_glucose.json",
    "blood_pressure" to "blood_pressure.json",
    "water" to "water.json",
    "disease_records" to "disease_records.json",
    "custom_foods" to "custom_foods.json",
    "sleep" to "sleep.json",
    "user_preferences" to "user_preferences.json",
)

private val diagnosticJson = Json { ignoreUnknownKeys = true }

private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
    .digest(bytes)
    .joinToString("") { "%02x".format(it) }

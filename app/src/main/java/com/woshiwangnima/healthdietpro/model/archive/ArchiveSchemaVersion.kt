package com.woshiwangnima.healthdietpro.model.archive

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

@Serializable(with = ArchiveSchemaVersionSerializer::class)
data class ArchiveSchemaVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
) : Comparable<ArchiveSchemaVersion> {
    init {
        require(major >= 0 && minor >= 0 && patch >= 0)
    }

    override fun compareTo(other: ArchiveSchemaVersion): Int = compareValuesBy(
        this,
        other,
        ArchiveSchemaVersion::major,
        ArchiveSchemaVersion::minor,
        ArchiveSchemaVersion::patch,
    )

    override fun toString(): String = "$major.$minor.$patch"

    companion object {
        val Unversioned = ArchiveSchemaVersion(0, 0, 0)
        val LegacyV1 = ArchiveSchemaVersion(0, 0, 1)
        val LegacyV2 = ArchiveSchemaVersion(0, 0, 2)
        val LegacyV3 = ArchiveSchemaVersion(0, 0, 3)
        val Current = ArchiveSchemaVersion(1, 0, 0)
    }
}

internal fun archiveSchemaVersionFromLegacy(value: Int?): ArchiveSchemaVersion = when (value) {
    null, 0 -> ArchiveSchemaVersion.Unversioned
    1 -> ArchiveSchemaVersion.LegacyV1
    2 -> ArchiveSchemaVersion.LegacyV2
    3 -> ArchiveSchemaVersion.LegacyV3
    else -> ArchiveSchemaVersion(0, 0, value.coerceAtLeast(0))
}

internal fun migrateArchiveSchemaVersion(
    storedVersion: ArchiveSchemaVersion?,
): ArchiveSchemaVersion {
    var migrated = storedVersion ?: ArchiveSchemaVersion.Unversioned
    if (migrated < ArchiveSchemaVersion.LegacyV1) migrated = ArchiveSchemaVersion.LegacyV1
    if (migrated < ArchiveSchemaVersion.LegacyV2) migrated = ArchiveSchemaVersion.LegacyV2
    if (migrated < ArchiveSchemaVersion.LegacyV3) migrated = ArchiveSchemaVersion.LegacyV3
    if (migrated < ArchiveSchemaVersion.Current) migrated = ArchiveSchemaVersion.Current
    return migrated
}

object ArchiveSchemaVersionSerializer : KSerializer<ArchiveSchemaVersion> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ArchiveSchemaVersion")

    override fun serialize(encoder: Encoder, value: ArchiveSchemaVersion) {
        val jsonEncoder = encoder as? JsonEncoder ?: error("Archive schema version requires JSON")
        jsonEncoder.encodeJsonElement(buildJsonObject {
            put("major", value.major)
            put("minor", value.minor)
            put("patch", value.patch)
        })
    }

    override fun deserialize(decoder: Decoder): ArchiveSchemaVersion {
        val jsonDecoder = decoder as? JsonDecoder ?: error("Archive schema version requires JSON")
        return when (val element = jsonDecoder.decodeJsonElement()) {
            is JsonPrimitive -> archiveSchemaVersionFromLegacy(element.intOrNull)
            is JsonObject -> ArchiveSchemaVersion(
                major = element["major"]?.jsonPrimitive?.intOrNull ?: 0,
                minor = element["minor"]?.jsonPrimitive?.intOrNull ?: 0,
                patch = element["patch"]?.jsonPrimitive?.intOrNull ?: 0,
            )
            else -> error("Invalid archive schema version")
        }
    }
}

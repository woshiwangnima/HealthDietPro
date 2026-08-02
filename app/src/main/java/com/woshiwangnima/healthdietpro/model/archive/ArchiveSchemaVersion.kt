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
        val UserArchiveEnvelope = ArchiveSchemaVersion(1, 4, 0)
        val Current = UserArchiveEnvelope
    }
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
        val element = jsonDecoder.decodeJsonElement() as? JsonObject
            ?: error("Archive schema version must be an object")
        return ArchiveSchemaVersion(
            major = element["major"]?.jsonPrimitive?.intOrNull ?: error("Missing archive schema major"),
            minor = element["minor"]?.jsonPrimitive?.intOrNull ?: error("Missing archive schema minor"),
            patch = element["patch"]?.jsonPrimitive?.intOrNull ?: error("Missing archive schema patch"),
        )
    }
}

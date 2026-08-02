package com.woshiwangnima.healthdietpro.model.archive

import android.content.Context
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

@Serializable
internal data class DomainEnvelope<T>(
    val domainId: String,
    val schemaVersion: ArchiveSchemaVersion,
    val appVersion: String,
    val updatedAtMillis: Long,
    val payload: T,
)

internal fun <T> Json.decodeDomain(
    raw: String,
    expectedDomainId: String,
    payloadSerializer: KSerializer<T>,
): T {
    val envelope = decodeFromString(DomainEnvelope.serializer(payloadSerializer), raw)
    require(envelope.domainId == expectedDomainId) { "Unexpected archive domain: ${envelope.domainId}" }
    require(envelope.schemaVersion == ArchiveSchemaVersion.Current) { "Unsupported archive schema: ${envelope.schemaVersion}" }
    return envelope.payload
}

internal fun <T> Json.encodeDomain(
    context: Context,
    domainId: String,
    payload: T,
    payloadSerializer: KSerializer<T>,
): String = encodeToString(
    DomainEnvelope.serializer(payloadSerializer),
    DomainEnvelope(
        domainId = domainId,
        schemaVersion = ArchiveSchemaVersion.Current,
        appVersion = appVersion(context),
        updatedAtMillis = System.currentTimeMillis(),
        payload = payload,
    ),
)

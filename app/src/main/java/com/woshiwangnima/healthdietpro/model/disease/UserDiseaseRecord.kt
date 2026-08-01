package com.woshiwangnima.healthdietpro.model.disease

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.put
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import java.time.LocalDate
import com.woshiwangnima.healthdietpro.model.profile.Gender

@Serializable
internal enum class DiseaseHistoryType { SELF, FAMILY, PAST, RISK }

@Serializable
internal enum class DiseaseRecordStatus { ACTIVE, RESOLVED, ONGOING_RISK, HISTORY_ONLY }

@Serializable
internal enum class DiseaseDurationKind { SHORT_TERM, LONG_TERM, UNKNOWN }

@Serializable
internal enum class FamilyRelation { PARENT, SIBLING, CHILD, GRANDPARENT, OTHER }

/** A disease reference is a tagged union: exactly one source can be selected. */
@Serializable(with = DiseaseReferenceSerializer::class)
sealed class DiseaseReference {
    @Serializable
    @SerialName("curated")
    data class Curated(
        /** Key is the ICD family/version, currently "11"; value is the stable app disease id. */
        val curatedDiseaseId: Map<String, String>,
    ) : DiseaseReference() {
        init { require(curatedDiseaseId.isNotEmpty() && curatedDiseaseId.values.all(String::isNotBlank)) }
    }

    @Serializable
    @SerialName("custom")
    data class Custom(
        val customDiseaseId: String,
    ) : DiseaseReference() {
        init { require(customDiseaseId.isNotBlank()) }
    }

    val isCustom: Boolean get() = this is Custom
}

internal fun DiseaseReference.curatedId(icdFamily: String = "11"): String? =
    (this as? DiseaseReference.Curated)?.curatedDiseaseId?.get(icdFamily)

internal fun DiseaseReference.customId(): String? =
    (this as? DiseaseReference.Custom)?.customDiseaseId

object DiseaseReferenceSerializer : KSerializer<DiseaseReference> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("DiseaseReference")

    override fun serialize(encoder: Encoder, value: DiseaseReference) {
        val jsonEncoder = encoder as? JsonEncoder ?: error("DiseaseReference requires JSON")
        jsonEncoder.encodeJsonElement(when (value) {
            is DiseaseReference.Curated -> buildJsonObject {
                put("kind", "curated")
                put("curatedDiseaseId", jsonEncoder.json.encodeToJsonElement(MapSerializer(String.serializer(), String.serializer()), value.curatedDiseaseId))
            }
            is DiseaseReference.Custom -> buildJsonObject {
                put("kind", "custom")
                put("customDiseaseId", value.customDiseaseId)
            }
        })
    }

    override fun deserialize(decoder: Decoder): DiseaseReference {
        val jsonDecoder = decoder as? JsonDecoder ?: error("DiseaseReference requires JSON")
        val objectValue = jsonDecoder.decodeJsonElement().jsonObject
        return when (objectValue["kind"]?.jsonPrimitive?.content) {
            "curated" -> DiseaseReference.Curated(
                jsonDecoder.json.decodeFromJsonElement(objectValue.getValue("curatedDiseaseId")),
            )
            "custom" -> DiseaseReference.Custom(objectValue.getValue("customDiseaseId").jsonPrimitive.content)
            null -> decodeLegacy(objectValue, jsonDecoder)
            else -> error("Unknown disease reference kind")
        }
    }

    private fun decodeLegacy(value: JsonObject, decoder: JsonDecoder): DiseaseReference {
        value["customDiseaseId"]?.jsonPrimitive?.contentOrNull?.let(DiseaseReference::Custom)
            ?.let { return it }
        val curated = value["curatedDiseaseId"] ?: error("Disease reference has no source")
        return when (curated) {
            is JsonPrimitive -> DiseaseReference.Curated(mapOf("11" to curated.content))
            else -> DiseaseReference.Curated(
                decoder.json.decodeFromJsonElement(MapSerializer(String.serializer(), String.serializer()), curated),
            )
        }
    }
}

@Serializable
internal data class UserCustomDisease(
    val id: String,
    val name: String,
    val code: String,
    val aliases: List<String> = emptyList(),
    val description: String = "",
    val applicableGenders: List<Gender> = emptyList(),
    val categoryIds: List<String> = emptyList(),
    val careDepartmentIds: List<String> = emptyList(),
    val note: String = "",
    val createdAt: Long,
    val updatedAt: Long,
) {
    init {
        require(id.startsWith(CUSTOM_DISEASE_ID_PREFIX))
        require(name.isNotBlank())
        require(code.isNotBlank())
    }
}

internal const val CUSTOM_DISEASE_ID_PREFIX = "custom:"

@Serializable
internal data class UserDiseaseRecord(
    val id: String,
    val disease: DiseaseReference,
    val historyType: DiseaseHistoryType,
    val status: DiseaseRecordStatus,
    val durationKind: DiseaseDurationKind = DiseaseDurationKind.UNKNOWN,
    val diagnosedOn: String? = null,
    val resolvedOn: String? = null,
    val familyRelation: FamilyRelation? = null,
    val careFacility: String = "",
    val clinicianName: String = "",
    val note: String = "",
    val createdAt: Long,
    val updatedAt: Long,
) {
    fun validate() {
        require(allowedStatuses(historyType).contains(status))
        require(historyType == DiseaseHistoryType.FAMILY == (familyRelation != null))
        require(status == DiseaseRecordStatus.RESOLVED == (resolvedOn != null))
        diagnosedOn?.let(::parseDate)
        resolvedOn?.let(::parseDate)
        if (diagnosedOn != null && resolvedOn != null) require(resolvedOn >= diagnosedOn)
    }
}

internal fun allowedStatuses(historyType: DiseaseHistoryType): Set<DiseaseRecordStatus> = when (historyType) {
    DiseaseHistoryType.SELF -> setOf(DiseaseRecordStatus.ACTIVE, DiseaseRecordStatus.RESOLVED, DiseaseRecordStatus.HISTORY_ONLY)
    DiseaseHistoryType.FAMILY -> setOf(DiseaseRecordStatus.HISTORY_ONLY)
    DiseaseHistoryType.PAST -> setOf(DiseaseRecordStatus.HISTORY_ONLY, DiseaseRecordStatus.RESOLVED)
    DiseaseHistoryType.RISK -> setOf(DiseaseRecordStatus.ONGOING_RISK, DiseaseRecordStatus.HISTORY_ONLY)
}

private fun parseDate(value: String) = LocalDate.parse(value)

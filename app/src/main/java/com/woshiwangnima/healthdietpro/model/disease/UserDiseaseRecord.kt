package com.woshiwangnima.healthdietpro.model.disease

import kotlinx.serialization.Serializable
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

@Serializable
internal data class DiseaseReference(
    val curatedDiseaseId: String? = null,
    val customDiseaseId: String? = null,
) {
    init {
        require((curatedDiseaseId != null) != (customDiseaseId != null))
    }

    val isCustom: Boolean get() = customDiseaseId != null
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

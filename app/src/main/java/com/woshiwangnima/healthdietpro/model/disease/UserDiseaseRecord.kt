package com.woshiwangnima.healthdietpro.model.disease

import com.woshiwangnima.healthdietpro.model.profile.Gender
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
enum class DiseaseSourceKind { CURATED, CUSTOM }

@Serializable
data class DiseaseReference(
    val sourceKind: DiseaseSourceKind,
    val diseaseId: String,
) {
    init {
        require(diseaseId.isNotBlank())
        require(
            when (sourceKind) {
                DiseaseSourceKind.CURATED -> diseaseId.startsWith("ICD-")
                DiseaseSourceKind.CUSTOM -> diseaseId.startsWith("CUSTOM-")
            },
        )
    }
}

internal fun DiseaseReference.curatedId(): String? = diseaseId.takeIf { sourceKind == DiseaseSourceKind.CURATED }
internal fun DiseaseReference.customId(): String? = diseaseId.takeIf { sourceKind == DiseaseSourceKind.CUSTOM }
internal fun DiseaseReference.stableId(): String = diseaseId

@Serializable
enum class DiseaseSubjectType {
    SELF,
    FATHER,
    MOTHER,
    OLDER_BROTHER,
    YOUNGER_BROTHER,
    OLDER_SISTER,
    YOUNGER_SISTER,
    HUSBAND,
    WIFE,
    FRIEND,
    CLASSMATE,
    OTHER,
}

@Serializable
enum class DiseaseStatus { ACTIVE, ONGOING_RISK, RESOLVED, HISTORY_ONLY }

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
        require(id.startsWith("CUSTOM-"))
        require(name.isNotBlank())
        require(code.isNotBlank())
    }
}

@Serializable
internal data class UserDiseaseRecord(
    val id: String,
    val disease: DiseaseReference,
    val subjectType: DiseaseSubjectType,
    val subjectNote: String? = null,
    val status: DiseaseStatus = DiseaseStatus.HISTORY_ONLY,
    val course: DiseaseCourse = DiseaseCourse.UNKNOWN,
    val startedOn: String? = null,
    val endedOn: String? = null,
    val medicalInstitution: String? = null,
    val doctorName: String? = null,
    val doctorPhone: String? = null,
    val note: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
) {
    fun validate() {
        require(id.isNotBlank())
        startedOn?.let(LocalDate::parse)
        endedOn?.let(LocalDate::parse)
    }
}

internal const val CUSTOM_DISEASE_ID_PREFIX = "CUSTOM-"

internal fun String.toCustomDiseaseId(): String =
    if (startsWith(CUSTOM_DISEASE_ID_PREFIX)) this else CUSTOM_DISEASE_ID_PREFIX + substringAfter("custom:", this).uppercase()

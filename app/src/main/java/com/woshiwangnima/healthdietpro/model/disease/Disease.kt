package com.woshiwangnima.healthdietpro.model.disease

import com.woshiwangnima.healthdietpro.model.i18n.localizedI18nValue
import com.woshiwangnima.healthdietpro.model.profile.Gender
import kotlinx.serialization.Serializable
import java.util.Locale

@Serializable
data class Disease(
    val id: String,
    val i18n: Map<String, DiseaseI18n> = emptyMap(),
    val nutrientRecommendations: List<NutrientRecommendation>,
    val clinicalKind: ClinicalKind,
    val categoryIds: List<String>,
    val applicability: SexApplicability = SexApplicability(),
    val icd11References: List<Icd11Reference> = emptyList(),
    val course: DiseaseCourse = DiseaseCourse.UNSPECIFIED,
    val careDepartmentIds: List<String> = emptyList(),
    val sourceIds: List<String> = emptyList(),
    val metricReferences: List<DiseaseMetricReference> = emptyList(),
) {
    fun displayName(locale: Locale = Locale.getDefault()): String {
        return localizedI18nValue(i18n, locale) { it.label } ?: id
    }
}

@Serializable
data class DiseaseI18n(
    val label: String,
    val aliases: List<String> = emptyList(),
    val description: String = "",
)

@Serializable
data class Icd11Reference(
    val release: String,
    val chapterCode: String,
    val chapterTitle: Map<String, String>,
    val blockCode: String? = null,
    val code: String,
    val title: Map<String, String>,
    val mappingType: Icd11MappingType = Icd11MappingType.PRIMARY,
)

@Serializable
enum class Icd11MappingType {
    PRIMARY,
    BROADER_TERM,
    NARROWER_TERM,
    DIFFERENTIAL,
}

@Serializable
enum class ClinicalKind {
    DISEASE,
    SYNDROME,
    RISK_STATE,
    LABORATORY_ABNORMALITY,
}

@Serializable
enum class DiseaseCourse {
    ACUTE,
    CHRONIC,
    EPISODIC,
    UNSPECIFIED,
}

@Serializable
enum class HealthMetricKind {
    BLOOD_GLUCOSE,
    BLOOD_PRESSURE,
}

/** Read-only educational link; it does not change measurement rules or reminders. */
@Serializable
data class DiseaseMetricReference(
    val metric: HealthMetricKind,
    val sourceId: String,
)

@Serializable
data class DiseaseCategory(
    val id: String,
    val i18n: Map<String, String>,
) {
    fun displayName(locale: Locale = Locale.getDefault()): String {
        return localizedI18nValue(i18n, locale) { it } ?: id
    }
}

@Serializable
data class CareDepartment(
    val id: String,
    val i18n: Map<String, String>,
) {
    fun displayName(locale: Locale = Locale.getDefault()): String {
        return localizedI18nValue(i18n, locale) { it } ?: id
    }
}

@Serializable
data class SexApplicability(
    val applicableGenders: List<Gender> = emptyList(),
    val requiredAnatomicalTraits: List<AnatomicalTrait> = emptyList(),
) {
    fun allows(gender: Gender): Boolean = applicableGenders.isEmpty() || gender in applicableGenders

    @Deprecated("Use gender applicability")
    fun allows(anatomicalTraits: Set<AnatomicalTrait>): Boolean =
        requiredAnatomicalTraits.isEmpty() || anatomicalTraits.containsAll(requiredAnatomicalTraits)
}

@Serializable
enum class AnatomicalTrait {
    OVARIES,
    TESTES,
}

@Serializable
data class DiseaseSource(
    val id: String,
    val title: Map<String, String>,
    val url: String,
    val version: String = "",
    val accessedOn: String = "",
)

@Serializable
data class DiseaseCatalog(
    val schemaVersion: Int,
    val categories: List<DiseaseCategory>,
    val departments: List<CareDepartment>,
    val sources: List<DiseaseSource>,
    val diseases: List<Disease>,
)





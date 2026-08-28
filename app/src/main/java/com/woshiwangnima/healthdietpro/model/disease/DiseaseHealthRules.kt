package com.woshiwangnima.healthdietpro.model.disease

import android.content.Context

private val diabetesDiseaseIds = setOf(
    "type1_diabetes",
    "type2_diabetes",
    "gestational_diabetes",
)

/** The only shared rule for features that react to the user's diabetes risk. */
internal fun hasCurrentUserDiabetesRisk(context: Context): Boolean {
    val diseaseRepository = DiseaseRepository.fromContext(context)
    val diabetesReferences = diseaseRepository.loadAll()
        .filter { it.id in diabetesDiseaseIds }
        .flatMap { it.referenceIds() }
        .toSet()
    return UserDiseaseRecordRepository.fromContext(context).load().any { record ->
        record.subjectType == DiseaseSubjectType.SELF &&
            record.status in setOf(DiseaseStatus.ACTIVE, DiseaseStatus.ONGOING_RISK) &&
            record.disease.curatedId() in diabetesReferences
    }
}

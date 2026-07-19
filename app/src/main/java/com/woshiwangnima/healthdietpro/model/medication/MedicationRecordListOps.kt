package com.woshiwangnima.healthdietpro.model.medication

internal fun List<MedicationRecord>.removeRecordById(id: String): List<MedicationRecord> =
    filterNot { it.id == id }

package com.woshiwangnima.healthdietpro.model.disease

import android.content.Context

internal class UserDiseaseRecordRepository private constructor(context: Context) {
    private val archive = UserDiseaseArchiveStore.current(context)

    fun load(): List<UserDiseaseRecord> = archive.load().records
        .mapNotNull { record -> runCatching { record.validate() }.getOrNull()?.let { record } }
        .sortedByDescending(UserDiseaseRecord::updatedAt)

    fun save(records: List<UserDiseaseRecord>) {
        records.forEach(UserDiseaseRecord::validate)
        archive.update { it.copy(records = records.sortedByDescending(UserDiseaseRecord::updatedAt)) }
    }

    /** Restores the pre-union raw data once if a migration needs manual recovery. */
    fun restoreLatestReferenceBackup(): Boolean {
        return false
    }

    fun loadCustomDiseases(): List<UserCustomDisease> = archive.load().customDiseases
        .sortedByDescending(UserCustomDisease::updatedAt)

    fun saveCustomDiseases(diseases: List<UserCustomDisease>) {
        archive.update { it.copy(customDiseases = diseases.sortedByDescending(UserCustomDisease::updatedAt)) }
    }

    companion object {
        fun fromContext(context: Context) = UserDiseaseRecordRepository(context.applicationContext)
    }
}

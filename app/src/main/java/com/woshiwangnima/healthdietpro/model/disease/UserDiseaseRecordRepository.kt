package com.woshiwangnima.healthdietpro.model.disease

import android.content.Context
import com.woshiwangnima.healthdietpro.model.prefs.UserPrefs
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal class UserDiseaseRecordRepository private constructor(context: Context) {
    private val prefs = UserPrefs.current(context)
    private val json = Json { ignoreUnknownKeys = true }

    fun load(): List<UserDiseaseRecord> = runCatching {
        val raw = prefs.getString(KEY_RECORDS, "[]")
        val records = json.decodeFromString<List<UserDiseaseRecord>>(raw)
            .onEach(UserDiseaseRecord::validate)
            .sortedByDescending { it.updatedAt }
        val canonical = json.encodeToString(records)
        if (canonical != raw) {
            if (!prefs.contains(KEY_RECORDS_BACKUP)) prefs.putString(KEY_RECORDS_BACKUP, raw)
            prefs.putString(KEY_RECORDS, canonical)
        }
        records
    }.getOrDefault(emptyList())

    fun save(records: List<UserDiseaseRecord>) {
        records.forEach(UserDiseaseRecord::validate)
        prefs.putString(KEY_RECORDS, json.encodeToString(records.sortedByDescending { it.updatedAt }))
    }

    /** Restores the pre-union raw data once if a migration needs manual recovery. */
    fun restoreLatestReferenceBackup(): Boolean {
        if (!prefs.contains(KEY_RECORDS_BACKUP)) return false
        prefs.putString(KEY_RECORDS, prefs.getString(KEY_RECORDS_BACKUP, "[]"))
        prefs.remove(KEY_RECORDS_BACKUP)
        return true
    }

    fun loadCustomDiseases(): List<UserCustomDisease> = runCatching {
        json.decodeFromString<List<UserCustomDisease>>(prefs.getString(KEY_CUSTOM_DISEASES, "[]"))
            .sortedByDescending { it.updatedAt }
    }.getOrDefault(emptyList())

    fun saveCustomDiseases(diseases: List<UserCustomDisease>) {
        prefs.putString(KEY_CUSTOM_DISEASES, json.encodeToString(diseases.sortedByDescending { it.updatedAt }))
    }

    companion object {
        private const val KEY_RECORDS = "disease_records_v1"
        private const val KEY_RECORDS_BACKUP = "disease_records_v1_backup_before_reference_union"
        private const val KEY_CUSTOM_DISEASES = "disease_custom_items_v1"
        fun fromContext(context: Context) = UserDiseaseRecordRepository(context.applicationContext)
    }
}

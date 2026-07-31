package com.woshiwangnima.healthdietpro.model.disease

import android.content.Context
import com.woshiwangnima.healthdietpro.model.prefs.UserPrefs
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal class UserDiseaseRecordRepository private constructor(context: Context) {
    private val prefs = UserPrefs.current(context)
    private val json = Json { ignoreUnknownKeys = true }

    fun load(): List<UserDiseaseRecord> = runCatching {
        json.decodeFromString<List<UserDiseaseRecord>>(prefs.getString(KEY_RECORDS, "[]"))
            .onEach(UserDiseaseRecord::validate)
            .sortedByDescending { it.updatedAt }
    }.getOrDefault(emptyList())

    fun save(records: List<UserDiseaseRecord>) {
        records.forEach(UserDiseaseRecord::validate)
        prefs.putString(KEY_RECORDS, json.encodeToString(records.sortedByDescending { it.updatedAt }))
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
        private const val KEY_CUSTOM_DISEASES = "disease_custom_items_v1"
        fun fromContext(context: Context) = UserDiseaseRecordRepository(context.applicationContext)
    }
}

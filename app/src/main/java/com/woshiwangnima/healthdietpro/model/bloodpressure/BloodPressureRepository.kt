package com.woshiwangnima.healthdietpro.model.bloodpressure

import android.content.Context
import com.woshiwangnima.healthdietpro.model.prefs.UserPrefs
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal class BloodPressureRepository private constructor(context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    private val userPrefs = UserPrefs.current(context)

    fun load(): List<BloodPressureRecord> = runCatching {
        json.decodeFromString<List<BloodPressureRecord>>(userPrefs.getString(KEY_RECORDS, "[]"))
    }.getOrDefault(emptyList())

    fun save(records: List<BloodPressureRecord>) {
        userPrefs.putString(KEY_RECORDS, json.encodeToString(records.sortedByDescending { it.timestamp }))
    }

    companion object {
        private const val KEY_RECORDS = "blood_pressure_records_v1"
        fun fromContext(context: Context) = BloodPressureRepository(context.applicationContext)
    }
}

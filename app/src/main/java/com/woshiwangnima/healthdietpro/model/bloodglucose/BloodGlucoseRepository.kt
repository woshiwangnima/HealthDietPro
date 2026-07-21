package com.woshiwangnima.healthdietpro.model.bloodglucose

import android.content.Context
import com.woshiwangnima.healthdietpro.model.prefs.UserPrefs
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal class BloodGlucoseRepository private constructor(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    private val userPrefs = UserPrefs.current(context)

    fun load(): List<BloodGlucoseRecord> = runCatching {
        json.decodeFromString<List<BloodGlucoseRecord>>(userPrefs.getString(KEY_RECORDS, "[]"))
    }.getOrDefault(emptyList())

    fun save(records: List<BloodGlucoseRecord>) {
        userPrefs.putString(KEY_RECORDS, json.encodeToString(records.sortedByDescending { it.timestamp }))
    }

    companion object {
        private const val KEY_RECORDS = "blood_glucose_records_v1"

        fun fromContext(context: Context) = BloodGlucoseRepository(context.applicationContext)
    }
}

package com.woshiwangnima.healthdietpro.model.profile

import android.content.Context
import com.google.gson.Gson
import com.woshiwangnima.healthdietpro.model.unit.UnitCategory

object ProfilePrefs {
    private const val KEY_PROFILE = "user_profile"
    private const val PREFS_NAME = "health_diet_prefs"

    fun save(context: Context, profile: UserProfile) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PROFILE, Gson().toJson(profile)).apply()
    }

    fun load(context: Context): UserProfile {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_PROFILE, null)
        val savedProfile = if (json != null) {
            try {
                Gson().fromJson(json, UserProfile::class.java)
            } catch (_: Exception) {
                null
            }
        } else null
        val seed = createSeedProfile()
        return UserProfile(
            name = savedProfile?.name ?: seed.name,
            gender = savedProfile?.gender ?: seed.gender,
            birthday = savedProfile?.birthday ?: seed.birthday,
            province = savedProfile?.province ?: seed.province,
            diseaseIds = savedProfile?.diseaseIds ?: seed.diseaseIds,
            heightRecords = joinRecords(savedProfile?.heightRecords.orEmpty(), seed.heightRecords, false),
            weightRecords = joinRecords(savedProfile?.weightRecords.orEmpty(), seed.weightRecords, true)
        )
    }

    private fun joinRecords(saved: List<BodyRecord>, seed: List<BodyRecord>, isWeight: Boolean): List<BodyRecord> {
        val fixedSaved = saved.map { fixUnit(it, isWeight) }
        val savedDates = fixedSaved.map { it.date }.toSet()
        return (fixedSaved + seed.filter { it.date !in savedDates }).sortedBy { it.date }
    }

    private fun fixUnit(record: BodyRecord, isWeight: Boolean): BodyRecord {
        val u = record.unit
        if (u != null && u.isNotEmpty()) return record
        return record.copy(unit = if (isWeight) UnitCategory.DEFAULT_UNIT_WEIGHT else UnitCategory.DEFAULT_UNIT_LENGTH)
    }

    private fun createSeedProfile(): UserProfile {
        val now = java.time.LocalDate.now()
        val rng = java.util.Random(42)

        val weightRecords = (0 until 30).map { daysAgo ->
            val date = now.minusDays(daysAgo.toLong())
            val trend = daysAgo * 0.02f
            val noise = (rng.nextFloat() - 0.5f) * 1.6f
            val value = (67.5f - trend + noise).coerceIn(63f, 72f)
            BodyRecord(
                date = date.toString(),
                value = value,
                unit = "kg"
            )
        }

        val heightRecords = listOf(
            now.minusMonths(12) to 169.2f,
            now.minusMonths(10) to 169.5f,
            now.minusMonths(7) to 169.8f,
            now.minusMonths(4) to 170.1f,
            now.minusMonths(2) to 170.3f,
            now.minusMonths(0) to 170.5f
        ).map { (dateOffset, value) ->
            BodyRecord(
                date = dateOffset.toString(),
                value = value,
                unit = "cm"
            )
        }

        return UserProfile(
            weightRecords = weightRecords.sortedBy { it.date },
            heightRecords = heightRecords.sortedBy { it.date }
        )
    }

}

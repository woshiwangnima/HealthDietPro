package com.woshiwangnima.healthdietpro.model.profile

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.woshiwangnima.healthdietpro.model.unit.UnitCategory
import java.io.File

object ProfilePrefs {
    private const val PREFS_NAME = "health_diet_prefs"
    private const val KEY_LEGACY_PROFILE = "user_profile"
    private const val KEY_ALL_USERS = "all_users"
    private const val KEY_CURRENT_USER_ID = "current_user_id"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun ensureMigrated(context: Context) {
        val p = prefs(context)
        if (p.contains(KEY_ALL_USERS)) return
        val legacyJson = p.getString(KEY_LEGACY_PROFILE, null) ?: return
        try {
            val legacy: UserProfile = Gson().fromJson(legacyJson, UserProfile::class.java)
            val migrated = legacy.copy(id = "default")
            saveUserMap(context, listOf(migrated))
            setCurrentUserId(context, "default")
            p.edit().remove(KEY_LEGACY_PROFILE).apply()
        } catch (_: Exception) {}
    }

    private fun loadUserMap(context: Context): List<UserProfile> {
        ensureMigrated(context)
        val json = prefs(context).getString(KEY_ALL_USERS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<UserProfile>>() {}.type
            Gson().fromJson(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveUserMap(context: Context, users: List<UserProfile>) {
        prefs(context).edit().putString(KEY_ALL_USERS, Gson().toJson(users)).apply()
    }

    fun getAllUsers(context: Context): List<UserProfile> = loadUserMap(context)

    fun getCurrentUserId(context: Context): String {
        ensureMigrated(context)
        val id = prefs(context).getString(KEY_CURRENT_USER_ID, null)
        if (!id.isNullOrEmpty()) return id
        val users = loadUserMap(context)
        if (users.isNotEmpty()) {
            setCurrentUserId(context, users.first().id)
            return users.first().id
        }
        return ""
    }

    fun makeChartStateKey(context: Context, baseKey: String): String {
        val uid = getCurrentUserId(context)
        return if (uid.isNotEmpty()) "${baseKey}_${uid}" else baseKey
    }

    fun setCurrentUserId(context: Context, id: String) {
        prefs(context).edit().putString(KEY_CURRENT_USER_ID, id).apply()
    }

    fun getProfile(context: Context, userId: String): UserProfile? {
        return loadUserMap(context).find { it.id == userId }
    }

    fun save(context: Context, profile: UserProfile) {
        val withId = if (profile.id.isEmpty()) profile.copy(id = genId()) else profile
        val users = loadUserMap(context).toMutableList()
        val idx = users.indexOfFirst { it.id == withId.id }
        if (idx >= 0) users[idx] = withId else users.add(withId)
        saveUserMap(context, users)
        setCurrentUserId(context, withId.id)
    }

    fun load(context: Context): UserProfile {
        val current = getProfile(context, getCurrentUserId(context))
        val seed = createSeedProfile()
        return UserProfile(
            id = current?.id ?: "",
            name = current?.name ?: seed.name,
            gender = current?.gender ?: seed.gender,
            birthday = current?.birthday ?: seed.birthday,
            province = current?.province ?: seed.province,
            diseaseIds = current?.diseaseIds ?: seed.diseaseIds,
            heightRecords = joinRecords(current?.heightRecords.orEmpty(), seed.heightRecords, false),
            weightRecords = joinRecords(current?.weightRecords.orEmpty(), seed.weightRecords, true),
            // avatarFileName lives on the persisted user record; surface it here so
            // ProfileFragment / ProfileEditActivity can read the saved avatar back after
            // restart (otherwise it always came back empty).
            avatarFileName = current?.avatarFileName ?: ""
        )
    }

    fun deleteUser(context: Context, id: String) {
        // Clean up per-user data before removing from list
        val user = getProfile(context, id)
        val users = loadUserMap(context).filter { it.id != id }
        saveUserMap(context, users)
        if (getCurrentUserId(context) == id) {
            val next = users.firstOrNull()
            setCurrentUserId(context, next?.id ?: "")
        }
        if (user != null) {
            cleanupPerUserData(context, user)
        }
    }

    fun cleanupPerUserData(context: Context, user: UserProfile) {
        // Delete avatar file
        if (user.avatarFileName.isNotEmpty()) {
            File(context.filesDir, "avatars/${user.avatarFileName}").delete()
        }
        // Delete per-user chart preferences (keys ending with _${userId})
        val appPrefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val editor = appPrefs.edit()
        val suffix = "_${user.id}"
        for (key in appPrefs.all.keys) {
            if (key.endsWith(suffix)) {
                editor.remove(key)
            }
        }
        editor.apply()
    }

    fun createDefaultIfEmpty(context: Context): UserProfile {
        val users = loadUserMap(context)
        if (users.isNotEmpty()) return users.first()
        val profile = load(context).copy(id = genId())
        save(context, profile)
        return profile
    }

    private fun genId(): String = (System.currentTimeMillis() xor Math.random().toLong().and(0xFFFF)).toString()

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

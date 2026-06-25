package com.woshiwangnima.healthdietpro.model.profile

import android.content.Context
import com.google.gson.Gson

object ProfilePrefs {
    private const val KEY_PROFILE = "user_profile"
    private const val PREFS_NAME = "health_diet_prefs"

    fun save(context: Context, profile: UserProfile) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PROFILE, Gson().toJson(profile)).apply()
    }

    fun load(context: Context): UserProfile {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_PROFILE, null) ?: return UserProfile()
        return try {
            Gson().fromJson(json, UserProfile::class.java) ?: UserProfile()
        } catch (_: Exception) {
            UserProfile()
        }
    }

}

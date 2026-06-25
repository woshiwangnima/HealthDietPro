package com.woshiwangnima.healthdietpro.model.prefs

import android.content.Context

object AppPrefs {
    private const val PREFS_NAME = "app_prefs"
    private const val KEY_HEIGHT_UNIT = "pref_height_unit"
    private const val KEY_WEIGHT_UNIT = "pref_weight_unit"
    private const val KEY_ENERGY_UNIT = "pref_energy_unit"
    private const val KEY_FIRST_DAY = "pref_first_day_of_week"
    private const val KEY_DARK_MODE = "pref_dark_mode"
    private const val KEY_FIRST_LAUNCH = "is_first_launch"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getHeightUnit(context: Context): String =
        prefs(context).getString(KEY_HEIGHT_UNIT, "cm") ?: "cm"
    fun setHeightUnit(context: Context, id: String) =
        prefs(context).edit().putString(KEY_HEIGHT_UNIT, id).apply()

    fun getWeightUnit(context: Context): String =
        prefs(context).getString(KEY_WEIGHT_UNIT, "kg") ?: "kg"
    fun setWeightUnit(context: Context, id: String) =
        prefs(context).edit().putString(KEY_WEIGHT_UNIT, id).apply()

    fun getEnergyUnit(context: Context): String =
        prefs(context).getString(KEY_ENERGY_UNIT, "kcal") ?: "kcal"
    fun setEnergyUnit(context: Context, id: String) =
        prefs(context).edit().putString(KEY_ENERGY_UNIT, id).apply()

    fun getFirstDayOfWeek(context: Context): String =
        prefs(context).getString(KEY_FIRST_DAY, "MONDAY") ?: "MONDAY"
    fun setFirstDayOfWeek(context: Context, day: String) =
        prefs(context).edit().putString(KEY_FIRST_DAY, day).apply()

    fun getDarkMode(context: Context): String =
        prefs(context).getString(KEY_DARK_MODE, "FOLLOW_SYSTEM") ?: "FOLLOW_SYSTEM"
    fun setDarkMode(context: Context, mode: String) =
        prefs(context).edit().putString(KEY_DARK_MODE, mode).apply()

    fun isFirstLaunch(context: Context): Boolean =
        prefs(context).getBoolean(KEY_FIRST_LAUNCH, true)
    fun markFirstLaunchComplete(context: Context) =
        prefs(context).edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
}

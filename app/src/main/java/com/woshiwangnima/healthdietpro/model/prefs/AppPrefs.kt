package com.woshiwangnima.healthdietpro.model.prefs

import android.content.Context

object AppPrefs {
    private const val PREFS_NAME = "app_prefs"
    private const val KEY_FIRST_DAY = "pref_first_day_of_week"
    private const val KEY_DARK_MODE = "pref_dark_mode"
    private const val KEY_FIRST_LAUNCH = "is_first_launch"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getUnit(context: Context, categoryId: String, defaultUnitId: String): String =
        prefs(context).getString("pref_unit_$categoryId", defaultUnitId) ?: defaultUnitId
    fun setUnit(context: Context, categoryId: String, unitId: String) =
        prefs(context).edit().putString("pref_unit_$categoryId", unitId).apply()

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

    fun getReminderDrinkWater(context: Context): Boolean =
        prefs(context).getBoolean("reminder_drink_water", false)
    fun setReminderDrinkWater(context: Context, enabled: Boolean) =
        prefs(context).edit().putBoolean("reminder_drink_water", enabled).apply()

    fun getReminderMedication(context: Context): Boolean =
        prefs(context).getBoolean("reminder_medication", false)
    fun setReminderMedication(context: Context, enabled: Boolean) =
        prefs(context).edit().putBoolean("reminder_medication", enabled).apply()

    fun getReminderPeriod(context: Context): Boolean =
        prefs(context).getBoolean("reminder_period", false)
    fun setReminderPeriod(context: Context, enabled: Boolean) =
        prefs(context).edit().putBoolean("reminder_period", enabled).apply()

    fun getReminderFasting(context: Context): Boolean =
        prefs(context).getBoolean("reminder_fasting", false)
    fun setReminderFasting(context: Context, enabled: Boolean) =
        prefs(context).edit().putBoolean("reminder_fasting", enabled).apply()
}

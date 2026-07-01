package com.woshiwangnima.healthdietpro.model.prefs

import android.content.Context

/**
 * 应用偏好设置入口。
 *
 * 用户级设置（提醒开关、字体大小、文字溢出、单位、图表操作状态等）委托
 * [UserPrefs.current]，按当前登录用户存储在独立文件 `user_prefs_<uid>`。
 * 真正 app 级的键（首次启动标记、字体风格令牌）直接读写 `app_prefs`。
 * 现有调用方签名保持不变，透明获得按用户存储能力。
 */
object AppPrefs {
    private const val PREFS_NAME = "app_prefs"
    private const val KEY_FIRST_LAUNCH = "is_first_launch"

    // ---- app 级 ----

    private fun appPrefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isFirstLaunch(context: Context): Boolean =
        appPrefs(context).getBoolean(KEY_FIRST_LAUNCH, true)
    fun markFirstLaunchComplete(context: Context) =
        appPrefs(context).edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()

    // ---- 字体风格令牌（app 级，详见 spec §5）----

    private const val KEY_FONTSTYLE_ALPHA_LOW = "fontstyle_alpha_low"
    private const val KEY_FONTSTYLE_ALPHA_MID = "fontstyle_alpha_mid"
    private const val KEY_FONTSTYLE_ALPHA_HIGH = "fontstyle_alpha_high"
    private const val KEY_FONTSTYLE_TOK_UNDERLINE = "fontstyle_tok_underline"
    private const val KEY_FONTSTYLE_TOK_STRIKE = "fontstyle_tok_strike"
    private const val KEY_FONTSTYLE_TOK_ITALIC = "fontstyle_tok_italic"

    const val FONTSTYLE_ALPHA_LOW_DEFAULT = 0.70f
    const val FONTSTYLE_ALPHA_MID_DEFAULT = 0.50f
    const val FONTSTYLE_ALPHA_HIGH_DEFAULT = 0.30f

    fun getFontStyleTokenAlphaLow(context: Context): Float =
        appPrefs(context).getFloat(KEY_FONTSTYLE_ALPHA_LOW, FONTSTYLE_ALPHA_LOW_DEFAULT)
    fun getFontStyleTokenAlphaMid(context: Context): Float =
        appPrefs(context).getFloat(KEY_FONTSTYLE_ALPHA_MID, FONTSTYLE_ALPHA_MID_DEFAULT)
    fun getFontStyleTokenAlphaHigh(context: Context): Float =
        appPrefs(context).getFloat(KEY_FONTSTYLE_ALPHA_HIGH, FONTSTYLE_ALPHA_HIGH_DEFAULT)

    fun getFontStyleTokenUnderline(context: Context): Boolean =
        appPrefs(context).getBoolean(KEY_FONTSTYLE_TOK_UNDERLINE, true)
    fun getFontStyleTokenStrike(context: Context): Boolean =
        appPrefs(context).getBoolean(KEY_FONTSTYLE_TOK_STRIKE, true)
    fun getFontStyleTokenItalic(context: Context): Boolean =
        appPrefs(context).getBoolean(KEY_FONTSTYLE_TOK_ITALIC, true)

    /** 幂等写入字体风格令牌默认值（键缺失时才写）。 */
    fun ensureFontStyleTokenDefaults(context: Context) {
        val p = appPrefs(context)
        val e = p.edit()
        if (!p.contains(KEY_FONTSTYLE_ALPHA_LOW)) e.putFloat(KEY_FONTSTYLE_ALPHA_LOW, FONTSTYLE_ALPHA_LOW_DEFAULT)
        if (!p.contains(KEY_FONTSTYLE_ALPHA_MID)) e.putFloat(KEY_FONTSTYLE_ALPHA_MID, FONTSTYLE_ALPHA_MID_DEFAULT)
        if (!p.contains(KEY_FONTSTYLE_ALPHA_HIGH)) e.putFloat(KEY_FONTSTYLE_ALPHA_HIGH, FONTSTYLE_ALPHA_HIGH_DEFAULT)
        if (!p.contains(KEY_FONTSTYLE_TOK_UNDERLINE)) e.putBoolean(KEY_FONTSTYLE_TOK_UNDERLINE, true)
        if (!p.contains(KEY_FONTSTYLE_TOK_STRIKE)) e.putBoolean(KEY_FONTSTYLE_TOK_STRIKE, true)
        if (!p.contains(KEY_FONTSTYLE_TOK_ITALIC)) e.putBoolean(KEY_FONTSTYLE_TOK_ITALIC, true)
        e.apply()
    }

    // ---- 用户级（委托 UserPrefs.current）----

    private fun user(context: Context) = UserPrefs.current(context)

    fun getUnit(context: Context, categoryId: String, defaultUnitId: String): String =
        user(context).getString("pref_unit_$categoryId", defaultUnitId)
    fun setUnit(context: Context, categoryId: String, unitId: String) =
        user(context).putString("pref_unit_$categoryId", unitId)

    fun getFirstDayOfWeek(context: Context): String =
        user(context).getString("pref_first_day_of_week", "MONDAY")
    fun setFirstDayOfWeek(context: Context, day: String) =
        user(context).putString("pref_first_day_of_week", day)

    fun getDarkMode(context: Context): String =
        user(context).getString("pref_dark_mode", "FOLLOW_SYSTEM")
    fun setDarkMode(context: Context, mode: String) =
        user(context).putString("pref_dark_mode", mode)

    fun getReminderDrinkWater(context: Context): Boolean =
        user(context).getBoolean("reminder_drink_water", false)
    fun setReminderDrinkWater(context: Context, enabled: Boolean) =
        user(context).putBoolean("reminder_drink_water", enabled)

    fun getReminderMedication(context: Context): Boolean =
        user(context).getBoolean("reminder_medication", false)
    fun setReminderMedication(context: Context, enabled: Boolean) =
        user(context).putBoolean("reminder_medication", enabled)

    fun getReminderPeriod(context: Context): Boolean =
        user(context).getBoolean("reminder_period", false)
    fun setReminderPeriod(context: Context, enabled: Boolean) =
        user(context).putBoolean("reminder_period", enabled)

    fun getReminderFasting(context: Context): Boolean =
        user(context).getBoolean("reminder_fasting", false)
    fun setReminderFasting(context: Context, enabled: Boolean) =
        user(context).putBoolean("reminder_fasting", enabled)

    fun getWeightChartTab(context: Context): Int =
        user(context).getInt("tab_weight_chart", 0)
    fun setWeightChartTab(context: Context, tab: Int) =
        user(context).putInt("tab_weight_chart", tab)

    fun getHeightChartTab(context: Context): Int =
        user(context).getInt("tab_height_chart", 0)
    fun setHeightChartTab(context: Context, tab: Int) =
        user(context).putInt("tab_height_chart", tab)

    fun getFontScale(context: Context): Float =
        user(context).getFloat("pref_font_scale", 1.0f)
    fun setFontScale(context: Context, scale: Float) =
        user(context).putFloat("pref_font_scale", scale)

    fun getChartStyle(context: Context, key: String): Int =
        user(context).getInt("chart_style_$key", 0)
    fun setChartStyle(context: Context, key: String, pos: Int) =
        user(context).putInt("chart_style_$key", pos)

    fun getChartTimeRange(context: Context, key: String): Long =
        user(context).getLong("chart_timerange_$key", Long.MAX_VALUE)
    fun setChartTimeRange(context: Context, key: String, range: Long) =
        user(context).putLong("chart_timerange_$key", range)

    fun getChartYMin(context: Context, key: String): Float =
        user(context).getFloat("chart_ymin_$key", 0f)
    fun setChartYMin(context: Context, key: String, pct: Float) =
        user(context).putFloat("chart_ymin_$key", pct)

    fun getChartYMax(context: Context, key: String): Float =
        user(context).getFloat("chart_ymax_$key", 100f)
    fun setChartYMax(context: Context, key: String, pct: Float) =
        user(context).putFloat("chart_ymax_$key", pct)

    fun getChartLabelInterval(context: Context, key: String): Long =
        user(context).getLong("chart_label_$key", 0L)
    fun setChartLabelInterval(context: Context, key: String, ms: Long) =
        user(context).putLong("chart_label_$key", ms)

    fun getBmiChartTab(context: Context): Int =
        user(context).getInt("tab_bmi_chart", 0)
    fun setBmiChartTab(context: Context, tab: Int) =
        user(context).putInt("tab_bmi_chart", tab)

    fun getTextOverflowMode(context: Context): String =
        user(context).getString("pref_text_overflow", "shrink")
    fun setTextOverflowMode(context: Context, mode: String) =
        user(context).putString("pref_text_overflow", mode)

    fun getMarqueeSpeed(context: Context): Int =
        user(context).getInt("pref_marquee_speed", 200)
    fun setMarqueeSpeed(context: Context, speed: Int) =
        user(context).putInt("pref_marquee_speed", speed)
}
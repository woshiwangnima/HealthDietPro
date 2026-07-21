package com.woshiwangnima.healthdietpro.model.prefs

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 应用偏好设置入口（app 存档模块，DESIGN §3.7.1）。
 *
 * **存储归属**：
 * - **app 级**（`app_prefs` 文件）：首次启动标记、字体风格令牌（alpha/token）。
 * - **用户级**（委托 [UserPrefs.current]，`user_prefs_<uid>` 文件）：单位选择、
 *   首日、深色、字号、文字溢出、轮播速度、图表状态键、Tab 选中、提醒开关。
 *   按当前登录用户隔离，删用户时随文件清理。
 *
 * **契约**：`getChart*(ctx, chartStateKey)` / `setChart*(...)` 涵盖
 * `chartStyle / chartTimeRange / chartLabelInterval / chartYMin / chartYMax` 五组键值。
 * `chartStateKey` 由 [com.woshiwangnima.healthdietpro.model.profile.ProfilePrefs.makeChartStateKey]
 * 构造 `_<userId>` 后缀。删除用户时 [ProfilePrefs.deleteUser] 调用清理所有以 `_<uid>` 结尾的键。
 *
 * **不变量**：用户级键的存储归属后续偏好设置重评估时可能迁回 app 级（DESIGN §10 待定）。
 */
object AppPrefs {
    private const val PREFS_NAME = "app_prefs"
    private const val KEY_FIRST_LAUNCH = "is_first_launch"
    private const val KEY_FONT_SCALE = "pref_font_scale"
    private const val KEY_DARK_MODE = "pref_dark_mode"
    private const val KEY_FIRST_DAY_OF_WEEK = "pref_first_day_of_week"
    private const val KEY_TEXT_OVERFLOW = "pref_text_overflow"
    private const val KEY_MARQUEE_SPEED = "pref_marquee_speed"
    private const val KEY_APP_LANGUAGE = "pref_app_language"
    private val darkModeState = MutableStateFlow("FOLLOW_SYSTEM")
    val darkMode = darkModeState.asStateFlow()

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
        appPrefs(context).getString(KEY_FIRST_DAY_OF_WEEK, "MONDAY") ?: "MONDAY"
    fun setFirstDayOfWeek(context: Context, day: String) =
        appPrefs(context).edit().putString(KEY_FIRST_DAY_OF_WEEK, day).apply()

    fun getDarkMode(context: Context): String =
        appPrefs(context).getString(KEY_DARK_MODE, "FOLLOW_SYSTEM") ?: "FOLLOW_SYSTEM"
    fun loadDarkMode(context: Context) { darkModeState.value = getDarkMode(context) }
    fun setDarkMode(context: Context, mode: String) {
        appPrefs(context).edit().putString(KEY_DARK_MODE, mode).apply()
        darkModeState.value = mode
    }

    fun getAppLanguage(context: Context): String =
        appPrefs(context).getString(KEY_APP_LANGUAGE, "SYSTEM") ?: "SYSTEM"

    fun setAppLanguage(context: Context, language: String) =
        appPrefs(context).edit().putString(KEY_APP_LANGUAGE, language).apply()

    fun getTextOverflowMode(context: Context): String =
        appPrefs(context).getString(KEY_TEXT_OVERFLOW, "ellipsis") ?: "ellipsis"
    fun setTextOverflowMode(context: Context, mode: String) =
        appPrefs(context).edit().putString(KEY_TEXT_OVERFLOW, mode).apply()
    fun getMarqueeSpeed(context: Context): Int = appPrefs(context).getInt(KEY_MARQUEE_SPEED, 200)
    fun setMarqueeSpeed(context: Context, speed: Int) =
        appPrefs(context).edit().putInt(KEY_MARQUEE_SPEED, speed.coerceIn(50, 2000)).apply()

    fun getReminderDrinkWater(context: Context): Boolean =
        user(context).getBoolean("reminder_drink_water", false)
    fun setReminderDrinkWater(context: Context, enabled: Boolean) =
        user(context).putBoolean("reminder_drink_water", enabled)

    fun getReminderMedication(context: Context): Boolean =
        user(context).getBoolean("reminder_medication", true)
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

    fun getFontScale(context: Context): Float {
        val app = appPrefs(context)
        if (app.contains(KEY_FONT_SCALE)) return app.getFloat(KEY_FONT_SCALE, 1.0f)
        val legacy = user(context).getFloat(KEY_FONT_SCALE, 1.0f)
        app.edit().putFloat(KEY_FONT_SCALE, legacy).apply()
        return legacy
    }
    fun setFontScale(context: Context, scale: Float) =
        appPrefs(context).edit().putFloat(KEY_FONT_SCALE, scale).apply()

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

    fun getMedicationTab(context: Context): Int =
        user(context).getInt("tab_medication", 0)
    fun setMedicationTab(context: Context, tab: Int) =
        user(context).putInt("tab_medication", tab)

}

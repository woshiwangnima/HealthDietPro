package com.woshiwangnima.healthdietpro.ui.widget.tab

import android.content.Context

/**
 * Tab 选中状态持久化（app 存档模块，DESIGN §3.7.1）。
 *
 * **存储归属**：`app_prefs` 文件，int-set 序列化为 StringSet。
 * **位置注**：当前物理位置在 `ui/widget/tab/`（基础设施模块），
 * 但职责属存档模块。规范化时迁至 `archive/app/`（DESIGN §3 目标分组）。
 * **契约**：键名 `tab_${screenId}_level_${level}`，后续接入多层 Tag 替代字符串拼装。
 */
object TabPersistence {
    private const val PREFS_NAME = "app_prefs"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveIndex(context: Context, key: String, index: Int) {
        prefs(context).edit().putInt(key, index).apply()
    }

    fun loadIndex(context: Context, key: String, default: Int = -1): Int =
        prefs(context).getInt(key, default)

    fun saveSet(context: Context, key: String, set: Set<Int>) {
        val stringSet = set.map { it.toString() }.toSet()
        prefs(context).edit().putStringSet(key, stringSet).apply()
    }

    fun loadSet(context: Context, key: String): Set<Int> =
        prefs(context).getStringSet(key, emptySet())
            ?.mapNotNull { it.toIntOrNull() }
            ?.toSet()
            ?: emptySet()
}
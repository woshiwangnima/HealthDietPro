package com.woshiwangnima.healthdietpro.ui.widget.tab

import android.content.Context

class MultiLevelTabCoordinator(
    private val context: Context,
    private val screenId: String
) {
    var onLevelChanged: ((level: Int, selection: Any) -> Unit)? = null

    fun registerLevel(level: Int, bar: ToggleBar, defaultIndex: Int = 0) {
        val key = "tab_${screenId}_level_$level"
        bar.bindPersistence(key)
        bar.restore(key, defaultIndex)
        bar.listener = { idx, _ -> onLevelChanged?.invoke(level, idx) }
    }

    fun registerFilter(level: Int, bar: FilterBar, defaultSet: Set<Int> = emptySet()) {
        val key = "tab_${screenId}_level_$level"
        bar.bindPersistence(key)
        bar.restore(key, defaultSet)
        bar.listener = { set -> onLevelChanged?.invoke(level, set) }
    }
}
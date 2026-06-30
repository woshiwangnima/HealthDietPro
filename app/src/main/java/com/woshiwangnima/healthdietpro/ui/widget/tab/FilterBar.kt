package com.woshiwangnima.healthdietpro.ui.widget.tab

import android.content.Context
import android.util.AttributeSet

class FilterBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : TabBar(context, attrs, defStyleAttr) {

    private val _selected = mutableSetOf<Int>()
    val selected: Set<Int> get() = _selected.toSet()

    var listener: ((selected: Set<Int>) -> Unit)? = null

    private var persistKey: String? = null

    init {
        // Filters can have many items; default to a scrollable threshold so
        // long filter lists degrade gracefully instead of over-squeezing tabs.
        if (maxVisible == Int.MAX_VALUE) maxVisible = 4
    }

    override fun toggle(index: Int) {
        if (index !in items.indices) return
        if (_selected.contains(index)) _selected.remove(index) else _selected.add(index)
        notifySelectionChanged()
        persistKey?.let { TabPersistence.saveSet(context, it, _selected) }
        listener?.invoke(selected)
    }

    override fun isSelected(index: Int): Boolean = _selected.contains(index)

    fun setSelected(set: Set<Int>) {
        _selected.clear()
        _selected.addAll(set.filter { it in items.indices })
        notifySelectionChanged()
        persistKey?.let { TabPersistence.saveSet(context, it, _selected) }
    }

    fun bindPersistence(key: String) {
        persistKey = key
    }

    fun restore(key: String, default: Set<Int> = emptySet()): Set<Int> {
        bindPersistence(key)
        val loaded = TabPersistence.loadSet(context, key)
        val target = if (loaded.isEmpty()) default else loaded
        _selected.clear()
        _selected.addAll(target.filter { it in items.indices })
        notifySelectionChanged()
        persistKey?.let { TabPersistence.saveSet(context, it, _selected) }
        return selected
    }
}
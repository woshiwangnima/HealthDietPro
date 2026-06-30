package com.woshiwangnima.healthdietpro.ui.widget.tab

import android.content.Context
import android.util.AttributeSet

class ToggleBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : TabBar(context, attrs, defStyleAttr) {

    var selectedIndex: Int = -1
        private set

    var listener: ((index: Int, prev: Int) -> Unit)? = null

    private var persistKey: String? = null

    init {
        // Fluid sliding indicator is the default selection animation for toggle bars.
        // It only activates for horizontal + non-scrollable bars (see TabBar.rebuild);
        // vertical / scrollable bars fall back to the per-tab animator.
        indicator = SlidingIndicator()
    }

    fun select(index: Int) {
        if (index == selectedIndex) return
        if (index !in items.indices) return
        val prev = selectedIndex
        selectedIndex = index
        notifySelectionChanged()
        persistKey?.let { TabPersistence.saveIndex(context, it, index) }
        listener?.invoke(index, prev)
    }

    override fun isSelected(index: Int): Boolean = index == selectedIndex

    override fun toggle(index: Int) = select(index)

    fun bindPersistence(key: String) {
        persistKey = key
    }

    fun restore(key: String, default: Int = 0): Int {
        bindPersistence(key)
        val idx = TabPersistence.loadIndex(context, key, default)
        val target = if (idx in items.indices) idx else default
        // Silent restore: set selection without firing the listener so the
        // activity controls initial content rendering independently.
        selectedIndex = target
        notifySelectionChanged()
        persistKey?.let { TabPersistence.saveIndex(context, it, target) }
        return target
    }
}
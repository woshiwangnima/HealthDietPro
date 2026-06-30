package com.woshiwangnima.healthdietpro.ui.widget.tab

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import com.woshiwangnima.healthdietpro.R

abstract class TabBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    enum class DisplayMode { NORMAL, CENTER_HIGHLIGHT }

    val items: MutableList<TabItem> = mutableListOf()
    val itemViews: MutableList<TabItemView> = mutableListOf()
    lateinit var strip: LinearLayout
        private set
    var scrollView: ViewGroup? = null
        private set

    var binder: TabBinder = DefaultTabBinder
        set(value) {
            field = value
            renderSelection()
        }

    var displayMode: DisplayMode = DisplayMode.NORMAL
        set(value) {
            field = value
            rebuild()
        }

    var maxVisible: Int = Int.MAX_VALUE
        set(value) {
            field = value
            rebuild()
        }

    var orientation: Int = LinearLayout.HORIZONTAL
        set(value) {
            field = value
            rebuild()
        }

    init {
        strip = LinearLayout(context)
        strip.orientation = LinearLayout.HORIZONTAL

        val a = context.obtainStyledAttributes(attrs, R.styleable.TabBar, defStyleAttr, 0)
        try {
            displayMode = when (a.getInt(R.styleable.TabBar_displayMode, 0)) {
                1 -> DisplayMode.CENTER_HIGHLIGHT
                else -> DisplayMode.NORMAL
            }
            maxVisible = a.getInt(R.styleable.TabBar_maxVisible, Int.MAX_VALUE)
        } finally {
            a.recycle()
        }
    }

    fun setTabs(items: List<TabItem>) {
        this.items.clear()
        this.items.addAll(items)
        rebuild()
    }

    private fun scrollable(): Boolean = items.size > maxVisible

    private fun rebuild() {
        strip.removeAllViews()
        itemViews.clear()

        val horizontal = orientation == LinearLayout.HORIZONTAL
        val scroll = scrollable()

        for ((index, item) in items.withIndex()) {
            val view = TabItemView(context)
            itemViews.add(view)
            view.setOnClickListener { toggle(index) }
            val lp = LinearLayout.LayoutParams(
                if (horizontal) {
                    if (scroll) LinearLayout.LayoutParams.WRAP_CONTENT
                    else 0
                } else LinearLayout.LayoutParams.MATCH_PARENT,
                if (horizontal) LinearLayout.LayoutParams.MATCH_PARENT
                else {
                    if (scroll) LinearLayout.LayoutParams.WRAP_CONTENT
                    else 0
                }
            )
            if (!scroll) lp.weight = 1f
            strip.addView(view, lp)
        }

        strip.orientation = orientation

        if (scroll) {
            scrollView = if (horizontal) HorizontalScrollView(context) else ScrollView(context)
            scrollView!!.removeAllViews()
            scrollView!!.addView(
                strip,
                LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            )
        } else {
            scrollView = null
        }

        removeAllViews()
        addView(
            scrollView ?: strip,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )

        renderSelection()
    }

    protected fun renderSelection() {
        val density = resources.displayMetrics.density
        for (i in items.indices) {
            val view = itemViews[i]
            binder.bind(items[i], view, isSelected(i), isCenter(i))
            if (isCenter(i)) {
                view.elevation = 4f * density
                view.scaleX = 1.2f
                view.scaleY = 1.2f
                // Lift the center tab up. For vertical orientation translationX would be more
                // appropriate, but translationY is fine for the common bottom-bar case.
                view.translationY = -6f * density
            } else {
                view.elevation = 0f
                view.scaleX = 1f
                view.scaleY = 1f
                view.translationY = 0f
            }
        }
    }

    protected fun isCenter(index: Int): Boolean =
        displayMode == DisplayMode.CENTER_HIGHLIGHT &&
            items.size % 2 == 1 &&
            index == items.size / 2

    protected abstract fun isSelected(index: Int): Boolean

    protected abstract fun toggle(index: Int)

    protected fun notifySelectionChanged() {
        renderSelection()
    }
}
package com.woshiwangnima.healthdietpro.ui.widget.tab

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.core.content.ContextCompat
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

    var animator: TabAnimator? = DefaultTabAnimator
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

    /** Pluggable selection indicator (e.g. [SlidingIndicator]). When non-null and the bar is
     *  horizontal + non-scrollable, an indicator view is hosted behind the strip and this
     *  object drives its animation. null = no overlay indicator (per-tab animator only). */
    var indicator: TabIndicator? = null
        set(value) {
            field = value
            rebuild()
        }

    private var indicatorView: View? = null

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
        // Detach reusable children from any prior parent before re-parenting (a previous
        // rebuild may have placed the strip inside a ScrollView that we're about to discard).
        strip.parent?.let { (it as ViewGroup).removeView(strip) }
        indicatorView?.parent?.let { (it as ViewGroup).removeView(indicatorView!!) }
        removeAllViews()

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
                    if (scroll) LinearLayout.LayoutParams.WRAP_CONTENT else 0
                } else LinearLayout.LayoutParams.MATCH_PARENT,
                if (horizontal) LinearLayout.LayoutParams.MATCH_PARENT
                else {
                    if (scroll) LinearLayout.LayoutParams.WRAP_CONTENT else 0
                }
            )
            if (!scroll) lp.weight = 1f
            strip.addView(view, lp)
        }

        strip.orientation = orientation

        if (scroll) {
            scrollView = if (horizontal) HorizontalScrollView(context) else ScrollView(context)
            // The strip must wrap on the scroll axis so it can exceed the viewport and scroll.
            val stripLp = if (horizontal) {
                LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT)
            } else {
                LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            }
            scrollView!!.addView(strip, stripLp)
        } else {
            scrollView = null
        }

        // Host an indicator view behind the strip when an indicator is set and the layout
        // is horizontal + non-scrollable (the sliding-pill math relies on equal-weight tabs).
        indicatorView = if (indicator != null && horizontal && !scroll && items.isNotEmpty()) {
            (indicatorView ?: View(context).apply {
                background = ContextCompat.getDrawable(context, R.drawable.tab_pill_selected)
            }).also {
                it.alpha = 0f
                it.visibility = VISIBLE
                indicator?.onReset()
            }
        } else null

        // Strip/scrollView: fill the primary axis, wrap on the cross axis, center within the bar.
        val rootLp = LayoutParams(
            if (horizontal) LayoutParams.MATCH_PARENT else LayoutParams.WRAP_CONTENT,
            if (horizontal) LayoutParams.WRAP_CONTENT else LayoutParams.MATCH_PARENT
        )
        rootLp.gravity = if (horizontal) Gravity.CENTER_VERTICAL else Gravity.CENTER_HORIZONTAL

        if (indicatorView != null) {
            addView(indicatorView!!, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        }
        addView(scrollView ?: strip, rootLp)

        // Let the center-highlight tab's elevation + scale + translation render outside the
        // strip's bounds instead of being clipped at the edges.
        strip.clipChildren = false
        strip.clipToPadding = false
        scrollView?.clipChildren = false
        scrollView?.clipToPadding = false
        if (displayMode == DisplayMode.CENTER_HIGHLIGHT) {
            clipChildren = false
            clipToPadding = false
        }

        renderSelection()
    }

    protected fun renderSelection() {
        for (i in items.indices) {
            val view = itemViews[i]
            binder.bind(items[i], view, isSelected(i), isCenter(i))
            // The indicator is the sole selected highlight; clear the per-tab background so
            // the pill doesn't draw twice on top of the indicator.
            if (indicatorView != null && !isCenter(i)) {
                view.background = null
            }
            animator?.animate(view, isSelected(i), isCenter(i))
        }
        val current = items.indices.firstOrNull { isSelected(it) } ?: -1
        indicatorView?.let { ind -> indicator?.onSelectionChanged(ind, this, current) }
    }

    fun isCenter(index: Int): Boolean =
        displayMode == DisplayMode.CENTER_HIGHLIGHT &&
            items.size % 2 == 1 &&
            index == items.size / 2

    protected abstract fun isSelected(index: Int): Boolean

    protected abstract fun toggle(index: Int)

    protected fun notifySelectionChanged() {
        renderSelection()
    }
}

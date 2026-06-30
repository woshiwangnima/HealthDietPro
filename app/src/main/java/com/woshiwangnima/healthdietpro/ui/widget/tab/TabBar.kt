package com.woshiwangnima.healthdietpro.ui.widget.tab

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
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

    var useSlidingIndicator: Boolean = false
        set(value) {
            field = value
            rebuild()
        }

    private var indicatorView: View? = null
    private var previousSelectedIndex: Int = -1
    private var indicatorVisible: Boolean = false

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

        // Sliding indicator lives behind the strip; only for horizontal non-scrollable bars.
        indicatorView = if (useSlidingIndicator && horizontal && !scroll && items.isNotEmpty()) {
            (indicatorView ?: View(context).apply {
                background = ContextCompat.getDrawable(context, R.drawable.tab_pill_selected)
            }).also {
                it.alpha = 0f
                it.visibility = VISIBLE
            }
        } else null

        removeAllViews()
        if (indicatorView != null) {
            addView(indicatorView!!, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        }
        addView(
            scrollView ?: strip,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        )

        // Let the center-highlight tab's elevation + scale + translation render
        // outside the strip's bounds instead of being clipped at the edges.
        strip.clipChildren = false
        strip.clipToPadding = false
        scrollView?.clipChildren = false
        scrollView?.clipToPadding = false
        if (displayMode == DisplayMode.CENTER_HIGHLIGHT) {
            clipChildren = false
            clipToPadding = false
        }

        previousSelectedIndex = -1
        indicatorVisible = false
        renderSelection()
    }

    protected fun renderSelection() {
        for (i in items.indices) {
            val view = itemViews[i]
            binder.bind(items[i], view, isSelected(i), isCenter(i))
            // The sliding indicator is the sole selected highlight; clear the per-tab
            // background so the pill doesn't draw twice on top of the indicator.
            if (indicatorView != null && !isCenter(i)) {
                view.background = null
            }
            animator?.animate(view, isSelected(i), isCenter(i))
        }
        positionIndicator()
    }

    private fun positionIndicator() {
        val indicator = indicatorView ?: return
        val current = items.indices.firstOrNull { isSelected(it) } ?: -1
        // No selection, or the center tab is selected: hide the indicator (the center
        // circle from the binder is the highlight there).
        if (current == -1 || isCenter(current)) {
            if (indicatorVisible) {
                indicator.animate().cancel()
                indicator.animate().alpha(0f).setDuration(120).start()
                indicatorVisible = false
            }
            previousSelectedIndex = current
            return
        }
        if (strip.width == 0) {
            // Not laid out yet; retry once layout completes.
            post { positionIndicator() }
            return
        }
        val tabWidth = strip.width.toFloat() / items.size
        val targetX = current * tabWidth
        val lp = indicator.layoutParams
        if (lp.width != tabWidth.toInt()) {
            lp.width = tabWidth.toInt()
            indicator.layoutParams = lp
        }
        if (!indicatorVisible) {
            // Appear: if coming from the center, start at the center position and flow out;
            // otherwise just fade in at the target.
            val startX = if (previousSelectedIndex >= 0) previousSelectedIndex * tabWidth else targetX
            indicator.translationX = startX
            indicator.alpha = 0f
            indicator.animate()
                .alpha(1f)
                .translationX(targetX)
                .setInterpolator(OvershootInterpolator(1.5f))
                .setDuration(280)
                .start()
            indicatorVisible = true
        } else if (current != previousSelectedIndex) {
            // Slide fluidly to the new tab with an overshoot snap.
            indicator.animate().cancel()
            indicator.animate()
                .translationX(targetX)
                .setInterpolator(OvershootInterpolator(1.5f))
                .setDuration(280)
                .start()
        }
        previousSelectedIndex = current
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

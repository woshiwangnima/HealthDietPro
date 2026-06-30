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
        // Cross-axis child size: when the bar itself wraps on the cross axis, children must
        // also wrap — otherwise MATCH_PARENT children in a wrap_content bar create a sizing
        // cycle that expands the bar to fill its parent. When the bar is fixed-size on the
        // cross axis, children fill (MATCH_PARENT) for full-height tap targets.
        val barWrapCross = layoutParams?.let { if (horizontal) it.height else it.width } ==
            LayoutParams.WRAP_CONTENT
        val crossAxis = if (barWrapCross)
            LinearLayout.LayoutParams.WRAP_CONTENT
        else LinearLayout.LayoutParams.MATCH_PARENT

        for ((index, item) in items.withIndex()) {
            val view = TabItemView(context)
            itemViews.add(view)
            view.setOnClickListener { toggle(index) }
            val lp = LinearLayout.LayoutParams(
                if (horizontal) {
                    if (scroll) LinearLayout.LayoutParams.WRAP_CONTENT else 0
                } else crossAxis,
                if (horizontal) crossAxis
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

        // Strip/scrollView: fill the primary axis, cross-axis per `crossAxis`, centered.
        val rootLp = LayoutParams(
            if (horizontal) LayoutParams.MATCH_PARENT else crossAxis,
            if (horizontal) crossAxis else LayoutParams.MATCH_PARENT
        )
        rootLp.gravity = if (horizontal) Gravity.CENTER_VERTICAL else Gravity.CENTER_HORIZONTAL

        if (indicatorView != null) {
            // Height 0 at measure: a plain View with WRAP_CONTENT returns the parent's AT_MOST
            // max via getDefaultSize (would expand a wrap_content bar). Use a concrete 0 so it
            // can't expand; SlidingIndicator pins it to the strip's real height once laid out.
            addView(indicatorView!!, LayoutParams(LayoutParams.MATCH_PARENT, 0))
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
            // the pill doesn't draw twice on top of the indicator. Applies to the center
            // tab too — every selected tab now uses the same sliding capsule style.
            if (indicatorView != null) {
                view.background = null
            }
            animator?.animate(view, isSelected(i), isCenter(i))
        }
        val current = items.indices.firstOrNull { isSelected(it) } ?: -1
        indicatorView?.let { ind -> indicator?.onSelectionChanged(ind, this, current) }
    }

    /**
     * Size the bar's height so the enlarged (selected) item — icon + caption→label-scaled
     * label — fits fully inside the strip/capsule, even at large user 默认字体大小.
     * [hasIcon] should be true for bars whose tabs carry an icon. Both the label dimen
     * (`sp`) and the icon size scale to the font-preference / system font scale where
     * applicable, so the resulting bar height tracks 设置 → 偏好设置 → 默认字体大小
     * automatically. Call after [setTabs] once the bar has been laid out in its parent.
     */
    fun applyEnlargedTabHeight(hasIcon: Boolean) {
        val dm = resources.displayMetrics
        val density = dm.density
        // Visible "selected" target sizes: caption(12sp)×scale → label(14sp) glyph.
        val labelTextPx = resources.getDimension(R.dimen.text_size_label)
        val labelLinePx = labelTextPx * 1.5f
        // item_tab.xml ships a 24dp icon base; the animator scales it by ~1.17× when
        // selected, so budget for a ~28dp glyph.
        val iconEnlarged = if (hasIcon) 28f * density else 0f
        val gap = 4f * density
        val verticalPadding = 12f * density * 2f
        val h = (iconEnlarged + gap + labelLinePx + verticalPadding)
            .toInt().coerceAtLeast((56f * density).toInt())
        val lp = layoutParams ?: return
        lp.height = h
        layoutParams = lp
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

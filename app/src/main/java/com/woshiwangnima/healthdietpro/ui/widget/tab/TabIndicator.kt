package com.woshiwangnima.healthdietpro.ui.widget.tab

import android.view.View
import android.view.animation.OvershootInterpolator

/**
 * Pluggable selection indicator for a [TabBar]. When a [TabIndicator] is set on a TabBar
 * (and the bar is horizontal + non-scrollable), the TabBar hosts an indicator view behind
 * the strip and delegates its animation to this object. Implement this to create new
 * selection-animation styles (sliding pill, morphing blob, fade, spring, etc.).
 */
interface TabIndicator {
    /** Called when the indicator is (re)attached to a fresh indicator view; reset internal state. */
    fun onReset()

    /** Called after every selection render. `selectedIndex` is -1 when nothing is selected. */
    fun onSelectionChanged(indicatorView: View, tabBar: TabBar, selectedIndex: Int)
}

/**
 * Default indicator: a rounded pill that slides from the previous tab to the selected tab
 * with an overshoot snap ("fluid flow" feel). Hides when the center tab of a center-highlight
 * bar is selected (the center's circle is the highlight there).
 */
class SlidingIndicator : TabIndicator {

    private var previous: Int = -1
    private var visible: Boolean = false

    override fun onReset() {
        previous = -1
        visible = false
    }

    override fun onSelectionChanged(indicatorView: View, tabBar: TabBar, selectedIndex: Int) {
        // The capsule is the same for every tab (including the center "醒目" tab), so we
        // only hide it when there is genuinely no selection.
        if (selectedIndex == -1) {
            if (visible) {
                indicatorView.animate().cancel()
                indicatorView.animate().alpha(0f).setDuration(120).start()
                visible = false
            }
            previous = selectedIndex
            return
        }
        if (tabBar.strip.width == 0 || tabBar.strip.height == 0) {
            // Not laid out yet; retry once layout completes.
            tabBar.post { onSelectionChanged(indicatorView, tabBar, selectedIndex) }
            return
        }
        // Pin the indicator's size to the strip's measured size: it was sized 0×MATCH_PARENT
        // at measure to avoid expanding a wrap_content bar, so it can't be visible until we
        // resize it to the strip's real height here.
        val count = tabBar.items.size
        if (count == 0) return
        val tabWidth = tabBar.strip.width.toFloat() / count
        val stripH = tabBar.strip.height
        val targetX = selectedIndex * tabWidth
        val lp = indicatorView.layoutParams
        if (lp.width != tabWidth.toInt() || lp.height != stripH) {
            lp.width = tabWidth.toInt()
            lp.height = stripH
            indicatorView.layoutParams = lp
        }
        if (!visible) {
            // Appear: slide in from the previously selected tab if any, else fade in
            // at the target.
            val startX = if (previous >= 0) previous * tabWidth else targetX
            indicatorView.translationX = startX
            indicatorView.alpha = 0f
            indicatorView.animate()
                .alpha(1f)
                .translationX(targetX)
                .setInterpolator(OvershootInterpolator(1.5f))
                .setDuration(280)
                .start()
            visible = true
        } else if (selectedIndex != previous) {
            indicatorView.animate().cancel()
            indicatorView.animate()
                .translationX(targetX)
                .setInterpolator(OvershootInterpolator(1.5f))
                .setDuration(280)
                .start()
        }
        previous = selectedIndex
    }
}

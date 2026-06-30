package com.woshiwangnima.healthdietpro.ui.widget.tab

import android.view.animation.OvershootInterpolator

interface TabAnimator {
    fun animate(view: TabItemView, selected: Boolean, isCenter: Boolean)
}

/**
 * Selection animation for the bottom toggle bars.
 *
 *  - Unselected non-center: scale 1.0 (the caption-size text/icon stays at its rest
 *    size — NOT enlarged), muted alpha, baseline Y.
 *  - Unselected center ("醒目" tab): same 1.0 scale, nudged up a few dp so it sits a
 *    little higher than its neighbours — eye-catch without enlargement.
 *  - Selected (any tab): the whole item scales up by [SELECTED_SCALE] (≈ "脚注"→"标注"
 *    text gap and matching icon bump), full opacity, settles to baseline Y. Runs in
 *    parallel with the sliding capsule indicator → "放大 + 胶囊 + 切换动画".
 */
object DefaultTabAnimator : TabAnimator {
    private const val DURATION = 240L
    /** caption(12sp) → label(14sp) visually. */
    private const val SELECTED_SCALE = 14f / 12f
    private const val CENTER_RAISE_DP = 6f
    private const val SELECTED_ELEVATION_DP = 4f

    override fun animate(view: TabItemView, selected: Boolean, isCenter: Boolean) {
        val density = view.resources.displayMetrics.density
        view.animate().cancel()

        val targetScale = if (selected) SELECTED_SCALE else 1f
        val targetAlpha = if (selected) 1f else 0.85f
        view.elevation = if (selected) SELECTED_ELEVATION_DP * density else 0f
        val targetTranslationY = when {
            selected -> 0f
            isCenter -> -CENTER_RAISE_DP * density
            else -> 0f
        }

        view.animate()
            .alpha(targetAlpha)
            .scaleX(targetScale)
            .scaleY(targetScale)
            .translationY(targetTranslationY)
            .setInterpolator(OvershootInterpolator(1.2f))
            .setDuration(DURATION)
            .start()
    }
}
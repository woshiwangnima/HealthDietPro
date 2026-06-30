package com.woshiwangnima.healthdietpro.ui.widget.tab

interface TabAnimator {
    fun animate(view: TabItemView, selected: Boolean, isCenter: Boolean)
}

object DefaultTabAnimator : TabAnimator {
    private const val DURATION = 150L

    override fun animate(view: TabItemView, selected: Boolean, isCenter: Boolean) {
        val density = view.resources.displayMetrics.density
        view.animate().cancel()
        val targetScale = when {
            isCenter -> 1.1f
            selected -> 1.05f
            else -> 1f
        }
        val targetAlpha = if (selected) 1f else 0.7f
        if (isCenter) {
            view.elevation = 4f * density
            view.translationY = -8f * density
        } else {
            view.elevation = 0f
            view.translationY = 0f
        }
        view.animate()
            .alpha(targetAlpha)
            .scaleX(targetScale)
            .scaleY(targetScale)
            .setDuration(DURATION)
            .start()
    }
}

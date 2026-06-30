package com.woshiwangnima.healthdietpro.ui.widget.tab

import android.graphics.Typeface
import androidx.core.content.ContextCompat
import com.woshiwangnima.healthdietpro.R

interface TabBinder {
    fun bind(item: TabItem, view: TabItemView, selected: Boolean, isCenter: Boolean)
}

object DefaultTabBinder : TabBinder {

    override fun bind(item: TabItem, view: TabItemView, selected: Boolean, isCenter: Boolean) {
        // Label base is "脚注" (caption) size — the unselected rest size. The selected
        // enlargement appears via TabAnimator's scale (selected visual size ≈ "标注"/label).
        view.labelView.text = item.label
        val labelPx = view.resources.getDimension(R.dimen.text_size_caption)
        view.labelView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, labelPx)
        view.labelView.setTypeface(null, if (selected) Typeface.BOLD else Typeface.NORMAL)

        // Icon
        view.showIcon(item.icon != null)
        if (item.icon != null) {
            view.iconView.setImageResource(item.icon)
            val tintColor = if (selected) R.color.primary else R.color.on_surface_variant
            view.iconView.setColorFilter(ContextCompat.getColor(view.context, tintColor))
        }

        // Label color
        val ctx = view.context
        val labelColor = if (selected) R.color.primary else R.color.on_surface_variant
        view.labelView.setTextColor(ContextCompat.getColor(ctx, labelColor))

        // Background: the indicator hosts the capsule highlight. The per-tab pill is only
        // a fallback (when no overlay indicator is in use). matches the surface color / no
        // highlight in the unselected state (so it merges with the bar's colorSurface bg).
        view.background = if (selected)
            ContextCompat.getDrawable(ctx, R.drawable.tab_pill_selected) else null
    }
}
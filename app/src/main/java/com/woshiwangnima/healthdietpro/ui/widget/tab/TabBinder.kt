package com.woshiwangnima.healthdietpro.ui.widget.tab

import android.graphics.Typeface
import androidx.core.content.ContextCompat
import com.woshiwangnima.healthdietpro.R

interface TabBinder {
    fun bind(item: TabItem, view: TabItemView, selected: Boolean, isCenter: Boolean)
}

object DefaultTabBinder : TabBinder {

    override fun bind(item: TabItem, view: TabItemView, selected: Boolean, isCenter: Boolean) {
        // Label
        view.labelView.text = item.label
        val labelPx = view.resources.getDimension(R.dimen.text_size_label)
        view.labelView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, labelPx)
        view.labelView.setTypeface(null, if (selected) Typeface.BOLD else Typeface.NORMAL)

        // Icon
        view.showIcon(item.icon != null)
        if (item.icon != null) {
            view.iconView.setImageResource(item.icon)
            val tintColor = if (selected) R.color.primary else R.color.on_surface_variant
            view.iconView.setColorFilter(ContextCompat.getColor(view.context, tintColor))
        }

        // Background + label color
        val ctx = view.context
        if (isCenter) {
            view.background = ContextCompat.getDrawable(
                ctx,
                if (selected) R.drawable.bg_nav_center_selected else R.drawable.bg_nav_center
            )
            view.labelView.setTextColor(ContextCompat.getColor(ctx, R.color.primary))
        } else {
            view.background = if (selected)
                ContextCompat.getDrawable(ctx, R.drawable.tab_pill_selected) else null
            val labelColor = if (selected) R.color.primary else R.color.on_surface_variant
            view.labelView.setTextColor(ContextCompat.getColor(ctx, labelColor))
        }
    }
}
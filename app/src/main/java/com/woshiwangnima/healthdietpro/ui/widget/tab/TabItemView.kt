package com.woshiwangnima.healthdietpro.ui.widget.tab

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.woshiwangnima.healthdietpro.R

class TabItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    val iconView: ImageView
    val labelView: TextView

    init {
        inflate(context, R.layout.item_tab, this)
        orientation = VERTICAL
        gravity = android.view.Gravity.CENTER
        iconView = findViewById(R.id.tabIcon)
        labelView = findViewById(R.id.tabLabel)
    }

    fun showIcon(show: Boolean) {
        iconView.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
    }
}
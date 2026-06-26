package com.woshiwangnima.healthdietpro.ui.profile.chart

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import com.woshiwangnima.healthdietpro.ui.theme.FontStyle
import com.woshiwangnima.healthdietpro.ui.theme.applyFontStyle

class BmiReferenceView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val bands = BmiUtil.loadBmiBands()

    init {
        orientation = VERTICAL
        setPadding(16, 8, 16, 8)

        val title = TextView(context).apply {
            text = "BMI 中国标准划分对照表"
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(0xFF333333.toInt())
            setPadding(0, 16, 0, 8)
            gravity = Gravity.START
            applyFontStyle(FontStyle.SUBTITLE)
        }
        addView(title)

        // Header row
        addView(buildRow("BMI 范围", "分类", true))

        // Data rows
        for (band in bands) {
            val rangeText = when {
                band.min < 0f -> "< ${band.max}"
                band.max < 0f || band.max == Float.MAX_VALUE -> "≥ ${band.min}"
                else -> "${band.min} ~ ${band.max}"
            }
            addView(buildRow(rangeText, band.label, false, band.color))
        }
    }

    private fun buildRow(left: String, right: String, isHeader: Boolean, bgColor: Int = Color.TRANSPARENT): LinearLayout {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(8, 10, 8, 10)
            setBackgroundColor(bgColor)
        }
        val leftTv = TextView(context).apply {
            text = left; textSize = if (isHeader) 14f else 13f
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
            setTypeface(typeface, if (isHeader) Typeface.BOLD else Typeface.NORMAL)
        }
        val rightTv = TextView(context).apply {
            text = right; textSize = if (isHeader) 14f else 13f
            gravity = Gravity.END
            setTypeface(typeface, if (isHeader) Typeface.BOLD else Typeface.NORMAL)
        }
        row.addView(leftTv)
        row.addView(rightTv)
        return row
    }
}

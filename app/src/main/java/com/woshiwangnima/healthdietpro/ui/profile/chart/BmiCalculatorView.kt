package com.woshiwangnima.healthdietpro.ui.profile.chart

import android.content.Context
import android.graphics.Typeface
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.Gravity
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import com.woshiwangnima.healthdietpro.R

class BmiCalculatorView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private lateinit var heightInput: EditText
    private lateinit var weightInput: EditText
    private lateinit var resultText: TextView
    private val bands = BmiUtil.loadBmiBands()

    init {
        orientation = VERTICAL
        setPadding(16, 8, 16, 16)

        val title = TextView(context).apply {
            text = "BMI 计算器"
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(0xFF333333.toInt())
            setPadding(0, 16, 0, 8)
            gravity = Gravity.START
            setTextSize(
                android.util.TypedValue.COMPLEX_UNIT_PX,
                resources.getDimension(R.dimen.text_size_subtitle)
            )
        }
        addView(title)

        heightInput = EditText(context).apply {
            textSize = 14f; gravity = Gravity.CENTER
            inputType = EditorInfo.TYPE_CLASS_NUMBER or EditorInfo.TYPE_NUMBER_FLAG_DECIMAL
            hint = "170.0"
        }
        addView(buildInputRow("身高 (cm)", heightInput))

        weightInput = EditText(context).apply {
            textSize = 14f; gravity = Gravity.CENTER
            inputType = EditorInfo.TYPE_CLASS_NUMBER or EditorInfo.TYPE_NUMBER_FLAG_DECIMAL
            hint = "65.0"
        }
        addView(buildInputRow("体重 (kg)", weightInput))

        resultText = TextView(context).apply {
            textSize = 18f; gravity = Gravity.CENTER
            setPadding(0, 16, 0, 8)
            text = "BMI: --"
            setTypeface(typeface, Typeface.BOLD)
        }
        addView(resultText)

        val watcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { compute() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
        heightInput.addTextChangedListener(watcher)
        weightInput.addTextChangedListener(watcher)
    }

    private fun buildInputRow(label: String, input: EditText): LinearLayout {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 6, 0, 6)
        }
        val labelTv = TextView(context).apply {
            text = label; textSize = 14f
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        }
        input.layoutParams = LayoutParams((120 * resources.displayMetrics.density).toInt(), LayoutParams.WRAP_CONTENT)
        row.addView(labelTv)
        row.addView(input)
        return row
    }

    private fun compute() {
        val h = heightInput.text.toString().toFloatOrNull()
        val w = weightInput.text.toString().toFloatOrNull()
        if (h == null || w == null || h <= 0f || w <= 0f) {
            resultText.text = "BMI: --"
            return
        }
        val bmi = BmiUtil.computeBmi(w, h)
        val label = BmiUtil.getBmiLabel(bmi, bands)
        resultText.text = "BMI: %.1f  (%s)".format(bmi, label)
    }
}

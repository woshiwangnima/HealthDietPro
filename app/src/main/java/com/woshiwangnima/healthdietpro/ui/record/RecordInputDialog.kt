package com.woshiwangnima.healthdietpro.ui.record

import android.app.DatePickerDialog
import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.woshiwangnima.healthdietpro.R
import java.util.Calendar

class RecordInputDialog(
    private val context: Context,
    private val title: String,
    private val units: List<String>,
    private val defaultUnit: String,
    private val onConfirm: (date: String, value: Float, unit: String) -> Unit
) {
    private var selectedDate: String
    private var selectedUnit: String = defaultUnit
    private var unitIndex: Int = units.indexOf(defaultUnit).coerceAtLeast(0)

    init {
        val cal = Calendar.getInstance()
        selectedDate = "%04d-%02d-%02d".format(
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
    }

    fun show() {
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 24, 32, 8)
        }

        // Date display (centered)
        val dateTv = TextView(context).apply {
            text = selectedDate
            textSize = 18f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 16)
        }
        dateTv.setOnClickListener {
            val cal = Calendar.getInstance()
            val parts = selectedDate.split("-")
            if (parts.size == 3) {
                cal.set(Calendar.YEAR, parts[0].toIntOrNull() ?: cal.get(Calendar.YEAR))
                cal.set(Calendar.MONTH, (parts[1].toIntOrNull() ?: 1) - 1)
                cal.set(Calendar.DAY_OF_MONTH, parts[2].toIntOrNull() ?: 1)
            }
            DatePickerDialog(context, { _, year, month, day ->
                selectedDate = "%04d-%02d-%02d".format(year, month + 1, day)
                dateTv.text = selectedDate
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }
        root.addView(dateTv)

        // Value input + unit selector row
        val inputRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val valueInput = EditText(context).apply {
            hint = "数值"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        inputRow.addView(valueInput)

        // Unit selector with left/right arrows
        val unitRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        val unitLabel = TextView(context).apply {
            text = selectedUnit; textSize = 14f; setPadding(4, 0, 4, 0)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        val leftArrow = TextView(context).apply {
            text = "◀"; textSize = 18f; setPadding(8, 0, 4, 0)
            setOnClickListener {
                unitIndex = (unitIndex - 1 + units.size) % units.size
                selectedUnit = units[unitIndex]
                unitLabel.text = selectedUnit
            }
        }
        val rightArrow = TextView(context).apply {
            text = "▶"; textSize = 18f; setPadding(4, 0, 8, 0)
            setOnClickListener {
                unitIndex = (unitIndex + 1) % units.size
                selectedUnit = units[unitIndex]
                unitLabel.text = selectedUnit
            }
        }
        unitRow.addView(leftArrow)
        unitRow.addView(unitLabel)
        unitRow.addView(rightArrow)
        inputRow.addView(unitRow)
        root.addView(inputRow)

        AlertDialog.Builder(context)
            .setTitle(title)
            .setView(root)
            .setPositiveButton("确认") { _, _ ->
                val v = valueInput.text.toString().toFloatOrNull() ?: return@setPositiveButton
                onConfirm(selectedDate, v, selectedUnit)
            }
            .setNegativeButton("取消", null)
            .show()
    }
}

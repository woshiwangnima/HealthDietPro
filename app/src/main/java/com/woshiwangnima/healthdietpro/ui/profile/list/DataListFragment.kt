package com.woshiwangnima.healthdietpro.ui.profile.list

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.textfield.TextInputEditText
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.model.profile.BodyRecord
import com.woshiwangnima.healthdietpro.model.unit.UnitCategory
import com.woshiwangnima.healthdietpro.ui.profile.BodyRecordAdapter
import com.woshiwangnima.healthdietpro.util.UnitConverter
import java.util.Calendar

class DataListFragment : Fragment() {

    var onRecordsChanged: (() -> Unit)? = null
    var records: MutableList<BodyRecord> = mutableListOf()
    private var unitId: String = UnitCategory.DEFAULT_UNIT_LENGTH
    private var category: String = UnitCategory.ID_LENGTH
    private var isHeight: Boolean = true
    private lateinit var adapter: BodyRecordAdapter
    private var isDialogShowing = false

    companion object {
        private const val ARG_RECORDS = "records"
        private const val ARG_UNIT = "unit"
        private const val ARG_CATEGORY = "category"
        private const val ARG_IS_HEIGHT = "is_height"

        fun newInstance(records: ArrayList<BodyRecord>, unit: String, category: String, isHeight: Boolean): DataListFragment {
            val fragment = DataListFragment()
            val args = Bundle()
            args.putSerializable(ARG_RECORDS, records)
            args.putString(ARG_UNIT, unit)
            args.putString(ARG_CATEGORY, category)
            args.putBoolean(ARG_IS_HEIGHT, isHeight)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            @Suppress("UNCHECKED_CAST")
            records = (it.getSerializable(ARG_RECORDS, ArrayList::class.java) as? ArrayList<BodyRecord>)?.toMutableList() ?: mutableListOf()
            unitId = it.getString(ARG_UNIT, UnitCategory.DEFAULT_UNIT_LENGTH) ?: UnitCategory.DEFAULT_UNIT_LENGTH
            category = it.getString(ARG_CATEGORY, UnitCategory.ID_LENGTH) ?: UnitCategory.ID_LENGTH
            isHeight = it.getBoolean(ARG_IS_HEIGHT, true)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_data_list, container, false)
        val recyclerView = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recordList)
        val addBtn = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.addRecordBtn)

        adapter = BodyRecordAdapter(unitId)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        refreshList()

        adapter.onDelete = { pos ->
            records.removeAt(pos)
            refreshList()
            onRecordsChanged?.invoke()
        }

        adapter.onClick = { pos -> showEditDialog(pos) }

        addBtn.setOnClickListener { showAddDialog() }
        return view
    }

    private fun refreshList() {
        records.sortByDescending { it.date }
        adapter.records = records
    }

    private fun showAddDialog() {
        if (isDialogShowing) return
        isDialogShowing = true
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_body_record, null)
        val dateInput = view.findViewById<TextView>(R.id.dialogRecordDate)
        val valueInput = view.findViewById<TextInputEditText>(R.id.dialogRecordValue)
        val unitText = view.findViewById<TextView>(R.id.dialogRecordUnit)
        unitText.text = unitId

        val cal = Calendar.getInstance()
        val today = "%04d-%02d-%02d".format(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
        dateInput.text = today
        dateInput.setOnClickListener {
            DatePickerDialog(requireContext(), { _, year, month, day ->
                dateInput.text = "%04d-%02d-%02d".format(year, month + 1, day)
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(if (isHeight) "添加身高记录" else "添加体重记录")
            .setView(view)
            .setPositiveButton("添加") { _, _ ->
                val dateStr = dateInput.text.toString()
                val valueStr = valueInput.text.toString()
                if (valueStr.isBlank()) {
                    Toast.makeText(requireContext(), "请输入数值", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val value = valueStr.toFloatOrNull()
                if (value == null || value <= 0) {
                    Toast.makeText(requireContext(), "请输入有效数值", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val baseValue = UnitConverter.toBase(category, value, unitId)
                records.add(BodyRecord(date = dateStr, value = baseValue, unit = unitId))
                records.sortByDescending { it.date }
                refreshList()
                onRecordsChanged?.invoke()
            }
            .setNegativeButton("取消", null)
            .setOnDismissListener { isDialogShowing = false }
            .show()
        dialog.setOnDismissListener { isDialogShowing = false }
    }

    private fun showEditDialog(position: Int) {
        if (isDialogShowing) return
        isDialogShowing = true
        val record = records[position]
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_body_record, null)
        val dateInput = view.findViewById<TextView>(R.id.dialogRecordDate)
        val valueInput = view.findViewById<TextInputEditText>(R.id.dialogRecordValue)
        val unitText = view.findViewById<TextView>(R.id.dialogRecordUnit)
        unitText.text = unitId

        val displayValue = UnitConverter.fromBase(category, record.value, unitId)
        dateInput.text = record.date
        valueInput.setText(String.format("%.1f", displayValue))

        dateInput.setOnClickListener {
            val parts = record.date.split("-")
            val cal = Calendar.getInstance()
            if (parts.size == 3) {
                cal.set(Calendar.YEAR, parts[0].toIntOrNull() ?: cal.get(Calendar.YEAR))
                cal.set(Calendar.MONTH, (parts[1].toIntOrNull() ?: 1) - 1)
                cal.set(Calendar.DAY_OF_MONTH, parts[2].toIntOrNull() ?: 1)
            }
            DatePickerDialog(requireContext(), { _, year, month, day ->
                dateInput.text = "%04d-%02d-%02d".format(year, month + 1, day)
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        AlertDialog.Builder(requireContext())
            .setTitle(if (isHeight) "编辑身高记录" else "编辑体重记录")
            .setView(view)
            .setPositiveButton("保存") { _, _ ->
                val dateStr = dateInput.text.toString()
                val valueStr = valueInput.text.toString()
                if (valueStr.isBlank()) {
                    Toast.makeText(requireContext(), "请输入数值", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val value = valueStr.toFloatOrNull()
                if (value == null || value <= 0) {
                    Toast.makeText(requireContext(), "请输入有效数值", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val baseValue = UnitConverter.toBase(category, value, unitId)
                records[position] = BodyRecord(date = dateStr, value = baseValue, unit = unitId)
                records.sortByDescending { it.date }
                refreshList()
                onRecordsChanged?.invoke()
            }
            .setNegativeButton("取消", null)
            .setOnDismissListener { isDialogShowing = false }
            .show()
    }
}

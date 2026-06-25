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
import com.woshiwangnima.healthdietpro.ui.profile.BodyRecordAdapter
import com.woshiwangnima.healthdietpro.util.UnitConverter
import java.util.Calendar

class DataListFragment : Fragment() {

    var onRecordsChanged: (() -> Unit)? = null
    var records: MutableList<BodyRecord> = mutableListOf()
    private var unitId: String = "cm"
    private var category: String = "height"
    private var isHeight: Boolean = true
    private lateinit var adapter: BodyRecordAdapter

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
            records = (it.getSerializable(ARG_RECORDS) as? ArrayList<BodyRecord>)?.toMutableList() ?: mutableListOf()
            unitId = it.getString(ARG_UNIT, "cm") ?: "cm"
            category = it.getString(ARG_CATEGORY, "height") ?: "height"
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

        addBtn.setOnClickListener { showAddDialog() }
        return view
    }

    private fun refreshList() {
        adapter.records = records
    }

    private fun showAddDialog() {
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

        AlertDialog.Builder(requireContext())
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
            .show()
    }
}

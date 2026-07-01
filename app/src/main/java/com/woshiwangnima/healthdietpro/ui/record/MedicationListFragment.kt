package com.woshiwangnima.healthdietpro.ui.record

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.model.medication.MedicationPrefs
import com.woshiwangnima.healthdietpro.ui.record.MedicationRecordActivity.Companion.EXTRA_RECORD_ID
import com.woshiwangnima.healthdietpro.util.showConfirmDialog

class MedicationListFragment : Fragment() {

    var onRecordsChanged: (() -> Unit)? = null

    private lateinit var adapter: MedicationRecordAdapter
    private var rootView: View? = null

    private val medicationLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            refreshList()
            onRecordsChanged?.invoke()
        }
    }

    companion object {
        fun newInstance(): MedicationListFragment {
            return MedicationListFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_medication_list, container, false)
        val recyclerView = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.recordList)
        val addBtn = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.addRecordBtn)

        adapter = MedicationRecordAdapter()
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        rootView = view
        refreshList()

        adapter.onDelete = { pos ->
            val record = adapter.records[pos]
            requireContext().showConfirmDialog("确认删除", "确定要删除「${record.medicationName}」的记录吗？") {
                val all = MedicationPrefs.getRecords(requireContext()).toMutableList()
                all.removeAll { it.id == record.id }
                MedicationPrefs.saveRecords(requireContext(), all)
                refreshList()
                onRecordsChanged?.invoke()
            }
        }

        adapter.onClick = { pos ->
            val record = adapter.records[pos]
            val intent = Intent(requireContext(), MedicationRecordActivity::class.java).apply {
                putExtra(EXTRA_RECORD_ID, record.id)
            }
            medicationLauncher.launch(intent)
        }

        addBtn.setOnClickListener {
            val intent = Intent(requireContext(), MedicationRecordActivity::class.java)
            medicationLauncher.launch(intent)
        }

        return view
    }

    fun refreshList() {
        val records = MedicationPrefs.getRecords(requireContext())
        // 按 timestamp 降序（最新在前）
        adapter.records = records.sortedByDescending { it.timestamp }.toMutableList()
        rootView?.findViewById<TextView>(R.id.totalCount)?.text = "共 ${records.size} 条记录"
    }
}

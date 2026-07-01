package com.woshiwangnima.healthdietpro.ui.record

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.model.medication.MedicationRecord
import com.woshiwangnima.healthdietpro.util.time.DateTimePicker

class MedicationRecordAdapter : RecyclerView.Adapter<MedicationRecordAdapter.ViewHolder>() {

    var records: MutableList<MedicationRecord> = mutableListOf()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var onDelete: ((Int) -> Unit)? = null
    var onClick: ((Int) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_medication_record, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val record = records[position]
        holder.timeText.text = DateTimePicker.format(record.timestamp)
        holder.medNameText.text = record.medicationName

        val doseStr = if (record.doseValue > 0f || record.doseUnit.isNotEmpty()) {
            "${record.doseValue}${record.doseUnit}"
        } else ""
        holder.doseText.text = doseStr

        holder.methodText.text = record.method

        val feelingStr = if (record.feelings.isEmpty()) "" else record.feelings.joinToString("、")
        holder.feelingsText.text = feelingStr

        holder.itemView.setOnClickListener { onClick?.invoke(position) }
        holder.deleteBtn.setOnClickListener { onDelete?.invoke(position) }
    }

    override fun getItemCount(): Int = records.size

    class ViewHolder(view: android.view.View) : RecyclerView.ViewHolder(view) {
        val timeText: TextView = view.findViewById(R.id.recordTime)
        val medNameText: TextView = view.findViewById(R.id.recordMedName)
        val doseText: TextView = view.findViewById(R.id.recordDose)
        val methodText: TextView = view.findViewById(R.id.recordMethod)
        val feelingsText: TextView = view.findViewById(R.id.recordFeelings)
        val deleteBtn: ImageButton = view.findViewById(R.id.deleteBtn)
    }
}

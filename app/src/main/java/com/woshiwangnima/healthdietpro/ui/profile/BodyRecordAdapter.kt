package com.woshiwangnima.healthdietpro.ui.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ImageButton
import androidx.recyclerview.widget.RecyclerView
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.model.profile.BodyRecord

class BodyRecordAdapter(
    private val unit: String
) : RecyclerView.Adapter<BodyRecordAdapter.ViewHolder>() {

    var records: MutableList<BodyRecord> = mutableListOf()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var onDelete: ((Int) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_body_record, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val record = records[position]
        holder.dateText.text = record.date
        holder.valueText.text = "${record.value} $unit"
        holder.deleteBtn.setOnClickListener { onDelete?.invoke(position) }
    }

    override fun getItemCount(): Int = records.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dateText: TextView = view.findViewById(R.id.recordDate)
        val valueText: TextView = view.findViewById(R.id.recordValue)
        val deleteBtn: ImageButton = view.findViewById(R.id.deleteBtn)
    }
}

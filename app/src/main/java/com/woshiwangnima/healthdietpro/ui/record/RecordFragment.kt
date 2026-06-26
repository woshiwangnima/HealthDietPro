package com.woshiwangnima.healthdietpro.ui.record

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.model.profile.BodyRecord
import com.woshiwangnima.healthdietpro.model.profile.ProfilePrefs
import com.woshiwangnima.healthdietpro.model.unit.UnitCategory
import com.woshiwangnima.healthdietpro.ui.profile.HeightDetailActivity
import com.woshiwangnima.healthdietpro.ui.profile.WeightDetailActivity

class RecordFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_record, container, false)

        val baseline = view.findViewById<LinearLayout>(R.id.baselineContainer)
        val daily = view.findViewById<LinearLayout>(R.id.dailyContainer)
        val status = view.findViewById<LinearLayout>(R.id.statusContainer)

        // 我的基准 — 身高 / 体重 / 腰围 / 经期
        addRecordItem(baseline, "身高", "cm") {
            val profile = ProfilePrefs.load(requireContext())
            val records = ArrayList(profile.heightRecords)
            val intent = Intent(requireContext(), HeightDetailActivity::class.java).apply {
                putExtra("records", records)
                putExtra("unit", UnitCategory.DEFAULT_UNIT_LENGTH)
            }
            startActivity(intent)
        }
        addRecordItem(baseline, "体重", "kg") {
            val profile = ProfilePrefs.load(requireContext())
            val records = ArrayList(profile.weightRecords)
            val intent = Intent(requireContext(), WeightDetailActivity::class.java).apply {
                putExtra("records", records)
                putExtra("unit", UnitCategory.DEFAULT_UNIT_WEIGHT)
            }
            startActivity(intent)
        }
        addRecordItem(baseline, "腰围", "cm") {
            RecordInputDialog(requireContext(), "记录腰围",
                listOf("cm", "in"), "cm") { date, value, unit ->
                // TODO: save waist record
            }.show()
        }
        addRecordItem(baseline, "经期", "天") {
            RecordInputDialog(requireContext(), "记录经期",
                listOf("天"), "天") { date, value, unit ->
                // TODO: save period record
            }.show()
        }

        // 每日记录 — 饮食/喝水/运动/睡眠/排便/用药/习惯
        addRecordItem(daily, "饮食", "") {
            // TODO: diet record
        }
        addRecordItem(daily, "喝水", "ml") {
            RecordInputDialog(requireContext(), "记录喝水",
                listOf("ml", "L", "杯"), "ml") { date, value, unit ->
                // TODO: save water record
            }.show()
        }
        addRecordItem(daily, "运动", "分钟") {
            RecordInputDialog(requireContext(), "记录运动",
                listOf("分钟", "小时", "步"), "分钟") { date, value, unit ->
                // TODO: save exercise record
            }.show()
        }
        addRecordItem(daily, "睡眠", "小时") {
            RecordInputDialog(requireContext(), "记录睡眠",
                listOf("小时", "分钟"), "小时") { date, value, unit ->
                // TODO: save sleep record
            }.show()
        }
        addRecordItem(daily, "排便", "次") {
            RecordInputDialog(requireContext(), "记录排便",
                listOf("次"), "次") { date, value, unit ->
                // TODO: save stool record
            }.show()
        }
        addRecordItem(daily, "用药", "次") {
            RecordInputDialog(requireContext(), "记录用药",
                listOf("次", "mg"), "次") { date, value, unit ->
                // TODO: save medication record
            }.show()
        }
        addRecordItem(daily, "习惯", "次") {
            RecordInputDialog(requireContext(), "记录习惯",
                listOf("次", "分钟"), "次") { date, value, unit ->
                // TODO: save habit record
            }.show()
        }

        // 状态记录 — 感受
        addRecordItem(status, "感受", "") {
            // TODO: feeling record (might use text input instead of numeric)
        }

        return view
    }

    private fun addRecordItem(container: LinearLayout, label: String, unit: String, onClick: () -> Unit) {
        val row = TextView(requireContext()).apply {
            text = if (unit.isNotEmpty()) "$label ($unit)" else label
            textSize = 14f
            setPadding(4, 10, 4, 10)
            setOnClickListener { onClick() }
        }
        // Divider
        if (container.childCount > 0) {
            val divider = View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1)
                setBackgroundColor(0x1A000000)
            }
            container.addView(divider)
        }
        container.addView(row)
    }
}

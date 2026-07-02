package com.woshiwangnima.healthdietpro.ui.record

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.model.profile.ProfilePrefs
import com.woshiwangnima.healthdietpro.model.unit.UnitCategoryType
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

        // 我的基准
        addRecordButton(baseline, "身高", R.drawable.ic_height) {
            val profile = ProfilePrefs.load(requireContext())
            val records = ArrayList(profile.heightRecords)
            startActivity(Intent(requireContext(), HeightDetailActivity::class.java).apply {
                putExtra("records", records)
                putExtra("unit", UnitCategoryType.Length.defaultUnitId)
            })
        }
        addRecordButton(baseline, "体重", R.drawable.ic_weight) {
            val profile = ProfilePrefs.load(requireContext())
            val records = ArrayList(profile.weightRecords)
            startActivity(Intent(requireContext(), WeightDetailActivity::class.java).apply {
                putExtra("records", records)
                putExtra("unit", UnitCategoryType.Weight.defaultUnitId)
            })
        }
        addPlaceholder(baseline, "腰围")
        addPlaceholder(baseline, "经期")

        // 每日记录
        addPlaceholder(daily, "饮食")
        addPlaceholder(daily, "喝水")
        addPlaceholder(daily, "运动")
        addPlaceholder(daily, "睡眠")
        addPlaceholder(daily, "排便")
        addRecordButton(daily, "用药", R.drawable.ic_medication) {
            startActivity(Intent(requireContext(), MedicationListActivity::class.java))
        }
        addPlaceholder(daily, "习惯")

        // 状态记录
        addPlaceholder(status, "感受")

        return view
    }

    private fun addRecordButton(container: LinearLayout, label: String, iconRes: Int, onClick: () -> Unit) {
        val btn = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(12, 8, 12, 8)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            isClickable = true; isFocusable = true
            foreground = resources.getDrawable(R.drawable.ripple_bg, null)
            setOnClickListener { onClick() }
        }
        val icon = ImageView(requireContext()).apply {
            setImageResource(iconRes)
            layoutParams = LinearLayout.LayoutParams((36 * resources.displayMetrics.density).toInt(),
                (36 * resources.displayMetrics.density).toInt())
        }
        val text = TextView(requireContext()).apply {
            text = "记${label}"
            textSize = 12f
            gravity = Gravity.CENTER
            setPadding(0, 4, 0, 0)
        }
        btn.addView(icon)
        btn.addView(text)
        container.addView(btn)
    }

    private fun addPlaceholder(container: LinearLayout, label: String) {
        val btn = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(12, 8, 12, 8)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val icon = ImageView(requireContext()).apply {
            setImageResource(R.drawable.ic_placeholder)
            layoutParams = LinearLayout.LayoutParams((36 * resources.displayMetrics.density).toInt(),
                (36 * resources.displayMetrics.density).toInt())
        }
        val text = TextView(requireContext()).apply {
            text = "记${label}"
            textSize = 12f
            gravity = Gravity.CENTER
            setPadding(0, 4, 0, 0)
            setTextColor(0xFF999999.toInt())
        }
        btn.addView(icon)
        btn.addView(text)
        container.addView(btn)
    }
}

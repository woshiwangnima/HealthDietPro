package com.woshiwangnima.healthdietpro.ui.profile

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.base.BaseBackActivity
import com.woshiwangnima.healthdietpro.databinding.ActivityWeightDetailBinding
import com.woshiwangnima.healthdietpro.model.profile.BodyRecord
import com.woshiwangnima.healthdietpro.model.profile.DataPoint
import com.woshiwangnima.healthdietpro.model.profile.ProfilePrefs
import com.woshiwangnima.healthdietpro.model.unit.UnitCategory
import com.woshiwangnima.healthdietpro.ui.profile.chart.ChartSeries
import com.woshiwangnima.healthdietpro.ui.profile.chart.ChartView
import com.woshiwangnima.healthdietpro.ui.profile.chart.LineStyle
import com.woshiwangnima.healthdietpro.ui.profile.chart.LineType
import com.woshiwangnima.healthdietpro.ui.profile.chart.PointFill
import com.woshiwangnima.healthdietpro.ui.profile.chart.PointShape
import com.woshiwangnima.healthdietpro.ui.profile.list.DataListFragment
import com.woshiwangnima.healthdietpro.ui.widget.tab.TabItem
import com.woshiwangnima.healthdietpro.ui.widget.tab.ToggleBar
import com.woshiwangnima.healthdietpro.util.UnitConverter
import com.woshiwangnima.healthdietpro.util.applySystemBarInsets
import java.time.LocalDate
import java.time.ZoneId

class WeightDetailActivity : BaseBackActivity() {

    private lateinit var binding: ActivityWeightDetailBinding
    private var records: MutableList<BodyRecord> = mutableListOf()
    private var unit: String = UnitCategory.DEFAULT_UNIT_WEIGHT
    private var category: String = UnitCategory.ID_WEIGHT
    private var currentTab = -1
    private var chartView: ChartView? = null

    override fun getTitleText(): String = "体重历史"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWeightDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets()
        setupToolbar(binding.toolbar)

        @Suppress("UNCHECKED_CAST")
        records = (intent.getSerializableExtra("records", ArrayList::class.java) as? ArrayList<BodyRecord>)?.toMutableList() ?: mutableListOf()
        unit = intent.getStringExtra("unit") ?: UnitCategory.DEFAULT_UNIT_WEIGHT
        category = UnitCategory.ID_WEIGHT

        binding.tabBar.setTabs(listOf(
            TabItem(R.drawable.ic_chart, "图表"),
            TabItem(R.drawable.ic_list, "数据")
        ))
        binding.tabBar.applyEnlargedTabHeight(hasIcon = true)
        binding.tabBar.restore("tab_weight_chart", 0)
        binding.tabBar.listener = { idx, _ -> switchTab(idx) }
        switchTab(binding.tabBar.selectedIndex)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val cv = chartView
                if (cv != null) {
                    supportFinishAfterTransition()
                } else {
                    saveAndFinish()
                }
            }
        })
    }

    private fun saveAndFinish() {
        intent.putExtra("records", ArrayList(records))
        setResult(RESULT_OK, intent)
        finish()
    }

    private fun switchTab(index: Int) {
        if (index == currentTab) return
        currentTab = index
        when (index) {
            0 -> showChartTab()
            1 -> showDataTab()
        }
    }

    private fun showChartTab() {
        val cv = chartView ?: run {
            val newCv = ChartView(this)
            chartView = newCv
            newCv
        }
        cv.setChartStateKey(ProfilePrefs.makeChartStateKey(this, "weight_history"))
        cv.setChartTitle("体重 历史")
        // Always refresh data
        val dataPoints = if (records.isEmpty()) emptyList() else {
            val sorted = records.sortedBy { it.date }
            sorted.map { record ->
                val converted = UnitConverter.fromBase(category, record.value, unit)
                val localDate = LocalDate.parse(record.date)
                val ts = localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                DataPoint(timestamp = ts, value = converted, dateLabel = record.date.takeLast(5))
            }
        }
        val series = ChartSeries(
            points = dataPoints, label = "测量值",
            color = resources.getColor(R.color.primary, null),
            lineStyle = LineStyle.LINEAR, lineType = LineType.SOLID,
            pointShape = PointShape.CIRCLE, pointFill = PointFill.FILLED
        )
        cv.setSeries(listOf(series), unit)
        if (cv.parent == null) {
            binding.contentFrame.removeAllViews()
            binding.contentFrame.addView(cv)
        }
    }

    private fun showDataTab() {
        chartView = null
        binding.contentFrame.removeAllViews()
        val fragment = DataListFragment.newInstance(ArrayList(records), unit, category, isHeight = false).also {
            it.onRecordsChanged = { this.records = it.records }
        }
        supportFragmentManager.beginTransaction()
            .replace(R.id.contentFrame, fragment)
            .commit()
    }
}

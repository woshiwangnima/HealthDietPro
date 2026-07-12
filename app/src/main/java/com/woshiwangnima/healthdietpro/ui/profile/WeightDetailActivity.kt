package com.woshiwangnima.healthdietpro.ui.profile

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.base.BaseBackActivity
import com.woshiwangnima.healthdietpro.common.ui.DetailTabBar
import com.woshiwangnima.healthdietpro.common.ui.DetailTabItem
import com.woshiwangnima.healthdietpro.common.ui.HealthDietProTheme
import com.woshiwangnima.healthdietpro.databinding.ActivityWeightDetailBinding
import com.woshiwangnima.healthdietpro.model.prefs.AppPrefs
import com.woshiwangnima.healthdietpro.model.profile.BodyRecord
import com.woshiwangnima.healthdietpro.model.profile.DataPoint
import com.woshiwangnima.healthdietpro.model.profile.ProfilePrefs
import com.woshiwangnima.healthdietpro.model.profile.bodyRecordEpochMillis
import com.woshiwangnima.healthdietpro.model.profile.formatBodyRecordDisplayDateTime
import com.woshiwangnima.healthdietpro.model.unit.UnitCategoryType
import com.woshiwangnima.healthdietpro.ui.profile.chart.ChartSeries
import com.woshiwangnima.healthdietpro.ui.profile.chart.ChartView
import com.woshiwangnima.healthdietpro.ui.profile.chart.LineStyle
import com.woshiwangnima.healthdietpro.ui.profile.chart.LineType
import com.woshiwangnima.healthdietpro.ui.profile.chart.PointFill
import com.woshiwangnima.healthdietpro.ui.profile.chart.PointShape
import com.woshiwangnima.healthdietpro.ui.profile.list.DataListFragment
import com.woshiwangnima.healthdietpro.util.UnitConverter
import com.woshiwangnima.healthdietpro.util.applySystemBarInsets

class WeightDetailActivity : BaseBackActivity() {

    private lateinit var binding: ActivityWeightDetailBinding
    private var records: MutableList<BodyRecord> = mutableListOf()
    private var unit: String = UnitCategoryType.Weight.defaultUnitId
    private var category: String = UnitCategoryType.Weight.id
    private var currentTab = -1
    private var chartView: ChartView? = null

    override fun getTitleText(): String = getString(R.string.weight_history_title)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWeightDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets()
        setupToolbar(binding.toolbar)

        @Suppress("UNCHECKED_CAST")
        records = (intent.getSerializableExtra("records", ArrayList::class.java) as? ArrayList<BodyRecord>)?.toMutableList() ?: mutableListOf()
        unit = intent.getStringExtra("unit") ?: UnitCategoryType.Weight.defaultUnitId
        category = UnitCategoryType.Weight.id

        setupTabBar()
        switchTab(AppPrefs.getWeightChartTab(this))
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

    private fun setupTabBar() {
        val items = listOf(
            DetailTabItem("0", R.string.detail_tab_chart, R.drawable.ic_chart),
            DetailTabItem("1", R.string.detail_tab_data, R.drawable.ic_list),
        )
        binding.tabBar.setContent {
            HealthDietProTheme {
                var selectedTab by remember {
                    mutableIntStateOf(AppPrefs.getWeightChartTab(this@WeightDetailActivity))
                }
                DetailTabBar(
                    items = items,
                    selectedId = selectedTab.toString(),
                    onSelected = { item ->
                        val index = item.id.toInt()
                        selectedTab = index
                        AppPrefs.setWeightChartTab(this@WeightDetailActivity, index)
                        switchTab(index)
                    },
                )
            }
        }
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
        cv.setChartTitle(getString(R.string.weight_history_title))
        // Always refresh data
        val dataPoints = if (records.isEmpty()) emptyList() else {
            val sorted = records.sortedBy { bodyRecordEpochMillis(it.date) }
            sorted.map { record ->
                val converted = UnitConverter.fromBase(category, record.value, unit)
                val ts = bodyRecordEpochMillis(record.date)
                DataPoint(timestamp = ts, value = converted, dateLabel = formatBodyRecordDisplayDateTime(record.date))
            }
        }
        val series = ChartSeries(
            points = dataPoints, label = getString(R.string.body_record_value),
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

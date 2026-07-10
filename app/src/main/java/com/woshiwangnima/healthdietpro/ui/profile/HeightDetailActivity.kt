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
import com.woshiwangnima.healthdietpro.databinding.ActivityHeightDetailBinding
import com.woshiwangnima.healthdietpro.model.prefs.AppPrefs
import com.woshiwangnima.healthdietpro.model.profile.BodyRecord
import com.woshiwangnima.healthdietpro.model.profile.DataPoint
import com.woshiwangnima.healthdietpro.model.profile.ProfilePrefs
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
import java.time.LocalDate
import java.time.ZoneId

class HeightDetailActivity : BaseBackActivity() {

    private lateinit var binding: ActivityHeightDetailBinding
    private var records: MutableList<BodyRecord> = mutableListOf()
    private var unit: String = UnitCategoryType.Length.defaultUnitId
    private var category: String = UnitCategoryType.Length.id
    private var currentTab = -1
    private var chartView: ChartView? = null

    override fun getTitleText(): String = "身高历史"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHeightDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets()
        setupToolbar(binding.toolbar)

        @Suppress("UNCHECKED_CAST")
        records = (intent.getSerializableExtra("records", ArrayList::class.java) as? ArrayList<BodyRecord>)?.toMutableList() ?: mutableListOf()
        unit = intent.getStringExtra("unit") ?: UnitCategoryType.Length.defaultUnitId
        category = UnitCategoryType.Length.id

        setupTabBar()
        switchTab(AppPrefs.getHeightChartTab(this))
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
                    mutableIntStateOf(AppPrefs.getHeightChartTab(this@HeightDetailActivity))
                }
                DetailTabBar(
                    items = items,
                    selectedId = selectedTab.toString(),
                    onSelected = { item ->
                        val index = item.id.toInt()
                        selectedTab = index
                        AppPrefs.setHeightChartTab(this@HeightDetailActivity, index)
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
        cv.setChartStateKey(ProfilePrefs.makeChartStateKey(this, "height_history"))
        cv.setChartTitle("身高 历史")
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
        val fragment = DataListFragment.newInstance(ArrayList(records), unit, category, isHeight = true).also {
            it.onRecordsChanged = { this.records = it.records }
        }
        supportFragmentManager.beginTransaction()
            .replace(R.id.contentFrame, fragment)
            .commit()
    }
}

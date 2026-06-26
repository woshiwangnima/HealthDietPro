package com.woshiwangnima.healthdietpro.ui.profile

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.base.BaseBackActivity
import com.woshiwangnima.healthdietpro.config.NavConfig
import com.woshiwangnima.healthdietpro.databinding.ActivityHeightDetailBinding
import com.woshiwangnima.healthdietpro.model.prefs.AppPrefs
import com.woshiwangnima.healthdietpro.model.profile.BodyRecord
import com.woshiwangnima.healthdietpro.model.profile.DataPoint
import com.woshiwangnima.healthdietpro.model.unit.UnitCategory
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
    private var unit: String = UnitCategory.DEFAULT_UNIT_LENGTH
    private var category: String = UnitCategory.ID_LENGTH
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
        unit = intent.getStringExtra("unit") ?: UnitCategory.DEFAULT_UNIT_LENGTH
        category = UnitCategory.ID_LENGTH

        setupBottomBar()
        val savedTab = AppPrefs.getHeightChartTab(this)
        switchTab(savedTab)
        setupTabListeners()
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val cv = chartView
                if (cv != null && cv.isFullscreen()) {
                    cv.toggleFullscreen()
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

    private fun setupBottomBar() {
        val screenHeight = resources.displayMetrics.heightPixels
        val density = resources.displayMetrics.density
        val barHeight = NavConfig.calculateBarHeightPx(screenHeight.toInt(), density)
        val params = binding.bottomBar.layoutParams
        params.height = barHeight
        binding.bottomBar.layoutParams = params
    }

    private fun switchTab(index: Int) {
        if (index == currentTab) return
        currentTab = index
        AppPrefs.setHeightChartTab(this, index)
        updateTabSelection()
        when (index) {
            0 -> showChartTab()
            1 -> showDataTab()
        }
    }

    private fun showChartTab() {
        val cv = ChartView(this).also { chartView = it }
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
        cv.setOnFullscreenListener { isFs ->
            if (isFs) {
                binding.toolbar.visibility = View.GONE
                binding.bottomBar.visibility = View.GONE
                window.decorView.windowInsetsController?.hide(WindowInsets.Type.systemBars())
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            } else {
                binding.toolbar.visibility = View.VISIBLE
                binding.bottomBar.visibility = View.VISIBLE
                window.decorView.windowInsetsController?.show(WindowInsets.Type.systemBars())
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
        binding.contentFrame.removeAllViews()
        binding.contentFrame.addView(cv)
    }

    private fun showDataTab() {
        chartView = null
        val fragment = DataListFragment.newInstance(ArrayList(records), unit, category, isHeight = true).also {
            it.onRecordsChanged = { this.records = it.records }
        }
        supportFragmentManager.beginTransaction()
            .replace(R.id.contentFrame, fragment)
            .commit()
    }

    private fun updateTabSelection() {
        val bgSelected = ContextCompat.getDrawable(this, R.drawable.tab_selected_bg)
        val bgDefault = ContextCompat.getDrawable(this, R.drawable.tab_default_bg)
        binding.tabChart.background = if (currentTab == 0) bgSelected else bgDefault
        binding.tabData.background = if (currentTab == 1) bgSelected else bgDefault
        binding.tabChart.isSelected = currentTab == 0
        binding.tabData.isSelected = currentTab == 1
    }

    private fun setupTabListeners() {
        binding.tabChart.setOnClickListener { switchTab(0) }
        binding.tabData.setOnClickListener { switchTab(1) }
    }
}

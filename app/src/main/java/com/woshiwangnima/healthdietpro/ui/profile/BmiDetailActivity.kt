package com.woshiwangnima.healthdietpro.ui.profile

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.base.BaseBackActivity
import com.woshiwangnima.healthdietpro.config.NavConfig
import com.woshiwangnima.healthdietpro.model.profile.ProfilePrefs
import com.woshiwangnima.healthdietpro.ui.profile.chart.BmiUtil
import com.woshiwangnima.healthdietpro.ui.profile.chart.ChartSeries
import com.woshiwangnima.healthdietpro.ui.profile.chart.ChartView
import com.woshiwangnima.healthdietpro.ui.profile.chart.LineStyle
import com.woshiwangnima.healthdietpro.ui.profile.chart.LineType
import com.woshiwangnima.healthdietpro.ui.profile.chart.PointFill
import com.woshiwangnima.healthdietpro.ui.profile.chart.PointShape
import com.woshiwangnima.healthdietpro.ui.profile.chart.YAxisBand
import com.woshiwangnima.healthdietpro.model.prefs.AppPrefs
import com.woshiwangnima.healthdietpro.util.applySystemBarInsets

class BmiDetailActivity : BaseBackActivity() {

    private var currentTab = 0
    private var chartView: ChartView? = null

    override fun getTitleText(): String = "BMI历史"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            applySystemBarInsets()
        }

        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(android.R.id.content) ?: run {
            val t = androidx.appcompat.widget.Toolbar(this)
            t.id = View.generateViewId()
            t
        }
        root.addView(toolbar, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
            resources.getDimensionPixelSize(com.google.android.material.R.dimen.abc_action_bar_default_height_material)))

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        root.addView(content)

        val bottomBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
            setBackgroundColor(resolveColor(com.google.android.material.R.attr.colorSurface))
        }
        val tabChart = TextView(this).apply {
            text = "图表"; gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
        }
        val tabData = TextView(this).apply {
            text = "数据"; gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
        }
        bottomBar.addView(tabChart); bottomBar.addView(tabData)

        val screenHeight = resources.displayMetrics.heightPixels
        val barHeight = NavConfig.calculateBarHeightPx(screenHeight.toInt(), resources.displayMetrics.density)
        bottomBar.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, barHeight)
        root.addView(bottomBar)

        val bmiData = BmiUtil.buildBmiDataPoints(ProfilePrefs.load(this).weightRecords, ProfilePrefs.load(this).heightRecords)

        fun switchTab(idx: Int) {
            currentTab = idx
            val bgSelected = ContextCompat.getDrawable(this, R.drawable.tab_selected_bg)
            val bgDefault = ContextCompat.getDrawable(this, R.drawable.tab_default_bg)
            tabChart.background = if (idx == 0) bgSelected else bgDefault
            tabData.background = if (idx == 1) bgSelected else bgDefault
            content.removeAllViews()
            when (idx) {
                0 -> showChart(content, bmiData)
                1 -> showData(content, bmiData)
            }
        }
        tabChart.setOnClickListener { switchTab(0) }; tabData.setOnClickListener { switchTab(1) }
        switchTab(AppPrefs.getBmiChartTab(this))

        setContentView(root)
    }

    private fun resolveColor(attrRes: Int): Int {
        val ta = theme.obtainStyledAttributes(intArrayOf(attrRes))
        val c = ta.getColor(0, 0xFF000000.toInt()); ta.recycle(); return c
    }

    private fun showChart(parent: LinearLayout, bmiData: List<com.woshiwangnima.healthdietpro.model.profile.DataPoint>) {
        val cv = ChartView(this).also { chartView = it }
        cv.setChartTitle("BMI 历史")
        cv.setChartStateKey("bmi_history")
        cv.setYAxisBands(BmiUtil.loadBmiBands().map { YAxisBand(it.min.coerceAtLeast(0f), it.max, it.color) })
        val series = ChartSeries(points = bmiData, label = "BMI", color = resources.getColor(R.color.primary, null),
            lineStyle = LineStyle.LINEAR, lineType = LineType.SOLID, pointShape = PointShape.CIRCLE, pointFill = PointFill.FILLED)
        cv.setSeries(listOf(series), "kg/m²")
        cv.setOnFullscreenListener { isFs ->
            if (isFs) {
                window.decorView.windowInsetsController?.hide(WindowInsets.Type.systemBars())
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            } else {
                window.decorView.windowInsetsController?.show(WindowInsets.Type.systemBars())
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
        parent.addView(cv)
    }

    private fun showData(parent: LinearLayout, bmiData: List<com.woshiwangnima.healthdietpro.model.profile.DataPoint>) {
        chartView = null
        val scroll = android.widget.ScrollView(this)
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(16, 8, 16, 8) }

        val countTv = TextView(this).apply {
            text = "共 ${bmiData.size} 条记录"
            textSize = 14f; setPadding(0, 8, 0, 8)
            setTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurfaceVariant))
        }
        list.addView(countTv)

        val bands = BmiUtil.loadBmiBands()
        for (dp in bmiData.sortedByDescending { it.timestamp }) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 12, 0, 12)
            }
            val dateTv = TextView(this).apply {
                text = dp.dateLabel; textSize = 14f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val label = BmiUtil.getBmiLabel(dp.value, bands)
            val valueTv = TextView(this).apply {
                text = "%.1f %s".format(dp.value, label); textSize = 14f
            }
            row.addView(dateTv); row.addView(valueTv)
            list.addView(row)
            list.addView(View(this).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
                setBackgroundColor(resolveColor(com.google.android.material.R.attr.colorOutlineVariant))
            })
        }
        scroll.addView(list)
        parent.addView(scroll)
    }

    override fun onDestroy() {
        super.onDestroy()
        AppPrefs.setBmiChartTab(this, currentTab)
    }
}

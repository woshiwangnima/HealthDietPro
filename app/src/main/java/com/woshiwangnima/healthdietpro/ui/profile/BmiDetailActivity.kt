package com.woshiwangnima.healthdietpro.ui.profile

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.base.BaseBackActivity
import com.woshiwangnima.healthdietpro.model.profile.ProfilePrefs
import com.woshiwangnima.healthdietpro.ui.profile.chart.BmiCalculatorView
import com.woshiwangnima.healthdietpro.ui.profile.chart.BmiReferenceView
import com.woshiwangnima.healthdietpro.ui.profile.chart.BmiUtil
import com.woshiwangnima.healthdietpro.ui.profile.chart.ChartSeries
import com.woshiwangnima.healthdietpro.ui.profile.chart.ChartView
import com.woshiwangnima.healthdietpro.ui.profile.chart.LineStyle
import com.woshiwangnima.healthdietpro.ui.profile.chart.LineType
import com.woshiwangnima.healthdietpro.ui.profile.chart.PointFill
import com.woshiwangnima.healthdietpro.ui.profile.chart.PointShape
import com.woshiwangnima.healthdietpro.ui.profile.chart.YAxisBand
import com.woshiwangnima.healthdietpro.ui.widget.tab.TabItem
import com.woshiwangnima.healthdietpro.ui.widget.tab.ToggleBar
import com.woshiwangnima.healthdietpro.util.applySystemBarInsets

class BmiDetailActivity : BaseBackActivity() {

    private var currentTab = -1
    private var chartView: ChartView? = null
    private lateinit var content: LinearLayout
    private val bmiData: List<com.woshiwangnima.healthdietpro.model.profile.DataPoint> by lazy {
        val profile = ProfilePrefs.load(this)
        BmiUtil.buildBmiDataPoints(profile.weightRecords, profile.heightRecords)
    }

    override fun getTitleText(): String = "BMI历史"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            applySystemBarInsets()
        }

        val toolbar = androidx.appcompat.widget.Toolbar(this).apply {
            id = R.id.toolbar
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                resources.getDimensionPixelSize(com.google.android.material.R.dimen.abc_action_bar_default_height_material))
            setBackgroundColor(resolveColor(com.google.android.material.R.attr.colorSurface))
            elevation = 2f
            setTitleTextColor(resolveColor(com.google.android.material.R.attr.colorOnSurface))
        }
        setupToolbar(toolbar)
        root.addView(toolbar)

        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        root.addView(content)

        val bottomBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
            setBackgroundColor(resolveColor(com.google.android.material.R.attr.colorSurface))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        root.addView(bottomBar)

        val tabBar = ToggleBar(this).apply {
            displayMode = com.woshiwangnima.healthdietpro.ui.widget.tab.TabBar.DisplayMode.NORMAL
            setTabs(listOf(TabItem(label = "图表"), TabItem(label = "数据")))
            restore("tab_bmi_chart", 0)
            listener = { idx, _ -> switchTab(idx) }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(48))
        }
        bottomBar.addView(tabBar)

        setContentView(root)
        switchTab(tabBar.selectedIndex)
    }

    private fun dp(n: Int): Int = (n * resources.displayMetrics.density).toInt()

    private fun resolveColor(attrRes: Int): Int {
        val ta = theme.obtainStyledAttributes(intArrayOf(attrRes))
        val c = ta.getColor(0, 0xFF000000.toInt()); ta.recycle(); return c
    }

    private fun switchTab(idx: Int) {
        if (idx == currentTab) return
        currentTab = idx
        content.removeAllViews()
        when (idx) {
            0 -> showChart()
            1 -> showData()
        }
    }

    private fun showChart() {
        val scroll = android.widget.ScrollView(this)
        val wrapper = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12, 12, 12, 12)
        }

        // Card 1: BMI chart
        val chartCard = FrameLayout(this).apply {
            setBackgroundResource(R.drawable.card_bg)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (12 * resources.displayMetrics.density).toInt()
            }
            elevation = 1f
        }
        val cv = ChartView(this).also { chartView = it }
        cv.setChartTitle("BMI 历史", android.view.Gravity.START)
        cv.setChartStateKey(ProfilePrefs.makeChartStateKey(this, "bmi_history"))
        cv.setYAxisBands(BmiUtil.loadBmiBands().map { YAxisBand(it.min.coerceAtLeast(0f), it.max, it.color) })
        val series = ChartSeries(points = bmiData, label = "BMI",
            color = resources.getColor(R.color.primary, null),
            lineStyle = LineStyle.LINEAR, lineType = LineType.SOLID,
            pointShape = PointShape.CIRCLE, pointFill = PointFill.FILLED)
        cv.setSeries(listOf(series), "kg/m²")
        chartCard.addView(cv)
        wrapper.addView(chartCard, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
            (resources.displayMetrics.heightPixels * 0.45).toInt()))

        // Card 2: BMI reference table
        BmiReferenceView(this).apply {
            setBackgroundResource(R.drawable.card_bg)
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            lp.bottomMargin = (12 * resources.displayMetrics.density).toInt()
            layoutParams = lp
            elevation = 1f
        }.also { wrapper.addView(it) }

        // Card 3: BMI calculator
        BmiCalculatorView(this).apply {
            setBackgroundResource(R.drawable.card_bg)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            elevation = 1f
        }.also { wrapper.addView(it) }

        scroll.addView(wrapper)
        content.addView(scroll)
    }

    private fun showData() {
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
                orientation = LinearLayout.HORIZONTAL; setPadding(0, 12, 0, 12)
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
        content.addView(scroll)
    }
}

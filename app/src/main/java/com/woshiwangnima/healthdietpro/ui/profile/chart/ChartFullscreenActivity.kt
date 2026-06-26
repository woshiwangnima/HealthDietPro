package com.woshiwangnima.healthdietpro.ui.profile.chart

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.widget.FrameLayout
import com.google.android.material.button.MaterialButton
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.base.BaseBackActivity
import com.woshiwangnima.healthdietpro.util.applySystemBarInsets

class ChartFullscreenActivity : BaseBackActivity() {

    override fun getTitleText(): String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val data = ChartFullscreenHolder.data ?: run { finish(); return }
        ChartFullscreenHolder.data = null

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        window.decorView.windowInsetsController?.hide(WindowInsets.Type.systemBars())

        val root = FrameLayout(this).apply { applySystemBarInsets() }

        val chartView = ChartView(this)
        chartView.setChartTitle(data.chartTitle)
        chartView.setChartStateKey(data.chartStateKey)
        chartView.setFullscreenMode(true)
        if (data.yAxisBands.isNotEmpty()) chartView.setYAxisBands(data.yAxisBands)
        chartView.setSeries(data.series, data.unitLabel)
        chartView.setVisibleRange(data.visibleRangeMs)
        chartView.setYAxisRange(data.yMinPct, data.yMaxPct)
        chartView.setLabelInterval(data.labelIntervalMs)
        chartView.invalidateChart()
        root.addView(chartView)

        val exitBtn = MaterialButton(this).apply {
            text = "缩放"
            setIconResource(R.drawable.ic_fullscreen_exit)
            iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
            iconSize = 20
            setPadding(8, 0, 8, 0)
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT).apply {
                gravity = android.view.Gravity.TOP or android.view.Gravity.END
                setMargins(8, 8, 8, 8)
            }
            setOnClickListener { finish() }
        }
        root.addView(exitBtn)

        setContentView(root)
    }
}

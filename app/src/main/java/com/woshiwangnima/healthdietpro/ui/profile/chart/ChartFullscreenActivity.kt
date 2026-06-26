package com.woshiwangnima.healthdietpro.ui.profile.chart

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.Gravity
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

        val data = ChartFullscreenHolder.data
        if (data == null) { finish(); return }
        ChartFullscreenHolder.data = null

        try {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            window.decorView.windowInsetsController?.hide(WindowInsets.Type.systemBars())
        } catch (_: Exception) {}

        val root = FrameLayout(this).apply { applySystemBarInsets() }

        val chartView = ChartView(this).apply {
            setFullscreenMode(true)
            setChartTitle(data.chartTitle)
            setChartStateKey(data.chartStateKey)
            if (data.yAxisBands.isNotEmpty()) setYAxisBands(data.yAxisBands)
            setSeries(data.series, data.unitLabel)
            // Apply state after setSeries to not be overwritten
            post {
                setVisibleRange(data.visibleRangeMs)
                setYAxisRange(data.yMinPct, data.yMaxPct)
                setLabelInterval(data.labelIntervalMs)
            }
        }
        root.addView(chartView)

        val exitBtn = MaterialButton(this).apply {
            text = "缩放"
            setIconResource(R.drawable.ic_fullscreen_exit)
            iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
            iconSize = 20
            contentDescription = "退出全屏"
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.TOP or Gravity.END
                setMargins(8, 8, 8, 8)
            }
            setOnClickListener { finish() }
        }
        root.addView(exitBtn)

        setContentView(root)
    }
}

package com.woshiwangnima.healthdietpro.ui.profile

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.WindowInsets
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.base.BaseBackActivity
import com.woshiwangnima.healthdietpro.model.profile.ProfilePrefs
import com.woshiwangnima.healthdietpro.ui.profile.chart.BmiUtil
import com.woshiwangnima.healthdietpro.ui.profile.chart.ChartSeries
import com.woshiwangnima.healthdietpro.ui.profile.chart.ChartView
import com.woshiwangnima.healthdietpro.ui.profile.chart.LineStyle
import com.woshiwangnima.healthdietpro.ui.profile.chart.LineType
import com.woshiwangnima.healthdietpro.ui.profile.chart.PointFill
import com.woshiwangnima.healthdietpro.ui.profile.chart.PointShape
import com.woshiwangnima.healthdietpro.ui.profile.chart.YAxisBand
import com.woshiwangnima.healthdietpro.util.applySystemBarInsets

class BmiDetailActivity : BaseBackActivity() {

    override fun getTitleText(): String = "BMI历史"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val chartView = ChartView(this)
        chartView.applySystemBarInsets()
        setContentView(chartView)

        val profile = ProfilePrefs.load(this)
        val dataPoints = BmiUtil.buildBmiDataPoints(profile.weightRecords, profile.heightRecords)
        val bands = BmiUtil.loadBmiBands()

        chartView.setYAxisBands(bands.map {
            YAxisBand(it.min.coerceAtLeast(0f), it.max, it.color)
        })

        val series = ChartSeries(
            points = dataPoints,
            label = "BMI",
            color = resources.getColor(R.color.primary, null),
            lineStyle = LineStyle.LINEAR,
            lineType = LineType.SOLID,
            pointShape = PointShape.CIRCLE,
            pointFill = PointFill.FILLED
        )
        chartView.setSeries(listOf(series), "kg/m²")
        chartView.setChartStateKey("bmi_history")

        chartView.setOnFullscreenListener { isFs ->
            if (isFs) {
                supportActionBar?.hide()
                window.decorView.windowInsetsController?.hide(WindowInsets.Type.systemBars())
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            } else {
                supportActionBar?.show()
                window.decorView.windowInsetsController?.show(WindowInsets.Type.systemBars())
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
    }
}

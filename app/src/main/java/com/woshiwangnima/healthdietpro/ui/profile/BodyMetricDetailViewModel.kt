package com.woshiwangnima.healthdietpro.ui.profile

import android.app.Application
import com.woshiwangnima.healthdietpro.common.ui.chart.BaseChartViewModel

internal class HeightDetailViewModel(application: Application) : BaseChartViewModel(
    application = application,
    chartBaseKey = "height_history",
)

internal class WeightDetailViewModel(application: Application) : BaseChartViewModel(
    application = application,
    chartBaseKey = "weight_history",
)

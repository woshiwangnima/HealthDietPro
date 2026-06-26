package com.woshiwangnima.healthdietpro.ui.profile.chart

import android.os.Bundle
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.base.BaseBackActivity
import com.woshiwangnima.healthdietpro.model.profile.BodyRecord
import com.woshiwangnima.healthdietpro.model.profile.DataPoint
import com.woshiwangnima.healthdietpro.model.unit.UnitCategory
import com.woshiwangnima.healthdietpro.util.UnitConverter
import com.woshiwangnima.healthdietpro.util.applySystemBarInsets
import java.time.LocalDate
import java.time.ZoneId

class HeightChartActivity : BaseBackActivity() {

    override fun getTitleText(): String = "身高历史"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val chartView = ChartView(this)
        chartView.applySystemBarInsets()
        setContentView(chartView)

        @Suppress("UNCHECKED_CAST")
        val records = (intent.getSerializableExtra(EXTRA_RECORDS, ArrayList::class.java) as? ArrayList<BodyRecord>) ?: emptyList()
        val unit = intent.getStringExtra(EXTRA_UNIT) ?: UnitCategory.DEFAULT_UNIT_LENGTH
        val dataPoints = parseRecords(records, UnitCategory.ID_LENGTH, unit)
        val series = ChartSeries(
            points = dataPoints, label = "测量值",
            color = resources.getColor(R.color.primary, null),
            lineStyle = LineStyle.LINEAR, lineType = LineType.SOLID,
            pointShape = PointShape.CIRCLE, pointFill = PointFill.FILLED
        )
        chartView.setSeries(listOf(series), unit)
    }

    private fun parseRecords(records: List<BodyRecord>, category: String, unitId: String): List<DataPoint> {
        val sorted = records.sortedBy { it.date }
        return sorted.map { record ->
            val converted = UnitConverter.fromBase(category, record.value, unitId)
            val localDate = LocalDate.parse(record.date)
            val ts = localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            DataPoint(timestamp = ts, value = converted, dateLabel = record.date.takeLast(5))
        }
    }

    companion object {
        const val EXTRA_RECORDS = "records"
        const val EXTRA_UNIT = "unit"
    }
}

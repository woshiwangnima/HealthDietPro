package com.woshiwangnima.healthdietpro.ui.profile.chart

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.model.profile.BodyRecord
import com.woshiwangnima.healthdietpro.model.profile.DataPoint
import com.woshiwangnima.healthdietpro.model.unit.UnitCategory
import com.woshiwangnima.healthdietpro.util.UnitConverter
import java.time.LocalDate
import java.time.ZoneId

class ChartFragment : Fragment() {

    private var records: List<BodyRecord> = emptyList()
    private var unitId: String = UnitCategory.DEFAULT_UNIT_LENGTH
    private var category: String = UnitCategory.ID_LENGTH
    private var isHeight: Boolean = true
    private var chartStateKey: String = ""

    companion object {
        private const val ARG_RECORDS = "records"
        private const val ARG_UNIT = "unit"
        private const val ARG_CATEGORY = "category"
        private const val ARG_IS_HEIGHT = "is_height"
        private const val ARG_CHART_STATE_KEY = "chart_state_key"

        fun newInstance(
            records: ArrayList<BodyRecord>, unit: String, category: String,
            isHeight: Boolean, chartStateKey: String = ""
        ): ChartFragment {
            val fragment = ChartFragment()
            val args = Bundle()
            args.putSerializable(ARG_RECORDS, records)
            args.putString(ARG_UNIT, unit)
            args.putString(ARG_CATEGORY, category)
            args.putBoolean(ARG_IS_HEIGHT, isHeight)
            args.putString(ARG_CHART_STATE_KEY, chartStateKey)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            @Suppress("UNCHECKED_CAST")
            records = (it.getSerializable(ARG_RECORDS, ArrayList::class.java) as? ArrayList<BodyRecord>) ?: emptyList()
            unitId = it.getString(ARG_UNIT, UnitCategory.DEFAULT_UNIT_LENGTH) ?: UnitCategory.DEFAULT_UNIT_LENGTH
            category = it.getString(ARG_CATEGORY, UnitCategory.ID_LENGTH) ?: UnitCategory.ID_LENGTH
            isHeight = it.getBoolean(ARG_IS_HEIGHT, true)
            chartStateKey = it.getString(ARG_CHART_STATE_KEY) ?: ""
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val chartView = ChartView(requireContext())
        chartView.id = View.generateViewId()

        if (chartStateKey.isNotEmpty()) chartView.setChartStateKey(chartStateKey)

        val dataPoints = if (records.isEmpty()) emptyList() else {
            val sorted = records.sortedBy { it.date }
            sorted.map { record ->
                val converted = UnitConverter.fromBase(category, record.value, unitId)
                val localDate = LocalDate.parse(record.date)
                val ts = localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                DataPoint(timestamp = ts, value = converted, dateLabel = record.date.takeLast(5))
            }
        }

        val series = ChartSeries(
            points = dataPoints,
            label = "测量值",
            color = resources.getColor(R.color.primary, null),
            lineStyle = LineStyle.LINEAR,
            lineType = LineType.SOLID,
            pointShape = PointShape.CIRCLE,
            pointFill = PointFill.FILLED
        )
        chartView.setSeries(listOf(series), unitId)

        return chartView
    }
}

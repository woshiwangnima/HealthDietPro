package com.woshiwangnima.healthdietpro.ui.profile.chart

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.model.profile.BodyRecord
import com.woshiwangnima.healthdietpro.util.UnitConverter

class ChartFragment : Fragment() {

    private var records: List<BodyRecord> = emptyList()
    private var unitId: String = "cm"
    private var category: String = "height"

    companion object {
        private const val ARG_RECORDS = "records"
        private const val ARG_UNIT = "unit"
        private const val ARG_CATEGORY = "category"

        fun newInstance(records: ArrayList<BodyRecord>, unit: String, category: String): ChartFragment {
            val fragment = ChartFragment()
            val args = Bundle()
            args.putSerializable(ARG_RECORDS, records)
            args.putString(ARG_UNIT, unit)
            args.putString(ARG_CATEGORY, category)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            @Suppress("UNCHECKED_CAST")
            records = (it.getSerializable(ARG_RECORDS) as? ArrayList<BodyRecord>) ?: emptyList()
            unitId = it.getString(ARG_UNIT, "cm") ?: "cm"
            category = it.getString(ARG_CATEGORY, "height") ?: "height"
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_chart, container, false)
        val chart = view.findViewById<LineChart>(R.id.chart)
        setupChart(chart)
        return view
    }

    private fun setupChart(chart: LineChart) {
        if (records.isEmpty()) {
            chart.setNoDataText("暂无数据")
            return
        }
        val sorted = records.sortedBy { it.date }
        val entries = sorted.mapIndexed { index, record ->
            val convertedValue = UnitConverter.fromBase(category, record.value, unitId)
            Entry(index.toFloat(), convertedValue)
        }
        val labels = sorted.map { it.date.takeLast(5) }

        val dataSet = LineDataSet(entries, "").apply {
            color = resources.getColor(R.color.primary, null)
            setCircleColor(resources.getColor(R.color.primary, null))
            lineWidth = 2f
            circleRadius = 4f
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }
        chart.apply {
            data = LineData(dataSet)
            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(labels)
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setDrawGridLines(false)
            }
            axisLeft.axisMinimum = 0f
            axisRight.isEnabled = false
            legend.isEnabled = false
            description.isEnabled = false
            animateX(1000)
            invalidate()
        }
    }
}

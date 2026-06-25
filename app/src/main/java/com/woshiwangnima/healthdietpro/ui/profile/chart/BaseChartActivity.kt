package com.woshiwangnima.healthdietpro.ui.profile.chart

import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.*
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.utils.MPPointF
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.highlight.Highlight
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.base.BaseBackActivity
import com.woshiwangnima.healthdietpro.databinding.ActivityBaseChartBinding
import com.woshiwangnima.healthdietpro.model.profile.BodyRecord
import com.woshiwangnima.healthdietpro.model.profile.DataPoint
import com.woshiwangnima.healthdietpro.util.UnitConverter
import com.woshiwangnima.healthdietpro.util.applySystemBarInsets
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class LineType { SOLID, DASHED, DOTTED }

abstract class BaseChartActivity : BaseBackActivity() {

    private lateinit var binding: ActivityBaseChartBinding
    private lateinit var chart: LineChart
    protected var dataPoints: List<DataPoint> = emptyList()
    private var filteredDataPoints: List<DataPoint> = emptyList()
    protected var unitLabel: String = ""

    private var currentRangeMillis: Long = Long.MAX_VALUE
    private var isFullscreen = false
    private var chartTypeReady = false
    private var timeRangeReady = false
    private var yAxisReady = false
    private var lastDragX = 0f
    private lateinit var crosshairView: ChartCrosshairView

    companion object {
        const val EXTRA_RECORDS = "records"
        const val EXTRA_UNIT = "unit"
        const val EXTRA_CATEGORY = "category"
    }

    abstract fun loadDataPoints(): List<DataPoint>
    abstract fun getUnitDisplay(): String
    open fun getTargetValue(): Float? = null
    open fun getSecondaryDataPoints(): List<DataPoint>? = null
    open fun getSecondaryLabel(): String? = null
    open fun getMainLineType(): LineType = LineType.SOLID
    open fun getTargetLineType(): LineType = LineType.DASHED

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            isFullscreen = savedInstanceState.getBoolean("isFullscreen", false)
        }

        binding = ActivityBaseChartBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.root.applySystemBarInsets()
        setupToolbar(binding.toolbar)

        dataPoints = loadDataPoints()
        filteredDataPoints = dataPoints
        unitLabel = getUnitDisplay()

        chart = binding.chart
        initChart()
        initChartTypeSpinner()
        initTimeRangeSpinner()
        initFullscreenButtons()
        initYAxisInputs()
        initChartTouch()
        initDragIndicator()
        initBackHandler()
        applyChartLayout()
        updateChart()
        initLegend()

        if (isFullscreen) {
            restoreFullscreenState()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("isFullscreen", isFullscreen)
    }

    protected fun parseRecords(records: List<BodyRecord>, category: String, unitId: String): List<DataPoint> {
        val sorted = records.sortedBy { it.date }
        return sorted.map { record ->
            val converted = UnitConverter.fromBase(category, record.value, unitId)
            val localDate = LocalDate.parse(record.date)
            val ts = localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            DataPoint(
                timestamp = ts,
                value = converted,
                dateLabel = record.date.takeLast(5)
            )
        }
    }

    private fun resolveThemeColor(attrRes: Int): Int {
        val ta = theme.obtainStyledAttributes(intArrayOf(attrRes))
        val color = ta.getColor(0, 0xFF000000.toInt())
        ta.recycle()
        return color
    }

    private fun initChart() {
        val onSurface = resolveThemeColor(com.google.android.material.R.attr.colorOnSurface)
        val surface = resolveThemeColor(com.google.android.material.R.attr.colorSurface)
        chart.apply {
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setDrawGridLines(false)
                textSize = 12f
                textColor = onSurface
            }
            axisLeft.isEnabled = false
            axisRight.apply {
                isEnabled = true
                setDrawGridLines(true)
                textSize = 10f
                textColor = onSurface
                gridColor = resolveThemeColor(com.google.android.material.R.attr.colorOutlineVariant)
            }
            legend.isEnabled = false
            description.isEnabled = false
            isDragEnabled = false
            setScaleEnabled(false)
            setPinchZoom(false)
            setExtraBottomOffset(36f)
            setBackgroundColor(surface)
            marker = null
            setDrawMarkers(false)
        }
    }

    private fun initChartTypeSpinner() {
        val adapter = ArrayAdapter.createFromResource(this, R.array.chart_type_options, android.R.layout.simple_spinner_item)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.chartTypeSpinner.adapter = adapter
        binding.chartTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!chartTypeReady) return
                updateChartStyle(LineStyle.fromSpinnerPosition(position))
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        chartTypeReady = true
    }

    private fun initTimeRangeSpinner() {
        rebuildTimeRangeSpinner()
        timeRangeReady = true
    }

    private fun rebuildTimeRangeSpinner() {
        val opts = getTimeRangeOptions()
        val labels = opts.map { it.label }.toTypedArray()
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, labels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.timeRangeSpinner.adapter = adapter
        timeRangeReady = false
        binding.timeRangeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!timeRangeReady) return
                currentRangeMillis = opts[position].millis
                updateTimeRange()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        timeRangeReady = true
        if (opts.isNotEmpty()) {
            currentRangeMillis = opts[0].millis
        }
    }

    private data class TimeRangeOption(val label: String, val millis: Long)

    private fun getTimeRangeOptions(): List<TimeRangeOption> {
        val allOptions = listOf(
            TimeRangeOption("\u5168\u90E8", Long.MAX_VALUE),
            TimeRangeOption("15\u5206\u949F", 15 * 60 * 1000L),
            TimeRangeOption("30\u5206\u949F", 30 * 60 * 1000L),
            TimeRangeOption("1\u5C0F\u65F6", 60 * 60 * 1000L),
            TimeRangeOption("6\u5C0F\u65F6", 6 * 60 * 60 * 1000L),
            TimeRangeOption("12\u5C0F\u65F6", 12 * 60 * 60 * 1000L),
            TimeRangeOption("1\u5929", 24 * 60 * 60 * 1000L),
            TimeRangeOption("1\u5468", 7 * 24 * 60 * 60 * 1000L),
            TimeRangeOption("1\u4E2A\u6708", 30 * 24 * 60 * 60 * 1000L)
        )
        if (dataPoints.isEmpty()) return allOptions.take(1)
        val totalSpan = dataPoints.maxOf { it.timestamp } - dataPoints.minOf { it.timestamp }
        return allOptions.filter { it.millis == Long.MAX_VALUE || it.millis <= totalSpan }
    }

    private fun updateChartStyle(style: LineStyle) {
        val data = chart.data ?: return
        val mode = mpModeFor(style)
        for (i in 0 until data.dataSetCount) {
            val ds = data.getDataSetByIndex(i) as? LineDataSet ?: continue
            ds.mode = mode
        }
        chart.invalidate()
    }

    private fun updateTimeRange() {
        if (dataPoints.isEmpty()) return
        filteredDataPoints = if (currentRangeMillis == Long.MAX_VALUE) {
            dataPoints
        } else {
            val latestTs = dataPoints.maxOf { it.timestamp }
            val cutoff = latestTs - currentRangeMillis
            dataPoints.filter { it.timestamp >= cutoff }
        }
        updateChart()
        updateDragIndicator()
        crosshairView.clear()
    }

    // --- Fullscreen ---

    private fun initFullscreenButtons() {
        val listener = View.OnClickListener { toggleFullscreen() }
        binding.btnFullscreen.setOnClickListener(listener)
        binding.btnFullscreenOverlay.setOnClickListener(listener)
    }

    private fun updateFullscreenButton() {
        val inFs = isFullscreen
        binding.btnFullscreen.text = if (inFs) "\u7F29\u653E" else "\u5168\u5C4F"
        binding.btnFullscreen.setIconResource(
            if (inFs) R.drawable.ic_fullscreen_exit else R.drawable.ic_fullscreen
        )
        binding.btnFullscreenOverlay.text = if (inFs) "\u7F29\u653E" else "\u5168\u5C4F"
        binding.btnFullscreenOverlay.setIconResource(
            if (inFs) R.drawable.ic_fullscreen_exit else R.drawable.ic_fullscreen
        )
    }

    private fun restoreFullscreenState() {
        window.decorView.windowInsetsController?.hide(
            android.view.WindowInsets.Type.systemBars()
        )
        binding.toolbar.visibility = View.GONE
        binding.controlsRow.visibility = View.GONE
        binding.yAxisRow.visibility = View.GONE
        binding.legendLayout.visibility = View.GONE
        binding.btnFullscreen.visibility = View.GONE
        binding.btnFullscreenOverlay.visibility = View.VISIBLE
        updateFullscreenButton()
        applyChartLayout()
        chart.invalidate()
    }

    private fun toggleFullscreen() {
        isFullscreen = !isFullscreen
        updateFullscreenButton()
        if (isFullscreen) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            window.decorView.windowInsetsController?.hide(
                android.view.WindowInsets.Type.systemBars()
            )
            binding.toolbar.visibility = View.GONE
            binding.controlsRow.visibility = View.GONE
            binding.yAxisRow.visibility = View.GONE
            binding.legendLayout.visibility = View.GONE
            binding.btnFullscreen.visibility = View.GONE
            binding.btnFullscreenOverlay.visibility = View.VISIBLE
        } else {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            window.decorView.windowInsetsController?.show(
                android.view.WindowInsets.Type.systemBars()
            )
            supportActionBar?.show()
            binding.toolbar.visibility = View.VISIBLE
            binding.controlsRow.visibility = View.VISIBLE
            binding.yAxisRow.visibility = View.VISIBLE
            binding.legendLayout.visibility = if (binding.legendLayout.childCount > 0) View.VISIBLE else View.GONE
            binding.btnFullscreen.visibility = View.VISIBLE
            binding.btnFullscreenOverlay.visibility = View.GONE
        }
        applyChartLayout()
        chart.invalidate()
    }

    private fun applyChartLayout() {
        val lp = binding.chartFrame.layoutParams as LinearLayout.LayoutParams
        if (isFullscreen) {
            lp.height = 0
            lp.weight = 1f
        } else {
            lp.weight = 0f
            lp.height = (resources.displayMetrics.heightPixels * 0.45).toInt()
        }
        binding.chartFrame.layoutParams = lp
    }

    private fun initBackHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isFullscreen) {
                    toggleFullscreen()
                } else {
                    finish()
                }
            }
        })
    }

    // --- Y-Axis ---

    private fun initYAxisInputs() {
        binding.yAxisRow.visibility = View.VISIBLE
        val watcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { applyYAxisRange() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
        binding.yMinInput.addTextChangedListener(watcher)
        binding.yMaxInput.addTextChangedListener(watcher)
        yAxisReady = true
    }

    private fun applyYAxisRange() {
        if (!yAxisReady || dataPoints.isEmpty()) return
        val dataMin = dataPoints.minOf { it.value }
        val dataMax = dataPoints.maxOf { it.value }
        val dataRange = dataMax - dataMin

        val minVal = parseAxisValue(binding.yMinInput.text.toString(), dataMin, dataRange, dataMin)
        val maxVal = parseAxisValue(binding.yMaxInput.text.toString(), dataMin, dataRange, dataMax)

        if (minVal < maxVal) {
            chart.axisRight.axisMinimum = minVal
            chart.axisRight.axisMaximum = maxVal
            chart.invalidate()
        }
    }

    private fun parseAxisValue(text: String, dataMin: Float, dataRange: Float, absoluteDefault: Float): Float {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return absoluteDefault
        val pct = trimmed.toFloatOrNull() ?: return absoluteDefault
        return dataMin + pct / 100f * dataRange
    }

    // --- Chart Touch + Crosshair ---

    private fun initChartTouch() {
        crosshairView = ChartCrosshairView(this)
        crosshairView.isClickable = false
        crosshairView.isFocusable = false
        val idx = if (binding.dragIndicatorContainer.id != -1)
            binding.chartFrame.indexOfChild(binding.dragIndicatorContainer) else -1
        if (idx >= 0) {
            binding.chartFrame.addView(crosshairView, idx)
        } else {
            binding.chartFrame.addView(crosshairView)
        }

        chart.setOnTouchListener { _, event ->
            if (filteredDataPoints.isEmpty()) return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    updateCrosshair(event.x, event.y)
                    true
                }
                else -> false
            }
        }
    }

    private fun updateCrosshair(px: Float, py: Float) {
        if (filteredDataPoints.size < 2) return
        val ds = chart.data?.getDataSetByIndex(0) as? LineDataSet ?: return
        val entries = ds.values
        if (entries.size < 2) return

        val transformer = chart.getTransformer(YAxis.AxisDependency.RIGHT)
        val touchVals = transformer.getValuesByTouchPoint(px, py)
        val chartX = touchVals.x.toFloat().coerceIn(entries.first().x, entries.last().x)

        val style = LineStyle.fromSpinnerPosition(binding.chartTypeSpinner.selectedItemPosition)
        val y = when (style) {
            LineStyle.LINEAR -> ChartSegmentMath.interpolateLinear(entries, chartX)
            LineStyle.STEPPED -> ChartSegmentMath.interpolateStepped(entries, chartX)
            LineStyle.CUBIC_BEZIER -> {
                val i = ChartSegmentMath.findSegmentIndex(entries, chartX)
                val cubicY = interpolateCubicBezier(entries, chartX.toDouble(), i)
                if (cubicY.isFinite()) cubicY.toFloat()
                else ChartSegmentMath.interpolateLinear(entries, chartX)
            }
        }
        crosshairView.setCrosshair(chartX, y, entries, filteredDataPoints)
    }

    private fun interpolateCubicBezier(entries: List<Entry>, x: Double, i: Int): Double {
        if (i < 0 || i + 1 >= entries.size) return Double.NaN
        val p0x = entries[i].x.toDouble()
        val p0y = entries[i].y.toDouble()
        val p3x = entries[i + 1].x.toDouble()
        val p3y = entries[i + 1].y.toDouble()
        val dx = p3x - p0x
        val dy = p3y - p0y
        val cpx1 = p0x + dx * 0.2
        val cpx2 = p3x - dx * 0.2
        val cIntensity = 0.2
        val prevY = if (i > 0) entries[i - 1].y.toDouble() else p0y - (p3y - p0y)
        val nextY = if (i + 2 < entries.size) entries[i + 2].y.toDouble() else p3y + (p3y - p0y)
        val cpy1 = p0y + (dy * cIntensity) + ((p3y - prevY) * cIntensity) * 0.5
        val cpy2 = p3y - (dy * cIntensity) - ((nextY - p0y) * cIntensity) * 0.5

        val t = if (dx != 0.0) (x - p0x) / dx else 0.0
        if (t < 0.0 || t > 1.0) return Double.NaN
        val ti = 1.0 - t
        return ti * ti * ti * p0y + 3 * ti * ti * t * cpy1 + 3 * ti * t * t * cpy2 + t * t * t * p3y
    }

    // --- Drag Indicator ---

    private fun updateDragIndicator() {
        val dragEnabled = currentRangeMillis != Long.MAX_VALUE && filteredDataPoints.isNotEmpty()
        binding.dragIndicatorContainer.visibility = if (dragEnabled) View.VISIBLE else View.GONE
        if (dragEnabled) {
            val color = resolveThemeColor(com.google.android.material.R.attr.colorSecondaryContainer)
            binding.dragIndicator.setBackgroundColor(Color.argb(100, Color.red(color), Color.green(color), Color.blue(color)))
            chart.post { updateDragArrows() }
        }
    }

    private fun updateDragArrows() {
        val visibleRange = chart.highestVisibleX - chart.lowestVisibleX
        if (visibleRange <= 0f || filteredDataPoints.isEmpty()) {
            binding.dragArrowLeft.visibility = View.GONE
            binding.dragArrowRight.visibility = View.GONE
            return
        }
        val lastIndex = (filteredDataPoints.size - 1).toFloat()
        val tolerance = 0.5f
        binding.dragArrowLeft.visibility = if (chart.lowestVisibleX > tolerance) View.VISIBLE else View.GONE
        binding.dragArrowRight.visibility = if (chart.highestVisibleX < lastIndex - tolerance) View.VISIBLE else View.GONE
    }

    private fun initDragIndicator() {
        binding.dragIndicator.setOnTouchListener { _, event ->
            if (currentRangeMillis == Long.MAX_VALUE || filteredDataPoints.isEmpty()) return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastDragX = event.x
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.x - lastDragX
                    lastDragX = event.x
                    val visibleRange = chart.highestVisibleX - chart.lowestVisibleX
                    if (visibleRange <= 0f) return@setOnTouchListener true
                    val pxPerEntry = binding.dragIndicatorContainer.width / visibleRange
                    val deltaEntry = -(deltaX / pxPerEntry)
                    val maxStart = (filteredDataPoints.size - visibleRange.toInt()).coerceAtLeast(0)
                    val newLow = (chart.lowestVisibleX + deltaEntry).coerceIn(0f, maxStart.toFloat())
                    chart.moveViewToX(newLow)
                    chart.post { updateDragArrows() }
                    true
                }
                else -> false
            }
        }

        binding.dragArrowLeft.setOnClickListener {
            val visibleRange = chart.highestVisibleX - chart.lowestVisibleX
            if (visibleRange <= 0f) return@setOnClickListener
            val step = (visibleRange * 0.3f).coerceAtLeast(1f)
            val maxStart = (filteredDataPoints.size - visibleRange.toInt()).coerceAtLeast(0)
            val newLow = (chart.lowestVisibleX - step).coerceIn(0f, maxStart.toFloat())
            chart.moveViewToX(newLow)
            chart.post { updateDragArrows() }
        }

        binding.dragArrowRight.setOnClickListener {
            val visibleRange = chart.highestVisibleX - chart.lowestVisibleX
            if (visibleRange <= 0f) return@setOnClickListener
            val step = (visibleRange * 0.3f).coerceAtLeast(1f)
            val maxStart = (filteredDataPoints.size - visibleRange.toInt()).coerceAtLeast(0)
            val newLow = (chart.lowestVisibleX + step).coerceIn(0f, maxStart.toFloat())
            chart.moveViewToX(newLow)
            chart.post { updateDragArrows() }
        }
    }

    // --- Legend ---

    private fun initLegend() {
        val legendLayout = binding.legendLayout
        legendLayout.removeAllViews()
        legendLayout.addView(createLegendItem(R.color.primary, getMainLineType(), "\u6D4B\u91CF\u503C"))

        getTargetValue()?.let {
            legendLayout.addView(createLegendItem(R.color.secondary, getTargetLineType(), "\u76EE\u6807\u503C"))
        }

        legendLayout.visibility = if (legendLayout.childCount > 0 && !isFullscreen) View.VISIBLE else View.GONE
    }

    private fun createLegendItem(colorRes: Int, lineType: LineType, text: String): LinearLayout {
        val lineView = LegendLineView(this, resources.getColor(colorRes, null), lineType)
        val lp = LinearLayout.LayoutParams(
            (32 * resources.displayMetrics.density).toInt(),
            (8 * resources.displayMetrics.density).toInt()
        )
        lp.setMargins(0, 0, 4, 0)
        lp.gravity = Gravity.CENTER_VERTICAL
        lineView.layoutParams = lp

        val tv = TextView(this).apply {
            this.text = text
            textSize = 12f
            setTextColor(resources.getColor(R.color.on_surface, null))
        }

        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val containerLp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            containerLp.setMargins(8, 0, 8, 0)
            layoutParams = containerLp
            addView(lineView)
            addView(tv)
        }
    }

    // --- Chart Data ---

    private fun updateChart() {
        if (filteredDataPoints.isEmpty()) {
            if (dataPoints.isEmpty()) {
                chart.setNoDataText("\u6682\u65E0\u6570\u636E")
            } else {
                chart.setNoDataText("\u8BE5\u65F6\u95F4\u8303\u56F4\u65E0\u6570\u636E")
            }
            chart.data = null
            chart.invalidate()
            return
        }

        val entries = filteredDataPoints.map { dp ->
            Entry(dp.timestamp.toFloat(), dp.value)
        }
        val labelsByTimestamp: Map<Long, String> =
            filteredDataPoints.associate { it.timestamp to it.dateLabel }

        val style = LineStyle.fromSpinnerPosition(binding.chartTypeSpinner.selectedItemPosition)

        val dataSet = LineDataSet(entries, "\u6D4B\u91CF\u503C").apply {
            color = resources.getColor(R.color.primary, null)
            setCircleColor(resources.getColor(R.color.primary, null))
            lineWidth = 2f
            circleRadius = 3f
            setDrawValues(false)
            setDrawCircleHole(false)
            mode = mpModeFor(style)
            applyLineType(getMainLineType())
        }

        val lineData = LineData(dataSet)

        getTargetValue()?.let { target ->
            val targetEntries = entries.map { Entry(it.x, target) }
            val targetSet = LineDataSet(targetEntries, "\u76EE\u6807\u503C").apply {
                color = resources.getColor(R.color.secondary, null)
                setCircleColor(resources.getColor(R.color.secondary, null))
                lineWidth = 1.5f
                circleRadius = 0f
                setDrawCircleHole(false)
                setDrawCircles(false)
                setDrawValues(false)
                mode = LineDataSet.Mode.LINEAR
                applyLineType(getTargetLineType())
            }
            lineData.addDataSet(targetSet)
        }

        chart.apply {
            data = lineData
            xAxis.valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                override fun getFormattedValue(value: Float): String =
                    labelsByTimestamp[value.toLong()] ?: ""
            }
            xAxis.setLabelCount(5, false)
            axisRight.axisMinimum = 0f
            fitScreen()
            invalidate()
        }
        applyYAxisRange()
    }

    private fun mpModeFor(style: LineStyle): LineDataSet.Mode = when (style) {
        LineStyle.LINEAR -> LineDataSet.Mode.LINEAR
        LineStyle.CUBIC_BEZIER -> LineDataSet.Mode.CUBIC_BEZIER
        LineStyle.STEPPED -> LineDataSet.Mode.STEPPED
    }

    private fun LineDataSet.applyLineType(type: LineType) {
        when (type) {
            LineType.SOLID -> enableDashedLine(0f, 0f, 0f)
            LineType.DASHED -> enableDashedLine(10f, 5f, 0f)
            LineType.DOTTED -> enableDashedLine(3f, 5f, 0f)
        }
    }

    // --- Crosshair Overlay View ---

    private inner class ChartCrosshairView(context: Context) : View(context) {
        private var chartX = Float.NaN
        private var chartY = Float.NaN
        private var hasCrosshair = false
        private var dateText = ""
        private var valueText = ""

        private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = resolveThemeColor(com.google.android.material.R.attr.colorOnSurface)
            strokeWidth = 1.5f
            style = Paint.Style.STROKE
            pathEffect = DashPathEffect(floatArrayOf(6f, 4f), 0f)
        }

        private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = resources.getColor(R.color.primary, null)
            style = Paint.Style.FILL
        }

        private val bubbleBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            val bg = resolveThemeColor(com.google.android.material.R.attr.colorSurface)
            color = Color.argb(230, Color.red(bg), Color.green(bg), Color.blue(bg))
            style = Paint.Style.FILL
        }

        private val bubbleBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = resolveThemeColor(com.google.android.material.R.attr.colorOutlineVariant)
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = resolveThemeColor(com.google.android.material.R.attr.colorOnSurface)
            textSize = 11f * resources.displayMetrics.density
        }

        private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = resolveThemeColor(com.google.android.material.R.attr.colorOnSurfaceVariant)
            textSize = 10f * resources.displayMetrics.density
        }

        fun setCrosshair(x: Float, y: Float, entries: List<Entry>, dps: List<DataPoint>) {
            chartX = x
            chartY = y
            hasCrosshair = true

            val i = x.toInt().coerceIn(0, dps.size - 2)
            val t = x - i
            val dp = dps[i.coerceAtMost(dps.size - 1)]
            val dpNext = dps[(i + 1).coerceAtMost(dps.size - 1)]
            val ts = dp.timestamp + ((dpNext.timestamp - dp.timestamp) * t).toLong()
            val instant = Instant.ofEpochMilli(ts)
            val localDt = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
            dateText = localDt.format(dateFormatter)
            valueText = String.format("%.2f %s", y, unitLabel)
            invalidate()
        }

        fun clear() {
            hasCrosshair = false
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            if (!hasCrosshair || chartX.isNaN() || filteredDataPoints.size < 2) return

            val transformer = chart.getTransformer(YAxis.AxisDependency.RIGHT)
            val pixel = transformer.getPixelForValues(chartX, chartY)
            val px = pixel.x.toFloat()
            val py = pixel.y.toFloat()

            val vph = chart.viewPortHandler
            val contentLeft = vph.contentLeft()
            val contentTop = vph.contentTop()
            val contentRight = vph.contentRight()
            val contentBottom = vph.contentBottom()

            if (px < contentLeft || px > contentRight) return

            // vertical dashed line
            canvas.drawLine(px, contentTop, px, contentBottom, linePaint)

            // circle at intersection
            canvas.drawCircle(px, py, 5f, circlePaint)

            // info bubble
            drawInfoBubble(canvas, px, py, contentLeft, contentTop, contentRight, contentBottom)
        }

        private fun drawInfoBubble(
            canvas: Canvas, px: Float, py: Float,
            cl: Float, ct: Float, cr: Float, cb: Float
        ) {
            val topLine = "\u65F6\u95F4: $dateText"
            val bottomLine = "\u503C: $valueText"

            val textW1 = textPaint.measureText(topLine)
            val textW2 = textPaint.measureText(bottomLine)
            val maxTextW = maxOf(textW1, textW2)

            val fontMetrics = textPaint.fontMetrics
            val lineH = fontMetrics.descent - fontMetrics.ascent
            val labelH = labelPaint.fontMetrics.descent - labelPaint.fontMetrics.ascent
            val pad = 8f * resources.displayMetrics.density
            val bubbleW = maxTextW + pad * 2 + 4f
            val bubbleH = lineH + labelH + pad * 2 + 4f

            val defaultOffsetX = 12f * resources.displayMetrics.density
            val defaultOffsetY = -12f * resources.displayMetrics.density
            var bx = px + defaultOffsetX
            var by = py + defaultOffsetY - bubbleH

            if (bx + bubbleW > cr - 4f) {
                bx = px - defaultOffsetX - bubbleW
            }
            if (by < ct + 4f) {
                by = py - defaultOffsetY + 4f
            }
            if (bx < cl + 4f) {
                bx = cl + 4f
            }
            if (by + bubbleH > cb - 4f) {
                by = cb - 4f - bubbleH
            }

            val r = 6f * resources.displayMetrics.density
            val bubbleRect = RectF(bx, by, bx + bubbleW, by + bubbleH)
            canvas.drawRoundRect(bubbleRect, r, r, bubbleBgPaint)
            canvas.drawRoundRect(bubbleRect, r, r, bubbleBorderPaint)

            val textX = bx + pad
            val firstY = by + pad - fontMetrics.ascent + 2f
            val secondY = firstY + lineH
            canvas.drawText(topLine, textX, firstY, textPaint)
            canvas.drawText(bottomLine, textX, secondY, textPaint)
        }

        private val dateFormatter: DateTimeFormatter =
            DateTimeFormatter.ofPattern("MM-dd HH:mm")
    }

    private class LegendLineView(
        context: Context,
        private val lineColor: Int,
        private val lineType: LineType
    ) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = lineColor
            strokeWidth = 3f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            pathEffect = when (lineType) {
                LineType.SOLID -> null
                LineType.DASHED -> DashPathEffect(floatArrayOf(8f, 5f), 0f)
                LineType.DOTTED -> DashPathEffect(floatArrayOf(3f, 5f), 0f)
            }
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val midY = height / 2f
            val pad = 2f * resources.displayMetrics.density
            canvas.drawLine(pad, midY.toFloat(), (width - pad).toFloat(), midY.toFloat(), paint)
        }
    }
}

package com.woshiwangnima.healthdietpro.ui.profile.chart

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.*
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.model.prefs.AppPrefs

class ChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val chartCanvas: ChartCanvas
    private val chartTypeSpinner: Spinner
    private val timeRangeSpinner: Spinner
    private val labelIntervalSpinner: Spinner
    private val btnFullscreen: MaterialButton
    private val yMinInput: EditText
    private val yMaxInput: EditText
    private val controlsRow: LinearLayout
    private val chartFrame: FrameLayout
    private val dragIndicatorContainer: FrameLayout
    private val progressBarContainer: FrameLayout
    private val progressBarThumb: View
    private val dragIndicator: View
    private val dragArrowLeft: ImageButton
    private val dragArrowRight: ImageButton
    private val legendScrollView: HorizontalScrollView
    private val legendLayout: LinearLayout
    private val chartTitle: TextView

    private var seriesList: List<ChartSeries> = emptyList()
    private var unitLabel: String = ""
    private var lineStyle: LineStyle = LineStyle.LINEAR
    private var chartTypeReady = false
    private var timeRangeReady = false
    private var yAxisReady = false

    private var lastDragX = 0f

    private val timelineHandler = Handler(Looper.getMainLooper())
    private var isTimelineVisible = false
    private val timelineHideRunnable = Runnable { hideTimeline() }
    private val timelineShowDurationMs = 4000L

    init {
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(R.layout.view_chart, this, true)

        chartCanvas = findViewById(R.id.chartCanvas)
        chartTypeSpinner = findViewById(R.id.chartTypeSpinner)
        timeRangeSpinner = findViewById(R.id.timeRangeSpinner)
        btnFullscreen = findViewById(R.id.btnFullscreen)
        yMinInput = findViewById(R.id.yMinInput)
        yMaxInput = findViewById(R.id.yMaxInput)
        controlsRow = findViewById(R.id.controlsRow)
        chartFrame = findViewById(R.id.chartFrame)
        dragIndicatorContainer = findViewById(R.id.dragIndicatorContainer)
        progressBarContainer = findViewById(R.id.progressBarContainer)
        progressBarThumb = findViewById(R.id.progressBarThumb)
        dragIndicator = findViewById(R.id.dragIndicator)
        dragArrowLeft = findViewById(R.id.dragArrowLeft)
        dragArrowRight = findViewById(R.id.dragArrowRight)
        legendScrollView = findViewById(R.id.legendScrollView)
        legendLayout = findViewById(R.id.legendLayout)
        labelIntervalSpinner = findViewById(R.id.labelIntervalSpinner)
        chartTitle = findViewById(R.id.chartTitle)

        initChartTypeSpinner()
        initTimeRangeSpinner()
        initLabelIntervalSpinner()
        initFullscreenButtons()
        initYAxisInputs()
        initDragIndicator()
        setupTimeline()
    }

    fun setSeries(series: List<ChartSeries>, unitLabel: String) {
        this.seriesList = series
        this.unitLabel = unitLabel
        chartCanvas.series = series
        chartCanvas.unitLabel = unitLabel
        chartCanvas.clearCrosshair()
        val allPoints = series.flatMap { it.points }
        if (allPoints.isEmpty()) {
            chartCanvas.invalidate()
            return
        }
        val totalSpan = allPoints.maxOf { it.timestamp } - allPoints.minOf { it.timestamp }
        // Only set defaults if no pending state or pending state says "全部" (Long.MAX_VALUE)
        val pendingTimeRange = if (chartStateKey.isNotEmpty()) AppPrefs.getChartTimeRange(context, chartStateKey) else Long.MAX_VALUE
        if (pendingTimeRange != Long.MAX_VALUE) {
            chartCanvas.visibleRangeMs = pendingTimeRange
            val latestTs = allPoints.maxOf { it.timestamp }
            chartCanvas.windowStartMs = (latestTs - pendingTimeRange).coerceAtLeast(allPoints.minOf { it.timestamp })
        } else {
            chartCanvas.visibleRangeMs = totalSpan
            chartCanvas.windowStartMs = allPoints.minOf { it.timestamp }
        }
        rebuildTimeRangeSpinner()
        restoreChartState()
        applyYAxisRange()
        updateDragIndicator()
        updateLegend()
        chartCanvas.invalidate()
    }

    fun setLineStyle(style: LineStyle) {
        lineStyle = style
        seriesList = seriesList.map { it.copy(lineStyle = style) }
        chartCanvas.series = seriesList
        chartCanvas.invalidate()
    }

    fun getLineStyle(): LineStyle = lineStyle

    fun setVisibleRange(millis: Long) {
        val allPoints = seriesList.flatMap { it.points }
        if (allPoints.isEmpty()) return
        val latestTs = allPoints.maxOf { it.timestamp }
        chartCanvas.visibleRangeMs = millis
        chartCanvas.windowStartMs = (latestTs - millis).coerceAtLeast(allPoints.minOf { it.timestamp })
        chartCanvas.invalidate()
        updateDragIndicator()
        updateDragArrows()
        updateTimelineBar()
    }

    fun getVisibleRange(): Long = chartCanvas.visibleRangeMs

    fun setLabelInterval(millis: Long) {
        chartCanvas.labelIntervalMs = millis
        chartCanvas.invalidate()
    }

    fun getLabelInterval(): Long = chartCanvas.labelIntervalMs

    fun setYAxisRange(minPct: Float, maxPct: Float) {
        chartCanvas.yMinPct = minPct
        chartCanvas.yMaxPct = maxPct
        yMinInput.setText("%.0f".format(minPct))
        yMaxInput.setText("%.0f".format(maxPct))
        chartCanvas.invalidate()
    }

    fun getYAxisMinPct(): Float = chartCanvas.yMinPct
    fun getYAxisMaxPct(): Float = chartCanvas.yMaxPct

    fun setYAxisBands(bands: List<YAxisBand>) {
        chartCanvas.yAxisBands = bands
        chartCanvas.invalidate()
    }

    fun setChartTitle(title: String) {
        chartTitle.text = title
        chartTitle.visibility = View.VISIBLE
    }

    fun setFullscreenMode(enabled: Boolean) {
        if (enabled) {
            controlsRow.visibility = View.GONE
            legendScrollView.visibility = View.GONE
            btnFullscreen.visibility = View.GONE
            val lp = chartFrame.layoutParams as LinearLayout.LayoutParams
            lp.weight = 1f; lp.height = 0
            chartFrame.layoutParams = lp
        }
    }

    fun buildFullscreenData(title: String): ChartFullscreenData {
        return ChartFullscreenData(
            series = seriesList, unitLabel = unitLabel,
            visibleRangeMs = chartCanvas.visibleRangeMs,
            windowStartMs = chartCanvas.windowStartMs,
            yMinPct = chartCanvas.yMinPct, yMaxPct = chartCanvas.yMaxPct,
            labelIntervalMs = chartCanvas.labelIntervalMs,
            yAxisBands = chartCanvas.yAxisBands,
            chartTitle = title, chartStateKey = chartStateKey
        )
    }

    fun invalidateChart() { chartCanvas.invalidate() }

    private var chartStateKey: String = ""
    private var timeRangeOptions: List<TimeRangeOption> = emptyList()
    private var labelIntervalValues: MutableList<Long>? = mutableListOf(0L)

    fun setChartStateKey(key: String) {
        chartStateKey = key
    }

    private fun restoreChartState() {
        if (chartStateKey.isEmpty()) return
        val ctx = context

        // Chart style spinner
        val stylePos = AppPrefs.getChartStyle(ctx, chartStateKey)
        chartTypeSpinner.setSelection(stylePos.coerceIn(0, chartTypeSpinner.adapter.count - 1))

        // Time range spinner: find option matching saved millis
        val savedRange = AppPrefs.getChartTimeRange(ctx, chartStateKey)
        val trIdx = timeRangeOptions.indexOfFirst { it.millis == savedRange }
        if (trIdx >= 0) timeRangeSpinner.setSelection(trIdx)

        // Label interval spinner: find option matching saved millis
        val savedInterval = AppPrefs.getChartLabelInterval(ctx, chartStateKey)
        val liVals = labelIntervalValues
        if (liVals != null) {
            val liIdx = liVals.indexOfFirst { it == savedInterval }
            if (liIdx >= 0) labelIntervalSpinner.setSelection(liIdx)
        }
        if (savedInterval > 0L) chartCanvas.labelIntervalMs = savedInterval

        val yMin = AppPrefs.getChartYMin(ctx, chartStateKey)
        val yMax = AppPrefs.getChartYMax(ctx, chartStateKey)
        setYAxisRange(yMin, yMax)
    }

    private fun saveChartState() {
        if (chartStateKey.isEmpty()) return
        val ctx = context
        AppPrefs.setChartStyle(ctx, chartStateKey, chartTypeSpinner.selectedItemPosition)
        // Save the selected option's millis, not the actual visibleRangeMs (for "全部" use Long.MAX_VALUE)
        val trSelIdx = timeRangeSpinner.selectedItemPosition
        val trMillis = if (trSelIdx in timeRangeOptions.indices) timeRangeOptions[trSelIdx].millis else Long.MAX_VALUE
        AppPrefs.setChartTimeRange(ctx, chartStateKey, trMillis)
        AppPrefs.setChartLabelInterval(ctx, chartStateKey, chartCanvas.labelIntervalMs)
        AppPrefs.setChartYMin(ctx, chartStateKey, chartCanvas.yMinPct)
        AppPrefs.setChartYMax(ctx, chartStateKey, chartCanvas.yMaxPct)
    }

    private fun initChartTypeSpinner() {
        val adapter = ArrayAdapter.createFromResource(context, R.array.chart_type_options, android.R.layout.simple_spinner_item)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        chartTypeSpinner.adapter = adapter
        chartTypeSpinner.setSelection(0)
        chartTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!chartTypeReady) return
                chartCanvas.clearCrosshair()
                lineStyle = LineStyle.fromSpinnerPosition(position)
                seriesList = seriesList.map { it.copy(lineStyle = lineStyle) }
                chartCanvas.series = seriesList
                saveChartState()
                chartCanvas.invalidate()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        chartTypeReady = true
    }

    private data class TimeRangeOption(val label: String, val millis: Long)

    private fun getTimeRangeOptions(): List<TimeRangeOption> {
        val allPoints = seriesList.flatMap { it.points }
        val allOptions = listOf(
            TimeRangeOption("全部", Long.MAX_VALUE),
            TimeRangeOption("1天", 24 * 60 * 60 * 1000L),
            TimeRangeOption("1周", 7 * 24 * 60 * 60 * 1000L),
            TimeRangeOption("1个月", 30 * 24 * 60 * 60 * 1000L),
            TimeRangeOption("3个月", 90 * 24 * 60 * 60 * 1000L),
            TimeRangeOption("6个月", 180 * 24 * 60 * 60 * 1000L),
            TimeRangeOption("1年", 365 * 24 * 60 * 60 * 1000L)
        )
        if (allPoints.isEmpty()) return allOptions.take(1)
        val totalSpan = allPoints.maxOf { it.timestamp } - allPoints.minOf { it.timestamp }
        val minInterval = if (allPoints.size >= 2) {
            allPoints.zipWithNext { a, b -> b.timestamp - a.timestamp }.minOrNull() ?: 0L
        } else 0L
        val withinSpan = allOptions.filter { opt ->
            opt.millis != Long.MAX_VALUE && opt.millis <= totalSpan && opt.millis >= minInterval
        }
        val nextAbove = allOptions.filter { it.millis != Long.MAX_VALUE && it.millis > totalSpan }
            .minByOrNull { it.millis }
        val result = withinSpan.toMutableList()
        if (nextAbove != null && nextAbove !in result) result.add(nextAbove)
        result.add(TimeRangeOption("全部", Long.MAX_VALUE))
        timeRangeOptions = result
        return result
    }

    private fun rebuildTimeRangeSpinner() {
        val opts = getTimeRangeOptions()
        val labels = opts.map { it.label }.toTypedArray()
        val adapter = ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, labels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        timeRangeSpinner.adapter = adapter
        timeRangeReady = false
        timeRangeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!timeRangeReady || position >= opts.size) return
                chartCanvas.clearCrosshair()
                val rangeMs = opts[position].millis
                val allPoints = seriesList.flatMap { it.points }
                if (allPoints.isEmpty()) return
                if (rangeMs == Long.MAX_VALUE) {
                    chartCanvas.visibleRangeMs = allPoints.maxOf { it.timestamp } - allPoints.minOf { it.timestamp }
                    chartCanvas.windowStartMs = allPoints.minOf { it.timestamp }
                } else {
                    val latestTs = allPoints.maxOf { it.timestamp }
                    chartCanvas.visibleRangeMs = rangeMs
                    chartCanvas.windowStartMs = (latestTs - rangeMs).coerceAtLeast(allPoints.minOf { it.timestamp })
                }
                chartCanvas.invalidate()
                saveChartState()
                updateDragIndicator()
                showTimeline()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        timeRangeReady = true
    }

    private fun initTimeRangeSpinner() {
        rebuildTimeRangeSpinner()
        timeRangeReady = true
    }

    private fun initLabelIntervalSpinner() {
        val baseOpts = listOf(
            "自动" to 0L,
            "1分钟" to 60_000L,
            "5分钟" to 5 * 60_000L,
            "30分钟" to 30 * 60_000L,
            "1小时" to 3_600_000L,
            "2小时" to 2 * 3_600_000L,
            "6小时" to 6 * 3_600_000L,
            "1天" to 86_400_000L,
            "3天" to 3 * 86_400_000L,
            "1周" to 7 * 86_400_000L,
            "1月" to 30 * 86_400_000L
        )
        val filtered = baseOpts.filter { it.second == 0L || it.second * 2 <= chartCanvas.visibleRangeMs } + baseOpts.filter { it.second >= chartCanvas.visibleRangeMs }.take(1)
        val liVals = mutableListOf<Long>()
        labelIntervalValues = liVals
        val labels = mutableListOf<String>()
        for ((label, millis) in filtered.distinctBy { it.second }) {
            labels.add(label)
            liVals.add(millis)
        }
        val adapter = ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, labels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        labelIntervalSpinner.adapter = adapter
        labelIntervalSpinner.setSelection(0)
        labelIntervalSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val vals = labelIntervalValues ?: return
                chartCanvas.labelIntervalMs = vals[position.coerceIn(0, vals.size - 1)]
                saveChartState()
                chartCanvas.invalidate()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun initFullscreenButtons() {
        btnFullscreen.setOnClickListener {
            val data = buildFullscreenData(chartTitle.text.toString())
            ChartFullscreenActivity.launch(context, data)
        }
    }

    private fun initYAxisInputs() {
        val watcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                chartCanvas.clearCrosshair()
                applyYAxisRange()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
        yMinInput.addTextChangedListener(watcher)
        yMaxInput.addTextChangedListener(watcher)

        // Vertical swipe to adjust percentage values
        addSwipeAdjust(yMinInput, -200f, 200f)
        addSwipeAdjust(yMaxInput, -200f, 200f)

        yAxisReady = true
    }

    private fun addSwipeAdjust(editText: EditText, minVal: Float, maxVal: Float) {
        var startY = 0f
        var startPct = 0f
        var tracking = false
        editText.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (event.pointerCount == 1) {
                        startY = event.y
                        startPct = editText.text.toString().trim().toFloatOrNull() ?: 0f
                        tracking = true
                    }
                    false // let EditText handle the event for text input too
                }
                MotionEvent.ACTION_MOVE -> {
                    if (tracking && event.pointerCount == 1) {
                        val dy = startY - event.y
                        val sensitivity = 0.5f
                        val newPct = (startPct + dy * sensitivity).coerceIn(minVal, maxVal)
                        val rounded = Math.round(newPct * 10f) / 10f
                        editText.setText("%.1f".format(rounded))
                        editText.setSelection(editText.text.length)
                        true
                    } else false
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    tracking = false
                    false // let EditText handle cursor positioning
                }
                else -> false
            }
        }
    }

    private fun applyYAxisRange() {
        if (!yAxisReady) return
        val minPct = yMinInput.text.toString().trim().toFloatOrNull() ?: return
        val maxPct = yMaxInput.text.toString().trim().toFloatOrNull() ?: return
        chartCanvas.yMinPct = minPct
        chartCanvas.yMaxPct = maxPct
        saveChartState()
        chartCanvas.invalidate()
    }

    private fun initDragIndicator() {
        dragIndicator.setOnTouchListener { _, event ->
            val allPoints = seriesList.flatMap { it.points }
            if (allPoints.size < 2) return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastDragX = event.x
                    showTimeline()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.x - lastDragX
                    lastDragX = event.x
                    val visibleRange = chartCanvas.visibleRangeMs.toFloat()
                    if (visibleRange <= 0f) return@setOnTouchListener true
                    val stripWidth = dragIndicatorContainer.width.toFloat()
                    if (stripWidth <= 0f) return@setOnTouchListener true
                    val dataMin = allPoints.minOf { it.timestamp }
                    val dataMax = allPoints.maxOf { it.timestamp }
                    val maxDragRange = (dataMax - dataMin - visibleRange.toLong()).toFloat().coerceAtLeast(0f)
                    val pxPerMs = stripWidth / visibleRange
                    if (pxPerMs == 0f) return@setOnTouchListener true
                    val deltaMs = -(deltaX / pxPerMs)
                    val newOffset = ((chartCanvas.windowStartMs - dataMin).toFloat() + deltaMs).coerceIn(0f, maxDragRange)
                    chartCanvas.windowStartMs = dataMin + newOffset.toLong()
                    chartCanvas.invalidate()
                    post { updateDragArrows(); updateTimelineBar() }
                    true
                }
                MotionEvent.ACTION_UP -> { showTimeline(); true }
                else -> false
            }
        }
        dragArrowLeft.setOnClickListener {
            showTimeline()
            val allPoints = seriesList.flatMap { it.points }
            if (allPoints.isEmpty()) return@setOnClickListener
            val dataMin = allPoints.minOf { it.timestamp }
            val step = (chartCanvas.visibleRangeMs * 0.3f).toLong()
            chartCanvas.windowStartMs = (chartCanvas.windowStartMs - step).coerceAtLeast(dataMin)
            chartCanvas.invalidate()
            post { updateDragArrows(); updateTimelineBar() }
        }
        dragArrowRight.setOnClickListener {
            showTimeline()
            val allPoints = seriesList.flatMap { it.points }
            if (allPoints.isEmpty()) return@setOnClickListener
            val dataMax = allPoints.maxOf { it.timestamp }
            val step = (chartCanvas.visibleRangeMs * 0.3f).toLong()
            val maxStart = (dataMax - chartCanvas.visibleRangeMs).coerceAtLeast(allPoints.minOf { it.timestamp })
            chartCanvas.windowStartMs = (chartCanvas.windowStartMs + step).coerceAtMost(maxStart)
            chartCanvas.invalidate()
            post { updateDragArrows(); updateTimelineBar() }
        }
    }

    private fun updateDragIndicator() {
        val allPoints = seriesList.flatMap { it.points }
        val totalSpan = allPoints.maxOfOrNull { it.timestamp }?.let { it - (allPoints.minOfOrNull { it.timestamp } ?: it) } ?: 0L
        val dragEnabled = totalSpan > 0L && allPoints.size >= 2 && chartCanvas.visibleRangeMs < totalSpan
        dragIndicatorContainer.visibility = if (dragEnabled) View.VISIBLE else View.GONE
        progressBarContainer.visibility = View.GONE
        isTimelineVisible = false
        if (dragEnabled) {
            val color = resolveColor(com.google.android.material.R.attr.colorSecondaryContainer)
            dragIndicator.setBackgroundColor(Color.argb(100, Color.red(color), Color.green(color), Color.blue(color)))
            post { updateDragArrows() }
        }
    }

    private fun updateDragArrows() {
        val allPoints = seriesList.flatMap { it.points }
        if (allPoints.size < 2) {
            setArrowEnabled(dragArrowLeft, false)
            setArrowEnabled(dragArrowRight, false)
            return
        }
        val dataMin = allPoints.minOf { it.timestamp }
        val dataMax = allPoints.maxOf { it.timestamp }
        val tolerance = (chartCanvas.visibleRangeMs * 0.01f).toLong()
        val canScrollLeft = chartCanvas.windowStartMs > dataMin + tolerance
        val canScrollRight = chartCanvas.windowStartMs + chartCanvas.visibleRangeMs < dataMax - tolerance
        setArrowEnabled(dragArrowLeft, canScrollLeft)
        setArrowEnabled(dragArrowRight, canScrollRight)
    }

    private fun resolveColor(attrRes: Int): Int {
        val ta = context.theme.obtainStyledAttributes(intArrayOf(attrRes))
        val color = ta.getColor(0, 0xFF000000.toInt())
        ta.recycle()
        return color
    }

    private fun setArrowEnabled(btn: View, enabled: Boolean) {
        btn.isEnabled = enabled
        btn.alpha = if (enabled) 1f else 0.3f
    }

    private fun setupTimeline() {
        progressBarContainer.visibility = View.GONE
        isTimelineVisible = false
    }

    private fun showTimeline() {
        val allPoints = seriesList.flatMap { it.points }
        val totalSpan = allPoints.maxOfOrNull { it.timestamp }?.let { it - (allPoints.minOfOrNull { it.timestamp } ?: it) } ?: 0L
        val dragEnabled = totalSpan > 0L && allPoints.size >= 2 && chartCanvas.visibleRangeMs < totalSpan
        if (!dragEnabled) return
        timelineHandler.removeCallbacks(timelineHideRunnable)
        if (!isTimelineVisible) {
            isTimelineVisible = true
            progressBarContainer.visibility = View.VISIBLE
            val fadeIn = AlphaAnimation(0f, 1f).apply { duration = 200 }
            progressBarContainer.startAnimation(fadeIn)
        }
        timelineHandler.postDelayed(timelineHideRunnable, timelineShowDurationMs)
        post { updateTimelineBar() }
    }

    private fun hideTimeline() {
        isTimelineVisible = false
        val fadeOut = AlphaAnimation(1f, 0f).apply {
            duration = 300
            setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(a: Animation) {}
                override fun onAnimationEnd(a: Animation) { progressBarContainer.visibility = View.GONE }
                override fun onAnimationRepeat(a: Animation) {}
            })
        }
        progressBarContainer.startAnimation(fadeOut)
    }

    private fun updateTimelineBar() {
        val allPoints = seriesList.flatMap { it.points }
        if (allPoints.size < 2) return
        val dataMin = allPoints.minOf { it.timestamp }
        val totalSpan = allPoints.maxOf { it.timestamp } - dataMin
        if (totalSpan <= 0L) return
        val barWidth = dragIndicatorContainer.width - dragIndicatorContainer.paddingLeft - dragIndicatorContainer.paddingRight
        if (barWidth <= 0) return
        val thumbWidthFraction = (chartCanvas.visibleRangeMs.toFloat() / totalSpan.toFloat()).coerceIn(0.05f, 1f)
        val thumbWidth = (barWidth * thumbWidthFraction).coerceAtLeast(20f)
        // Use left-edge mapping: thumb left = (windowStart - dataMin) / (totalSpan - visibleRange) * (barWidth - thumbWidth)
        val dragRange = totalSpan - chartCanvas.visibleRangeMs
        val thumbLeft = if (dragRange > 0L) {
            val offset = chartCanvas.windowStartMs - dataMin
            (offset.toFloat() / dragRange.toFloat() * (barWidth - thumbWidth)).coerceIn(0f, (barWidth - thumbWidth).coerceAtLeast(0f))
        } else 0f
        progressBarThumb.layoutParams = (progressBarThumb.layoutParams as FrameLayout.LayoutParams).also {
            it.width = thumbWidth.toInt()
            it.leftMargin = (thumbLeft + dragIndicatorContainer.paddingLeft).toInt()
            it.rightMargin = 0
        }
        progressBarThumb.requestLayout()
    }

    private fun updateLegend() {
        legendLayout.removeAllViews()
        for (s in seriesList) {
            legendLayout.addView(createLegendItem(s))
        }
        legendScrollView.visibility = if (legendLayout.childCount > 0) View.VISIBLE else View.GONE
    }

    private fun createLegendItem(s: ChartSeries): LinearLayout {
        val density = resources.displayMetrics.density

        // Point shape indicator
        val shapeView = object : View(context) {
            override fun onDraw(canvas: Canvas) {
                super.onDraw(canvas)
                val cx = width / 2f; val cy = height / 2f; val r = 3f * density
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = s.color
                    style = if (s.pointFill == PointFill.FILLED) Paint.Style.FILL else Paint.Style.STROKE
                    strokeWidth = 1.5f
                }
                when (s.pointShape) {
                    PointShape.CIRCLE -> canvas.drawCircle(cx, cy, r, paint)
                    PointShape.TRIANGLE -> {
                        val path = android.graphics.Path()
                        path.moveTo(cx, cy - r); path.lineTo(cx - r * 0.866f, cy + r * 0.5f)
                        path.lineTo(cx + r * 0.866f, cy + r * 0.5f); path.close()
                        canvas.drawPath(path, paint)
                    }
                    PointShape.SQUARE -> canvas.drawRect(cx - r, cy - r, cx + r, cy + r, paint)
                    PointShape.DIAMOND -> {
                        val path = android.graphics.Path()
                        path.moveTo(cx, cy - r); path.lineTo(cx + r, cy)
                        path.lineTo(cx, cy + r); path.lineTo(cx - r, cy); path.close()
                        canvas.drawPath(path, paint)
                    }
                    PointShape.CROSS -> {
                        canvas.drawLine(cx - r, cy - r, cx + r, cy + r, paint)
                        canvas.drawLine(cx + r, cy - r, cx - r, cy + r, paint)
                    }
                }
            }
        }
        shapeView.layoutParams = LinearLayout.LayoutParams(
            (14 * density).toInt(), (14 * density).toInt()
        ).apply {
            gravity = Gravity.CENTER_VERTICAL
            setMargins(0, 0, (2 * density).toInt(), 0)
        }

        // Line segment
        val lineView = LegendLineView(context, s.color, s.lineType)
        lineView.layoutParams = LinearLayout.LayoutParams(
            (20 * density).toInt(), (8 * density).toInt()
        ).apply {
            setMargins(0, 0, (4 * density).toInt(), 0)
            gravity = Gravity.CENTER_VERTICAL
        }

        // Label text
        val tv = TextView(context).apply {
            text = s.label; textSize = 12f
            setTextColor(ContextCompat.getColor(context, R.color.on_surface))
        }

        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val containerLp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            containerLp.setMargins((8 * density).toInt(), 0, (12 * density).toInt(), 0)
            layoutParams = containerLp
            addView(shapeView)
            addView(lineView)
            addView(tv)
        }
    }
}

private class LegendLineView(
    context: Context, lineColor: Int, lineType: LineType
) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = lineColor; strokeWidth = 3f
        style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND
        pathEffect = when (lineType) {
            LineType.SOLID -> null
            LineType.DASHED -> DashPathEffect(floatArrayOf(8f, 5f), 0f)
            LineType.DOTTED -> DashPathEffect(floatArrayOf(3f, 5f), 0f)
        }
    }
    override fun onDraw(canvas: Canvas) {
        val midY = height / 2f; val pad = 2f * resources.displayMetrics.density
        canvas.drawLine(pad, midY, width - pad, midY, paint)
    }
}

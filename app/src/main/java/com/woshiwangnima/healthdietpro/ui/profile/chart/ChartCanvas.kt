package com.woshiwangnima.healthdietpro.ui.profile.chart

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ChartCanvas @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    var series: List<ChartSeries> = emptyList()
    var unitLabel: String = ""
    var visibleRangeMs: Long = Long.MAX_VALUE
    var windowStartMs: Long = 0L
    var yMinPct: Float = 0f
    var yMaxPct: Float = 100f
    var labelIntervalMs: Long = 0L
    var onCrosshairUpdate: ((String, String) -> Unit)? = null

    private var touchX: Float = -1f
    private var touchY: Float = -1f
    private var hasTouch = false

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x33000000
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }
    private val crosshairLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x88000000.toInt()
        strokeWidth = 1.5f
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(6f, 4f), 0f)
    }
    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val axisTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 28f; color = 0xFF666666.toInt()
    }
    private val xAxisTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 30f; color = 0xFF666666.toInt(); textAlign = Paint.Align.CENTER
    }
    private val bubbleBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xE6FFFFFF.toInt(); style = Paint.Style.FILL
    }
    private val bubbleBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFCCCCCC.toInt(); style = Paint.Style.STROKE; strokeWidth = 1f
    }
    private val bubbleTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 26f; color = 0xFF333333.toInt()
    }

    private val allDataPoints
        get() = series.flatMap { it.points }

    private val dataMinValue: Float
        get() = allDataPoints.minOfOrNull { it.value } ?: 0f

    private val dataMaxValue: Float
        get() = allDataPoints.maxOfOrNull { it.value } ?: 100f

    private val chartYMin: Float
        get() {
            val dMin = dataMinValue
            val dMax = dataMaxValue
            val range = dMax - dMin
            if (range == 0f) return dMin - 1f
            return dMin + yMinPct / 100f * range
        }

    private val chartYMax: Float
        get() {
            val dMin = dataMinValue
            val dMax = dataMaxValue
            val range = dMax - dMin
            if (range == 0f) return dMax + 1f
            return dMin + yMaxPct / 100f * range
        }

    private val paddingLeft = 60f
    private val paddingRight = 20f
    private val paddingTop = 20f
    private val paddingBottom = 50f

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (allDataPoints.size < 2) return false
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                touchX = event.x
                touchY = event.y
                hasTouch = true
                updateCrosshair()
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                hasTouch = false
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    fun clearCrosshair() {
        hasTouch = false
        invalidate()
    }

    private fun updateCrosshair() {
        val chartW = width - paddingLeft - paddingRight
        if (chartW <= 0) return
        val relLeft = paddingLeft
        val xFraction = ((touchX - relLeft) / chartW).coerceIn(0f, 1f)
        val touchTs = windowStartMs + (xFraction * visibleRangeMs).toLong()
        val primarySeries = series.firstOrNull() ?: return
        val entries = ChartMath.toChartEntries(primarySeries.points, windowStartMs)
        if (entries.size < 2) return
        val x = (touchTs - windowStartMs).toFloat().coerceIn(entries.first().x, entries.last().x)
        val y = ChartMath.interpolateY(entries, x, primarySeries.lineStyle)
        val instant = Instant.ofEpochMilli(touchTs)
        val localDt = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
        val dateText = localDt.format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))
        val valueText = "%.1f %s".format(y, unitLabel)
        onCrosshairUpdate?.invoke(dateText, valueText)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val chartL = paddingLeft
        val chartT = paddingTop
        val chartR = w - paddingRight
        val chartB = h - paddingBottom
        val chartW = chartR - chartL
        val chartH = chartB - chartT
        if (chartW <= 0 || chartH <= 0) return

        val yMin = chartYMin
        val yMax = chartYMax
        val yRange = if (yMax > yMin) yMax - yMin else 1f

        // 1. Grid lines
        val (niceMin, niceMax) = ChartMath.niceScale(yMin, yMax, 5)
        val niceRange = if (niceMax > niceMin) niceMax - niceMin else 1f
        val step = niceRange / 4f
        for (i in 0..4) {
            val yVal = niceMin + step * i
            val py = chartB - ((yVal - niceMin) / niceRange) * chartH
            canvas.drawLine(chartL, py, chartR, py, gridPaint)
            canvas.drawText("%.0f".format(yVal), 4f, py + 8f, axisTextPaint)
        }

        // 2. Data series
        for (s in series) {
            drawSeries(canvas, s, chartL, chartT, chartR, chartB, chartW, chartH, yMin, yRange)
        }

        // 3. X axis labels
        drawXAxis(canvas, chartL, chartB, chartR, chartW)

        // 4. Crosshair
        if (hasTouch && allDataPoints.size >= 2) {
            drawCrosshairOverlay(canvas, chartL, chartT, chartR, chartB, chartW, chartH, yMin, yRange)
        }
    }

    private fun drawSeries(
        canvas: Canvas, s: ChartSeries,
        l: Float, t: Float, r: Float, b: Float,
        cw: Float, ch: Float, yMin: Float, yRange: Float
    ) {
        val entries = ChartMath.toChartEntries(s.points, windowStartMs)
        if (entries.size < 2) return
        val filtered = entries.filter { it.x >= 0f && it.x <= visibleRangeMs.toFloat() }
        if (filtered.isEmpty()) return

        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = s.color
            strokeWidth = 2.5f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            when (s.lineType) {
                LineType.DASHED -> pathEffect = DashPathEffect(floatArrayOf(12f, 6f), 0f)
                LineType.DOTTED -> pathEffect = DashPathEffect(floatArrayOf(4f, 6f), 0f)
                LineType.SOLID -> pathEffect = null
            }
        }

        fun screenX(xVal: Float) = l + (xVal / visibleRangeMs.toFloat()) * cw
        fun screenY(yVal: Float) = b - ((yVal - yMin) / yRange) * ch

        if (filtered.size == 1) {
            val px = screenX(filtered[0].x)
            val py = screenY(filtered[0].y)
            drawPointShape(canvas, px, py, s.pointShape, s.pointFill, s.color)
            return
        }

        val path = Path()
        val firstPx = screenX(filtered.first().x)
        path.moveTo(firstPx, screenY(filtered.first().y))

        when (s.lineStyle) {
            LineStyle.LINEAR -> {
                for (entry in filtered.drop(1)) path.lineTo(screenX(entry.x), screenY(entry.y))
            }
            LineStyle.STEPPED_FRONT -> {
                for (i in 0 until filtered.size - 1) {
                    val e0 = filtered[i]; val e1 = filtered[i + 1]
                    path.lineTo(screenX(e1.x), screenY(e0.y))
                    path.lineTo(screenX(e1.x), screenY(e1.y))
                }
            }
            LineStyle.STEPPED_BACK -> {
                for (i in 0 until filtered.size - 1) {
                    val e0 = filtered[i]; val e1 = filtered[i + 1]
                    path.lineTo(screenX(e0.x), screenY(e1.y))
                    path.lineTo(screenX(e1.x), screenY(e1.y))
                }
            }
            else -> {
                val samples = 200
                val minX = filtered.first().x
                val maxX = filtered.last().x
                val allEntries = ChartMath.toChartEntries(s.points, windowStartMs)
                for (k in 1..samples) {
                    val frac = k.toFloat() / samples
                    val sampleX = minX + (maxX - minX) * frac
                    val sampleY = if (s.lineStyle == LineStyle.SPLINE)
                        ChartMath.interpolateSpline(allEntries, sampleX)
                    else
                        ChartMath.interpolateBezier(allEntries, sampleX)
                    path.lineTo(screenX(sampleX), screenY(sampleY))
                }
            }
        }
        canvas.drawPath(path, linePaint)

        val rds = 5f * resources.displayMetrics.density
        for (entry in filtered) {
            val px = screenX(entry.x)
            val py = screenY(entry.y)
            if (px in l..r && py in t..b) {
                drawPointShape(canvas, px, py, s.pointShape, s.pointFill, s.color)
            }
        }
    }

    private fun drawPointShape(canvas: Canvas, cx: Float, cy: Float, shape: PointShape, fill: PointFill, color: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = if (fill == PointFill.FILLED) Paint.Style.FILL else Paint.Style.STROKE
            strokeWidth = 2f
        }
        val r = 5f * resources.displayMetrics.density
        when (shape) {
            PointShape.CIRCLE -> canvas.drawCircle(cx, cy, r, paint)
            PointShape.TRIANGLE -> {
                val path = Path()
                path.moveTo(cx, cy - r)
                path.lineTo(cx - r * 0.866f, cy + r * 0.5f)
                path.lineTo(cx + r * 0.866f, cy + r * 0.5f)
                path.close()
                canvas.drawPath(path, paint)
            }
            PointShape.SQUARE -> canvas.drawRect(cx - r, cy - r, cx + r, cy + r, paint)
            PointShape.DIAMOND -> {
                val path = Path()
                path.moveTo(cx, cy - r)
                path.lineTo(cx + r, cy)
                path.lineTo(cx, cy + r)
                path.lineTo(cx - r, cy)
                path.close()
                canvas.drawPath(path, paint)
            }
            PointShape.CROSS -> {
                canvas.drawLine(cx - r, cy - r, cx + r, cy + r, paint)
                canvas.drawLine(cx + r, cy - r, cx - r, cy + r, paint)
            }
        }
    }

    private fun drawXAxis(canvas: Canvas, l: Float, b: Float, r: Float, cw: Float) {
        val interval = if (labelIntervalMs > 0) labelIntervalMs else ChartMath.computeLabelInterval(visibleRangeMs)
        val startLabel = ((windowStartMs / interval) + 1) * interval
        val formatter = DateTimeFormatter.ofPattern(
            when {
                interval < 60_000L -> "mm:ss"
                interval < 3_600_000L -> "HH:mm"
                interval < 86_400_000L -> "MM-dd HH:mm"
                else -> "MM-dd"
            }
        )
        var ts = startLabel
        while (ts <= windowStartMs + visibleRangeMs) {
            val xVal = (ts - windowStartMs).toFloat()
            val px = l + (xVal / visibleRangeMs.toFloat()) * cw
            if (px in l..r) {
                val instant = Instant.ofEpochMilli(ts)
                val localDt = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
                canvas.drawText(localDt.format(formatter), px, b + 32f, xAxisTextPaint)
            }
            ts += interval
        }
    }

    private fun drawCrosshairOverlay(
        canvas: Canvas, l: Float, t: Float, r: Float, b: Float,
        cw: Float, ch: Float, yMin: Float, yRange: Float
    ) {
        val primarySeries = series.firstOrNull() ?: return
        val entries = ChartMath.toChartEntries(primarySeries.points, windowStartMs)
        if (entries.size < 2) return
        val xFraction = ((touchX - l) / cw).coerceIn(0f, 1f)
        val xVal = xFraction * visibleRangeMs.toFloat()
        val yVal = ChartMath.interpolateY(entries, xVal, primarySeries.lineStyle)

        fun screenX(v: Float) = l + (v / visibleRangeMs.toFloat()) * cw
        fun screenY(v: Float) = b - ((v - yMin) / yRange) * ch

        val px = screenX(xVal)
        val py = screenY(yVal)
        if (px < l || px > r || py < t || py > b) return

        canvas.drawLine(px, t, px, b, crosshairLinePaint)
        canvas.drawLine(l, py, r, py, crosshairLinePaint)
        circlePaint.color = primarySeries.color
        canvas.drawCircle(px, py, 6f, circlePaint)

        val instant = Instant.ofEpochMilli(windowStartMs + xVal.toLong())
        val localDt = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
        val dateText = localDt.format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))
        val valueText = "%.1f %s".format(yVal, unitLabel)
        drawInfoBubble(canvas, px, py, dateText, valueText, l, t, r, b)
    }

    private fun drawInfoBubble(
        canvas: Canvas, px: Float, py: Float,
        dateText: String, valueText: String,
        cl: Float, ct: Float, cr: Float, cb: Float
    ) {
        val density = resources.displayMetrics.density
        val pad = 12f * density
        val tw1 = bubbleTextPaint.measureText(dateText)
        val tw2 = bubbleTextPaint.measureText(valueText)
        val maxW = maxOf(tw1, tw2)
        val fm = bubbleTextPaint.fontMetrics
        val lineH = fm.descent - fm.ascent
        val bw = maxW + pad * 2
        val bh = lineH * 2 + pad * 2 + 4f
        val offsetX = 12f * density
        val offsetY = -12f * density
        var bx = px + offsetX
        var by = py + offsetY - bh
        if (bx + bw > cr - 4f) bx = px - offsetX - bw
        if (by < ct + 4f) by = py - offsetY + 4f
        if (bx < cl + 4f) bx = cl + 4f
        if (by + bh > cb - 4f) by = cb - 4f - bh
        val rr = 8f * density
        val rect = RectF(bx, by, bx + bw, by + bh)
        canvas.drawRoundRect(rect, rr, rr, bubbleBgPaint)
        canvas.drawRoundRect(rect, rr, rr, bubbleBorderPaint)
        val tx = bx + pad
        val firstY = by + pad - fm.ascent
        canvas.drawText(dateText, tx, firstY, bubbleTextPaint)
        canvas.drawText(valueText, tx, firstY + lineH, bubbleTextPaint)
    }
}

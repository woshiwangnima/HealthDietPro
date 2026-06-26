package com.woshiwangnima.healthdietpro.ui.profile.chart

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class YAxisBand(val minValue: Float, val maxValue: Float, val color: Int)

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
    var yAxisBands: List<YAxisBand> = emptyList()
    var onCrosshairUpdate: ((String, String) -> Unit)? = null

    private var touchX: Float = -1f
    private var touchY: Float = -1f
    private var hasTouch = false
    private var persistedCrossX: Float = -1f
    private var persistedCrossY: Float = -1f
    private var persistedCrossDate: String = ""
    private var persistedCrossValue: String = ""
    private var persistedCrossVisible: Boolean = false
    private var themeResolved = false

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val crosshairLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(6f, 4f), 0f)
    }
    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val axisTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.RIGHT }
    private val xAxisTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
    private val bubbleBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val bubbleBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val bubbleTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val allDataPoints get() = series.flatMap { it.points }
    private val dataMinValue: Float get() = allDataPoints.minOfOrNull { it.value } ?: 0f
    private val dataMaxValue: Float get() = allDataPoints.maxOfOrNull { it.value } ?: 100f

    private val chartYMin: Float get() {
        val dMin = dataMinValue; val dMax = dataMaxValue
        val range = dMax - dMin
        if (range == 0f) return dMin - 1f
        return dMin + yMinPct / 100f * range
    }
    private val chartYMax: Float get() {
        val dMin = dataMinValue; val dMax = dataMaxValue
        val range = dMax - dMin
        if (range == 0f) return dMax + 1f
        return dMin + yMaxPct / 100f * range
    }

    private val density: Float get() = resources.displayMetrics.density
    private val paddingLeft = 16f
    private val paddingRight = 72f
    private val paddingTop = 20f
    private val paddingBottom = 50f

    private fun resolveThemeColors() {
        val onSurface = resolveColorAttr(com.google.android.material.R.attr.colorOnSurface)
        val onSurfaceVariant = resolveColorAttr(com.google.android.material.R.attr.colorOnSurfaceVariant)
        val outline = resolveColorAttr(com.google.android.material.R.attr.colorOutline)
        val outlineVariant = resolveColorAttr(com.google.android.material.R.attr.colorOutlineVariant)
        val surface = resolveColorAttr(com.google.android.material.R.attr.colorSurface)

        val gridAlpha = 0x33
        val gridColor = Color.argb(gridAlpha, Color.red(outlineVariant), Color.green(outlineVariant), Color.blue(outlineVariant))
        gridPaint.color = gridColor; gridPaint.strokeWidth = 1f

        crosshairLinePaint.color = Color.argb(0x88, Color.red(onSurface), Color.green(onSurface), Color.blue(onSurface))
        crosshairLinePaint.strokeWidth = 1.5f

        axisTextPaint.color = onSurfaceVariant; axisTextPaint.textSize = 28f
        xAxisTextPaint.color = onSurfaceVariant; xAxisTextPaint.textSize = 30f

        bubbleBgPaint.color = Color.argb(0xE6, Color.red(surface), Color.green(surface), Color.blue(surface))
        bubbleBorderPaint.color = outline; bubbleBorderPaint.strokeWidth = 1f

        bubbleTextPaint.color = onSurface; bubbleTextPaint.textSize = 32f
        bubbleTextPaint.isAntiAlias = true

        themeResolved = true
    }

    private fun resolveColorAttr(attrRes: Int): Int {
        val tv = TypedValue()
        context.theme.resolveAttribute(attrRes, tv, true)
        return tv.data
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (allDataPoints.size < 2) return false
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                touchX = event.x; touchY = event.y; hasTouch = true
                updateCrosshair(); invalidate(); return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                hasTouch = false; invalidate(); return true
            }
        }
        return super.onTouchEvent(event)
    }

    fun clearCrosshair() {
        hasTouch = false; persistedCrossVisible = false; invalidate()
    }

    fun crosshairActive(): Boolean = persistedCrossVisible

    private fun updateCrosshair() {
        val chartW = width - paddingLeft - paddingRight
        if (chartW <= 0) return
        val xFraction = ((touchX - paddingLeft) / chartW).coerceIn(0f, 1f)
        val touchTs = windowStartMs + (xFraction * visibleRangeMs).toLong()
        val primarySeries = series.firstOrNull() ?: return
        val entries = ChartMath.toChartEntries(primarySeries.points, windowStartMs)
        if (entries.size < 2) return
        val x = (touchTs - windowStartMs).toFloat().coerceIn(entries.first().x, entries.last().x)
        val y = ChartMath.interpolateY(entries, x, primarySeries.lineStyle)
        persistedCrossX = touchX; persistedCrossY = touchY; persistedCrossVisible = true
        val instant = Instant.ofEpochMilli(touchTs)
        val localDt = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
        persistedCrossDate = localDt.format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))
        persistedCrossValue = "%.1f %s".format(y, unitLabel)
        onCrosshairUpdate?.invoke(persistedCrossDate, persistedCrossValue)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!themeResolved) resolveThemeColors()

        val w = width.toFloat(); val h = height.toFloat()
        val chartL = paddingLeft; val chartT = paddingTop
        val chartR = w - paddingRight; val chartB = h - paddingBottom
        val chartW = chartR - chartL; val chartH = chartB - chartT
        if (chartW <= 0 || chartH <= 0) return

        val yMin = chartYMin; val yMax = chartYMax
        val yRange = if (yMax > yMin) yMax - yMin else 1f
        fun screenY(yVal: Float) = chartB - ((yVal - yMin) / yRange) * chartH

        // Background color bands for Y-axis classification
        for (band in yAxisBands) {
            val bTop = screenY(band.maxValue.coerceAtMost(yMax).coerceAtLeast(yMin))
            val bBot = screenY(band.minValue.coerceAtLeast(yMin).coerceAtMost(yMax))
            val bandPaint = Paint().apply { color = band.color; style = Paint.Style.FILL }
            canvas.drawRect(chartL, bTop, chartR, bBot, bandPaint)
        }

        val (niceMin, niceMax) = ChartMath.niceScale(yMin, yMax, 5)
        val niceRange = if (niceMax > niceMin) niceMax - niceMin else 1f
        val step = niceRange / 4f
        for (i in 0..4) {
            val yVal = niceMin + step * i
            val py = chartB - ((yVal - niceMin) / niceRange) * chartH
            canvas.drawLine(chartL, py, chartR, py, gridPaint)
            canvas.drawText("%.0f".format(yVal), w - 4f, py + 8f, axisTextPaint)
        }

        for (s in series) drawSeries(canvas, s, chartL, chartT, chartR, chartB, chartW, chartH, yMin, yRange)
        drawXAxis(canvas, chartL, chartB, chartR, chartW)
        if (allDataPoints.size >= 2) drawCrosshairOverlay(canvas, chartL, chartT, chartR, chartB, chartW, chartH, yMin, yRange)
    }

    private fun drawSeries(canvas: Canvas, s: ChartSeries, l: Float, t: Float, r: Float, b: Float,
                           cw: Float, ch: Float, yMin: Float, yRange: Float) {
        val entries = ChartMath.toChartEntries(s.points, windowStartMs)
        if (entries.isEmpty()) return

        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = s.color; strokeWidth = 3.5f
            style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
            when (s.lineType) {
                LineType.DASHED -> pathEffect = DashPathEffect(floatArrayOf(12f, 6f), 0f)
                LineType.DOTTED -> pathEffect = DashPathEffect(floatArrayOf(4f, 6f), 0f)
                LineType.SOLID -> pathEffect = null
            }
        }

        fun screenX(xVal: Float) = l + (xVal / visibleRangeMs.toFloat()) * cw
        fun screenY(yVal: Float) = b - ((yVal - yMin) / yRange) * ch

        val visibleMin = 0f; val visibleMax = visibleRangeMs.toFloat()
        val firstVisIdx = entries.indexOfLast { it.x <= visibleMin }.coerceAtLeast(0)
        val lastVisIdx = entries.indexOfFirst { it.x >= visibleMax }.let { if (it == -1) entries.size - 1 else it }
        val drawEntries = entries.subList(
            (firstVisIdx - 1).coerceAtLeast(0), (lastVisIdx + 2).coerceAtMost(entries.size))
        if (drawEntries.size < 2) return

        val path = Path()
        path.moveTo(screenX(drawEntries.first().x), screenY(drawEntries.first().y))
        when (s.lineStyle) {
            LineStyle.LINEAR -> drawEntries.drop(1).forEach { path.lineTo(screenX(it.x), screenY(it.y)) }
            LineStyle.STEPPED_FRONT -> for (i in 0 until drawEntries.size - 1) {
                val e0 = drawEntries[i]; val e1 = drawEntries[i + 1]
                path.lineTo(screenX(e1.x), screenY(e0.y)); path.lineTo(screenX(e1.x), screenY(e1.y))
            }
            LineStyle.STEPPED_BACK -> for (i in 0 until drawEntries.size - 1) {
                val e0 = drawEntries[i]; val e1 = drawEntries[i + 1]
                path.lineTo(screenX(e0.x), screenY(e1.y)); path.lineTo(screenX(e1.x), screenY(e1.y))
            }
            else -> {
                val allE = ChartMath.toChartEntries(s.points, windowStartMs)
                val minX = drawEntries.first().x; val maxX = drawEntries.last().x
                for (k in 1..200) {
                    val sx = minX + (maxX - minX) * k / 200f
                    val sy = if (s.lineStyle == LineStyle.SPLINE) ChartMath.interpolateSpline(allE, sx)
                    else ChartMath.interpolateBezier(allE, sx)
                    path.lineTo(screenX(sx), screenY(sy))
                }
            }
        }
        canvas.save(); canvas.clipRect(l, t, r, b); canvas.drawPath(path, linePaint); canvas.restore()

        for (entry in drawEntries) {
            if (entry.x < visibleMin || entry.x > visibleMax) continue
            val py = screenY(entry.y)
            if (py in t..b) drawPointShape(canvas, screenX(entry.x), py, s.pointShape, s.pointFill, s.color)
        }
    }

    private fun drawPointShape(canvas: Canvas, cx: Float, cy: Float, shape: PointShape, fill: PointFill, color: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color; strokeWidth = 2f
            style = if (fill == PointFill.FILLED) Paint.Style.FILL else Paint.Style.STROKE
        }
        val r = 5f * density
        when (shape) {
            PointShape.CIRCLE -> canvas.drawCircle(cx, cy, r, paint)
            PointShape.TRIANGLE -> { val p = Path(); p.moveTo(cx, cy - r); p.lineTo(cx - r * 0.866f, cy + r * 0.5f); p.lineTo(cx + r * 0.866f, cy + r * 0.5f); p.close(); canvas.drawPath(p, paint) }
            PointShape.SQUARE -> canvas.drawRect(cx - r, cy - r, cx + r, cy + r, paint)
            PointShape.DIAMOND -> { val p = Path(); p.moveTo(cx, cy - r); p.lineTo(cx + r, cy); p.lineTo(cx, cy + r); p.lineTo(cx - r, cy); p.close(); canvas.drawPath(p, paint) }
            PointShape.CROSS -> { canvas.drawLine(cx - r, cy - r, cx + r, cy + r, paint); canvas.drawLine(cx + r, cy - r, cx - r, cy + r, paint) }
        }
    }

    private fun drawXAxis(canvas: Canvas, l: Float, b: Float, r: Float, cw: Float) {
        val interval = if (labelIntervalMs > 0) labelIntervalMs else ChartMath.computeLabelInterval(visibleRangeMs)
        val startLabel = ((windowStartMs / interval) + 1) * interval
        val fmt = DateTimeFormatter.ofPattern(when {
            interval < 60_000L -> "mm:ss"; interval < 3_600_000L -> "HH:mm"
            interval < 86_400_000L -> "MM-dd HH:mm"; else -> "MM-dd"
        })
        var ts = startLabel
        while (ts <= windowStartMs + visibleRangeMs) {
            val px = l + ((ts - windowStartMs).toFloat() / visibleRangeMs.toFloat()) * cw
            if (px in l..r) canvas.drawText(
                LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneId.systemDefault()).format(fmt), px, b + 32f, xAxisTextPaint)
            ts += interval
        }
    }

    private fun drawCrosshairOverlay(canvas: Canvas, l: Float, t: Float, r: Float, b: Float,
                                     cw: Float, ch: Float, yMin: Float, yRange: Float) {
        if (!persistedCrossVisible) return
        val primarySeries = series.firstOrNull() ?: return
        val entries = ChartMath.toChartEntries(primarySeries.points, windowStartMs)
        if (entries.size < 2) return

        val xFraction = if (hasTouch) ((touchX - l) / cw).coerceIn(0f, 1f)
                        else ((persistedCrossX - l) / cw).coerceIn(0f, 1f)
        val xVal = xFraction * visibleRangeMs.toFloat()
        val yVal = ChartMath.interpolateY(entries, xVal, primarySeries.lineStyle)

        fun sx(v: Float) = l + (v / visibleRangeMs.toFloat()) * cw
        fun sy(v: Float) = b - ((v - yMin) / yRange) * ch
        val px = sx(xVal); val py = sy(yVal)
        if (px < l || px > r || py < t || py > b) { persistedCrossVisible = false; return }

        canvas.drawLine(px, t, px, b, crosshairLinePaint); canvas.drawLine(l, py, r, py, crosshairLinePaint)
        circlePaint.color = primarySeries.color; canvas.drawCircle(px, py, 6f, circlePaint)

        if (!hasTouch) {
            val instant = Instant.ofEpochMilli(windowStartMs + xVal.toLong())
            persistedCrossDate = LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))
            persistedCrossValue = "%.1f %s".format(yVal, unitLabel)
        }
        drawInfoBubble(canvas, px, py, persistedCrossDate, persistedCrossValue, l, t, r, b)
    }

    private fun drawInfoBubble(canvas: Canvas, px: Float, py: Float, dateText: String, valueText: String,
                               cl: Float, ct: Float, cr: Float, cb: Float) {
        val pad = 12f * density; val tw1 = bubbleTextPaint.measureText(dateText)
        val tw2 = bubbleTextPaint.measureText(valueText); val maxW = maxOf(tw1, tw2)
        val fm = bubbleTextPaint.fontMetrics; val lineH = fm.descent - fm.ascent
        val bw = maxW + pad * 2; val bh = lineH * 2 + pad * 2 + 4f
        val offX = 12f * density; val offY = -12f * density
        var bx = px + offX; var by = py + offY - bh
        if (bx + bw > cr - 4f) bx = px - offX - bw
        if (by < ct + 4f) by = py - offY + 4f
        if (bx < cl + 4f) bx = cl + 4f
        if (by + bh > cb - 4f) by = cb - 4f - bh
        val rr = 8f * density
        canvas.drawRoundRect(RectF(bx, by, bx + bw, by + bh), rr, rr, bubbleBgPaint)
        canvas.drawRoundRect(RectF(bx, by, bx + bw, by + bh), rr, rr, bubbleBorderPaint)
        canvas.drawText(dateText, bx + pad, by + pad - fm.ascent, bubbleTextPaint)
        canvas.drawText(valueText, bx + pad, by + pad - fm.ascent + lineH, bubbleTextPaint)
    }
}

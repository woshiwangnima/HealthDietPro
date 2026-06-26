# ChartView Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Refactor chart from MPAndroidChart-based Activity to pure-Canvas ChartView ViewGroup with multi-series support.

**Architecture:** ChartView (LinearLayout ViewGroup) composes ChartCanvas (custom View) plus control widgets. ChartMath provides pure math utilities. All data uses ChartSeries model supporting N dynamic data series.

**Tech Stack:** Kotlin, Android Canvas/Paint/Path, java.time, ViewGroup composition. MPAndroidChart dependency removed.

---

### Task 1: Data model — LineStyle enum (5 types)

**Files:**
- Modify: `app/src/main/java/com/woshiwangnima/healthdietpro/ui/profile/chart/LineStyle.kt`

- [ ] **Step 1: Rewrite LineStyle enum with 5 chart types**

```kotlin
package com.woshiwangnima.healthdietpro.ui.profile.chart

enum class LineStyle {
    LINEAR,
    BEZIER,
    SPLINE,
    STEPPED_FRONT,
    STEPPED_BACK;

    companion object {
        fun fromSpinnerPosition(position: Int): LineStyle = when (position) {
            0 -> LINEAR
            1 -> BEZIER
            2 -> SPLINE
            3 -> STEPPED_FRONT
            4 -> STEPPED_BACK
            else -> LINEAR
        }

        fun toSpinnerPosition(style: LineStyle): Int = when (style) {
            LINEAR -> 0
            BEZIER -> 1
            SPLINE -> 2
            STEPPED_FRONT -> 3
            STEPPED_BACK -> 4
        }
    }
}
```

- [ ] **Step 2: Update LineStyleTest for 5 types**

```kotlin
package com.woshiwangnima.healthdietpro.ui.profile.chart

import org.junit.Assert.assertEquals
import org.junit.Test

class LineStyleTest {

    @Test
    fun fromSpinnerPosition_mapsAllFiveSlots() {
        assertEquals(LineStyle.LINEAR, LineStyle.fromSpinnerPosition(0))
        assertEquals(LineStyle.BEZIER, LineStyle.fromSpinnerPosition(1))
        assertEquals(LineStyle.SPLINE, LineStyle.fromSpinnerPosition(2))
        assertEquals(LineStyle.STEPPED_FRONT, LineStyle.fromSpinnerPosition(3))
        assertEquals(LineStyle.STEPPED_BACK, LineStyle.fromSpinnerPosition(4))
    }

    @Test
    fun fromSpinnerPosition_outOfRangeDefaultsToLinear() {
        assertEquals(LineStyle.LINEAR, LineStyle.fromSpinnerPosition(-1))
        assertEquals(LineStyle.LINEAR, LineStyle.fromSpinnerPosition(99))
    }

    @Test
    fun toSpinnerPosition_roundTrip() {
        val all = LineStyle.entries
        for (style in all) {
            assertEquals(style, LineStyle.fromSpinnerPosition(LineStyle.toSpinnerPosition(style)))
        }
    }
}
```

- [ ] **Step 3: Run tests**

```bash
./gradlew :app:testDebugUnitTest --tests "com.woshiwangnima.healthdietpro.ui.profile.chart.LineStyleTest"
```

Expected: all 3 tests PASS

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/woshiwangnima/healthdietpro/ui/profile/chart/LineStyle.kt app/src/test/java/com/woshiwangnima/healthdietpro/ui/profile/chart/LineStyleTest.kt
git commit -m "feat: expand LineStyle enum to 5 types (LINEAR/BEZIER/SPLINE/STEPPED_FRONT/STEPPED_BACK)"
```

---

### Task 2: Data model — LineType, PointShape, PointFill, ChartSeries

**Files:**
- Create: `app/src/main/java/com/woshiwangnima/healthdietpro/ui/profile/chart/ChartSeries.kt`

- [ ] **Step 1: Create ChartSeries.kt with all enums and data class**

```kotlin
package com.woshiwangnima.healthdietpro.ui.profile.chart

import com.woshiwangnima.healthdietpro.model.profile.DataPoint

enum class LineType { SOLID, DASHED, DOTTED }

enum class PointShape { CIRCLE, TRIANGLE, SQUARE, DIAMOND, CROSS }

enum class PointFill { FILLED, HOLLOW }

data class ChartSeries(
    val points: List<DataPoint>,
    val label: String,
    val color: Int,
    val lineStyle: LineStyle = LineStyle.LINEAR,
    val lineType: LineType = LineType.SOLID,
    val pointShape: PointShape = PointShape.CIRCLE,
    val pointFill: PointFill = PointFill.FILLED
)
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/woshiwangnima/healthdietpro/ui/profile/chart/ChartSeries.kt
git commit -m "feat: add ChartSeries data model with PointShape/PointFill/LineType enums"
```

---

### Task 3: Math engine — ChartMath (no MPAndroidChart dependency)

**Files:**
- Create: `app/src/main/java/com/woshiwangnima/healthdietpro/ui/profile/chart/ChartMath.kt`
- Modify: `app/src/test/java/com/woshiwangnima/healthdietpro/ui/profile/chart/ChartSegmentMathTest.kt`

`ChartMath` uses a simple `Pair<Float, Float>` (x, y) instead of MPAndroidChart `Entry`.

- [ ] **Step 1: Write ChartMath.kt**

```kotlin
package com.woshiwangnima.healthdietpro.ui.profile.chart

import kotlin.math.*

object ChartMath {

    data class ChartEntry(val x: Float, val y: Float)

    fun toChartEntries(points: List<com.woshiwangnima.healthdietpro.model.profile.DataPoint>, baseTs: Long): List<ChartEntry> =
        points.map { ChartEntry((it.timestamp - baseTs).toFloat(), it.value) }

    fun findSegmentIndex(entries: List<ChartEntry>, x: Float): Int {
        if (entries.size < 2) return 0
        for (i in 0 until entries.size - 1) {
            if (x >= entries[i].x && x <= entries[i + 1].x) return i
        }
        return (entries.size - 2).coerceAtLeast(0)
    }

    fun interpolateLinear(entries: List<ChartEntry>, x: Float): Float {
        if (entries.size < 2) return entries.firstOrNull()?.y ?: 0f
        val i = findSegmentIndex(entries, x)
        val a = entries[i]
        val b = entries[i + 1]
        val span = b.x - a.x
        val t = if (span == 0f) 0f else (x - a.x) / span
        return a.y + t * (b.y - a.y)
    }

    fun interpolateSteppedFront(entries: List<ChartEntry>, x: Float): Float {
        if (entries.size < 2) return entries.firstOrNull()?.y ?: 0f
        val i = findSegmentIndex(entries, x)
        return entries[i].y
    }

    fun interpolateSteppedBack(entries: List<ChartEntry>, x: Float): Float {
        if (entries.size < 2) return entries.firstOrNull()?.y ?: 0f
        val i = findSegmentIndex(entries, x)
        return entries[i + 1].y
    }

    fun interpolateBezier(entries: List<ChartEntry>, x: Float): Float {
        val i = findSegmentIndex(entries, x)
        if (i < 0 || i + 1 >= entries.size) return interpolateLinear(entries, x)
        val p0 = entries[i]
        val p3 = entries[i + 1]
        val dx = p3.x - p0.x
        if (dx == 0f) return interpolateLinear(entries, x)
        val dy = p3.y - p0.y
        val cIntensity = 0.2f
        val prevY = if (i > 0) entries[i - 1].y else p0.y - dy
        val nextY = if (i + 2 < entries.size) entries[i + 2].y else p3.y + dy
        val cp1y = p0.y + dy * cIntensity + (p3.y - prevY) * cIntensity * 0.5f
        val cp2y = p3.y - dy * cIntensity - (nextY - p0.y) * cIntensity * 0.5f
        val t = ((x - p0.x) / dx).coerceIn(0f, 1f)
        val ti = 1f - t
        return ti * ti * ti * p0.y + 3f * ti * ti * t * cp1y + 3f * ti * t * t * cp2y + t * t * t * p3.y
    }

    fun interpolateSpline(entries: List<ChartEntry>, x: Float): Float {
        val i = findSegmentIndex(entries, x)
        if (i < 0 || i + 1 >= entries.size) return interpolateLinear(entries, x)
        val n = entries.size
        if (n < 3) return interpolateBezier(entries, x)
        // Natural cubic spline: precompute second derivatives via tridiagonal system
        val h = FloatArray(n - 1) { k -> entries[k + 1].x - entries[k].x }
        val alpha = FloatArray(n - 1)
        for (k in 1 until n - 1) {
            val hk = h[k]
            val hk1 = if (k - 1 >= 0) h[k - 1] else 1f
            if (hk + hk1 == 0f) {
                alpha[k] = 0f
            } else {
                alpha[k] = (3f / hk) * (entries[k + 1].y - entries[k].y) -
                           (3f / hk1) * (entries[k].y - entries[k - 1].y)
            }
        }
        val c = FloatArray(n)
        val l = FloatArray(n) { 1f }
        val mu = FloatArray(n)
        val z = FloatArray(n)
        for (k in 1 until n - 1) {
            val hk = h[k]
            val hk1 = h[k - 1]
            l[k] = 2f * (entries[k + 1].x - entries[k - 1].x) - hk1 * mu[k - 1]
            if (l[k] == 0f) l[k] = 1f
            mu[k] = hk / l[k]
            z[k] = (alpha[k] - hk1 * z[k - 1]) / l[k]
        }
        for (j in n - 2 downTo 0) {
            c[j] = z[j] - mu[j] * c[j + 1]
        }
        // Evaluate spline at x
        val p0 = entries[i]
        val p1 = entries[i + 1]
        val hi = h[i]
        if (hi == 0f) return p0.y
        val t = (x - p0.x) / hi
        val ti = 1f - t
        val a = p0.y
        val b = (p1.y - p0.y) / hi - hi * (2f * c[i] + c[i + 1]) / 3f
        val cc = c[i]
        val d = (c[i + 1] - c[i]) / (3f * hi)
        return a + b * t + cc * t * t + d * t * t * t
    }

    fun interpolateY(entries: List<ChartEntry>, x: Float, style: LineStyle): Float = when (style) {
        LineStyle.LINEAR -> interpolateLinear(entries, x)
        LineStyle.BEZIER -> interpolateBezier(entries, x)
        LineStyle.SPLINE -> interpolateSpline(entries, x)
        LineStyle.STEPPED_FRONT -> interpolateSteppedFront(entries, x)
        LineStyle.STEPPED_BACK -> interpolateSteppedBack(entries, x)
    }

    fun niceScale(minV: Float, maxV: Float, maxTicks: Int = 5): Pair<Float, Float> {
        if (minV == maxV) return (minV - 1f) to (maxV + 1f)
        val range = niceNum(maxV - minV, false)
        val step = niceNum(range / (maxTicks - 1), true)
        val niceMin = floor(minV / step) * step
        val niceMax = ceil(maxV / step) * step
        return niceMin to niceMax
    }

    private fun niceNum(range: Float, round: Boolean): Float {
        val exp = floor(log10(range.toDouble())).toFloat()
        val fraction = range / 10f.pow(exp)
        val nice = if (round) {
            when {
                fraction <= 1.5f -> 1f
                fraction <= 3f -> 2f
                fraction <= 7f -> 5f
                else -> 10f
            }
        } else {
            when {
                fraction <= 1f -> 1f
                fraction <= 2f -> 2f
                fraction <= 5f -> 5f
                else -> 10f
            }
        }
        return nice * 10f.pow(exp)
    }

    fun computeLabelInterval(visibleRangeMs: Long): Long {
        val h = 3_600_000L
        val d = 24 * h
        val w = 7 * d
        val m = 30 * d
        return when {
            visibleRangeMs <= 1 * h -> 5 * 60_000L
            visibleRangeMs <= 6 * h -> 30 * 60_000L
            visibleRangeMs <= 1 * d -> 2 * h
            visibleRangeMs <= 1 * w -> d
            visibleRangeMs <= 1 * m -> 3 * d
            visibleRangeMs <= 6 * m -> w
            visibleRangeMs <= 365 * d -> m
            else -> 3 * m
        }
    }
}
```

- [ ] **Step 2: Rewrite ChartSegmentMathTest for ChartEntry**

```kotlin
package com.woshiwangnima.healthdietpro.ui.profile.chart

import org.junit.Assert.assertEquals
import org.junit.Test

class ChartSegmentMathTest {

    private val entries = listOf(
        ChartMath.ChartEntry(0f, 10f),
        ChartMath.ChartEntry(100f, 20f),
        ChartMath.ChartEntry(300f, 40f),
        ChartMath.ChartEntry(700f, 100f),
    )

    @Test
    fun findSegmentIndex_returnsContainingSegment() {
        assertEquals(0, ChartMath.findSegmentIndex(entries, 0f))
        assertEquals(0, ChartMath.findSegmentIndex(entries, 50f))
        assertEquals(0, ChartMath.findSegmentIndex(entries, 100f))
        assertEquals(1, ChartMath.findSegmentIndex(entries, 100.01f))
        assertEquals(1, ChartMath.findSegmentIndex(entries, 200f))
        assertEquals(2, ChartMath.findSegmentIndex(entries, 400f))
        assertEquals(2, ChartMath.findSegmentIndex(entries, 700f))
    }

    @Test
    fun findSegmentIndex_clampsToLastSegment() {
        assertEquals(2, ChartMath.findSegmentIndex(entries, 5000f))
    }

    @Test
    fun interpolateLinear_returnsYAtMidpoint() {
        val y = ChartMath.interpolateLinear(entries, 50f)
        assertEquals(15f, y, 0.0001f)
    }

    @Test
    fun interpolateLinear_returnsRightYAtBoundary() {
        val y = ChartMath.interpolateLinear(entries, 100f)
        assertEquals(20f, y, 0.0001f)
    }

    @Test
    fun interpolateSteppedFront_returnsLeftY() {
        val y = ChartMath.interpolateSteppedFront(entries, 50f)
        assertEquals(10f, y, 0.0001f)
        val y700 = ChartMath.interpolateSteppedFront(entries, 700f)
        assertEquals(40f, y700, 0.0001f)
    }

    @Test
    fun interpolateSteppedBack_returnsRightY() {
        val y = ChartMath.interpolateSteppedBack(entries, 50f)
        assertEquals(20f, y, 0.0001f)
        val y700 = ChartMath.interpolateSteppedBack(entries, 700f)
        assertEquals(100f, y700, 0.0001f)
    }

    @Test
    fun interpolateBezier_interiorPoint() {
        val simple = listOf(
            ChartMath.ChartEntry(0f, 0f),
            ChartMath.ChartEntry(100f, 100f),
            ChartMath.ChartEntry(200f, 0f),
            ChartMath.ChartEntry(300f, 100f)
        )
        val y = ChartMath.interpolateBezier(simple, 50f)
        // Bezier should be between the two endpoints, roughly 50
        assertEquals(50f, y, 30f)
    }

    @Test
    fun interpolateSpline_passesThroughPoints() {
        val pts = listOf(
            ChartMath.ChartEntry(0f, 0f),
            ChartMath.ChartEntry(100f, 100f),
            ChartMath.ChartEntry(200f, 50f),
            ChartMath.ChartEntry(300f, 150f)
        )
        assertEquals(0f, ChartMath.interpolateSpline(pts, 0f), 0.001f)
        assertEquals(100f, ChartMath.interpolateSpline(pts, 100f), 0.001f)
        assertEquals(50f, ChartMath.interpolateSpline(pts, 200f), 0.001f)
        assertEquals(150f, ChartMath.interpolateSpline(pts, 300f), 0.001f)
    }

    @Test
    fun niceScale_examples() {
        val (min1, max1) = ChartMath.niceScale(7f, 93f, 5)
        assertEquals(0f, min1, 0.001f)
        assertEquals(100f, max1, 0.001f)

        val (min2, max2) = ChartMath.niceScale(23f, 67f, 5)
        assertEquals(20f, min2, 0.001f)
        assertEquals(70f, max2, 0.001f)
    }

    @Test
    fun computeLabelInterval_examples() {
        assertEquals(2 * 60 * 60 * 1000L, ChartMath.computeLabelInterval(24 * 60 * 60 * 1000L))  // 1 day -> 2h
        assertEquals(24 * 60 * 60 * 1000L, ChartMath.computeLabelInterval(7 * 24 * 60 * 60 * 1000L)) // 1 week -> 1 day
        assertEquals(7 * 24 * 60 * 60 * 1000L, ChartMath.computeLabelInterval(6L * 30 * 24 * 60 * 60 * 1000)) // 6 months -> 1 week
    }
}
```

- [ ] **Step 3: Run tests**

```bash
./gradlew :app:testDebugUnitTest --tests "com.woshiwangnima.healthdietpro.ui.profile.chart.ChartSegmentMathTest" --tests "com.woshiwangnima.healthdietpro.ui.profile.chart.LineStyleTest"
```

Expected: ALL tests PASS

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/woshiwangnima/healthdietpro/ui/profile/chart/ChartMath.kt app/src/test/java/com/woshiwangnima/healthdietpro/ui/profile/chart/ChartSegmentMathTest.kt
git commit -m "feat: add ChartMath engine (5 interpolations, NiceScale, label interval)"
```

---

### Task 4: ChartCanvas — the Canvas drawing View

**Files:**
- Create: `app/src/main/java/com/woshiwangnima/healthdietpro/ui/profile/chart/ChartCanvas.kt`

- [ ] **Step 1: Create ChartCanvas.kt**

```kotlin
package com.woshiwangnima.healthdietpro.ui.profile.chart

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.woshiwangnima.healthdietpro.model.profile.DataPoint
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
        color = 0x22000000
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }
    private val crosshairLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x88000000
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

    private val allPoints: List<DataPoint>
        get() = series.flatMap { it.points }

    private val dataMinValue: Float
        get() = allPoints.minOfOrNull { it.value } ?: 0f

    private val dataMaxValue: Float
        get() = allPoints.maxOfOrNull { it.value } ?: 100f

    private val chartYMin: Float
        get() {
            val dMin = dataMinValue
            val dMax = dataMaxValue
            val range = dMax - dMin
            return dMin + yMinPct / 100f * range
        }

    private val chartYMax: Float
        get() {
            val dMin = dataMinValue
            val dMax = dataMaxValue
            val range = dMax - dMin
            return dMin + yMaxPct / 100f * range
        }

    private val paddingLeft = 60f
    private val paddingRight = 20f
    private val paddingTop = 20f
    private val paddingBottom = 50f

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (allPoints.size < 2) return false
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
        val relX = paddingLeft + 0f
        val relRight = paddingLeft + chartW
        val xFraction = ((touchX - relX) / chartW).coerceIn(0f, 1f)
        val touchTs = windowStartMs + (xFraction * visibleRangeMs)

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
        val step = (niceMax - niceMin) / 4f
        for (i in 0..4) {
            val yVal = niceMin + step * i
            val py = chartB - ((yVal - niceMin) / (niceMax - niceMin)) * chartH
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
        if (hasTouch && allPoints.size >= 2) {
            drawCrosshairOverlay(canvas, chartL, chartT, chartR, chartB, chartW, chartH)
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
        if (filtered.size < 2) return

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
                // BEZIER or SPLINE: sample many points
                val samples = 200
                val minX = filtered.first().x
                val maxX = filtered.last().x
                for (k in 1..samples) {
                    val frac = k.toFloat() / samples
                    val sampleX = minX + (maxX - minX) * frac
                    val sampleY = if (s.lineStyle == LineStyle.SPLINE)
                        ChartMath.interpolateSpline(entries, sampleX)
                    else
                        ChartMath.interpolateBezier(entries, sampleX)
                    path.lineTo(screenX(sampleX), screenY(sampleY))
                }
            }
        }
        canvas.drawPath(path, linePaint)

        // Data point markers
        val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = s.color
            style = if (s.pointFill == PointFill.FILLED) Paint.Style.FILL else Paint.Style.STROKE
            strokeWidth = 2f
        }
        val rds = 5f * resources.displayMetrics.density
        for (entry in filtered) {
            val px = screenX(entry.x)
            val py = screenY(entry.y)
            if (px in l..r && py in t..b) {
                drawPointShape(canvas, px, py, rds, s.pointShape, pointPaint)
            }
        }
    }

    private fun drawPointShape(canvas: Canvas, cx: Float, cy: Float, r: Float, shape: PointShape, paint: Paint) {
        when (shape) {
            PointShape.CIRCLE -> canvas.drawCircle(cx, cy, r, paint)
            PointShape.TRIANGLE -> {
                val path = Path()
                path.moveTo(cx, cy - r); path.lineTo(cx - r * 0.866f, cy + r * 0.5f)
                path.lineTo(cx + r * 0.866f, cy + r * 0.5f); path.close()
                canvas.drawPath(path, paint)
            }
            PointShape.SQUARE -> canvas.drawRect(cx - r, cy - r, cx + r, cy + r, paint)
            PointShape.DIAMOND -> {
                val path = Path()
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
        cw: Float, ch: Float
    ) {
        val primarySeries = series.firstOrNull() ?: return
        val entries = ChartMath.toChartEntries(primarySeries.points, windowStartMs)
        if (entries.size < 2) return
        val xFraction = ((touchX - l) / cw).coerceIn(0f, 1f)
        val xVal = xFraction * visibleRangeMs.toFloat()
        val yVal = ChartMath.interpolateY(entries, xVal, primarySeries.lineStyle)

        fun screenX(v: Float) = l + (v / visibleRangeMs.toFloat()) * cw
        val yMin = chartYMin; val yRange = if (chartYMax > yMin) chartYMax - yMin else 1f
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
        val pad = 12f * resources.displayMetrics.density
        val tw1 = bubbleTextPaint.measureText(dateText)
        val tw2 = bubbleTextPaint.measureText(valueText)
        val maxW = maxOf(tw1, tw2)
        val fm = bubbleTextPaint.fontMetrics
        val lineH = fm.descent - fm.ascent
        val bw = maxW + pad * 2
        val bh = lineH * 2 + pad * 2 + 4f
        val offsetX = 12f * resources.displayMetrics.density
        val offsetY = -12f * resources.displayMetrics.density
        var bx = px + offsetX
        var by = py + offsetY - bh
        if (bx + bw > cr - 4f) bx = px - offsetX - bw
        if (by < ct + 4f) by = py - offsetY + 4f
        if (bx < cl + 4f) bx = cl + 4f
        if (by + bh > cb - 4f) by = cb - 4f - bh
        val rr = 8f * resources.displayMetrics.density
        val rect = RectF(bx, by, bx + bw, by + bh)
        canvas.drawRoundRect(rect, rr, rr, bubbleBgPaint)
        canvas.drawRoundRect(rect, rr, rr, bubbleBorderPaint)
        val tx = bx + pad
        val firstY = by + pad - fm.ascent
        canvas.drawText(dateText, tx, firstY, bubbleTextPaint)
        canvas.drawText(valueText, tx, firstY + lineH, bubbleTextPaint)
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/woshiwangnima/healthdietpro/ui/profile/chart/ChartCanvas.kt
git commit -m "feat: add ChartCanvas with Canvas-based chart rendering"
```

---

### Task 5: ChartView — the ViewGroup container

**Files:**
- Create: `app/src/main/java/com/woshiwangnima/healthdietpro/ui/profile/chart/ChartView.kt`
- Create: `app/src/main/res/layout/view_chart.xml`

- [ ] **Step 1: Create layout file view_chart.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<merge xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools">

    <LinearLayout
        android:id="@+id/controlsRow"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:gravity="center_vertical"
        android:paddingHorizontal="8dp"
        android:paddingVertical="4dp">

        <Spinner
            android:id="@+id/chartTypeSpinner"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:spinnerMode="dropdown" />

        <Space
            android:layout_width="0dp"
            android:layout_height="1dp"
            android:layout_weight="1" />

        <Spinner
            android:id="@+id/timeRangeSpinner"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:spinnerMode="dropdown" />

        <com.google.android.material.button.MaterialButton
            android:id="@+id/btnFullscreen"
            android:layout_width="wrap_content"
            android:layout_height="40dp"
            android:text="全屏"
            android:paddingHorizontal="8dp"
            android:contentDescription="全屏"
            app:icon="@drawable/ic_fullscreen"
            app:iconGravity="textStart"
            app:iconSize="20dp"
            style="@style/Widget.Material3.Button.TextButton" />

    </LinearLayout>

    <LinearLayout
        android:id="@+id/yAxisRow"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:gravity="center_vertical"
        android:paddingHorizontal="16dp"
        android:paddingVertical="2dp">

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="Y轴:"
            android:textSize="13sp"
            android:textColor="?attr/colorOnSurfaceVariant" />

        <EditText
            android:id="@+id/yMinInput"
            android:layout_width="56dp"
            android:layout_height="32dp"
            android:hint="0"
            android:text="0"
            android:textSize="13sp"
            android:gravity="center"
            android:inputType="numberDecimal|numberSigned"
            android:background="@drawable/input_bg"
            android:paddingHorizontal="4dp"
            android:layout_marginStart="4dp" />

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="%"
            android:textSize="13sp"
            android:textColor="?attr/colorOnSurfaceVariant" />

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="~"
            android:textSize="13sp"
            android:textColor="?attr/colorOnSurfaceVariant"
            android:layout_marginHorizontal="4dp" />

        <EditText
            android:id="@+id/yMaxInput"
            android:layout_width="56dp"
            android:layout_height="32dp"
            android:hint="100"
            android:text="100"
            android:textSize="13sp"
            android:gravity="center"
            android:inputType="numberDecimal|numberSigned"
            android:background="@drawable/input_bg"
            android:paddingHorizontal="4dp" />

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="%"
            android:textSize="13sp"
            android:textColor="?attr/colorOnSurfaceVariant" />

    </LinearLayout>

    <FrameLayout
        android:id="@+id/chartFrame"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1">

        <com.woshiwangnima.healthdietpro.ui.profile.chart.ChartCanvas
            android:id="@+id/chartCanvas"
            android:layout_width="match_parent"
            android:layout_height="match_parent" />

        <com.google.android.material.button.MaterialButton
            android:id="@+id/btnFullscreenOverlay"
            android:layout_width="wrap_content"
            android:layout_height="40dp"
            android:layout_gravity="top|end"
            android:layout_margin="8dp"
            android:text="缩放"
            android:paddingHorizontal="8dp"
            android:contentDescription="退出全屏"
            android:visibility="gone"
            app:icon="@drawable/ic_fullscreen_exit"
            app:iconGravity="textStart"
            app:iconSize="20dp"
            style="@style/Widget.Material3.Button.TextButton" />

    </FrameLayout>

    <FrameLayout
        android:id="@+id/dragIndicatorContainer"
        android:layout_width="match_parent"
        android:layout_height="44dp"
        android:visibility="gone">

        <FrameLayout
            android:id="@+id/progressBarContainer"
            android:layout_width="match_parent"
            android:layout_height="8dp"
            android:layout_gravity="top"
            android:paddingHorizontal="32dp"
            android:visibility="gone">

            <View
                android:id="@+id/progressBarBg"
                android:layout_width="match_parent"
                android:layout_height="2dp"
                android:layout_gravity="center_vertical"
                android:background="?attr/colorOutlineVariant" />

            <View
                android:id="@+id/progressBarThumb"
                android:layout_width="40dp"
                android:layout_height="6dp"
                android:layout_gravity="center_vertical"
                android:background="@drawable/timeline_thumb_bg" />

        </FrameLayout>

        <View
            android:id="@+id/dragIndicator"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:clickable="true"
            android:focusable="true" />

        <ImageButton
            android:id="@+id/dragArrowLeft"
            android:layout_width="32dp"
            android:layout_height="match_parent"
            android:layout_gravity="start|center_vertical"
            android:src="@drawable/ic_arrow_left"
            android:background="?attr/selectableItemBackgroundBorderless"
            android:contentDescription="左移"
            android:elevation="2dp" />

        <ImageButton
            android:id="@+id/dragArrowRight"
            android:layout_width="32dp"
            android:layout_height="match_parent"
            android:layout_gravity="end|center_vertical"
            android:src="@drawable/ic_arrow_right"
            android:background="?attr/selectableItemBackgroundBorderless"
            android:contentDescription="右移"
            android:elevation="2dp" />

    </FrameLayout>

    <LinearLayout
        android:id="@+id/legendLayout"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:gravity="center"
        android:paddingVertical="4dp"
        android:visibility="gone" />

</merge>
```

- [ ] **Step 2: Create ChartView.kt**

```kotlin
package com.woshiwangnima.healthdietpro.ui.profile.chart

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.ColorStateList
import android.graphics.Color
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
import com.woshiwangnima.healthdietpro.model.profile.DataPoint

class ChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val chartCanvas: ChartCanvas
    private val chartTypeSpinner: Spinner
    private val timeRangeSpinner: Spinner
    private val btnFullscreen: MaterialButton
    private val btnFullscreenOverlay: MaterialButton
    private val yMinInput: EditText
    private val yMaxInput: EditText
    private val controlsRow: LinearLayout
    private val yAxisRow: LinearLayout
    private val chartFrame: FrameLayout
    private val dragIndicatorContainer: FrameLayout
    private val progressBarContainer: FrameLayout
    private val progressBarThumb: View
    private val dragIndicator: View
    private val dragArrowLeft: ImageButton
    private val dragArrowRight: ImageButton
    private val legendLayout: LinearLayout

    private var series: List<ChartSeries> = emptyList()
    private var unitLabel: String = ""
    private var isFullscreen = false
    private var lineStyle: LineStyle = LineStyle.LINEAR
    private var onFullscreenListener: ((Boolean) -> Unit)? = null
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
        btnFullscreenOverlay = findViewById(R.id.btnFullscreenOverlay)
        yMinInput = findViewById(R.id.yMinInput)
        yMaxInput = findViewById(R.id.yMaxInput)
        controlsRow = findViewById(R.id.controlsRow)
        yAxisRow = findViewById(R.id.yAxisRow)
        chartFrame = findViewById(R.id.chartFrame)
        dragIndicatorContainer = findViewById(R.id.dragIndicatorContainer)
        progressBarContainer = findViewById(R.id.progressBarContainer)
        progressBarThumb = findViewById(R.id.progressBarThumb)
        dragIndicator = findViewById(R.id.dragIndicator)
        dragArrowLeft = findViewById(R.id.dragArrowLeft)
        dragArrowRight = findViewById(R.id.dragArrowRight)
        legendLayout = findViewById(R.id.legendLayout)

        initChartTypeSpinner()
        initTimeRangeSpinner()
        initFullscreenButtons()
        initYAxisInputs()
        initDragIndicator()
        setupTimeline()
    }

    // === Public API ===

    fun setSeries(series: List<ChartSeries>, unitLabel: String) {
        this.series = series
        this.unitLabel = unitLabel
        chartCanvas.series = series
        chartCanvas.unitLabel = unitLabel
        val allPoints = series.flatMap { it.points }
        if (allPoints.isEmpty()) return
        chartCanvas.visibleRangeMs = allPoints.maxOf { it.timestamp } - allPoints.minOf { it.timestamp }
        chartCanvas.windowStartMs = allPoints.minOf { it.timestamp }
        applyYAxisRange()
        rebuildTimeRangeSpinner()
        updateDragIndicator()
        updateLegend()
        chartCanvas.invalidate()
    }

    fun setLineStyle(style: LineStyle) {
        lineStyle = style
        series = series.map { it.copy(lineStyle = style) }
        chartCanvas.series = series
        chartCanvas.invalidate()
    }

    fun getLineStyle(): LineStyle = lineStyle

    fun setVisibleRange(millis: Long) {
        if (series.isEmpty()) return
        val allPoints = series.flatMap { it.points }
        val latestTs = allPoints.maxOf { it.timestamp }
        chartCanvas.visibleRangeMs = millis
        chartCanvas.windowStartMs = latestTs - millis
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

    fun toggleFullscreen() {
        isFullscreen = !isFullscreen
        updateFullscreenButton()
        onFullscreenListener?.invoke(isFullscreen)
        if (isFullscreen) {
            controlsRow.visibility = View.GONE
            yAxisRow.visibility = View.GONE
            legendLayout.visibility = View.GONE
            btnFullscreen.visibility = View.GONE
            btnFullscreenOverlay.visibility = View.VISIBLE
            dragIndicatorContainer.visibility = View.GONE
            progressBarContainer.visibility = View.GONE
            val lp = chartFrame.layoutParams as LinearLayout.LayoutParams
            lp.weight = 1f; lp.height = 0
            chartFrame.layoutParams = lp
        } else {
            controlsRow.visibility = View.VISIBLE
            yAxisRow.visibility = View.VISIBLE
            legendLayout.visibility = if (legendLayout.childCount > 0) View.VISIBLE else View.GONE
            btnFullscreen.visibility = View.VISIBLE
            btnFullscreenOverlay.visibility = View.GONE
            updateDragIndicator()
            val lp = chartFrame.layoutParams as LinearLayout.LayoutParams
            lp.weight = 0f
            lp.height = (resources.displayMetrics.heightPixels * 0.45).toInt()
            chartFrame.layoutParams = lp
        }
        chartCanvas.invalidate()
    }

    fun isFullscreen(): Boolean = isFullscreen
    fun setOnFullscreenListener(listener: ((Boolean) -> Unit)?) { onFullscreenListener = listener }

    // === Internal ===

    private fun initChartTypeSpinner() {
        val adapter = ArrayAdapter.createFromResource(context, R.array.chart_type_options, android.R.layout.simple_spinner_item)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        chartTypeSpinner.adapter = adapter
        chartTypeSpinner.setSelection(0)
        chartTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!chartTypeReady) return
                lineStyle = LineStyle.fromSpinnerPosition(position)
                series = series.map { it.copy(lineStyle = lineStyle) }
                chartCanvas.series = series
                chartCanvas.invalidate()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        chartTypeReady = true
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
                if (!timeRangeReady) return
                val rangeMs = opts[position].millis
                if (rangeMs == Long.MAX_VALUE) {
                    val allPoints = series.flatMap { it.points }
                    chartCanvas.visibleRangeMs = allPoints.maxOf { it.timestamp } - allPoints.minOf { it.timestamp }
                    chartCanvas.windowStartMs = allPoints.minOf { it.timestamp }
                } else {
                    val allPoints = series.flatMap { it.points }
                    val latestTs = allPoints.maxOf { it.timestamp }
                    chartCanvas.visibleRangeMs = rangeMs
                    chartCanvas.windowStartMs = latestTs - rangeMs
                }
                chartCanvas.clearCrosshair()
                chartCanvas.invalidate()
                updateDragIndicator()
                showTimeline()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        timeRangeReady = true
        if (opts.isNotEmpty()) {
            val rangeMs = opts[0].millis
            if (rangeMs != Long.MAX_VALUE) {
                chartCanvas.visibleRangeMs = rangeMs
                val allPoints = series.flatMap { it.points }
                chartCanvas.windowStartMs = allPoints.maxOf { it.timestamp } - rangeMs
            }
        }
    }

    private data class TimeRangeOption(val label: String, val millis: Long)

    private fun getTimeRangeOptions(): List<TimeRangeOption> {
        val allPoints = series.flatMap { it.points }
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
        return result
    }

    private fun initTimeRangeSpinner() {
        rebuildTimeRangeSpinner()
        timeRangeReady = true
    }

    private fun initFullscreenButtons() {
        val listener = View.OnClickListener { toggleFullscreen() }
        btnFullscreen.setOnClickListener(listener)
        btnFullscreenOverlay.setOnClickListener(listener)
    }

    private fun updateFullscreenButton() {
        val inFs = isFullscreen
        btnFullscreen.text = if (inFs) "缩放" else "全屏"
        btnFullscreen.setIconResource(if (inFs) R.drawable.ic_fullscreen_exit else R.drawable.ic_fullscreen)
        btnFullscreenOverlay.text = if (inFs) "缩放" else "全屏"
        btnFullscreenOverlay.setIconResource(if (inFs) R.drawable.ic_fullscreen_exit else R.drawable.ic_fullscreen)
    }

    private fun initYAxisInputs() {
        val watcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { applyYAxisRange() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }
        yMinInput.addTextChangedListener(watcher)
        yMaxInput.addTextChangedListener(watcher)
        yAxisReady = true
    }

    private fun applyYAxisRange() {
        if (!yAxisReady) return
        val allPoints = series.flatMap { it.points }
        if (allPoints.isEmpty()) return
        val dataMin = allPoints.minOf { it.value }
        val dataMax = allPoints.maxOf { it.value }
        val dataRange = dataMax - dataMin
        val minPct = yMinInput.text.toString().trim().toFloatOrNull() ?: 0f
        val maxPct = yMaxInput.text.toString().trim().toFloatOrNull() ?: 100f
        chartCanvas.yMinPct = minPct
        chartCanvas.yMaxPct = maxPct
        chartCanvas.invalidate()
    }

    private fun initDragIndicator() {
        dragIndicator.setOnTouchListener { _, event ->
            if (series.isEmpty()) return@setOnTouchListener false
            val allPoints = series.flatMap { it.points }
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
                    val maxStart = ((dataMax - visibleRange.toLong()) - dataMin).toFloat().coerceAtLeast(0f)
                    val pxPerMs = stripWidth / visibleRange
                    val deltaMs = -(deltaX / pxPerMs)
                    val newStart = ((chartCanvas.windowStartMs - dataMin).toFloat() + deltaMs).coerceIn(0f, maxStart)
                    chartCanvas.windowStartMs = dataMin + newStart.toLong()
                    chartCanvas.clearCrosshair()
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
            val allPoints = series.flatMap { it.points }
            val dataMin = allPoints.minOf { it.timestamp }
            val dataMax = allPoints.maxOf { it.timestamp }
            val step = chartCanvas.visibleRangeMs * 0.3f
            val newStart = (chartCanvas.windowStartMs - step.toLong()).coerceAtLeast(dataMin)
            chartCanvas.windowStartMs = newStart
            chartCanvas.clearCrosshair()
            chartCanvas.invalidate()
            post { updateDragArrows(); updateTimelineBar() }
        }
        dragArrowRight.setOnClickListener {
            showTimeline()
            val allPoints = series.flatMap { it.points }
            val dataMax = allPoints.maxOf { it.timestamp }
            val step = chartCanvas.visibleRangeMs * 0.3f
            val maxStart = dataMax - chartCanvas.visibleRangeMs
            val newStart = (chartCanvas.windowStartMs + step.toLong()).coerceAtMost(maxStart)
            chartCanvas.windowStartMs = newStart
            chartCanvas.clearCrosshair()
            chartCanvas.invalidate()
            post { updateDragArrows(); updateTimelineBar() }
        }
    }

    private fun updateDragIndicator() {
        val allPoints = series.flatMap { it.points }
        val dragEnabled = chartCanvas.visibleRangeMs != Long.MAX_VALUE && allPoints.size >= 2
        dragIndicatorContainer.visibility = if (dragEnabled && !isFullscreen) View.VISIBLE else View.GONE
        progressBarContainer.visibility = View.GONE
        isTimelineVisible = false
        if (dragEnabled) {
            post { updateDragArrows() }
        }
    }

    private fun updateDragArrows() {
        val allPoints = series.flatMap { it.points }
        if (allPoints.size < 2) {
            setArrowEnabled(dragArrowLeft, false)
            setArrowEnabled(dragArrowRight, false)
            return
        }
        val dataMin = allPoints.minOf { it.timestamp }
        val dataMax = allPoints.maxOf { it.timestamp }
        val tolerance = chartCanvas.visibleRangeMs * 0.01f
        val canScrollLeft = chartCanvas.windowStartMs > dataMin + tolerance.toLong()
        val canScrollRight = chartCanvas.windowStartMs + chartCanvas.visibleRangeMs < dataMax - tolerance.toLong()
        setArrowEnabled(dragArrowLeft, canScrollLeft)
        setArrowEnabled(dragArrowRight, canScrollRight)
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
        val allPoints = series.flatMap { it.points }
        val dragEnabled = chartCanvas.visibleRangeMs != Long.MAX_VALUE && allPoints.size >= 2
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
        val allPoints = series.flatMap { it.points }
        if (allPoints.size < 2) return
        val dataMin = allPoints.minOf { it.timestamp }
        val totalSpan = allPoints.maxOf { it.timestamp } - dataMin
        if (totalSpan <= 0L) return
        val barWidth = dragIndicatorContainer.width - dragIndicatorContainer.paddingLeft - dragIndicatorContainer.paddingRight
        if (barWidth <= 0) return
        val thumbWidthFraction = (chartCanvas.visibleRangeMs.toFloat() / totalSpan.toFloat()).coerceIn(0.05f, 1f)
        val thumbWidth = (barWidth * thumbWidthFraction).coerceAtLeast(20f)
        val relCenter = ((chartCanvas.windowStartMs + chartCanvas.visibleRangeMs / 2) - dataMin).toFloat().coerceAtLeast(0f)
        val centerFraction = (relCenter / totalSpan.toFloat()).coerceIn(0f, 1f)
        val left = (barWidth * centerFraction - thumbWidth / 2f).coerceIn(0f, (barWidth - thumbWidth).coerceAtLeast(0f))
        progressBarThumb.layoutParams = (progressBarThumb.layoutParams as FrameLayout.LayoutParams).also {
            it.width = thumbWidth.toInt()
            it.leftMargin = (left + dragIndicatorContainer.paddingLeft).toInt()
            it.rightMargin = 0
        }
        progressBarThumb.requestLayout()
    }

    private fun updateLegend() {
        legendLayout.removeAllViews()
        for (s in series) {
            legendLayout.addView(createLegendItem(s))
        }
        legendLayout.visibility = if (legendLayout.childCount > 0 && !isFullscreen) View.VISIBLE else View.GONE
    }

    private fun createLegendItem(s: ChartSeries): LinearLayout {
        val lineView = LegendLineView(context, s.color, s.lineType)
        val lp = LinearLayout.LayoutParams(
            (32 * resources.displayMetrics.density).toInt(),
            (8 * resources.displayMetrics.density).toInt()
        )
        lp.setMargins(0, 0, 4, 0)
        lp.gravity = Gravity.CENTER_VERTICAL
        lineView.layoutParams = lp
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
            containerLp.setMargins(8, 0, 8, 0)
            layoutParams = containerLp
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
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/woshiwangnima/healthdietpro/ui/profile/chart/ChartView.kt app/src/main/res/layout/view_chart.xml
git commit -m "feat: add ChartView ViewGroup with full chart UI"
```

---

### Task 6: Update resources (arrays.xml)

**Files:**
- Modify: `app/src/main/res/values/arrays.xml`

- [ ] **Step 1: Update chart_type_options to 5 items**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string-array name="chart_type_options">
        <item>折线图</item>
        <item>贝塞尔曲线</item>
        <item>自然样条曲线</item>
        <item>前置阶梯线</item>
        <item>后置阶梯线</item>
    </string-array>
    <string-array name="time_range_options">
        <item>全部</item>
        <item>1天</item>
        <item>1周</item>
        <item>1个月</item>
        <item>3个月</item>
        <item>6个月</item>
        <item>1年</item>
    </string-array>
</resources>
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/res/values/arrays.xml
git commit -m "feat: update chart_type_options (5 styles) and time_range_options (no sub-hour)"
```

---

### Task 7: Refactor WeightChartActivity to use ChartView

**Files:**
- Modify: `app/src/main/java/com/woshiwangnima/healthdietpro/ui/profile/chart/WeightChartActivity.kt`

- [ ] **Step 1: Rewrite WeightChartActivity**

```kotlin
package com.woshiwangnima.healthdietpro.ui.profile.chart

import android.os.Bundle
import android.view.WindowInsets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.base.BaseBackActivity
import com.woshiwangnima.healthdietpro.model.profile.BodyRecord
import com.woshiwangnima.healthdietpro.model.profile.DataPoint
import com.woshiwangnima.healthdietpro.model.unit.UnitCategory
import com.woshiwangnima.healthdietpro.util.UnitConverter
import com.woshiwangnima.healthdietpro.util.applySystemBarInsets
import java.time.LocalDate
import java.time.ZoneId

class WeightChartActivity : BaseBackActivity() {

    override fun getTitleText(): String = "体重历史"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val chartView = ChartView(this)
        chartView.applySystemBarInsets()
        setContentView(chartView)

        @Suppress("UNCHECKED_CAST")
        val records = (intent.getSerializableExtra(EXTRA_RECORDS, ArrayList::class.java) as? ArrayList<BodyRecord>) ?: emptyList()
        val unit = intent.getStringExtra(EXTRA_UNIT) ?: UnitCategory.DEFAULT_UNIT_WEIGHT
        val dataPoints = parseRecords(records, UnitCategory.ID_WEIGHT, unit)
        val series = ChartSeries(
            points = dataPoints,
            label = "测量值",
            color = resources.getColor(R.color.primary, null),
            lineStyle = LineStyle.LINEAR,
            lineType = LineType.SOLID,
            pointShape = PointShape.CIRCLE,
            pointFill = PointFill.FILLED
        )
        chartView.setSeries(listOf(series), unit)

        chartView.setOnFullscreenListener { isFs ->
            if (isFs) {
                window.decorView.windowInsetsController?.hide(WindowInsets.Type.systemBars())
                requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            } else {
                window.decorView.windowInsetsController?.show(WindowInsets.Type.systemBars())
                requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
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
```

- [ ] **Step 2: Do the same for HeightChartActivity**

```kotlin
package com.woshiwangnima.healthdietpro.ui.profile.chart

import android.os.Bundle
import android.view.WindowInsets
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
            points = dataPoints,
            label = "测量值",
            color = resources.getColor(R.color.primary, null),
            lineStyle = LineStyle.LINEAR,
            lineType = LineType.SOLID,
            pointShape = PointShape.CIRCLE,
            pointFill = PointFill.FILLED
        )
        chartView.setSeries(listOf(series), unit)

        chartView.setOnFullscreenListener { isFs ->
            if (isFs) {
                window.decorView.windowInsetsController?.hide(WindowInsets.Type.systemBars())
                requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            } else {
                window.decorView.windowInsetsController?.show(WindowInsets.Type.systemBars())
                requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
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
```

- [ ] **Step 3: Refactor ChartFragment to use ChartView**

Replace entire ChartFragment.kt content:

```kotlin
package com.woshiwangnima.healthdietpro.ui.profile.chart

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
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

    companion object {
        private const val ARG_RECORDS = "records"
        private const val ARG_UNIT = "unit"
        private const val ARG_CATEGORY = "category"
        private const val ARG_IS_HEIGHT = "is_height"

        fun newInstance(records: ArrayList<BodyRecord>, unit: String, category: String, isHeight: Boolean): ChartFragment {
            val fragment = ChartFragment()
            val args = Bundle()
            args.putSerializable(ARG_RECORDS, records)
            args.putString(ARG_UNIT, unit)
            args.putString(ARG_CATEGORY, category)
            args.putBoolean(ARG_IS_HEIGHT, isHeight)
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
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val chartView = ChartView(requireContext())
        chartView.id = R.id.chartView

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
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/woshiwangnima/healthdietpro/ui/profile/chart/WeightChartActivity.kt app/src/main/java/com/woshiwangnima/healthdietpro/ui/profile/chart/HeightChartActivity.kt app/src/main/java/com/woshiwangnima/healthdietpro/ui/profile/chart/ChartFragment.kt
git commit -m "refactor: migrate Weight/Height/ChartFragment to ChartView"
```

---

### Task 8: Cleanup — remove MPAndroidChart and old files

**Files:**
- Delete: `app/src/main/java/com/woshiwangnima/healthdietpro/ui/profile/chart/BaseChartActivity.kt`
- Delete: `app/src/main/res/layout/activity_base_chart.xml`
- Delete: `app/src/main/res/layout/marker_chart.xml`
- Delete: `app/src/main/java/com/woshiwangnima/healthdietpro/ui/profile/chart/ChartSegmentMath.kt`
- Modify: `app/build.gradle.kts` — remove MPAndroidChart dependency

- [ ] **Step 1: Remove MPAndroidChart from build.gradle.kts**

Find line 48: `implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")` and delete it.

- [ ] **Step 2: Delete old files**

```bash
Remove-Item "app/src/main/java/com/woshiwangnima/healthdietpro/ui/profile/chart/BaseChartActivity.kt" -Force
Remove-Item "app/src/main/res/layout/activity_base_chart.xml" -Force
Remove-Item "app/src/main/res/layout/marker_chart.xml" -Force
Remove-Item "app/src/main/java/com/woshiwangnima/healthdietpro/ui/profile/chart/ChartSegmentMath.kt" -Force
```

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "chore: remove MPAndroidChart dependency and old chart files"
```

---

### Task 9: Build verification

- [ ] **Step 1: Assemble debug APK**

```bash
./gradlew :app:assembleDebug
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Run all tests**

```bash
./gradlew :app:testDebugUnitTest
```

Expected: ALL tests pass

---

### Task 10: Git push

- [ ] **Step 1: Push changes**

```bash
git push origin master
```

# Chart Time-Axis, Stepped Lines & Xun Unit Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the `BaseChartActivity` / `ChartFragment` chart X-axis reflect real timestamps, the bottom drag strip move proportionally to visible time span, the crosshair read back the correct Y for each line style (折线 / 平滑曲线 / 后置阶梯线), add 后置阶梯线 to the chart-type spinner, and add 旬 (10 days) to `units.json`.

**Architecture:** Introduce a small `LineStyle` enum that owns the spinner-to-mode mapping. Replace the integer-index X entries with timestamp-based X entries plus a custom `ValueFormatter`. Rewrite `updateCrosshair` to branch on `LineStyle`. Rewrite the bottom drag math in `initDragIndicator` and the arrow click handlers to use millisecond ranges instead of entry counts. Pure helper `findSegmentIndex` is split out for unit-testability.

**Tech Stack:** Kotlin, MPAndroidChart v3.1.0, AndroidX, Gradle KTS, JUnit4 for local unit tests.

---

## File Structure

Files modified:

- `app/src/main/java/com/woshiwangnima/healthdietpro/ui/profile/chart/LineStyle.kt` (new) - enum + spinner mapping, pure Kotlin, testable.
- `app/src/main/java/com/woshiwangnima/healthdietpro/ui/profile/chart/ChartSegmentMath.kt` (new) - `findSegmentIndex` and `interpolateSegmentY` for linear/cubic/stepped, pure Kotlin, testable.
- `app/src/main/java/com/woshiwangnima/healthdietpro/ui/profile/chart/BaseChartActivity.kt` - wire the new enum + helper into the chart.
- `app/src/main/java/com/woshiwangnima/healthdietpro/ui/profile/chart/ChartFragment.kt` - mirror the X-axis fix and default mode.
- `app/src/main/res/values/arrays.xml` - add "后置阶梯线".
- `app/src/main/assets/units.json` - insert "旬".
- `app/src/test/java/com/woshiwangnima/healthdietpro/ui/profile/chart/LineStyleTest.kt` (new).
- `app/src/test/java/com/woshiwangnima/healthdietpro/ui/profile/chart/ChartSegmentMathTest.kt` (new).

Files unchanged:

- `DataPoint.kt`, `LineType`, `UnitConverter`, all layouts, all other activities.

---

## Task 1: Add `LineStyle` enum

**Files:**
- Create: `app/src/main/java/com/woshiwangnima/healthdietpro/ui/profile/chart/LineStyle.kt`
- Test: `app/src/test/java/com/woshiwangnima/healthdietpro/ui/profile/chart/LineStyleTest.kt`

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/woshiwangnima/healthdietpro/ui/profile/chart/LineStyleTest.kt`:

```kotlin
package com.woshiwangnima.healthdietpro.ui.profile.chart

import org.junit.Assert.assertEquals
import org.junit.Test

class LineStyleTest {

    @Test
    fun fromSpinnerPosition_mapsAllFourSlots() {
        assertEquals(LineStyle.LINEAR, LineStyle.fromSpinnerPosition(0))
        assertEquals(LineStyle.CUBIC_BEZIER, LineStyle.fromSpinnerPosition(1))
        assertEquals(LineStyle.STEPPED, LineStyle.fromSpinnerPosition(2))
    }

    @Test
    fun fromSpinnerPosition_outOfRangeDefaultsToLinear() {
        assertEquals(LineStyle.LINEAR, LineStyle.fromSpinnerPosition(-1))
        assertEquals(LineStyle.LINEAR, LineStyle.fromSpinnerPosition(99))
    }

    @Test
    fun toSpinnerPosition_roundTrip() {
        val all = listOf(LineStyle.LINEAR, LineStyle.CUBIC_BEZIER, LineStyle.STEPPED)
        for (style in all) {
            assertEquals(style, LineStyle.fromSpinnerPosition(LineStyle.toSpinnerPosition(style)))
        }
    }
}
```

- [ ] **Step 2: Run the test - verify it fails**

Run from the project root:

```bash
./gradlew :app:testDebugUnitTest --tests com.woshiwangnima.healthdietpro.ui.profile.chart.LineStyleTest
```

Expected: build error because `LineStyle` is undefined.

- [ ] **Step 3: Create the enum**

Create `app/src/main/java/com/woshiwangnima/healthdietpro/ui/profile/chart/LineStyle.kt`:

```kotlin
package com.woshiwangnima.healthdietpro.ui.profile.chart

enum class LineStyle {
    LINEAR,
    CUBIC_BEZIER,
    STEPPED;

    companion object {
        fun fromSpinnerPosition(pos: Int): LineStyle = when (pos) {
            1 -> CUBIC_BEZIER
            2 -> STEPPED
            else -> LINEAR
        }

        fun toSpinnerPosition(style: LineStyle): Int = when (style) {
            LINEAR -> 0
            CUBIC_BEZIER -> 1
            STEPPED -> 2
        }
    }
}
```

- [ ] **Step 4: Run the test - verify it passes**

```bash
./gradlew :app:testDebugUnitTest --tests com.woshiwangnima.healthdietpro.ui.profile.chart.LineStyleTest
```

Expected: 3 tests pass.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/woshiwangnima/healthdietpro/ui/profile/chart/LineStyle.kt \
        app/src/test/java/com/woshiwangnima/healthdietpro/ui/profile/chart/LineStyleTest.kt
git commit -m "feat(chart): introduce LineStyle enum"
```

---

## Task 2: Extract segment math helpers

**Files:**
- Create: `app/src/main/java/com/woshiwangnima/healthdietpro/ui/profile/chart/ChartSegmentMath.kt`
- Test: `app/src/test/java/com/woshiwangnima/healthdietpro/ui/profile/chart/ChartSegmentMathTest.kt`

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/woshiwangnima/healthdietpro/ui/profile/chart/ChartSegmentMathTest.kt`:

```kotlin
package com.woshiwangnima.healthdietpro.ui.profile.chart

import org.junit.Assert.assertEquals
import org.junit.Test

class ChartSegmentMathTest {

    private val entries = listOf(
        floatEntry(0f, 10f),
        floatEntry(100f, 20f),
        floatEntry(300f, 40f),
        floatEntry(700f, 100f),
    )

    @Test
    fun findSegmentIndex_returnsContainingSegment() {
        assertEquals(0, ChartSegmentMath.findSegmentIndex(entries, 0f))
        assertEquals(0, ChartSegmentMath.findSegmentIndex(entries, 50f))
        assertEquals(0, ChartSegmentMath.findSegmentIndex(entries, 100f))
        assertEquals(1, ChartSegmentMath.findSegmentIndex(entries, 100.01f))
        assertEquals(1, ChartSegmentMath.findSegmentIndex(entries, 200f))
        assertEquals(2, ChartSegmentMath.findSegmentIndex(entries, 400f))
        assertEquals(2, ChartSegmentMath.findSegmentIndex(entries, 700f))
    }

    @Test
    fun findSegmentIndex_clampsToLastSegment() {
        assertEquals(2, ChartSegmentMath.findSegmentIndex(entries, 5000f))
    }

    @Test
    fun interpolateLinear_returnsYAtMidpoint() {
        val y = ChartSegmentMath.interpolateLinear(entries, 50f)
        assertEquals(15f, y, 0.0001f)
    }

    @Test
    fun interpolateLinear_returnsRightYAtBoundary() {
        val y = ChartSegmentMath.interpolateLinear(entries, 100f)
        assertEquals(20f, y, 0.0001f)
    }

    @Test
    fun interpolateStepped_returnsRightY() {
        val y = ChartSegmentMath.interpolateStepped(entries, 50f)
        assertEquals(20f, y, 0.0001f)
        val y700 = ChartSegmentMath.interpolateStepped(entries, 700f)
        assertEquals(100f, y700, 0.0001f)
    }

    private fun floatEntry(x: Float, y: Float) =
        com.github.mikephil.charting.data.Entry(x, y)
}
```

- [ ] **Step 2: Run the test - verify it fails**

```bash
./gradlew :app:testDebugUnitTest --tests com.woshiwangnima.healthdietpro.ui.profile.chart.ChartSegmentMathTest
```

Expected: build error because `ChartSegmentMath` is undefined.

- [ ] **Step 3: Create the helpers**

Create `app/src/main/java/com/woshiwangnima/healthdietpro/ui/profile/chart/ChartSegmentMath.kt`:

```kotlin
package com.woshiwangnima.healthdietpro.ui.profile.chart

import com.github.mikephil.charting.data.Entry

object ChartSegmentMath {

    fun findSegmentIndex(entries: List<Entry>, x: Float): Int {
        if (entries.size < 2) return 0
        for (i in 0 until entries.size - 1) {
            if (x >= entries[i].x && x <= entries[i + 1].x) return i
        }
        return (entries.size - 2).coerceAtLeast(0)
    }

    fun interpolateLinear(entries: List<Entry>, x: Float): Float {
        if (entries.size < 2) return entries.firstOrNull()?.y ?: 0f
        val i = findSegmentIndex(entries, x)
        val a = entries[i]
        val b = entries[i + 1]
        val span = b.x - a.x
        val t = if (span == 0f) 0f else (x - a.x) / span
        return a.y + t * (b.y - a.y)
    }

    fun interpolateStepped(entries: List<Entry>, x: Float): Float {
        if (entries.size < 2) return entries.firstOrNull()?.y ?: 0f
        val i = findSegmentIndex(entries, x)
        return entries[i + 1].y
    }
}
```

- [ ] **Step 4: Run the test - verify it passes**

```bash
./gradlew :app:testDebugUnitTest --tests com.woshiwangnima.healthdietpro.ui.profile.chart.ChartSegmentMathTest
```

Expected: 5 tests pass.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/woshiwangnima/healthdietpro/ui/profile/chart/ChartSegmentMath.kt \
        app/src/test/java/com/woshiwangnima/healthdietpro/ui/profile/chart/ChartSegmentMathTest.kt
git commit -m "feat(chart): extract segment math helpers"
```

---

## Task 3: Wire `LineStyle` + new X entries into `BaseChartActivity.updateChart`

**Files:**
- Modify: `app/src/main/java/com/woshiwangnima/healthdietpro/ui/profile/chart/BaseChartActivity.kt:566-623`

- [ ] **Step 1: Replace the entry-building block and axis formatter**

In `BaseChartActivity.kt`, locate the `updateChart()` method. The current body is roughly:

```kotlin
val entries = filteredDataPoints.mapIndexed { index, dp ->
    Entry(index.toFloat(), dp.value)
}
val labels = filteredDataPoints.map { it.dateLabel }

val isSmooth = binding.chartTypeSpinner.selectedItemPosition == 1

val dataSet = LineDataSet(entries, "测量值").apply {
    ...
    mode = if (isSmooth) LineDataSet.Mode.CUBIC_BEZIER else LineDataSet.Mode.LINEAR
    applyLineType(getMainLineType())
}
```

Replace the entry-building + label block and the mode line with:

```kotlin
val entries = filteredDataPoints.map { dp ->
    Entry(dp.timestamp.toFloat(), dp.value)
}
val labelsByTimestamp: Map<Long, String> =
    filteredDataPoints.associate { it.timestamp to it.dateLabel }

val style = LineStyle.fromSpinnerPosition(binding.chartTypeSpinner.selectedItemPosition)

val dataSet = LineDataSet(entries, "测量值").apply {
    color = resources.getColor(R.color.primary, null)
    setCircleColor(resources.getColor(R.color.primary, null))
    lineWidth = 2f
    circleRadius = 3f
    setDrawValues(false)
    setDrawCircleHole(false)
    mode = mpModeFor(style)
    applyLineType(getMainLineType())
}
```

Also, locate the inner chart.apply block at the bottom of `updateChart`:

```kotlin
chart.apply {
    data = lineData
    xAxis.valueFormatter = IndexAxisValueFormatter(labels)
    xAxis.setLabelCount(5, false)
    axisRight.axisMinimum = 0f
    fitScreen()
    invalidate()
}
```

Replace with:

```kotlin
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
```

Add a private helper near the bottom of the class (just above `private fun applyLineType`):

```kotlin
private fun mpModeFor(style: LineStyle): LineDataSet.Mode = when (style) {
    LineStyle.LINEAR -> LineDataSet.Mode.LINEAR
    LineStyle.CUBIC_BEZIER -> LineDataSet.Mode.CUBIC_BEZIER
    LineStyle.STEPPED -> LineDataSet.Mode.STEPPED
}
```

- [ ] **Step 2: Compile**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Run all unit tests**

```bash
./gradlew :app:testDebugUnitTest
```

Expected: all previously passing tests still pass.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/woshiwangnima/healthdietpro/ui/profile/chart/BaseChartActivity.kt
git commit -m "feat(chart): base activity uses timestamp X axis"
```

---

## Task 4: Update spinner listener and `updateChartStyle` for new enum

**Files:**
- Modify: `app/src/main/java/com/woshiwangnima/healthdietpro/ui/profile/chart/BaseChartActivity.kt:161-173, 220-227`

- [ ] **Step 1: Replace `initChartTypeSpinner`'s callback body**

Locate:

```kotlin
binding.chartTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        if (!chartTypeReady) return
        updateChartStyle(position == 1)
    }
    override fun onNothingSelected(parent: AdapterView<*>?) {}
}
```

Replace with:

```kotlin
binding.chartTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        if (!chartTypeReady) return
        updateChartStyle(LineStyle.fromSpinnerPosition(position))
    }
    override fun onNothingSelected(parent: AdapterView<*>?) {}
}
```

- [ ] **Step 2: Replace `updateChartStyle(smooth: Boolean)` with the enum-based version**

Locate:

```kotlin
private fun updateChartStyle(smooth: Boolean) {
    val data = chart.data ?: return
    for (i in 0 until data.dataSetCount) {
        val ds = data.getDataSetByIndex(i) as? LineDataSet ?: continue
        ds.mode = if (smooth) LineDataSet.Mode.CUBIC_BEZIER else LineDataSet.Mode.LINEAR
    }
    chart.invalidate()
}
```

Replace with:

```kotlin
private fun updateChartStyle(style: LineStyle) {
    val data = chart.data ?: return
    val mode = mpModeFor(style)
    for (i in 0 until data.dataSetCount) {
        val ds = data.getDataSetByIndex(i) as? LineDataSet ?: continue
        ds.mode = mode
    }
    chart.invalidate()
}
```

- [ ] **Step 3: Compile**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/woshiwangnima/healthdietpro/ui/profile/chart/BaseChartActivity.kt
git commit -m "feat(chart): spinner drives LineStyle enum"
```

---

## Task 5: Rewrite `updateCrosshair` to honour `LineStyle`

**Files:**
- Modify: `app/src/main/java/com/woshiwangnima/healthdietpro/ui/profile/chart/BaseChartActivity.kt:396-447`

- [ ] **Step 1: Replace the method body**

Locate `updateCrosshair` and replace its body with:

```kotlin
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
```

`interpolateCubicBezier` is kept as-is.

- [ ] **Step 2: Compile**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Run unit tests**

```bash
./gradlew :app:testDebugUnitTest
```

Expected: all tests pass.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/woshiwangnima/healthdietpro/ui/profile/chart/BaseChartActivity.kt
git commit -m "feat(chart): crosshair Y readback matches LineStyle"
```

---

## Task 6: Bottom drag math now uses millisecond range

**Files:**
- Modify: `app/src/main/java/com/woshiwangnima/healthdietpro/ui/profile/chart/BaseChartActivity.kt:461-518`

- [ ] **Step 1: Replace `updateDragArrows`**

Locate:

```kotlin
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
```

Replace with:

```kotlin
private fun updateDragArrows() {
    val visibleRange = chart.highestVisibleX - chart.lowestVisibleX
    if (visibleRange <= 0f || filteredDataPoints.size < 2) {
        binding.dragArrowLeft.visibility = View.GONE
        binding.dragArrowRight.visibility = View.GONE
        return
    }
    val firstTs = filteredDataPoints.first().timestamp.toFloat()
    val lastTs = filteredDataPoints.last().timestamp.toFloat()
    val toleranceMs = 60_000f
    binding.dragArrowLeft.visibility =
        if (chart.lowestVisibleX > firstTs + toleranceMs) View.VISIBLE else View.GONE
    binding.dragArrowRight.visibility =
        if (chart.highestVisibleX < lastTs - toleranceMs) View.VISIBLE else View.GONE
}
```

- [ ] **Step 2: Replace the drag strip touch handler inside `initDragIndicator`**

Locate the `binding.dragIndicator.setOnTouchListener { ... }` block. Replace it with:

```kotlin
binding.dragIndicator.setOnTouchListener { _, event ->
    if (currentRangeMillis == Long.MAX_VALUE || filteredDataPoints.size < 2)
        return@setOnTouchListener false
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
            val stripWidth = binding.dragIndicatorContainer.width.toFloat()
            if (stripWidth <= 0f) return@setOnTouchListener true
            val pxPerMs = stripWidth / visibleRange
            val deltaMs = -(deltaX / pxPerMs)
            val firstTs = filteredDataPoints.first().timestamp.toFloat()
            val lastTs = filteredDataPoints.last().timestamp.toFloat()
            val maxStart = (lastTs - visibleRange).coerceAtLeast(firstTs)
            val newLow = (chart.lowestVisibleX + deltaMs).coerceIn(firstTs, maxStart)
            chart.moveViewToX(newLow)
            chart.post { updateDragArrows() }
            true
        }
        else -> false
    }
}
```

- [ ] **Step 3: Replace the left-arrow click handler**

Locate:

```kotlin
binding.dragArrowLeft.setOnClickListener {
    val visibleRange = chart.highestVisibleX - chart.lowestVisibleX
    if (visibleRange <= 0f) return@setOnClickListener
    val step = (visibleRange * 0.3f).coerceAtLeast(1f)
    val maxStart = (filteredDataPoints.size - visibleRange.toInt()).coerceAtLeast(0)
    val newLow = (chart.lowestVisibleX - step).coerceIn(0f, maxStart.toFloat())
    chart.moveViewToX(newLow)
    chart.post { updateDragArrows() }
}
```

Replace with:

```kotlin
binding.dragArrowLeft.setOnClickListener {
    val visibleRange = chart.highestVisibleX - chart.lowestVisibleX
    if (visibleRange <= 0f || filteredDataPoints.size < 2) return@setOnClickListener
    val step = (visibleRange * 0.3f).coerceAtLeast(1f)
    val firstTs = filteredDataPoints.first().timestamp.toFloat()
    val lastTs = filteredDataPoints.last().timestamp.toFloat()
    val maxStart = (lastTs - visibleRange).coerceAtLeast(firstTs)
    val newLow = (chart.lowestVisibleX - step).coerceIn(firstTs, maxStart)
    chart.moveViewToX(newLow)
    chart.post { updateDragArrows() }
}
```

- [ ] **Step 4: Replace the right-arrow click handler**

Locate:

```kotlin
binding.dragArrowRight.setOnClickListener {
    val visibleRange = chart.highestVisibleX - chart.lowestVisibleX
    if (visibleRange <= 0f) return@setOnClickListener
    val step = (visibleRange * 0.3f).coerceAtLeast(1f)
    val maxStart = (filteredDataPoints.size - visibleRange.toInt()).coerceAtLeast(0)
    val newLow = (chart.lowestVisibleX + step).coerceIn(0f, maxStart.toFloat())
    chart.moveViewToX(newLow)
    chart.post { updateDragArrows() }
}
```

Replace with:

```kotlin
binding.dragArrowRight.setOnClickListener {
    val visibleRange = chart.highestVisibleX - chart.lowestVisibleX
    if (visibleRange <= 0f || filteredDataPoints.size < 2) return@setOnClickListener
    val step = (visibleRange * 0.3f).coerceAtLeast(1f)
    val firstTs = filteredDataPoints.first().timestamp.toFloat()
    val lastTs = filteredDataPoints.last().timestamp.toFloat()
    val maxStart = (lastTs - visibleRange).coerceAtLeast(firstTs)
    val newLow = (chart.lowestVisibleX + step).coerceIn(firstTs, maxStart)
    chart.moveViewToX(newLow)
    chart.post { updateDragArrows() }
}
```

- [ ] **Step 5: Compile**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Run unit tests**

```bash
./gradlew :app:testDebugUnitTest
```

Expected: all tests pass.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/woshiwangnima/healthdietpro/ui/profile/chart/BaseChartActivity.kt
git commit -m "feat(chart): drag strip proportional to visible time span"
```

---

## Task 7: Mirror the X-axis fix in `ChartFragment`

**Files:**
- Modify: `app/src/main/java/com/woshiwangnima/healthdietpro/ui/profile/chart/ChartFragment.kt:51-93`

- [ ] **Step 1: Replace the entry-building block**

In `ChartFragment.kt` locate:

```kotlin
val entries = sorted.mapIndexed { index, record ->
    val convertedValue = UnitConverter.fromBase(category, record.value, unitId)
    Entry(index.toFloat(), convertedValue)
}
val labels = sorted.map { it.date.takeLast(5) }
```

Replace with:

```kotlin
val entries = sorted.map { record ->
    val convertedValue = UnitConverter.fromBase(category, record.value, unitId)
    val localDate = java.time.LocalDate.parse(record.date)
    val ts = localDate.atStartOfDay(java.time.ZoneId.systemDefault())
        .toInstant().toEpochMilli()
    Entry(ts.toFloat(), convertedValue)
}
val labelsByTimestamp: Map<Long, String> = sorted.associate { record ->
    val localDate = java.time.LocalDate.parse(record.date)
    val ts = localDate.atStartOfDay(java.time.ZoneId.systemDefault())
        .toInstant().toEpochMilli()
    ts to record.date.takeLast(5)
}
```

- [ ] **Step 2: Replace the mode assignment and the formatter**

Locate:

```kotlin
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
```

Replace with:

```kotlin
val dataSet = LineDataSet(entries, "").apply {
    color = resources.getColor(R.color.primary, null)
    setCircleColor(resources.getColor(R.color.primary, null))
    lineWidth = 2f
    circleRadius = 4f
    setDrawValues(false)
    mode = LineDataSet.Mode.LINEAR
}
chart.apply {
    data = LineData(dataSet)
    xAxis.apply {
        valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
            override fun getFormattedValue(value: Float): String =
                labelsByTimestamp[value.toLong()] ?: ""
        }
        position = XAxis.XAxisPosition.BOTTOM
        granularity = 1f
        setDrawGridLines(false)
    }
    axisLeft.axisMinimum = 0f
    axisRight.isEnabled = false
    legend.isEnabled = false
    description.isEnabled = false
    fitScreen()
    animateX(1000)
    invalidate()
}
```

- [ ] **Step 3: Remove the now-unused `IndexAxisValueFormatter` import**

In `ChartFragment.kt` remove:

```kotlin
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
```

- [ ] **Step 4: Compile**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/woshiwangnima/healthdietpro/ui/profile/chart/ChartFragment.kt
git commit -m "feat(chart): ChartFragment uses timestamp X axis"
```

---

## Task 8: Add "后置阶梯线" to the chart-type spinner

**Files:**
- Modify: `app/src/main/res/values/arrays.xml:3-6`

- [ ] **Step 1: Add the new item**

Replace:

```xml
<string-array name="chart_type_options">
    <item>折线图</item>
    <item>平滑曲线</item>
</string-array>
```

With:

```xml
<string-array name="chart_type_options">
    <item>折线图</item>
    <item>平滑曲线</item>
    <item>后置阶梯线</item>
</string-array>
```

- [ ] **Step 2: Compile (resources only - will pass on next build)**

```bash
./gradlew :app:processDebugResources
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/values/arrays.xml
git commit -m "feat(chart): add 后置阶梯线 to chart type options"
```

---

## Task 9: Add 旬 (10 days) to `units.json`

**Files:**
- Modify: `app/src/main/assets/units.json:62-64`

- [ ] **Step 1: Validate JSON before edit**

```bash
node -e "JSON.parse(require('fs').readFileSync('app/src/main/assets/units.json','utf8')); console.log('ok')"
```

Expected: `ok`.

- [ ] **Step 2: Insert the xun entry**

Replace:

```json
{ "id": "week", "symbolCn": "周(星期)",       "symbolEn": "wk",  "toBase": 604800 },
{ "id": "month","symbolCn": "月(月份)",       "symbolEn": "mo",  "toBase": 2592000 }
```

With:

```json
{ "id": "week", "symbolCn": "周(星期)",       "symbolEn": "wk",  "toBase": 604800 },
{ "id": "xun",  "symbolCn": "旬(10天)",       "symbolEn": "xun", "toBase": 864000 },
{ "id": "month","symbolCn": "月(月份)",       "symbolEn": "mo",  "toBase": 2592000 }
```

- [ ] **Step 3: Validate JSON after edit**

```bash
node -e "JSON.parse(require('fs').readFileSync('app/src/main/assets/unities.json','utf8')); console.log('ok')"
```

Wait — fix that command (typo `units.json`):

```bash
node -e "JSON.parse(require('fs').readFileSync('app/src/main/assets/units.json','utf8')); console.log('ok')"
```

Expected: `ok`.

- [ ] **Step 4: Add a unit-loading sanity test**

Create `app/src/test/java/com/woshiwangnima/healthdietpro/assets/UnitsJsonTest.kt`:

```kotlin
package com.woshiwangnima.healthdietpro.assets

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UnitsJsonTest {

    @Test
    fun timeCategoryContainsXunBetweenWeekAndMonth() {
        val json = readUnits()
        val time = json.first { it.getString("id") == "time" }
        val ids = time.getJSONArray("units").let { arr ->
            (0 until arr.length()).map { arr.getJSONObject(it).getString("id") }
        }
        val weekIdx = ids.indexOf("week")
        val xunIdx = ids.indexOf("xun")
        val monthIdx = ids.indexOf("month")
        assertTrue("week index must exist", weekIdx >= 0)
        assertTrue("xun index must exist", xunIdx >= 0)
        assertTrue("month index must exist", monthIdx >= 0)
        assertTrue("xun must come after week", xunIdx > weekIdx)
        assertTrue("xun must come before month", xunIdx < monthIdx)
    }

    @Test
    fun xunHasCorrectToBase() {
        val json = readUnits()
        val time = json.first { it.getString("id") == "time" }
        val xun = (0 until time.getJSONArray("units").length())
            .map { time.getJSONArray("units").getJSONObject(it) }
            .first { it.getString("id") == "xun" }
        assertEquals(864000.0, xun.getDouble("toBase"), 0.0001)
    }

    private fun readUnits(): List<JSONObject> {
        val raw = java.io.File("src/main/assets/units.json")
            .readText()
        val arr = org.json.JSONArray(raw)
        return (0 until arr.length()).map { arr.getJSONObject(it) }
    }
}
```

- [ ] **Step 5: Run the new test**

```bash
./gradlew :app:testDebugUnitTest --tests com.woshiwangnima.healthdietpro.assets.UnitsJsonTest
```

Expected: 2 tests pass.

If `org.json` is not on the test classpath, add to `app/build.gradle.kts` inside `dependencies`:

```kotlin
testImplementation("org.json:json:20240303")
```

then re-run.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/assets/units.json \
        app/src/test/java/com/woshiwangnima/healthdietpro/assets/UnitsJsonTest.kt
git commit -m "feat(units): add 旬 (10 days) between 周 and 月"
```

---

## Task 10: Final verification

- [ ] **Step 1: Full unit test suite**

```bash
./gradlew :app:testDebugUnitTest
```

Expected: all tests pass.

- [ ] **Step 2: Lint (if configured)**

```bash
./gradlew :app:lintDebug
```

Expected: no new errors. Warnings about MPAndroidChart package are pre-existing.

- [ ] **Step 3: Build debug APK**

```bash
./gradlew :app:assembleDebug
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Manual smoke checklist (documented, not enforced by gradle)**

Run on emulator/device:

1. Open weight chart. With records spanning 2 days vs 30 days, the wider gap appears wider on the X axis.
2. Switch chart type to 后置阶梯线; tap on a step - the crosshair's value equals the right-endpoint of that step.
3. Switch to 平滑曲线; the crosshair value tracks the visible cubic curve.
4. Select "1周" time range; drag the bottom strip - the visible window pans by an amount proportional to visible time span, not entry count.
5. Open the time-unit picker; "旬" appears between "周" and "月"; picking it and entering a value round-trips correctly.

- [ ] **Step 5: Final commit (only if anything changed)**

```bash
git status
```

If anything is dirty, commit with a descriptive message. Otherwise proceed.

---

## Self-Review Notes

- Spec coverage:
  - X axis uses timestamp - Tasks 3, 7.
  - Drag proportional to time - Task 6.
  - Crosshair matches LineStyle - Task 5.
  - 后置阶梯线 in spinner - Task 8.
  - 旬 in units.json - Task 9.
  - Per user, 前置阶梯线 is intentionally omitted (MPAndroidChart has no native support; user chose to skip).
- Placeholder scan: no TBDs; every step has concrete code or commands.
- Type consistency: `LineStyle` enum defined once in Task 1 and used unchanged in Tasks 3-7. `ChartSegmentMath` defined once in Task 2 and used in Tasks 5-7. `mpModeFor` helper added in Task 3 and reused in Task 4.
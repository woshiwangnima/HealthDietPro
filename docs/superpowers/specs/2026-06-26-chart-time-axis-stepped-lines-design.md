# Chart Time-Axis, Stepped Lines & Xun Unit - Design

## Purpose

Bring the existing `BaseChartActivity` / `ChartFragment` chart in line with user-visible reality on three fronts:

1. **X axis uses real time**, not index. Two data points whose timestamps differ by 3 days are spaced 3× farther apart than two points 1 day apart. Today every point sits at integer index, so a missing day looks identical to a recorded day.
2. **The bottom drag strip controls the *visible* range correctly under the new time-spaced axis.** The strip's visual is unchanged (a uniform track with arrows) but the drag math is now proportional to *visible time span*, not to *entry count*.
3. **The crosshair reads the value the user actually sees.** Today the crosshair does linear interpolation regardless of style, so on a smoothed curve it briefly diverges from the visible curve, and there is no path for stepped styles at all. Add *后置阶梯线* to the chart-type list (no native 前置阶梯 exists in MPAndroidChart; user chose to skip it) and let each style drive its own Y-readback.

Also add a new time unit **旬 (10 days)** between **周** and **月** in `units.json`.

## Scope

In scope:

- `BaseChartActivity.kt` - the chart host activity.
- `ChartFragment.kt` - the simpler fragment-based chart used elsewhere in the app.
- `app/src/main/res/values/arrays.xml` - chart-type dropdown strings.
- `app/src/main/assets/units.json` - new time unit.
- `app/src/main/res/layout/activity_base_chart.xml` - unchanged (the spinner entry list is data-driven; the drag-strip view tree is unchanged).

Out of scope:

- `LineType` (SOLID / DASHED / DOTTED) and `applyLineType` - stays as-is and still composes with the new styles.
- `DataPoint` model - already carries `timestamp`, no schema change.
- `UnitConverter`, category loading, other chart consumers.
- Label formats - keep `MM-dd` (already in use).

## Design

### 1. New enum: `LineStyle`

`BaseChartActivity.kt` (line 39 already declares `LineType`; the new enum sits next to it). MPAndroidChart's `LineDataSet.Mode` exposes only one stepped variant (`STEPPED`, which is a *后置阶梯*); no native 前置阶梯 exists. Per user decision, we add only 后置阶梯线.

```kotlin
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

The three-way mapping matches the array order set in `arrays.xml`.

### 2. X axis uses timestamp

`updateChart()` (BaseChartActivity.kt:566) currently builds:

```kotlin
val entries = filteredDataPoints.mapIndexed { index, dp ->
    Entry(index.toFloat(), dp.value)
}
```

Change to:

```kotlin
val entries = filteredDataPoints.map { dp ->
    Entry(dp.timestamp.toFloat(), dp.value)
}
```

`IndexAxisValueFormatter(labels)` is fed labels by *position*, which is index-based, not X-based. MPAndroidChart calls `formatter.getFormattedValue(value, axis)` where `value` is the raw float X. The current code only works because X == index. To make labels follow timestamps we replace it with a custom `ValueFormatter`:

```kotlin
val labelByTimestamp: (Float) -> String = { xf ->
    val ts = xf.toLong()
    val idx = filteredDataPoints.indexOfFirst { it.timestamp == ts }
    if (idx >= 0) filteredDataPoints[idx].dateLabel else ""
}

chart.xAxis.valueFormatter = object : ValueFormatter() {
    override fun getFormattedValue(value: Float): String = labelByTimestamp(value)
}
```

Same change applied in `ChartFragment.kt:81`.

`setLabelCount(5, false)` continues to work - MPAndroidChart will pick five tick marks across the X axis range.

`fitScreen()` (BaseChartActivity.kt:619) remains valid - it scales the visible window to the data range, now `[first.timestamp, last.timestamp]`.

### 3. Bottom drag strip - proportional to time

The drag strip visual (a uniform `dragIndicator` View) is unchanged per the user. What changes is the math in `initDragIndicator` (BaseChartActivity.kt:474) and `updateDragArrows` (BaseChartActivity.kt:461):

- `visibleRange = chart.highestVisibleX - chart.lowestVisibleX` is now in **milliseconds**, not in entry counts. The existing variable name still applies.
- `pxPerEntry` → `pxPerMs = binding.dragIndicatorContainer.width / visibleRange`.
- `deltaEntry` → `deltaMs = -(deltaX / pxPerMs)`.
- `maxStart` is no longer `filteredDataPoints.size - visibleRange.toInt()`. New bound:

```kotlin
val firstTs = filteredDataPoints.first().timestamp
val lastTs = filteredDataPoints.last().timestamp
val totalSpanMs = lastTs - firstTs
val maxStart = (totalSpanMs - visibleRange).coerceAtLeast(0f)
```

(Equivalent: clamp `newLow` into `[firstTs, lastTs - visibleRange]`.)

Arrow button step (BaseChartActivity.kt:499-517): replace `step = visibleRange * 0.3f` (already in ms now) and clamp against `maxStart` as above. Code shrinks; same shape.

### 4. Crosshair Y readback matches the visible line style

Replace `updateCrosshair` (BaseChartActivity.kt:396) to branch on `LineStyle`:

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
        LineStyle.LINEAR -> {
            val i = findSegmentIndex(entries, chartX)
            val t = (chartX - entries[i].x) / (entries[i + 1].x - entries[i].x)
            entries[i].y + t * (entries[i + 1].y - entries[i].y)
        }
        LineStyle.CUBIC_BEZIER -> {
            val i = findSegmentIndex(entries, chartX)
            interpolateCubicBezier(entries, chartX.toDouble(), i).toFloat()
        }
        LineStyle.STEPPED -> {
            val i = findSegmentIndex(entries, chartX)
            entries[i + 1].y
        }
    }
    crosshairView.setCrosshair(chartX, y, entries, filteredDataPoints)
}

private fun findSegmentIndex(entries: List<Entry>, x: Float): Int {
    for (i in 0 until entries.size - 1) {
        if (x >= entries[i].x && x <= entries[i + 1].x) return i
    }
    return (entries.size - 2).coerceAtLeast(0)
}
```

`findSegmentIndex` replaces the previous `chartX.toInt().coerceIn(...)` (which only worked because X was an integer index). Binary search is not worth it here - point counts are small.

`interpolateCubicBezier` (BaseChartActivity.kt:427) is unchanged.

`crosshairView.setCrosshair` already takes `(x, y, entries, dps)` and renders the info bubble - no inner change needed.

### 5. Spinner mapping

`arrays.xml`:

```xml
<string-array name="chart_type_options">
    <item>折线图</item>
    <item>平滑曲线</item>
    <item>后置阶梯线</item>
</string-array>
```

`initChartTypeSpinner` (BaseChartActivity.kt:161) currently passes `position == 1` into `updateChartStyle`. Replace with:

```kotlin
val style = LineStyle.fromSpinnerPosition(position)
updateChartStyle(style)
```

`updateChartStyle(style: LineStyle)` replaces the current `updateChartStyle(smooth: Boolean)`:

```kotlin
private fun updateChartStyle(style: LineStyle) {
    val data = chart.data ?: return
    val mpMode = when (style) {
        LineStyle.LINEAR -> LineDataSet.Mode.LINEAR
        LineStyle.CUBIC_BEZIER -> LineDataSet.Mode.CUBIC_BEZIER
        LineStyle.STEPPED -> LineDataSet.Mode.STEPPED
    }
    for (i in 0 until data.dataSetCount) {
        val ds = data.getDataSetByIndex(i) as? LineDataSet ?: continue
        ds.mode = mpMode
    }
    chart.invalidate()
}
```

`updateChart()` (BaseChartActivity.kt:566) also computes the initial mode from the spinner; reuse the same helper.

`ChartFragment.kt:76` currently hard-codes `mode = LineDataSet.Mode.CUBIC_BEZIER`. Change to `LINEAR` (matching the default spinner position 0).

### 6. units.json: add 旬

Insert into the `time` category between `week` and `month`:

```json
{ "id": "xun", "symbolCn": "旬(10天)", "symbolEn": "xun", "toBase": 864000 }
```

`toBase` = 10 × 86400 s = 864000. 1 旬 = 1/3 月 in seconds, which keeps ratio math consistent.

## Components & Data Flow

```
loadDataPoints()  ->  List<DataPoint>          // unchanged
   ↓
filteredDataPoints (current time range)        // unchanged
   ↓
updateChart()
   ├─ entries = dp.map { Entry(dp.timestamp, dp.value) }
   ├─ XAxisValueFormatter maps timestamp -> dateLabel
   ├─ LineDataSet.mode = mpModeFor(spinner)
   └─ chart.invalidate()
   ↓
[user drags / scrolls chart]                   // MPAndroidChart internal
   ↓
[user taps chartTypeSpinner]
   ↓
updateChartStyle(style) -> mutates ds.mode
   ↓
[user touches chart]
   ↓
updateCrosshair()
   ├─ chartX = touchVals.x (now a timestamp)
   ├─ style = LineStyle.fromSpinnerPosition(...)
   └─ y = branch on style: linear / cubic / stepped
   ↓
crosshairView.setCrosshair(chartX, y, entries, dps)
   ↓
[user drags the bottom strip]
   ↓
deltaMs = -deltaX / (stripWidth / visibleRangeMs)
chart.moveViewToX(clamp(newLow, firstTs, lastTs - visibleRange))
```

The only new abstraction is `LineStyle`. Everything else reuses existing channels.

## Error Handling

- `filteredDataPoints.size < 2` → crosshair updates no-op (already the case).
- `findSegmentIndex` always returns a valid `i` because the X is clamped to `[first.x, last.x]` upstream.
- `interpolateCubicBezier` returns `NaN` when out of range; the previous code's call site already handles that with `isFinite()`. After the refactor it is only called inside its own `when` branch, so the `NaN` guard can stay as is.
- `dragIndicatorContainer.width` can be 0 before layout; the existing code does not guard against that and the user can only drag after the strip is laid out. No regression.
- A 0-ms or 1-ms `totalSpanMs` (all points at the same instant) makes `pxPerMs` undefined. Guard:

```kotlin
if (visibleRange <= 0f) return@setOnTouchListener true
```

…which already exists for `visibleRange <= 0f`. Keep that guard.

## Testing

Unit-testable surface (no Android dependency):

- `LineStyle.fromSpinnerPosition` / `toSpinnerPosition` round-trip.
- `findSegmentIndex` over a sorted list of `(x, y)` entries.

Manual verification on device or emulator:

1. **Time spacing**: open the height chart (or weight chart) with two records 1 day apart and two records 30 days apart. The two pairs must be visibly different widths on screen.
2. **Stepped line**: switch spinner to 后置阶梯线. The chart should show a step pattern (value at `x_i` held flat until `x_{i+1}`). Touching the chart, the info bubble's value must equal the right endpoint of the step the touch falls into.
3. **Smooth line crosshair**: select 平滑曲线. Touch and slide. The info bubble's Y must track the actual cubic curve within ~1 px.
4. **Drag strip**: select "1周" time range, drag the strip left/right by ~10 px. The visible window should pan a corresponding amount, proportional to the visible time span, not to the entry count.
5. **Xun unit**: open unit picker for time; confirm "旬" appears between "周" and "月". Pick "旬" then enter "3旬"; conversion back and forth via kg or another category should not blow up (just confirms parsing loads).

## Risks

- **`IndexAxisValueFormatter` → custom formatter**: minor behavior shift if anyone relies on the existing X-as-index semantic elsewhere. Grep confirms only `BaseChartActivity` and `ChartFragment` build entries.
- **Drag math in milliseconds** vs. millisecond-cast-to-float precision: with 32-bit float, resolution near a Unix epoch in milliseconds is ~256 ms. For body-record histories (months/years) the precision is far better than needed, but if any consumer ever records sub-second data the float loses it. Document the limit; do not over-engineer.
- **`LabelCount` of 5 across a time-spaced axis**: when data is sparse (e.g. 3 points across 2 years), 5 ticks will spread and may show fewer than 5 labels. Acceptable; matches current behavior.

## Files Touched

1. `app/src/main/java/com/woshiwangnima/healthdietpro/ui/profile/chart/BaseChartActivity.kt`
   - Add `LineStyle` enum.
   - `updateChart()`: timestamp-based X entries; custom `ValueFormatter`; use `LineStyle.fromSpinnerPosition`.
   - `updateChartStyle(style: LineStyle)`: branched by enum.
   - `updateCrosshair`: branched by `LineStyle`; `findSegmentIndex` helper.
   - `initDragIndicator` + arrow click handlers: use millisecond range math.
   - `updateDragArrows`: bound on `maxStart` in ms.
2. `app/src/main/java/com/woshiwangnima/healthdietpro/ui/profile/chart/ChartFragment.kt`
   - Mirror the timestamp-as-X change and switch default `mode` to `LINEAR`.
3. `app/src/main/res/values/arrays.xml`
   - Append "后置阶梯线" entry.
4. `app/src/main/assets/units.json`
   - Insert "旬" between "周" and "月".
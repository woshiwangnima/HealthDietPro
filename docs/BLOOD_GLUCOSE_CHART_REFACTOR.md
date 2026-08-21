# Blood Glucose Chart Refactor

## Status

Draft requirements. This document scopes the blood-glucose chart refactor only;
no chart behavior is changed by this document.

## Goal

Replace the current date-based daily chart and generic chart-style controls with
a filtered, recent-time-window glucose chart. A record-time range selector
controls the historical data scope; the chart independently selects one of four
fixed X-axis windows: 3 hours, 6 hours, 12 hours, or 24 hours. The Y axis
always starts at zero and ends at the user's historical maximum blood-glucose
value.

## Current Behavior

The chart is implemented in `ui/record/BloodGlucoseActivity.kt` by
`BloodGlucoseChart`.

- A date field opens a date picker and selects one calendar day.
- The primary series includes only records within that local calendar day.
- A secondary dashed `Delayed data (1 day)` series shifts the prior day's
  records forward by 24 hours for visual comparison.
- The chart uses generic `ComposeBaseChart` controls. These expose line style,
  X-axis range and interval, Y-axis bounds and interval, and more style
  settings. State is persisted per user under `blood_glucose_history`.
- The chart supports crosshair interaction, panning, fullscreen, legend, and
  general series rendering through the shared chart component.

## Required Behavior

### Historical Data Scope

- Replace the chart-top date selector with the same collapsed/expanded visual
  pattern as `饮水记录 -> 数据`: `RecordTimeRangeFilter` and
  `RecordTimeRangePickerField`.
- The selector must support the existing natural-time, relative-time, all-time,
  and custom date-time ranges supplied by `RecordTimeRangeFilter`.
- Default selection is `全部` (`RecordTimeRangePreset.ALL`).
- The selected scope filters which historical records may appear in the primary
  and delayed chart series and defines the left/right panning boundary.
- The chart's visible 3/6/12/24-hour window remains independent from the scope
  selector. Choosing `全部` does not mean that every record is drawn at once.
- When the scope changes, reset the chart to its latest valid fixed window:
  right edge equals the latest primary record in the selected scope; if the
  scope is empty, use its end timestamp only for an empty chart state.

### Layout

- Remove the generic chart-style settings button and its expandable controls
  from the blood-glucose chart only.
- Add an always-visible, single-selection control directly above the chart with
  exactly four options: `3 hours`, `6 hours`, `12 hours`, and `24 hours`.
- Position the time-window control below the historical data-scope selector and
  above the chart canvas. Use an equal-width segmented control matching the
  existing water trend selector visual language.
- Follow the existing Compose screen visual language and use string resources
  in `values/`, `values-en/`, and `values-zh/`; do not hardcode user-visible
  text in Composables.
- The selected X-axis window and historical data scope must remain selected
  through recomposition, rotation, and process recreation. Persist both per
  current user. The duration defaults to `24 hours`; the scope defaults to
  `全部` for a user without stored choices.

### X Axis

- The selected option defines a fixed visible duration of exactly 3, 6, 12, or
  24 elapsed hours.
- The X axis must use actual timestamps and local time labels, including across
  midnight; it must not reset to `00:00` or depend on a chosen date.
- The window must contain only actual records. Missing CGM intervals remain
  gaps; there is no interpolation, padding, or fabricated zero value.
- The initial chart viewport is anchored at the selected scope's latest primary
  record: its timestamp is the X-axis maximum at the right edge. With Y minimum
  zero, this establishes the requested initial right-bottom origin. The initial
  left edge is `latestTimestamp - selectedDuration`.
- Horizontal panning is retained. It moves the fixed-duration window only; the
  visible duration cannot expand or shrink. Panning clamps to the selected
  scope's earliest and latest record timestamps. Returning the viewport to the
  right boundary restores the latest-record anchor.
- The chart must use fixed, appropriate time intervals rather than exposing a
  user setting. Recommended labels/ticks: 3 h = 30 min, 6 h = 1 h, 12 h = 2 h,
  24 h = 4 h. The renderer may suppress overlapping labels on narrow screens
  but must keep actual timestamp semantics.

### Y Axis

- The minimum is always exactly `0 mmol/L`.
- The maximum is the maximum `valueMmolPerL` over all stored blood-glucose
  records for the current user, not merely the records inside the selected data
  scope or visible X window.
- The range must remain stable while switching among 3/6/12/24 hour windows.
- A record added, edited, deleted, or imported must cause the historical
  maximum to be recalculated from the current record list.
- The Y axis must never be user-configurable on this screen. When no data
  exists, rendering must remain valid with a nonzero technical display maximum;
  recommended fallback is `1.0 mmol/L`, while showing the existing empty-chart
  treatment rather than implying a measured maximum.
- The implementation may add visual headroom above the historical maximum only
  if the top tick and crosshair values still represent the true value scale.
  Recommended default: use the exact historical maximum as the data bound and
  let the shared tick policy choose labels without changing the semantic bound.

### Data Series

- The primary series continues to render stored `BloodGlucoseRecord` values in
  canonical `mmol/L` with the current crosshair formatting. It includes only
  records in the selected data scope and current fixed X window.
- Keep the delayed comparison series, but replace its fixed one-day offset with
  the currently selected duration. For a 3/6/12/24-hour selection, records in
  the immediately preceding 3/6/12/24-hour interval are shifted forward by
  exactly that duration and use the label `延迟数据（3小时/6小时/12小时/24小时）`.
- Both series must respect the selected historical data scope. If the preceding
  interval falls outside the scope, it contributes no delayed points. Do not
  read outside the user-selected scope just to complete a comparison series.
- The delayed series is a visual comparison only. Its crosshair must display the
  original measurement timestamp and identify the point as the selected
  duration's delayed comparison; it must not represent the shifted drawing
  position as a newly measured timestamp.
- Preserve record ordering, source linkage, timing metadata, archive format,
  and all non-chart behaviors.

### Performance and Data Access

- The chart must not repeatedly map, sort, or path-build the entire blood
  glucose history during drag gestures, crosshair movement, or recomposition.
- Provide a repository/ViewModel read path that produces, for the selected
  scope, a timestamp-ascending immutable record slice plus the current user's
  historical maximum. Maintain an in-memory timestamp index/cache after archive
  load; invalidate or update it after add/edit/delete/import.
- For the visible window, retrieve only the primary interval plus the one
  preceding interval required by the delayed series. A 24-hour CGM window at
  five-minute cadence is normally 288 primary + 288 delayed points; the
  implementation must be linear in these visible points, not in the full
  archive size.
- Use binary-search range slicing on the timestamp-sorted cache (or equivalent
  indexed lookup), not `records.filter` over all history for every viewport
  update.
- Memoize converted `DataPoint`/chart-series data by immutable slice identity,
  selected duration, selected scope, and mutation generation. Do not allocate
  per-point objects during Canvas draw.
- Retain existing path rendering only for visible data. For dense future data,
  apply deterministic pixel-column downsampling only when points materially
  exceed the available horizontal pixels; never alter stored values, crosshair
  lookup, or the 5-minute source data.

### Retained Chart Functions

- Preserve chart drawing, target/reference bands if already supplied, line
  rendering, crosshair inspection, time/value formatting, legend, and
  fullscreen unless separately changed.
- Preserve generic chart configuration controls for other screens such as body
  metrics and blood pressure. This refactor must not globally remove them.
- Remove only the blood-glucose screen's chart-style configuration entry and
  its line/X/Y settings. Fullscreen remains available unless explicitly removed
  later.

## Implementation Boundary

### May Change

- `ui/record/BloodGlucoseActivity.kt`: replace date state, day filtering,
  delayed series, and generic chart-control wiring with scoped rolling-window
  state and the water-style record-time range selector.
- `ui/record/BloodGlucoseViewModel.kt`: expose focused chart UI state/events,
  indexed record slices, and historical maximum derivation.
- `ui/profile/chart/ComposeChart.kt` and `common/ui/chart/ComposeBaseChart.kt`:
  add narrowly scoped support for a caller-specified fixed X range and fixed Y
  bounds, fixed tick intervals, fixed-duration pan clamping, and independently
  disable generic controls for one caller.
- Blood-glucose string resources in all three locale directories.
- Focused JVM tests for fixed-window filtering and Y-bound derivation.

### Must Not Change

- `BloodGlucoseRecord`, `BloodGlucoseSource`, archive schema, import format,
  current-user ownership, source IDs, duplicate policy, reminders, and alerts.
- Data/HbA1c editors, source settings, target-range settings, reminder
  settings, event screen, or data list behavior.
- Generic chart behavior in body metric and blood-pressure screens.
- Static assets and user archives.

## Recommended Technical Shape

1. Introduce a pure Kotlin duration model with stable IDs for 3/6/12/24 hours.
2. Reuse `RecordTimeRangeFilter` for historical scope selection, defaulting to
   `ALL`, while persisting the scope separately from viewport position.
3. Build a timestamp-sorted in-memory record index. Slice the selected scope
   and then the primary/delayed fixed intervals using binary search.
4. Resolve the initial window end to the selected scope's latest record and
   calculate `start = end - duration`; preserve that duration while panning.
5. Derive the historical maximum once from the full current record list.
6. Extend the shared chart spec with explicit fixed viewport bounds, fixed tick
   intervals, and fixed-duration pan limits rather than encoding this behavior
   as persisted generic percentage controls.
7. Configure the blood-glucose chart with generic controls disabled while
   retaining fullscreen/crosshair/legend support.
8. Replace the date-dependent delayed series with a selected-duration delayed
   comparison series. Do not leave obsolete date-picker logic behind.

## Acceptance Criteria

- No date selector, date picker, chart-style button, or generic X/Y/line-style
  controls are visible on the blood-glucose chart tab.
- The chart-top scope selector matches `饮水记录 -> 数据`, defaults to `全部`, and
  constrains both primary and delayed series.
- The four time-window options are visible and mutually exclusive.
- Each selection renders the exact chosen elapsed duration on the X axis, with
  its initial right edge at the latest selected-scope record timestamp.
- Data around midnight stays on one continuous time axis.
- The Y axis starts at zero and uses the same historical maximum for all four
  selections.
- Records outside the selected X window are not drawn; missing intervals are
  not filled. The delayed series uses the immediately preceding same-duration
  interval and carries a matching duration label.
- Dragging cannot alter the selected duration and cannot pan outside the
  selected data scope.
- At five-minute CGM cadence, switching and dragging a 24-hour view remains
  responsive while rendering no more than the primary and delayed visible
  intervals.
- Existing data entry, edits, deletion, source selection, reminders, event
  screen, fullscreen, and crosshair behavior continue to work.
- Other chart screens retain their current configuration controls and behavior.
- JVM tests cover duration selection, boundary inclusion/exclusion, historical
  maximum derivation, empty data, and updating the maximum after mutations.

## Remaining Decision

1. When a selected scope has fewer records than one full duration, confirm that
   the chart still keeps the fixed X-axis duration and leaves unrecorded time
   empty. Recommended: keep the selected fixed duration.

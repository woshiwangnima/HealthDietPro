package com.woshiwangnima.healthdietpro.common.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.time.RecordTimePrecision
import com.woshiwangnima.healthdietpro.common.time.RecordTimeRange
import com.woshiwangnima.healthdietpro.common.time.RecordTimeRangePreset
import com.woshiwangnima.healthdietpro.common.time.RecordTimeRangeSelection
import com.woshiwangnima.healthdietpro.common.time.formatRecordTimestamp
import com.woshiwangnima.healthdietpro.common.time.resolve
import kotlinx.coroutines.delay

/** Shared absolute and relative time range picker for record lists and charts. */
@Composable
internal fun RecordTimeRangeFilter(
    selection: RecordTimeRangeSelection,
    onSelectionChanged: (RecordTimeRangeSelection) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var pickingStart by remember { mutableStateOf<Boolean?>(null) }
    var nowMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(selection is RecordTimeRangeSelection.Preset) {
        if (selection !is RecordTimeRangeSelection.Preset) return@LaunchedEffect
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(60_000L - nowMillis % 60_000L)
        }
    }
    val range = selection.resolve(nowMillis)
    Column(modifier.fillMaxWidth()) {
        RecordTimeRangePickerField(range, expanded, { expanded = !expanded })
        if (expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TimeEndpointButton(
                        text = formatRecordTimestamp(range.startMillis, RecordTimePrecision.MINUTE),
                        modifier = Modifier.weight(1f),
                        onClick = { pickingStart = true },
                    )
                    TimeEndpointButton(
                        text = formatRecordTimestamp(range.endMillis, RecordTimePrecision.MINUTE),
                        modifier = Modifier.weight(1f),
                        onClick = { pickingStart = false },
                    )
                }
                Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f), shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)) {
                    PresetPanel(selection, onSelectionChanged)
                }
            }
        }
    }
    pickingStart?.let { isStart ->
        ComposeDateTimePickerDialog(
            initialMillis = if (isStart) range.startMillis else range.endMillis,
            onDismiss = { pickingStart = null },
            onDateTimePicked = { selected ->
                val updated = if (isStart) {
                    RecordTimeRange(selected.coerceAtMost(range.endMillis), range.endMillis)
                } else {
                    RecordTimeRange(range.startMillis, selected.coerceAtLeast(range.startMillis))
                }
                onSelectionChanged(RecordTimeRangeSelection.Custom(updated))
                pickingStart = null
            },
            precision = RecordTimePrecision.MINUTE,
        )
    }
}

/** Standard collapsed entry for a start/end range selector. */
@Composable
internal fun RecordTimeRangePickerField(
    range: RecordTimeRange,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_birthday), contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = "${formatRecordTimestamp(range.startMillis, RecordTimePrecision.MINUTE)} - ${formatRecordTimestamp(range.endMillis, RecordTimePrecision.MINUTE)}",
                modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
            Icon(if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PresetPanel(
    selection: RecordTimeRangeSelection,
    onSelectionChanged: (RecordTimeRangeSelection) -> Unit,
) {
    val scrollState = rememberScrollState()
    Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Exactly one scroll container owns the gesture and moves every preset row together.
        Column(Modifier.horizontalScroll(scrollState), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PresetIconRow(
                iconRes = R.drawable.ic_birthday,
                tooltip = stringResource(R.string.record_time_range_natural_tooltip),
                presets = listOf(
                    RecordTimeRangePreset.TODAY to R.string.record_time_range_today,
                    RecordTimeRangePreset.THIS_WEEK to R.string.record_time_range_this_week,
                    RecordTimeRangePreset.THIS_MONTH to R.string.record_time_range_this_month,
                    RecordTimeRangePreset.THIS_YEAR to R.string.record_time_range_this_year,
                ),
                selection = selection,
                onSelectionChanged = onSelectionChanged,
            )
            PresetIconRow(
                iconRes = R.drawable.ic_time,
                tooltip = stringResource(R.string.record_time_range_relative_tooltip),
                presets = listOf(
                    RecordTimeRangePreset.LAST_24_HOURS to R.string.record_time_range_last_24_hours,
                    RecordTimeRangePreset.LAST_72_HOURS to R.string.record_time_range_last_72_hours,
                    RecordTimeRangePreset.LAST_7_DAYS to R.string.record_time_range_last_7_days,
                    RecordTimeRangePreset.LAST_30_DAYS to R.string.record_time_range_last_30_days,
                    RecordTimeRangePreset.LAST_1_YEAR to R.string.record_time_range_last_1_year,
                ),
                selection = selection,
                onSelectionChanged = onSelectionChanged,
            )
            PresetIconRow(
                iconRes = R.drawable.ic_list,
                tooltip = stringResource(R.string.record_time_range_all_tooltip),
                presets = listOf(
                    RecordTimeRangePreset.ALL to R.string.record_time_range_all,
                    null to R.string.record_time_range_custom,
                ),
                selection = selection,
                onSelectionChanged = onSelectionChanged,
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_arrow_left), contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(R.string.record_time_range_scroll_hint), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 6.dp))
            Icon(painterResource(R.drawable.ic_arrow_right), contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HorizontalScrollIndicator(scrollState)
    }
}

@Composable
private fun PresetIconRow(
    @androidx.annotation.DrawableRes iconRes: Int,
    tooltip: String,
    presets: List<Pair<RecordTimeRangePreset?, Int>>,
    selection: RecordTimeRangeSelection,
    onSelectionChanged: (RecordTimeRangeSelection) -> Unit,
) {
    InlineTooltip(message = tooltip) { _, onTooltipClick ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onTooltipClick) {
                Icon(painterResource(iconRes), contentDescription = tooltip, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                presets.forEach { (preset, labelRes) ->
                    val selected = when (selection) {
                        is RecordTimeRangeSelection.Preset -> selection.preset == preset
                        is RecordTimeRangeSelection.Custom -> preset == null
                    }
                    Surface(
                        onClick = {
                            onSelectionChanged(
                                preset?.let(RecordTimeRangeSelection::Preset)
                                    ?: RecordTimeRangeSelection.Custom(selection.resolve()),
                            )
                        },
                        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
                        modifier = Modifier.width(PRESET_BUTTON_WIDTH),
                    ) {
                        Text(
                            text = stringResource(labelRes),
                            modifier = Modifier.padding(vertical = 10.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeEndpointButton(text: String, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
        modifier = modifier,
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_birthday), contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text, modifier = Modifier.padding(start = 8.dp), style = MaterialTheme.typography.bodySmall)
        }
    }
}

private val PRESET_BUTTON_WIDTH = 104.dp

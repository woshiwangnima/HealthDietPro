package com.woshiwangnima.healthdietpro.ui.record

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.base.BaseActivity
import com.woshiwangnima.healthdietpro.common.time.RecordTimePrecision
import com.woshiwangnima.healthdietpro.common.time.RecordTimeRangeSelection
import com.woshiwangnima.healthdietpro.common.time.RecordTimeRangePreset
import com.woshiwangnima.healthdietpro.common.time.normalizeRecordTimestamp
import com.woshiwangnima.healthdietpro.common.time.resolve
import com.woshiwangnima.healthdietpro.common.ui.AnimatedPageContent
import com.woshiwangnima.healthdietpro.common.ui.AnimatedDonutChart
import com.woshiwangnima.healthdietpro.common.ui.chart.DateStackedBarChart
import com.woshiwangnima.healthdietpro.common.ui.chart.DateStackedBarEntry
import com.woshiwangnima.healthdietpro.common.ui.chart.DateStackedBarSegment
import com.woshiwangnima.healthdietpro.common.ui.AppDataTable
import com.woshiwangnima.healthdietpro.common.ui.AppDataTableColumn
import com.woshiwangnima.healthdietpro.common.ui.AppDataTableDeleteAction
import com.woshiwangnima.healthdietpro.common.ui.AppDataTableHeaderText
import com.woshiwangnima.healthdietpro.common.ui.AppDataTableText
import com.woshiwangnima.healthdietpro.common.ui.AppDropdownField
import com.woshiwangnima.healthdietpro.common.ui.AppDropdownOption
import com.woshiwangnima.healthdietpro.common.ui.AppIconTextButton
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen
import com.woshiwangnima.healthdietpro.common.ui.ColumnWidth
import com.woshiwangnima.healthdietpro.common.ui.ComposeDateTimePickerDialog
import com.woshiwangnima.healthdietpro.common.ui.DetailTabBar
import com.woshiwangnima.healthdietpro.common.ui.DetailTabItem
import com.woshiwangnima.healthdietpro.common.ui.DiscardChangesDialog
import com.woshiwangnima.healthdietpro.common.ui.EditorTextField
import com.woshiwangnima.healthdietpro.common.ui.FormSaveBar
import com.woshiwangnima.healthdietpro.common.ui.HealthDietProTheme
import com.woshiwangnima.healthdietpro.common.ui.InlineTooltip
import com.woshiwangnima.healthdietpro.common.ui.DonutChartSegment
import com.woshiwangnima.healthdietpro.common.ui.RecordTimePickerField
import com.woshiwangnima.healthdietpro.common.ui.RecordTimeRangeFilter
import com.woshiwangnima.healthdietpro.common.ui.SettingRow
import com.woshiwangnima.healthdietpro.common.ui.WaterGlassProgress
import com.woshiwangnima.healthdietpro.common.ui.NumericInputRange
import com.woshiwangnima.healthdietpro.common.ui.formatDateTime
import com.woshiwangnima.healthdietpro.model.food.CategorizedFood
import com.woshiwangnima.healthdietpro.model.food.FoodCategories
import com.woshiwangnima.healthdietpro.model.food.FoodKind
import com.woshiwangnima.healthdietpro.model.food.FoodNutrientRepository
import com.woshiwangnima.healthdietpro.model.food.UserCustomFoodRepository
import com.woshiwangnima.healthdietpro.model.profile.ProfilePrefs
import com.woshiwangnima.healthdietpro.model.water.ActivityLevel
import com.woshiwangnima.healthdietpro.model.water.WaterQuickRecord
import com.woshiwangnima.healthdietpro.model.water.WaterRecord
import com.woshiwangnima.healthdietpro.model.water.WaterRepository
import com.woshiwangnima.healthdietpro.model.water.WaterVolumeUnit
import com.woshiwangnima.healthdietpro.model.water.recommendedWaterMl
import java.util.UUID
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class WaterRecordActivity : BaseActivity() {
    companion object { const val EXTRA_OPEN_EDITOR = "open_editor" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { HealthDietProTheme { WaterRecordRoute(::finish, intent.getBooleanExtra(EXTRA_OPEN_EDITOR, false)) } }
    }
}

private enum class WaterRoute { HOME, EDITOR, SETTINGS, RECOMMENDATION, HYDRATION, QUICK_RECORDS, QUICK_EDITOR }

private data class Beverage(
    val id: String,
    val name: String,
    val kind: FoodKind,
    val hydrationMlPer100g: Double?,
    val densityGramsPerMl: Double?,
)

@Composable
private fun WaterRecordRoute(onFinish: () -> Unit, openEditorInitially: Boolean) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repository = remember { WaterRepository.fromContext(context) }
    val beverages = remember { loadBeverages(context) }
    var archive by remember { mutableStateOf(repository.load()) }
    var route by rememberSaveable { mutableStateOf(if (openEditorInitially) WaterRoute.EDITOR else WaterRoute.HOME) }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var editingRecord by remember { mutableStateOf<WaterRecord?>(null) }
    var editingQuickBeverageId by remember { mutableStateOf<String?>(null) }
    fun refresh() { archive = repository.load() }
    fun navigateBack() {
        route = when (route) {
            WaterRoute.EDITOR -> WaterRoute.HOME
            WaterRoute.SETTINGS -> WaterRoute.HOME
            WaterRoute.RECOMMENDATION, WaterRoute.HYDRATION, WaterRoute.QUICK_RECORDS -> WaterRoute.SETTINGS
            WaterRoute.QUICK_EDITOR -> WaterRoute.QUICK_RECORDS
            WaterRoute.HOME -> WaterRoute.HOME
        }
    }
    BackHandler(enabled = route != WaterRoute.HOME) {
        if (openEditorInitially && route == WaterRoute.EDITOR) onFinish() else navigateBack()
    }
    when (route) {
        WaterRoute.HOME -> WaterHomeScreen(archive.records, beverages, archive.activityLevel, selectedTab, { selectedTab = it }, { editingRecord = null; route = WaterRoute.EDITOR }, { route = WaterRoute.SETTINGS }, { repository.delete(it); refresh() }, { editingRecord = it; route = WaterRoute.EDITOR }, onFinish)
        WaterRoute.EDITOR -> WaterEditorScreen(editingRecord, beverages, archive.quickRecords, { if (openEditorInitially) onFinish() else navigateBack() }) { record -> repository.add(record); refresh(); route = WaterRoute.HOME }
        WaterRoute.SETTINGS -> WaterSettingsScreen({ route = WaterRoute.HOME }, { route = WaterRoute.RECOMMENDATION }, { route = WaterRoute.HYDRATION }, { route = WaterRoute.QUICK_RECORDS })
        WaterRoute.RECOMMENDATION -> WaterRecommendationScreen(archive.activityLevel, { route = WaterRoute.SETTINGS }) { level -> repository.saveSettings(level, archive.quickRecords); refresh() }
        WaterRoute.HYDRATION -> BeverageHydrationScreen(beverages, { route = WaterRoute.SETTINGS })
        WaterRoute.QUICK_RECORDS -> QuickRecordSettingsScreen(beverages, archive.quickRecords, { route = WaterRoute.SETTINGS }, { editingQuickBeverageId = null; route = WaterRoute.QUICK_EDITOR }, { quick -> editingQuickBeverageId = quick.beverageId; route = WaterRoute.QUICK_EDITOR }, { beverageId -> repository.saveSettings(archive.activityLevel, archive.quickRecords.filterNot { it.beverageId == beverageId }); refresh() })
        WaterRoute.QUICK_EDITOR -> QuickRecordEditorScreen(beverages, archive.quickRecords.firstOrNull { it.beverageId == editingQuickBeverageId }, { route = WaterRoute.QUICK_RECORDS }) { originalId, quick -> repository.saveSettings(archive.activityLevel, archive.quickRecords.filterNot { it.beverageId == originalId || it.beverageId == quick.beverageId } + quick); refresh(); route = WaterRoute.QUICK_RECORDS }
    }
}

@Composable
private fun WaterHomeScreen(records: List<WaterRecord>, beverages: List<Beverage>, activityLevel: ActivityLevel, selectedTab: Int, onTabSelected: (Int) -> Unit, onAdd: () -> Unit, onSettings: () -> Unit, onDelete: (String) -> Unit, onEdit: (WaterRecord) -> Unit, onBack: () -> Unit) {
    val tabs = remember { listOf(DetailTabItem("statistics", R.string.water_tab_statistics, R.drawable.ic_chart), DetailTabItem("data", R.string.water_tab_data, R.drawable.ic_list)) }
    BaseScreen(title = stringResource(R.string.water_title), onBack = onBack, includeNavigationBarPadding = false, actions = { IconButton(onClick = onSettings) { Icon(painterResource(R.drawable.ic_settings), stringResource(R.string.water_settings_title)) } }) { padding ->
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(padding)) {
            AnimatedPageContent(selectedTab, Modifier.weight(1f), direction = { initial, target -> target - initial }) { tab ->
                if (tab == 0) WaterStatisticsPage(records, beverages, activityLevel) else WaterDataPage(records, onAdd, onEdit, onDelete)
            }
            DetailTabBar(tabs, tabs[selectedTab].id) { onTabSelected(tabs.indexOf(it)) }
        }
    }
}

@Composable
private fun WaterStatisticsPage(records: List<WaterRecord>, beverages: List<Beverage>, activityLevel: ActivityLevel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val profile = remember(context) { ProfilePrefs.load(context) }
    val recommendation = recommendedWaterMl(profile.gender, profile.age, profile.weightRecords.maxByOrNull { it.recordedAtMillis }?.value, activityLevel)
    val startOfToday = remember { java.time.LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() }
    val beverageById = remember(beverages) { beverages.associateBy(Beverage::id) }
    val todayActualWater = remember(records, beverageById, startOfToday) {
        records.filter { it.timestamp >= startOfToday }.sumOf { it.actualWaterMl(beverageById[it.beverageId]) ?: 0.0 }
    }
    val remaining = (recommendation - todayActualWater).coerceAtLeast(0.0)
    var rangeSelection by remember { mutableStateOf<RecordTimeRangeSelection>(RecordTimeRangeSelection.Preset(RecordTimeRangePreset.LAST_7_DAYS)) }
    val rangeRecords = remember(records, rangeSelection) { records.filter { rangeSelection.resolve(System.currentTimeMillis()).contains(it.timestamp) } }
    val palette = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary, androidx.compose.ui.graphics.Color(0xFF039BE5), androidx.compose.ui.graphics.Color(0xFFF9A825), androidx.compose.ui.graphics.Color(0xFF8E24AA), androidx.compose.ui.graphics.Color(0xFF00897B))
    val rows = remember(rangeRecords, beverageById, palette) {
        rangeRecords.groupBy { it.beverageId }.map { (id, items) ->
            WaterCompositionRow(
                beverageId = id,
                beverageName = items.first().beverageName,
                beverageVolumeMl = items.sumOf(WaterRecord::volumeMl),
                actualWaterMl = items.sumOf { it.actualWaterMl(beverageById[id]) ?: 0.0 },
                hasKnownWaterContent = items.all { it.actualWaterMl(beverageById[id]) != null },
            )
        }.sortedByDescending(WaterCompositionRow::actualWaterMl).mapIndexed { index, row -> row.copy(color = palette[index % palette.size]) }
    }
    val totalActualWater = rows.sumOf(WaterCompositionRow::actualWaterMl)
    val segments = remember(rows) { rows.filter { it.actualWaterMl > 0.0 }.map { DonutChartSegment(it.beverageId, it.beverageName, it.actualWaterMl.toFloat(), it.color) } }
    val trendColorsByBeverageId = remember(rows, records, beverageById, palette) {
        buildMap {
            rows.forEach { put(it.beverageId, requireNotNull(it.color)) }
            records.asSequence()
                .filter { it.actualWaterMl(beverageById[it.beverageId]) != null }
                .map(WaterRecord::beverageId)
                .distinct()
                .sorted()
                .forEach { id -> putIfAbsent(id, palette[size % palette.size]) }
        }
    }
    var trendDays by rememberSaveable { mutableIntStateOf(7) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(20.dp), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                WaterStatAmount(stringResource(R.string.water_statistics_daily_target), formatMl(recommendation.toDouble()))
                WaterStatAmount(stringResource(R.string.water_statistics_remaining), formatMl(remaining))
            }
        }
        item { WaterGlassProgress((todayActualWater / recommendation).toFloat().coerceIn(0f, 1f), formatMl(todayActualWater), stringResource(R.string.water_statistics_progress, (todayActualWater / recommendation * 100).toInt())) }
        item { Text(stringResource(R.string.water_statistics_breakdown), modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.titleMedium) }
        item { RecordTimeRangeFilter(rangeSelection, { rangeSelection = it }) }
        item { AnimatedDonutChart(segments, formatMl(totalActualWater), stringResource(R.string.water_statistics_actual_water), Modifier.fillMaxWidth()) }
        if (rows.isNotEmpty()) {
            item { WaterCompositionTable(rows, totalActualWater) }
        } else {
            item { Text(stringResource(R.string.water_statistics_empty), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        item {
            WaterTrendChart(
                records = records,
                beverageById = beverageById,
                colorsByBeverageId = trendColorsByBeverageId,
                days = trendDays,
                onDaysChanged = { trendDays = it },
            )
        }
    }
}

private data class WaterCompositionRow(val beverageId: String, val beverageName: String, val beverageVolumeMl: Double, val actualWaterMl: Double, val hasKnownWaterContent: Boolean, val color: Color? = null)

private fun WaterRecord.actualWaterMl(beverage: Beverage?): Double? {
    val hydration = beverage?.hydrationMlPer100g ?: return null
    val density = beverage.densityGramsPerMl ?: return null
    return volumeMl * density * hydration / 100.0
}

@Composable
private fun WaterStatAmount(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun BeverageNameCell(name: String, color: androidx.compose.ui.graphics.Color?) {
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        androidx.compose.foundation.Canvas(Modifier.size(10.dp)) { drawCircle(color ?: androidx.compose.ui.graphics.Color.Transparent) }
        Text(name, modifier = Modifier.padding(start = 6.dp))
    }
}

@Composable
private fun WaterCompositionTable(rows: List<WaterCompositionRow>, totalActualWater: Double) {
    val totalVolume = rows.sumOf(WaterCompositionRow::beverageVolumeMl)
    androidx.compose.material3.Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .32f),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            WaterCompositionTableRow(
                beverage = stringResource(R.string.water_beverage),
                amount = stringResource(R.string.water_statistics_volume_water),
                percent = stringResource(R.string.water_statistics_percent_water),
                header = true,
            )
            rows.forEach { row ->
                val beveragePercent = if (totalVolume > 0.0) "${(row.beverageVolumeMl / totalVolume * 100).toInt()}%" else "-"
                val waterPercent = if (row.hasKnownWaterContent && totalActualWater > 0.0) "${(row.actualWaterMl / totalActualWater * 100).toInt()}%" else "-"
                WaterCompositionTableRow(
                    beverage = row.beverageName,
                    amount = "${formatMl(row.beverageVolumeMl)} / ${if (row.hasKnownWaterContent) formatMl(row.actualWaterMl) else "-"}",
                    percent = "$beveragePercent / $waterPercent",
                    color = row.color,
                )
            }
        }
    }
}

@Composable
private fun WaterCompositionTableRow(beverage: String, amount: String, percent: String, color: androidx.compose.ui.graphics.Color? = null, header: Boolean = false) {
    val style = if (header) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall
    Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Row(Modifier.weight(1.15f), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            if (color != null) androidx.compose.foundation.Canvas(Modifier.size(10.dp)) { drawCircle(color) }
            Text(beverage, modifier = Modifier.padding(start = if (color == null) 0.dp else 6.dp), style = style)
        }
        Text(amount, modifier = Modifier.weight(1.1f), style = style)
        Text(percent, modifier = Modifier.weight(1.05f), style = style)
    }
}

@Composable
private fun WaterTrendChart(
    records: List<WaterRecord>,
    beverageById: Map<String, Beverage>,
    colorsByBeverageId: Map<String, Color?>,
    days: Int,
    onDaysChanged: (Int) -> Unit,
) {
    val zone = remember { ZoneId.systemDefault() }
    val endDate = remember(days) { LocalDate.now(zone) }
    val dates = remember(endDate, days) { (0 until days).map { offsetDays: Int -> endDate.minusDays(offsetDays.toLong()) } }
    val startMillis = remember(endDate, days, zone) { endDate.minusDays((days - 1).toLong()).atStartOfDay(zone).toInstant().toEpochMilli() }
    val endMillis = remember(endDate, zone) { endDate.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() }
    val valuesByDate = remember(records, beverageById, dates, zone, startMillis, endMillis) {
        records.asSequence()
            .filter { it.timestamp in startMillis until endMillis }
            .mapNotNull { record ->
                record.actualWaterMl(beverageById[record.beverageId])?.takeIf { it > 0.0 }?.let { water ->
                    Instant.ofEpochMilli(record.timestamp).atZone(zone).toLocalDate() to (record.beverageId to water)
                }
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, entries) -> entries.groupBy({ it.first }, { it.second }).mapValues { it.value.sum() } }
    }
    val entries = remember(dates, valuesByDate, colorsByBeverageId) {
        dates.map { date ->
            DateStackedBarEntry(
                date = date,
                label = "%02d-%02d".format(date.monthValue, date.dayOfMonth),
                segments = valuesByDate[date].orEmpty().toSortedMap().map { (beverageId, amount) ->
                    DateStackedBarSegment(beverageId, amount, colorsByBeverageId[beverageId] ?: Color.Transparent)
                },
            )
        }
    }
    var selectedDate by remember(days) { mutableStateOf(dates.firstOrNull()) }
    val selectedEntry = entries.firstOrNull { it.date == selectedDate }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.water_statistics_trend), style = MaterialTheme.typography.titleMedium)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            listOf(7, 30).forEachIndexed { index, optionDays ->
                SegmentedButton(
                    selected = days == optionDays,
                    onClick = { onDaysChanged(optionDays) },
                    shape = SegmentedButtonDefaults.itemShape(index, 2),
                    label = { Text(stringResource(if (optionDays == 7) R.string.water_statistics_7_days else R.string.water_statistics_30_days)) },
                )
            }
        }
        DateStackedBarChart(
            entries = entries,
            yAxisTitle = stringResource(R.string.water_statistics_daily_water),
            formatValue = ::formatMl,
            labelEvery = if (days == 7) 1 else 7,
            selectedEntry = selectedEntry,
            onEntrySelected = { selectedDate = it.date },
        )
        selectedEntry?.let { entry ->
            val total = entry.segments.sumOf(DateStackedBarSegment::value)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.water_statistics_selected_day, entry.label, formatMl(total)), style = MaterialTheme.typography.titleSmall)
                if (entry.segments.isEmpty()) {
                    Text(stringResource(R.string.water_statistics_selected_day_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    entry.segments.forEach { segment ->
                        val name = beverageById[segment.id]?.name ?: segment.id
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            androidx.compose.foundation.Canvas(Modifier.size(10.dp)) { drawCircle(segment.color) }
                            Text("$name ${formatMl(segment.value)}", modifier = Modifier.padding(start = 6.dp), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WaterDataPage(records: List<WaterRecord>, onAdd: () -> Unit, onEdit: (WaterRecord) -> Unit, onDelete: (String) -> Unit) {
    // RecordTimeRangeSelection is a sealed domain type and has no Compose Saver.
    // The filter can reset when this transient page is recreated.
    var rangeSelection by remember { mutableStateOf<RecordTimeRangeSelection>(RecordTimeRangeSelection.Preset(RecordTimeRangePreset.TODAY)) }
    var deleting by remember { mutableStateOf<WaterRecord?>(null) }
    val filtered = remember(records, rangeSelection) { records.filter { rangeSelection.resolve(System.currentTimeMillis()).contains(it.timestamp) } }
    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { AppIconTextButton(stringResource(R.string.water_add), R.drawable.ic_add, onAdd) }
        RecordTimeRangeFilter(rangeSelection, { rangeSelection = it })
        if (filtered.isEmpty()) Text(stringResource(R.string.water_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
        else AppDataTable(rows = filtered, rowKey = { _, item -> item.id }, modifier = Modifier.weight(1f), columns = listOf(
            AppDataTableColumn("time", { AppDataTableHeaderText(stringResource(R.string.water_time)) }, ColumnWidth.Fixed(150.dp)) { AppDataTableText(formatDateTime(it.timestamp)) },
            AppDataTableColumn("beverage", { AppDataTableHeaderText(stringResource(R.string.water_beverage)) }, ColumnWidth.Flex(1f, 140.dp)) { AppDataTableText(it.beverageName) },
            AppDataTableColumn("volume", { AppDataTableHeaderText(stringResource(R.string.water_volume)) }, ColumnWidth.Fixed(108.dp)) { AppDataTableText(formatMl(it.volumeMl)) },
        ), actionsHeader = { AppDataTableHeaderText(stringResource(R.string.body_record_delete)) }, rowActions = { AppDataTableDeleteAction(stringResource(R.string.body_record_delete), { deleting = it }) }, onRowClick = onEdit)
    }
    deleting?.let { record -> AlertDialog(onDismissRequest = { deleting = null }, title = { Text(stringResource(R.string.body_record_delete_confirm_title)) }, text = { Text(stringResource(R.string.water_delete_message, record.beverageName)) }, confirmButton = { TextButton(onClick = { onDelete(record.id); deleting = null }) { Text(stringResource(R.string.body_record_delete)) } }, dismissButton = { TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.compose_confirm_dialog_cancel)) } }) }
}

@Composable
private fun WaterEditorScreen(record: WaterRecord?, beverages: List<Beverage>, quickRecords: List<WaterQuickRecord>, onBack: () -> Unit, onSave: (WaterRecord) -> Unit) {
    val defaultBeverage = beverages.firstOrNull { it.id == "food:water:drinking" } ?: beverages.firstOrNull()
    var beverageId by rememberSaveable(record?.id) { mutableStateOf(record?.beverageId ?: defaultBeverage?.id.orEmpty()) }
    val initialQuick = quickRecords.firstOrNull { it.beverageId == beverageId }
    var selectedQuickBeverageId by rememberSaveable(record?.id) { mutableStateOf<String?>(null) }
    var volume by rememberSaveable(record?.id) { mutableStateOf(record?.volumeMl?.toString() ?: initialQuick?.volume?.toString() ?: "250") }
    var unit by rememberSaveable(record?.id) { mutableStateOf(if (record != null) WaterVolumeUnit.ML else initialQuick?.unit ?: WaterVolumeUnit.ML) }
    var timestamp by rememberSaveable(record?.id) { mutableStateOf(record?.timestamp ?: normalizeRecordTimestamp(System.currentTimeMillis(), RecordTimePrecision.MINUTE)) }
    var pickTime by rememberSaveable { mutableStateOf(false) }
    val beverage = beverages.firstOrNull { it.id == beverageId }
    val volumeValue = volume.toDoubleOrNull()
    val valid = beverage != null && volumeValue != null && volumeValue > 0.0
    val current = if (valid) WaterRecord(record?.id.orEmpty(), timestamp, requireNotNull(beverage).id, beverage.name, requireNotNull(volumeValue) * unit.milliliters) else null
    val hasChanges = current != record
    val saveEnabled = valid && hasChanges
    var showDiscardDialog by rememberSaveable(record?.id) { mutableStateOf(false) }
    fun save() { current?.let { onSave(it.copy(id = record?.id ?: UUID.randomUUID().toString())) } }
    fun requestBack() { if (hasChanges) showDiscardDialog = true else onBack() }
    BackHandler(onBack = ::requestBack)
    BaseScreen(title = stringResource(if (record == null) R.string.water_add else R.string.water_edit), onBack = ::requestBack) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { RecordTimePickerField(stringResource(R.string.water_time), timestamp, RecordTimePrecision.MINUTE, { pickTime = true }) }
                if (quickRecords.isNotEmpty()) {
                    item {
                        val selectedQuick = quickRecords.firstOrNull { it.beverageId == selectedQuickBeverageId }
                        AppDropdownField(
                            label = stringResource(R.string.water_quick_record_select),
                            value = selectedQuick?.let { quick -> beverages.firstOrNull { it.id == quick.beverageId }?.name ?: quick.beverageId }.orEmpty(),
                            options = quickRecords.map { quick ->
                                val name = beverages.firstOrNull { it.id == quick.beverageId }?.name ?: quick.beverageId
                                AppDropdownOption(quick.beverageId, name, "${quick.volume} ${quick.unit.name.lowercase()}")
                            },
                            onSelect = { option ->
                                val quick = quickRecords.firstOrNull { it.beverageId == option.id } ?: return@AppDropdownField
                                selectedQuickBeverageId = quick.beverageId
                                beverageId = quick.beverageId
                                volume = quick.volume.toString()
                                unit = quick.unit
                            },
                        )
                    }
                }
                item { AppDropdownField(stringResource(R.string.water_beverage), beverage?.name.orEmpty(), beverages.map { AppDropdownOption(it.id, it.name, it.hydrationMlPer100g?.let { value -> stringResource(R.string.water_hydration_option, value) }) }, { beverageId = it.id; selectedQuickBeverageId = null }) }
                item { EditorTextField(label = stringResource(R.string.water_volume), value = volume, onValueChange = { volume = it }, required = true, numeric = true, range = NumericInputRange(minimum = 0.001)) }
                item { AppDropdownField(stringResource(R.string.water_unit), unit.name.lowercase(), WaterVolumeUnit.entries.map { AppDropdownOption(it.name, it.name.lowercase()) }, { unit = WaterVolumeUnit.valueOf(it.id) }) }
            }
            FormSaveBar(text = stringResource(R.string.water_save), enabled = saveEnabled, onSave = ::save)
        }
    }
    if (pickTime) ComposeDateTimePickerDialog(timestamp, { pickTime = false }, { timestamp = it; pickTime = false }, RecordTimePrecision.MINUTE)
    if (showDiscardDialog) DiscardChangesDialog(onDiscard = onBack, onSave = ::save, onDismiss = { showDiscardDialog = false }, saveEnabled = saveEnabled)
}

@Composable
private fun WaterSettingsScreen(onBack: () -> Unit, onRecommendation: () -> Unit, onHydration: () -> Unit, onQuickRecords: () -> Unit) {
    BaseScreen(title = stringResource(R.string.water_settings_title), onBack = onBack) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(vertical = 8.dp)) {
            item { SettingRow(stringResource(R.string.water_reminder), stringResource(R.string.water_reminder_unavailable), R.drawable.ic_notification, onClick = {}, clickable = false) }
            item { SettingRow(stringResource(R.string.water_recommendation), stringResource(R.string.water_recommendation_description), R.drawable.ic_volume, onRecommendation) }
            item { SettingRow(stringResource(R.string.water_hydration_title), stringResource(R.string.water_hydration_description), R.drawable.ic_info, onHydration) }
            item { SettingRow(stringResource(R.string.water_quick_records), stringResource(R.string.water_quick_records_description), R.drawable.ic_edit, onQuickRecords) }
        }
    }
}

@Composable
private fun WaterRecommendationScreen(level: ActivityLevel, onBack: () -> Unit, onLevelChanged: (ActivityLevel) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val profile = remember(context) { ProfilePrefs.load(context) }
    val recommended = recommendedWaterMl(profile.gender, profile.age, profile.weightRecords.maxByOrNull { it.recordedAtMillis }?.value, level)
    BaseScreen(title = stringResource(R.string.water_recommendation), onBack = onBack) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AppDropdownField(
                label = stringResource(R.string.water_activity_level),
                value = stringResource(level.labelRes()),
                options = ActivityLevel.entries.map { AppDropdownOption(it.name, stringResource(it.labelRes()), stringResource(it.descriptionRes())) },
                onSelect = { onLevelChanged(ActivityLevel.valueOf(it.id)) },
                showOptionDividers = true,
            )
            Text(stringResource(R.string.water_recommended_amount, recommended), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.water_recommendation_basis), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun BeverageHydrationScreen(beverages: List<Beverage>, onBack: () -> Unit) {
    val rows = remember(beverages) { beverages.filter { it.hydrationMlPer100g != null }.sortedByDescending { it.hydrationMlPer100g } }
    BaseScreen(title = stringResource(R.string.water_hydration_title), onBack = onBack) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            InlineTooltip(stringResource(R.string.water_hydration_tooltip)) { _, onClick ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { IconButton(onClick = onClick) { Icon(painterResource(R.drawable.ic_info), stringResource(R.string.water_hydration_tooltip)) } }
            }
            AppDataTable(rows = rows, rowKey = { _, item -> item.id }, modifier = Modifier.weight(1f), columns = listOf(
                AppDataTableColumn("beverage", { AppDataTableHeaderText(stringResource(R.string.water_beverage)) }, ColumnWidth.Flex(1f, 150.dp)) { AppDataTableText(it.name) },
                AppDataTableColumn("kind", { AppDataTableHeaderText(stringResource(R.string.water_food_kind)) }, ColumnWidth.Fixed(92.dp)) { AppDataTableText(stringResource(if (it.kind == FoodKind.INGREDIENT) R.string.water_kind_ingredient else R.string.water_kind_food)) },
                AppDataTableColumn("hydration", { AppDataTableHeaderText(stringResource(R.string.water_hydration)) }, ColumnWidth.Fixed(130.dp)) { AppDataTableText(stringResource(R.string.water_hydration_value, it.hydrationMlPer100g ?: 0.0)) },
            ))
        }
    }
}

@Composable
private fun QuickRecordSettingsScreen(
    beverages: List<Beverage>,
    quickRecords: List<WaterQuickRecord>,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (WaterQuickRecord) -> Unit,
    onDelete: (String) -> Unit,
) {
    var deleting by remember { mutableStateOf<WaterQuickRecord?>(null) }
    BaseScreen(title = stringResource(R.string.water_quick_records), onBack = onBack) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.weight(1f).padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.water_quick_records_hint), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    AppIconTextButton(stringResource(R.string.water_quick_add), R.drawable.ic_add, onAdd)
                }
                if (quickRecords.isEmpty()) Text(stringResource(R.string.water_quick_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                else AppDataTable(
                    rows = quickRecords,
                    rowKey = { _, item -> item.beverageId },
                    modifier = Modifier.weight(1f),
                    columns = listOf(
                        AppDataTableColumn("beverage", { AppDataTableHeaderText(stringResource(R.string.water_beverage)) }, ColumnWidth.Flex(1f, 140.dp)) { quick -> AppDataTableText(beverages.firstOrNull { it.id == quick.beverageId }?.name ?: quick.beverageId) },
                        AppDataTableColumn("volume", { AppDataTableHeaderText(stringResource(R.string.water_volume)) }, ColumnWidth.Fixed(110.dp)) { quick -> AppDataTableText("${quick.volume} ${quick.unit.name.lowercase()}") },
                    ),
                    actionsHeader = { AppDataTableHeaderText(stringResource(R.string.body_record_delete)) },
                    rowActions = { quick -> AppDataTableDeleteAction(stringResource(R.string.body_record_delete), { deleting = quick }) },
                    onRowClick = onEdit,
                )
            }
        }
    }
    deleting?.let { quick -> AlertDialog(onDismissRequest = { deleting = null }, title = { Text(stringResource(R.string.body_record_delete_confirm_title)) }, text = { Text(stringResource(R.string.water_quick_delete_message)) }, confirmButton = { TextButton(onClick = { onDelete(quick.beverageId); deleting = null }) { Text(stringResource(R.string.body_record_delete)) } }, dismissButton = { TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.compose_confirm_dialog_cancel)) } }) }
}

@Composable
private fun QuickRecordEditorScreen(beverages: List<Beverage>, quickRecord: WaterQuickRecord?, onBack: () -> Unit, onSave: (String?, WaterQuickRecord) -> Unit) {
    val defaultBeverageId = beverages.firstOrNull { it.id == "food:water:drinking" }?.id.orEmpty()
    var beverageId by rememberSaveable(quickRecord?.beverageId) { mutableStateOf(quickRecord?.beverageId ?: defaultBeverageId) }
    var volume by rememberSaveable(quickRecord?.beverageId) { mutableStateOf(quickRecord?.volume?.toString() ?: "250") }
    var unit by rememberSaveable(quickRecord?.beverageId) { mutableStateOf(quickRecord?.unit ?: WaterVolumeUnit.ML) }
    val selected = beverages.firstOrNull { it.id == beverageId }
    val volumeValue = volume.toDoubleOrNull()
    val current = if (selected != null && volumeValue != null && volumeValue > 0.0) WaterQuickRecord(beverageId, volumeValue, unit) else null
    val hasChanges = current != quickRecord
    val saveEnabled = current != null && hasChanges
    var showDiscardDialog by rememberSaveable(quickRecord?.beverageId) { mutableStateOf(false) }
    fun save() { current?.let { onSave(quickRecord?.beverageId, it) } }
    fun requestBack() { if (hasChanges) showDiscardDialog = true else onBack() }
    BackHandler(onBack = ::requestBack)
    BaseScreen(title = stringResource(if (quickRecord == null) R.string.water_quick_add else R.string.water_quick_edit), onBack = ::requestBack) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { AppDropdownField(stringResource(R.string.water_beverage), selected?.name.orEmpty(), beverages.map { AppDropdownOption(it.id, it.name) }, { beverageId = it.id }) }
                item { EditorTextField(stringResource(R.string.water_volume), volume, { volume = it }, required = true, numeric = true, range = NumericInputRange(minimum = 0.001)) }
                item { AppDropdownField(stringResource(R.string.water_unit), unit.name.lowercase(), WaterVolumeUnit.entries.map { AppDropdownOption(it.name, it.name.lowercase()) }, { unit = WaterVolumeUnit.valueOf(it.id) }) }
            }
            FormSaveBar(stringResource(R.string.water_save), saveEnabled, ::save)
        }
    }
    if (showDiscardDialog) DiscardChangesDialog(onDiscard = onBack, onSave = ::save, onDismiss = { showDiscardDialog = false }, saveEnabled = saveEnabled)
}

private fun loadBeverages(context: android.content.Context): List<Beverage> {
    val foods = FoodNutrientRepository.fromContext(context).foods() + UserCustomFoodRepository.fromContext(context).load()
    val language = context.resources.configuration.locales[0].language
    return foods.filterIsInstance<CategorizedFood>()
        .filter { it.kind != FoodKind.DISH && FoodCategories.hasTagWithin(it.categoryTags, "food.beverage") }
        .map { Beverage(it.id, it.displayName(language), it.kind, it.hydrationMlPer100g, when (it) {
            is com.woshiwangnima.healthdietpro.model.food.Ingredient -> it.densityGramsPerMl
            is com.woshiwangnima.healthdietpro.model.food.PreparedFood -> it.densityGramsPerMl
        }) }
        .sortedBy(Beverage::name)
}

private fun formatMl(value: Double): String = if (value % 1.0 == 0.0) "${value.toInt()} ml" else "%.1f ml".format(value)

private fun ActivityLevel.labelRes(): Int = when (this) {
    ActivityLevel.NONE -> R.string.water_activity_none
    ActivityLevel.LOW -> R.string.water_activity_low
    ActivityLevel.MODERATE -> R.string.water_activity_moderate
    ActivityLevel.HIGH -> R.string.water_activity_high
}

private fun ActivityLevel.descriptionRes(): Int = when (this) {
    ActivityLevel.NONE -> R.string.water_activity_none_description
    ActivityLevel.LOW -> R.string.water_activity_low_description
    ActivityLevel.MODERATE -> R.string.water_activity_moderate_description
    ActivityLevel.HIGH -> R.string.water_activity_high_description
}

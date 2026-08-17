@file:OptIn(ExperimentalLayoutApi::class)

package com.woshiwangnima.healthdietpro.ui.record

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.IconButton
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.common.ui.ActionGridItem
import com.woshiwangnima.healthdietpro.common.ui.ActionSectionCard
import com.woshiwangnima.healthdietpro.common.ui.SearchActivityPanel
import com.woshiwangnima.healthdietpro.common.ui.RecentSearchItem
import com.woshiwangnima.healthdietpro.common.time.RelativeTimeUnit
import com.woshiwangnima.healthdietpro.common.time.relativeTimeSince
import com.woshiwangnima.healthdietpro.model.prefs.UserPrefs

@Composable
fun RecordScreen(
    uiState: RecordUiState,
    onActionClick: (RecordActionId) -> Unit,
    onAddActionClick: (RecordActionId) -> Unit,
    onQueryChange: (String) -> Unit,
    onSubmitQuery: (String) -> Unit,
    onRemoveSearchHistory: (String) -> Unit,
    onClearSearchHistory: () -> Unit,
    onOpenRecentAction: (RecordActionId) -> Unit,
    onRemoveRecentAction: (RecordActionId) -> Unit,
    onClearRecentActions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var searchFocused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    var collapsedSections by remember { mutableStateOf(loadCollapsedSections(context)) }
    var plannedExpanded by remember { mutableStateOf(loadPlannedExpanded(context)) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(value = uiState.query, onValueChange = onQueryChange, modifier = Modifier.fillMaxWidth().onFocusChanged { focusState -> searchFocused = focusState.isFocused }, singleLine = true, leadingIcon = null, trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                uiState.query.takeIf { it.isNotBlank() }?.let { IconButton(onClick = { onQueryChange(""); onSubmitQuery("") }) { androidx.compose.material3.Icon(Icons.Filled.Close, stringResource(com.woshiwangnima.healthdietpro.R.string.nutrition_search_clear)) } }
                IconButton(onClick = { onSubmitQuery(uiState.query) }) { androidx.compose.material3.Icon(Icons.Filled.Search, stringResource(com.woshiwangnima.healthdietpro.R.string.record_search_submit)) }
            }
        }, placeholder = { Text(stringResource(com.woshiwangnima.healthdietpro.R.string.record_search)) })
        if (searchFocused && uiState.query.isBlank() && uiState.submittedQuery.isBlank() && (uiState.searchHistory.isNotEmpty() || uiState.recentActionIds.isNotEmpty())) {
            val itemById = uiState.sections.flatMap { it.items }.associateBy { it.id }
            SearchActivityPanel(uiState.searchHistory, uiState.recentActionIds.mapNotNull { id -> itemById[id]?.let { RecentSearchItem(id.name, stringResource(it.titleRes), it.iconRes) } }, stringResource(com.woshiwangnima.healthdietpro.R.string.record_search_history), stringResource(com.woshiwangnima.healthdietpro.R.string.record_recently_used), { entry -> onQueryChange(entry); onSubmitQuery(entry) }, onRemoveSearchHistory, onClearSearchHistory, { item -> onOpenRecentAction(RecordActionId.valueOf(item.id)) }, { item -> onRemoveRecentAction(RecordActionId.valueOf(item.id)) }, onClearRecentActions, { searchFocused = false; focusManager.clearFocus() })
        }
        uiState.sections.forEach { section ->
            val items = section.items.filter { item ->
                item.enabled && item.matchesQuery(uiState.submittedQuery)
            }
            if (items.isEmpty()) return@forEach
            val sectionKey = section.key()
            ActionSectionCard(
                title = stringResource(section.titleRes),
                titleIconRes = section.titleIconRes,
                collapsed = sectionKey in collapsedSections,
                onToggleCollapse = {
                    collapsedSections = if (sectionKey in collapsedSections) {
                        collapsedSections - sectionKey
                    } else {
                        collapsedSections + sectionKey
                    }
                    saveCollapsedSections(context, collapsedSections)
                },
            ) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    maxItemsInEachRow = 2,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items.forEach { item ->
                        ActionGridItem(
                            title = stringResource(item.titleRes),
                            iconRes = item.iconRes,
                            enabled = item.enabled,
                            summary = item.latestTimestamp?.let { timestamp -> stringResource(com.woshiwangnima.healthdietpro.R.string.record_latest_summary, relativeTime(timestamp), item.latestValue.orEmpty()) } ?: stringResource(com.woshiwangnima.healthdietpro.R.string.record_no_data),
                            showSummary = item.showSummary,
                            onClick = { onActionClick(item.id) },
                            onAddClick = item.id.takeIf { it in quickAddActionIds }?.let { actionId -> { onAddActionClick(actionId) } },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
        val plannedItems = uiState.sections.flatMap { section ->
            section.items.filter { item -> !item.enabled && item.matchesQuery(uiState.submittedQuery) }
        }
        if (plannedItems.isNotEmpty()) {
            val searching = uiState.submittedQuery.isNotBlank()
            ActionSectionCard(
                title = stringResource(com.woshiwangnima.healthdietpro.R.string.record_planned_features_count, plannedItems.size),
                titleIconRes = com.woshiwangnima.healthdietpro.R.drawable.ic_time,
                collapsed = !searching && !plannedExpanded,
                onToggleCollapse = {
                    plannedExpanded = !plannedExpanded
                    savePlannedExpanded(context, plannedExpanded)
                },
            ) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    maxItemsInEachRow = 2,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    plannedItems.forEach { item ->
                        ActionGridItem(
                            title = stringResource(item.titleRes),
                            iconRes = item.iconRes,
                            enabled = false,
                            summary = stringResource(com.woshiwangnima.healthdietpro.R.string.record_no_data),
                            showSummary = false,
                            onClick = {},
                            onAddClick = null,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

private const val RECORD_COLLAPSED_SECTIONS_KEY = "record_collapsed_sections_v1"
private const val RECORD_PLANNED_EXPANDED_KEY = "record_planned_expanded_v1"

private fun RecordSectionUiState.key(): String = titleRes.toString()

private fun loadCollapsedSections(context: android.content.Context): Set<String> =
    UserPrefs.current(context).getStringSet(RECORD_COLLAPSED_SECTIONS_KEY)

private fun savePlannedExpanded(context: android.content.Context, expanded: Boolean) {
    UserPrefs.current(context).putBoolean(RECORD_PLANNED_EXPANDED_KEY, expanded)
}

private fun loadPlannedExpanded(context: android.content.Context): Boolean =
    UserPrefs.current(context).getBoolean(RECORD_PLANNED_EXPANDED_KEY, false)

private fun saveCollapsedSections(context: android.content.Context, collapsed: Set<String>) {
    UserPrefs.current(context).putStringSet(RECORD_COLLAPSED_SECTIONS_KEY, collapsed)
}

private val quickAddActionIds = setOf(
    RecordActionId.Height,
    RecordActionId.Weight,
    RecordActionId.BloodGlucose,
    RecordActionId.BloodPressure,
    RecordActionId.Waist,
    RecordActionId.Medication,
    RecordActionId.Water,
    RecordActionId.Container,
    RecordActionId.Sleep,
    RecordActionId.Diet,
)

@Composable
private fun relativeTime(timestamp: Long): String {
    val relativeTime = relativeTimeSince(timestamp, System.currentTimeMillis())
    val textRes = when (relativeTime.unit) {
        RelativeTimeUnit.SECOND -> com.woshiwangnima.healthdietpro.R.string.record_seconds_ago
        RelativeTimeUnit.MINUTE -> com.woshiwangnima.healthdietpro.R.string.record_minutes_ago
        RelativeTimeUnit.HOUR -> com.woshiwangnima.healthdietpro.R.string.record_hours_ago
        RelativeTimeUnit.DAY -> com.woshiwangnima.healthdietpro.R.string.record_days_ago
        RelativeTimeUnit.MONTH -> com.woshiwangnima.healthdietpro.R.string.record_months_ago
        RelativeTimeUnit.YEAR -> com.woshiwangnima.healthdietpro.R.string.record_years_ago
    }
    return stringResource(textRes, relativeTime.amount)
}

@Composable
private fun RecordActionItemUiState.matchesQuery(query: String): Boolean =
    stringResource(titleRes).contains(query, ignoreCase = true) ||
        searchAliasRes.any { stringResource(it).contains(query, ignoreCase = true) }

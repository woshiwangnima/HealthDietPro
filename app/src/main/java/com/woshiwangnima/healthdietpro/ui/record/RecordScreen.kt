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
                stringResource(item.titleRes).contains(uiState.submittedQuery, ignoreCase = true) ||
                    item.searchAliasRes.any { stringResource(it).contains(uiState.submittedQuery, ignoreCase = true) }
            }
            if (items.isEmpty()) return@forEach
            ActionSectionCard(
                title = stringResource(section.titleRes),
                titleIconRes = section.titleIconRes,
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
                            onClick = { onActionClick(item.id) },
                            onAddClick = item.id.takeIf { it in quickAddActionIds }?.let { actionId -> { onAddActionClick(actionId) } },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

private val quickAddActionIds = setOf(
    RecordActionId.Height,
    RecordActionId.Weight,
    RecordActionId.BloodGlucose,
    RecordActionId.BloodPressure,
    RecordActionId.Waist,
    RecordActionId.Medication,
    RecordActionId.Water,
)

@Composable
private fun relativeTime(timestamp: Long): String {
    val hours = ((System.currentTimeMillis() - timestamp).coerceAtLeast(0) / 3_600_000L)
    return if (hours < 24) stringResource(com.woshiwangnima.healthdietpro.R.string.record_hours_ago, hours.coerceAtLeast(1)) else stringResource(com.woshiwangnima.healthdietpro.R.string.record_days_ago, hours / 24)
}

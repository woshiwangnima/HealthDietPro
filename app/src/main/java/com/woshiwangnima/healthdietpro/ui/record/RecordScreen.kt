@file:OptIn(ExperimentalLayoutApi::class)

package com.woshiwangnima.healthdietpro.ui.record

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.ui.ActionGridItem
import com.woshiwangnima.healthdietpro.common.ui.ActionSectionCard
import com.woshiwangnima.healthdietpro.common.ui.RecentSearchItem
import com.woshiwangnima.healthdietpro.common.ui.SearchActivityPanel
import com.woshiwangnima.healthdietpro.common.ui.SearchPanelDismissLayer
import com.woshiwangnima.healthdietpro.common.ui.SearchPanelMaxHeight
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppEmptyState
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppSearchField
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppTheme
import com.woshiwangnima.healthdietpro.common.ui.designsystem.LocalAppTokens
import com.woshiwangnima.healthdietpro.common.ui.designsystem.rememberAppDarkTheme
import com.woshiwangnima.healthdietpro.common.ui.recordLatestSummary
import com.woshiwangnima.healthdietpro.model.prefs.UserPrefs

/**
 * 记录中心。
 *
 * 搜索口径（§4.4-2、§6.2）：输入即过滤，不再需要「提交」这一步；回车 / 点开结果才写历史。
 * 曾经是「输入只改文本、点搜索图标才过滤」，于是大部分用户打完字看到列表没反应，以为搜索坏了。
 */
@Composable
fun RecordScreen(
    uiState: RecordUiState,
    onActionClick: (RecordActionId) -> Unit,
    onAddActionClick: (RecordActionId) -> Unit,
    onQueryChange: (String) -> Unit,
    onCommitSearch: (String) -> Unit,
    onRemoveSearchHistory: (String) -> Unit,
    onClearSearchHistory: () -> Unit,
    onOpenRecentAction: (RecordActionId) -> Unit,
    onRemoveRecentAction: (RecordActionId) -> Unit,
    onClearRecentActions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppTheme(darkTheme = rememberAppDarkTheme()) {
        RecordContent(
            uiState = uiState,
            onActionClick = { actionId -> onCommitSearch(uiState.query); onActionClick(actionId) },
            onAddActionClick = onAddActionClick,
            onQueryChange = onQueryChange,
            onCommitSearch = onCommitSearch,
            onRemoveSearchHistory = onRemoveSearchHistory,
            onClearSearchHistory = onClearSearchHistory,
            onOpenRecentAction = onOpenRecentAction,
            onRemoveRecentAction = onRemoveRecentAction,
            onClearRecentActions = onClearRecentActions,
            modifier = modifier,
        )
    }
}

@Composable
private fun RecordContent(
    uiState: RecordUiState,
    onActionClick: (RecordActionId) -> Unit,
    onAddActionClick: (RecordActionId) -> Unit,
    onQueryChange: (String) -> Unit,
    onCommitSearch: (String) -> Unit,
    onRemoveSearchHistory: (String) -> Unit,
    onClearSearchHistory: () -> Unit,
    onOpenRecentAction: (RecordActionId) -> Unit,
    onRemoveRecentAction: (RecordActionId) -> Unit,
    onClearRecentActions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalAppTokens.current
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    var searchFocused by remember { mutableStateOf(false) }
    var collapsedSections by remember { mutableStateOf(loadCollapsedSections(context)) }
    var plannedExpanded by remember { mutableStateOf(loadPlannedExpanded(context)) }
    val query = uiState.query.trim()
    val searching = query.isNotBlank()
    val itemById = remember(uiState.sections) { uiState.sections.flatMap { it.items }.associateBy { it.id } }
    val matchedSections = uiState.sections.map { section ->
        section to section.items.filter { item -> item.enabled && item.matchesQuery(query) }
    }.filter { (_, items) -> items.isNotEmpty() }
    val plannedItems = uiState.sections.flatMap { section ->
        section.items.filter { item -> !item.enabled && item.matchesQuery(query) }
    }
    val hasMatches = matchedSections.isNotEmpty() || plannedItems.isNotEmpty()
    // 光标一落进搜索框就展开（哪怕历史是空的）：顶部那句「点选标签即可填入」必须跟着光标出现，
    // 而不是等用户先攒下几条历史才肯露面。输入框一有内容（searching）立即消失。
    val showSearchPanel = searchFocused && !searching

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(tokens.colors.bg),
    ) {
        val pagePadding = tokens.spacing.s3
        // 下拉面板贴在搜索框下沿，作为浮层叠在内容之上：展开时不参与排版，不会把下方功能网格顶下去。
        val panelTop = pagePadding + tokens.spacing.hitMin + tokens.spacing.s2
        val collapseSearch = {
            searchFocused = false
            focusManager.clearFocus()
        }
        // 返回键先收起面板，而不是直接切走页面（§5.3）。
        BackHandler(enabled = showSearchPanel) { collapseSearch() }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = pagePadding, vertical = pagePadding),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.s2),
        ) {
            AppSearchField(
                value = uiState.query,
                onValueChange = onQueryChange,
                placeholder = stringResource(R.string.record_search),
                onSearch = { onCommitSearch(uiState.query) },
                onFocusChanged = { searchFocused = it },
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(top = tokens.spacing.s1),
                verticalArrangement = Arrangement.spacedBy(tokens.spacing.s3),
            ) {
                // 搜索命中为空时给结论 + 出口（§6.2），而不是把整页收成一片空白。
                if (searching && !hasMatches) {
                    AppEmptyState(
                        title = stringResource(R.string.record_search_empty),
                        description = stringResource(R.string.record_search_empty_hint, uiState.query.trim()),
                        iconRes = R.drawable.ic_search_empty,
                        action = {
                            TextButton(onClick = { onQueryChange("") }) {
                                Text(
                                    text = stringResource(R.string.record_search_clear),
                                    style = tokens.type.bodyS,
                                    color = tokens.colors.primary,
                                )
                            }
                        },
                    )
                }

                matchedSections.forEach { (section, items) ->
                    val sectionKey = section.key()
                    ActionSectionCard(
                        title = stringResource(section.titleRes),
                        titleIconRes = section.titleIconRes,
                        // 搜索时展开所有命中的分组，避免结果藏在折叠状态里。
                        collapsed = !searching && sectionKey in collapsedSections,
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
                            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.s1),
                            verticalArrangement = Arrangement.spacedBy(tokens.spacing.s1),
                        ) {
                            items.forEach { item ->
                                ActionGridItem(
                                    title = stringResource(item.titleRes),
                                    iconRes = item.iconRes,
                                    enabled = item.enabled,
                                    summary = item.latestTimestamp?.let { timestamp -> recordLatestSummary(timestamp, item.latestValue.orEmpty()) }
                                        ?: stringResource(R.string.record_no_data),
                                    showSummary = item.showSummary,
                                    onClick = { onActionClick(item.id) },
                                    onAddClick = item.id.takeIf { it in quickAddActionIds }?.let { actionId -> { onAddActionClick(actionId) } },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }

                if (plannedItems.isNotEmpty()) {
                    ActionSectionCard(
                        title = stringResource(R.string.record_planned_features_count, plannedItems.size),
                        titleIconRes = R.drawable.ic_time,
                        collapsed = !searching && !plannedExpanded,
                        onToggleCollapse = {
                            plannedExpanded = !plannedExpanded
                            savePlannedExpanded(context, plannedExpanded)
                        },
                    ) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            maxItemsInEachRow = 2,
                            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.s1),
                            verticalArrangement = Arrangement.spacedBy(tokens.spacing.s1),
                        ) {
                            plannedItems.forEach { item ->
                                ActionGridItem(
                                    title = stringResource(item.titleRes),
                                    iconRes = item.iconRes,
                                    enabled = false,
                                    summary = stringResource(R.string.record_no_data),
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

        if (showSearchPanel) {
            // 320 dp 上限之外再按可用高度的一半封顶：横屏或大号键盘时不会顶出屏幕。
            val panelMaxHeight = minOf(
                SearchPanelMaxHeight,
                maxHeight * 0.55f,
                maxHeight - panelTop - pagePadding,
            ).coerceAtLeast(160.dp)

            SearchPanelDismissLayer(
                onDismiss = { collapseSearch() },
                modifier = Modifier.padding(top = panelTop),
            )
            SearchActivityPanel(
                history = uiState.searchHistory,
                recentItems = uiState.recentActionIds.mapNotNull { id ->
                    itemById[id]?.let { RecentSearchItem(id.name, stringResource(it.titleRes), it.iconRes) }
                },
                historyTitle = stringResource(R.string.record_search_history),
                recentsTitle = stringResource(R.string.record_recently_used),
                onSelectHistory = { entry ->
                    onQueryChange(entry)
                    onCommitSearch(entry)
                    collapseSearch()
                },
                onRemoveHistory = onRemoveSearchHistory,
                onClearHistory = onClearSearchHistory,
                onOpenRecent = { item -> onOpenRecentAction(RecordActionId.valueOf(item.id)) },
                onRemoveRecent = { item -> onRemoveRecentAction(RecordActionId.valueOf(item.id)) },
                onClearRecents = onClearRecentActions,
                onCollapse = { collapseSearch() },
                maxHeight = panelMaxHeight,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = pagePadding, end = pagePadding, top = panelTop),
            )
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
    RecordActionId.Price,
)

@Composable
private fun RecordActionItemUiState.matchesQuery(query: String): Boolean =
    query.isBlank() ||
        stringResource(titleRes).contains(query, ignoreCase = true) ||
        searchAliasRes.any { stringResource(it).contains(query, ignoreCase = true) }

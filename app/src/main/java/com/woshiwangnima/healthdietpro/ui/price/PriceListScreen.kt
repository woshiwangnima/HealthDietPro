package com.woshiwangnima.healthdietpro.ui.price

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.DialogProperties
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.time.RecordTimePrecision
import com.woshiwangnima.healthdietpro.common.time.formatRecordTimestamp
import com.woshiwangnima.healthdietpro.common.ui.AnimatedPageContent
import com.woshiwangnima.healthdietpro.common.ui.AppIconTextButton
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen
import com.woshiwangnima.healthdietpro.common.ui.DetailTabBar
import com.woshiwangnima.healthdietpro.common.ui.DetailTabItem
import com.woshiwangnima.healthdietpro.common.ui.FoodSearchField
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppSectionCard
import com.woshiwangnima.healthdietpro.common.ui.designsystem.LocalAppTokens
import com.woshiwangnima.healthdietpro.model.price.PriceGroup
import com.woshiwangnima.healthdietpro.model.price.PriceItemStats
import com.woshiwangnima.healthdietpro.model.price.PriceRecord

/**
 * 物价列表（记物价首页），底部两个切换栏：
 *
 * - **【统计】**（左）：只按物品聚合，回答「这东西我买过多少钱」——跨地区、跨日期、跨单位全收在一个物品下；
 * - **【记录】**（右）：原始记录，按「物品 + 地区 + 日期 + 单位」合并成区间卡片。
 *
 * 顺序是用户定的（统计在左、记录在右）；**默认停在【记录】**，因为「添加物价记录」这个主动作在那一页，
 * 进来就能记一笔，不用先切 tab。切页用 `AnimatedPageContent` + `DetailTabBar`，与睡眠 / 饮食 / 血压页同一套切换语法。
 */
@Composable
internal fun PriceListScreen(
    uiState: PriceUiState,
    onQueryChange: (String) -> Unit,
    onToggleRegion: (String) -> Unit,
    onClearRegions: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (PriceRecord) -> Unit,
    onDeleteGroup: (PriceGroup) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = remember {
        listOf(
            DetailTabItem("statistics", R.string.price_tab_statistics, R.drawable.ic_chart),
            DetailTabItem("records", R.string.price_tab_records, R.drawable.ic_list),
        )
    }
    var selectedTab by rememberSaveable {
        mutableIntStateOf(tabs.indexOfFirst { it.id == "records" }.coerceAtLeast(0))
    }
    // 删除确认放在页面层而不是 tab 内：切 tab 时这个弹窗不该跟着消失或重建。
    var pendingDelete by remember { mutableStateOf<PriceGroup?>(null) }
    val colors = LocalAppTokens.current.colors

    BaseScreen(
        title = stringResource(R.string.price_title),
        onBack = onBack,
        // 底部由 DetailTabBar 自己让位给系统导航栏，页面不再重复加一圈 padding。
        includeNavigationBarPadding = false,
    ) { padding ->
        Column(
            modifier = modifier.fillMaxSize().padding(padding),
        ) {
            AnimatedPageContent(
                targetState = selectedTab,
                modifier = Modifier.weight(1f),
                direction = { initial, target -> target - initial },
            ) { tabIndex ->
                if (tabs[tabIndex].id == "records") {
                    PriceRecordsTab(
                        uiState = uiState,
                        onQueryChange = onQueryChange,
                        onToggleRegion = onToggleRegion,
                        onClearRegions = onClearRegions,
                        onAdd = onAdd,
                        onEdit = onEdit,
                        onDeleteRequest = { pendingDelete = it },
                    )
                } else {
                    PriceStatisticsTab(
                        stats = uiState.visibleStats,
                        // 统计跟着记录页的筛选走，但筛选控件不在这页——必须在顶部说清楚，
                        // 否则用户看到「少了几个物品」会以为是统计漏了数据。
                        filterSummary = buildList {
                            if (uiState.query.isNotBlank()) add("\u300c${uiState.query.trim()}\u300d")
                            if (uiState.selectedRegions.isNotEmpty()) add(uiState.selectedRegions.joinToString("\u3001"))
                        }.joinToString(" \u00b7 "),
                        onClearFilter = {
                            onQueryChange("")
                            onClearRegions()
                        },
                    )
                }
            }
            DetailTabBar(items = tabs, selectedId = tabs[selectedTab].id) { item ->
                selectedTab = tabs.indexOf(item)
            }
        }
    }

    pendingDelete?.let { group ->
        AlertDialog(
            // 删除不可逆：点遮罩不关闭，焦点默认落在「取消」（§5.5.2）。
            onDismissRequest = { pendingDelete = null },
            properties = DialogProperties(dismissOnClickOutside = false),
            title = { Text(stringResource(R.string.price_delete_confirm_title)) },
            text = {
                Text(
                    // 一张卡片可能合并了多条记录，必须说清楚会一起删掉，别让人以为只删一行。
                    if (group.count > 1) {
                        stringResource(
                            R.string.price_delete_group_message,
                            group.itemName,
                            group.priceDisplay,
                            group.count,
                        )
                    } else {
                        stringResource(
                            R.string.price_delete_confirm_message,
                            group.itemName,
                            group.priceDisplay,
                        )
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteGroup(group)
                    pendingDelete = null
                }) { Text(stringResource(R.string.body_record_delete), color = colors.danger) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.compose_confirm_dialog_cancel))
                }
            },
        )
    }
}

/**
 * 【记录】页：原始记录按「物品 + 地区 + 日期 + 单位」合并成区间卡片。
 *
 * 顺序按「先做 → 再看 → 后筛」排：主动作在最上，筛选放在记录之前，
 * 是因为筛选条件的产出（地区 chip）依赖已有记录，没有记录时不占位（§6.2 空态不留空白控件）。
 */
@Composable
private fun PriceRecordsTab(
    uiState: PriceUiState,
    onQueryChange: (String) -> Unit,
    onToggleRegion: (String) -> Unit,
    onClearRegions: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (PriceRecord) -> Unit,
    onDeleteRequest: (PriceGroup) -> Unit,
) {
    val tokens = LocalAppTokens.current
    val colors = tokens.colors
    val spacing = tokens.spacing
    // 卡片是「合并后的区间」，不是单条记录：同条件多条合成一行（见 PriceGroup）。
    val groups = uiState.visibleGroups

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.page, vertical = spacing.s3),
        verticalArrangement = Arrangement.spacedBy(spacing.s4),
    ) {
        // 单一主动作（§1.3）：新增是全页唯一填充按钮，其余操作都是行内图标或文本按钮。
        AppIconTextButton(
            text = stringResource(R.string.price_add),
            iconRes = R.drawable.ic_add,
            onClick = onAdd,
            modifier = Modifier.fillMaxWidth(),
        )

        // 没有记录时不摆一个筛不出东西的筛选项（§6.2）；但搜索词还在时保留卡片，否则用户看不到也清不掉它。
        if (uiState.records.isNotEmpty() || uiState.query.isNotBlank()) {
            AppSectionCard(
                iconRes = R.drawable.ic_list,
                title = stringResource(R.string.price_filter_section),
            ) {
                FoodSearchField(
                    value = uiState.query,
                    onValueChange = onQueryChange,
                    placeholder = stringResource(R.string.price_search_hint),
                )
                if (uiState.regionOptions.isNotEmpty()) {
                    Spacer(Modifier.height(spacing.s2))
                    PriceRegionFilterRow(
                        regions = uiState.regionOptions,
                        selected = uiState.selectedRegions,
                        onToggle = onToggleRegion,
                        onClear = onClearRegions,
                    )
                }
            }
        }

        AppSectionCard(
            iconRes = R.drawable.ic_price,
            title = stringResource(R.string.price_section_records),
            // 空态时标题右侧不再挂「共 0 条」，否则一句话说两遍。
            action = if (uiState.records.isEmpty()) {
                null
            } else {
                {
                    Text(
                        text = stringResource(R.string.price_count, groups.size),
                        style = tokens.type.bodyS,
                        color = colors.textMuted,
                        modifier = Modifier.padding(end = spacing.s2),
                    )
                }
            },
            // 行自己带左右内边距，分隔线才能和文字左缘对齐（与「相关菜肴」一致）。
            contentPadding = PaddingValues(bottom = spacing.s2),
        ) {
            when {
                uiState.records.isEmpty() -> PriceEmptyState(
                    title = stringResource(R.string.price_empty),
                    description = stringResource(R.string.price_empty_hint),
                )

                groups.isEmpty() -> {
                    PriceEmptyState(
                        title = stringResource(R.string.price_filter_empty),
                        description = stringResource(R.string.price_filter_empty_hint),
                    )
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        TextButton(onClick = { onQueryChange(""); onClearRegions() }) {
                            Text(stringResource(R.string.price_filter_clear))
                        }
                    }
                }

                else -> groups.forEachIndexed { index, group ->
                    if (index > 0) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = spacing.s3),
                            thickness = tokens.radii.highlight,
                            color = colors.border,
                        )
                    }
                    PriceRecordRow(
                        group = group,
                        onClick = { onEdit(group.latest) },
                        onDelete = { onDeleteRequest(group) },
                    )
                }
            }
        }
    }
}

/**
 * 【统计】页：**只按物品聚合**的价格区间。
 *
 * 同一个物品下可能出现多个单位（元/斤、元/个），各占一行——不同售卖单位的价格不可比，
 * 合成一个区间只会得到一个不存在的数（详见 `PriceUnitRange` 的注释）。
 */
@Composable
private fun PriceStatisticsTab(
    stats: List<PriceItemStats>,
    filterSummary: String,
    onClearFilter: () -> Unit,
) {
    val tokens = LocalAppTokens.current
    val colors = tokens.colors
    val spacing = tokens.spacing

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.page, vertical = spacing.s3),
        verticalArrangement = Arrangement.spacedBy(spacing.s4),
    ) {
        if (filterSummary.isNotBlank()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.price_stats_filtered_by, filterSummary),
                    style = tokens.type.caption,
                    color = colors.textMuted,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                TextButton(onClick = onClearFilter) {
                    Text(stringResource(R.string.price_filter_clear))
                }
            }
        }

        AppSectionCard(
            iconRes = R.drawable.ic_chart,
            title = stringResource(R.string.price_stats_section),
            action = if (stats.isEmpty()) {
                null
            } else {
                {
                    Text(
                        text = stringResource(R.string.price_stats_count, stats.size),
                        style = tokens.type.bodyS,
                        color = colors.textMuted,
                        modifier = Modifier.padding(end = spacing.s2),
                    )
                }
            },
            contentPadding = PaddingValues(bottom = spacing.s2),
        ) {
            if (stats.isEmpty()) {
                PriceEmptyState(
                    iconRes = R.drawable.ic_chart,
                    title = stringResource(R.string.price_stats_empty),
                    description = stringResource(R.string.price_stats_empty_hint),
                )
            } else {
                stats.forEachIndexed { index, item ->
                    if (index > 0) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = spacing.s3),
                            thickness = tokens.radii.highlight,
                            color = colors.border,
                        )
                    }
                    PriceStatsRow(item)
                }
            }
        }

        // 说明统计口径，避免用户把「不同单位分行」误读成数据缺失。
        if (stats.isNotEmpty()) {
            Text(
                text = stringResource(R.string.price_stats_hint),
                style = tokens.type.caption,
                color = colors.textMuted,
                modifier = Modifier.padding(horizontal = spacing.s1),
            )
        }
    }
}

/**
 * 统计行：左列物品名 + 「N 个地区 · N 条记录」，右列按单位分行显示区间。
 * 单位最多显示两行，再多给一行「另有 N 种单位」——再长会让行高失控，也不常用。
 */
@Composable
private fun PriceStatsRow(item: PriceItemStats) {
    val tokens = LocalAppTokens.current
    val colors = tokens.colors
    val spacing = tokens.spacing
    val meta = buildList {
        if (item.regionCount > 0) add(stringResource(R.string.price_stats_regions, item.regionCount))
        add(stringResource(R.string.price_stats_records, item.recordCount))
    }.joinToString(" · ")
    val visibleRanges = item.ranges.take(2)
    val hiddenCount = item.ranges.size - visibleRanges.size

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = spacing.rowMinHeight)
            .padding(horizontal = spacing.s3, vertical = spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(spacing.s1),
        ) {
            Text(
                text = item.itemName,
                style = tokens.type.body,
                color = colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = meta,
                style = tokens.type.caption,
                color = colors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(spacing.s3))
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(spacing.s1),
        ) {
            visibleRanges.forEach { range ->
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = range.rangeText,
                        style = tokens.type.numeric,
                        color = colors.text,
                        maxLines = 1,
                    )
                    Spacer(Modifier.width(spacing.s1))
                    Text(
                        text = range.unitText,
                        style = tokens.type.caption,
                        color = colors.textMuted,
                        maxLines = 1,
                    )
                }
            }
            if (hiddenCount > 0) {
                Text(
                    text = stringResource(R.string.price_stats_more_units, hiddenCount),
                    style = tokens.type.caption,
                    color = colors.textMuted,
                    maxLines = 1,
                )
            }
        }
    }
}

/**
 * 地区筛选：横向可滚动的 chip 行。
 * chip 视觉高 28 dp（与站内其他筛选 chip 同档），外层补到 48 dp 热区（§2.1-2、§10.4）。
 */
@Composable
private fun PriceRegionFilterRow(
    regions: List<String>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    onClear: () -> Unit,
) {
    val tokens = LocalAppTokens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selected.isNotEmpty()) {
            PriceFilterChip(
                label = stringResource(R.string.price_filter_region_all),
                selected = false,
                onClick = onClear,
            )
        }
        regions.forEach { region ->
            PriceFilterChip(
                label = region,
                selected = region in selected,
                onClick = { onToggle(region) },
            )
        }
    }
}

@Composable
private fun PriceFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val tokens = LocalAppTokens.current
    val colors = tokens.colors
    // 选中态用主色 2 dp 外描边 + 填充底，不只改文字颜色（§2.6、§4.6）。
    Box(
        modifier = Modifier
            .heightIn(min = tokens.spacing.hitMin)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(tokens.radii.label),
            color = if (selected) colors.primaryContainer else Color.Transparent,
            border = BorderStroke(
                width = if (selected) tokens.radii.stroke else tokens.radii.highlight,
                color = if (selected) colors.primary else colors.border,
            ),
        ) {
            Box(
                modifier = Modifier.padding(horizontal = tokens.spacing.s3, vertical = tokens.spacing.s1),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = tokens.type.bodyS,
                    color = if (selected) colors.onPrimaryContainer else colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * 一张物价卡片：左列物品名 + 「地区 · 日期（· 合并条数）」+ 备注，右侧价格区间与售卖单位，末端删除。
 *
 * 价格是合并后的区间（`3.2–4.5`），用等宽数字右对齐（§2.4-2）；
 * 只有一条记录时不写条数，避免给单条记录也挂一个「1 条」的噪音。
 * 删除是唯一危险操作，用 danger 色（§2.3）。
 */
@Composable
private fun PriceRecordRow(
    group: PriceGroup,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val tokens = LocalAppTokens.current
    val colors = tokens.colors
    val spacing = tokens.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = spacing.rowMinHeight)
            .padding(start = spacing.s3, end = spacing.s1, top = spacing.s2, bottom = spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(spacing.s1),
        ) {
            Text(
                text = group.itemName,
                style = tokens.type.body,
                color = colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = buildList {
                    add(group.regionDisplay.ifBlank { stringResource(R.string.price_region_unset) })
                    add(formatRecordTimestamp(group.recordedAt, RecordTimePrecision.DATE))
                    if (group.count > 1) add(stringResource(R.string.price_group_count, group.count))
                }.joinToString(" · "),
                style = tokens.type.caption,
                color = colors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (group.note.isNotBlank()) {
                Text(
                    text = group.note,
                    style = tokens.type.caption,
                    color = colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(spacing.s2))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = group.priceRangeText,
                style = tokens.type.numeric,
                color = colors.text,
                maxLines = 1,
            )
            Text(
                text = group.priceUnit + " / " + group.sellUnit,
                style = tokens.type.caption,
                color = colors.textMuted,
                maxLines = 1,
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                painter = painterResource(R.drawable.ic_delete),
                contentDescription = stringResource(R.string.body_record_delete),
                modifier = Modifier.size(spacing.s5),
                tint = colors.danger,
            )
        }
    }
}

/** 空态：说明「为什么空、下一步做什么」，不留一片空白（§6.2）。 */
@Composable
private fun PriceEmptyState(
    title: String,
    description: String,
    @DrawableRes iconRes: Int = R.drawable.ic_price,
) {
    val tokens = LocalAppTokens.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = tokens.spacing.s3, vertical = tokens.spacing.s5),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(tokens.spacing.s6),
            tint = tokens.colors.disabled,
        )
        Spacer(Modifier.height(tokens.spacing.s3))
        Text(
            text = title,
            style = tokens.type.body,
            color = tokens.colors.text,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(tokens.spacing.s1))
        Text(
            text = description,
            style = tokens.type.caption,
            color = tokens.colors.textMuted,
            textAlign = TextAlign.Center,
        )
    }
}

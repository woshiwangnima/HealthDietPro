package com.woshiwangnima.healthdietpro.ui.nutrition

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.ui.TextOverflowText
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppEaseOut
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppEmptyState
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppFilterDropdown
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppFilterOption
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppPillIconTextButton
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppPyramidChart
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppSectionCard
import com.woshiwangnima.healthdietpro.common.ui.designsystem.LocalAppTokens
import com.woshiwangnima.healthdietpro.common.ui.designsystem.PyramidBand
import com.woshiwangnima.healthdietpro.common.ui.nutrientColor
import com.woshiwangnima.healthdietpro.model.food.FoodItem
import kotlin.math.abs

/**
 * 金字塔与榜单条形的「可比项代表色」**唯一入口**。
 *
 * 能量 / 蛋白质 / 脂肪 / 碳水 / 膳食纤维直接走 [nutrientColor]，与【营养档案】-【营养素供能】
 * 的环图、以及【记饮食】-【统计】-【当日摄入】的指标卡取同一支颜色；GI / GL 不是营养素，
 * 没有供能占比，沿用提示色，避免假装它们也是三大产能营养素。
 */
@Composable
private fun rankMetricColor(metric: RankMetric?): Color {
    val tokens = LocalAppTokens.current
    return if (metric == RankMetric.GI || metric == RankMetric.GL) {
        tokens.colors.info
    } else {
        nutrientColor(metric?.code.orEmpty())
    }
}

/**
 * 详情页的「排行」页签：把当前这一条放进同类食物里定位，而不是给一张与它无关的通用榜单。
 *
 * 顺序按「先定条件，再看结果」：一行两格的筛选面板固定在页签栏下方不随内容滚走，
 * 下面依次是「左金字塔 / 右说明」的名次概览与完整榜单。
 *
 * 控件归属按「谁的结果谁来控制」：指标与范围属于筛选，方向属于榜单，因此排序切换按钮挂在
 * 榜单卡头右上角；筛选面板不再放排序、重置与说明图标。说明一律收进长按提示，卡底不常显脚注。
 * 所有数值都折成「每 100 g」口径，菜肴按整份克数折回。
 */
@Composable
internal fun FoodRankingTab(
    food: FoodItem,
    foods: List<FoodItem>,
    viewModel: NutritionViewModel,
    language: String,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalAppTokens.current
    val samples = remember(foods, food.kind, viewModel) {
        collectRankSamples(foods, food.kind, viewModel::resolvePer100g, viewModel.nutrientMetas())
    }
    var metric by remember(food.id) { mutableStateOf(RankMetric.ENERGY) }
    var scope by remember(food.id) { mutableStateOf(RankScope.ALL) }
    var descending by remember(food.id) { mutableStateOf(true) }
    var visibleCount by remember(food.id) { mutableIntStateOf(RankPageSize) }
    // 金字塔点选状态放在控制器这一层：切指标 / 范围 / 方向时统一清掉，避免选中一个已经不存在的档。
    var selectedTierId by remember(food.id) { mutableStateOf<String?>(null) }
    val groupTag = food.rankGroupTag()
    val available = remember(samples, scope, groupTag) { availableRankMetrics(samples, scope, groupTag) }
    // 范围切窄后当前指标可能一项都不剩，自动回落到第一个有数据的指标，不留空榜。
    val activeMetric = metric.takeIf { available.contains(it) } ?: available.firstOrNull()
    LaunchedEffect(activeMetric, scope, descending) {
        visibleCount = RankPageSize
        selectedTierId = null
    }
    val unit = remember(activeMetric, viewModel) {
        activeMetric?.let { rankUnit(it, viewModel.nutrientMetas()) }.orEmpty()
    }
    val ranking = remember(samples, activeMetric, scope, descending, unit, language, food.id) {
        activeMetric?.let { m -> buildFoodRanking(food, samples, m, scope, descending, unit, language) }
    }
    val pyramid = remember(ranking) { ranking?.pyramidBands().orEmpty() }
    val barColor = rankMetricColor(activeMetric)

    Column(modifier = modifier.fillMaxWidth()) {
        // 固定筛选：切指标 / 范围后下面的结果立刻变，条件本身不该被滚出视野。
        RankFilterPanel(
            available = available,
            activeMetric = activeMetric,
            scope = scope,
            groupTag = groupTag,
            onMetric = { metric = it },
            onScope = { scope = it },
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = tokens.spacing.s4)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.s4),
        ) {
            RankOverviewCard(
                ranking = ranking,
                pyramid = pyramid,
                metric = activeMetric,
                unit = unit,
                barColor = barColor,
                selectedTierId = selectedTierId,
                onTierSelect = { selectedTierId = it },
            )
            RankListCard(
                ranking = ranking,
                unit = unit,
                barColor = barColor,
                language = language,
                visibleCount = visibleCount,
                descending = descending,
                onToggleDirection = { descending = !descending },
                onShowMore = { visibleCount += RankPageSize },
                onCollapse = { visibleCount = RankPageSize },
                onOpen = { viewModel.openFood(it) },
            )
        }
    }
}

/**
 * 无标题筛选面板：形态对齐【记物价】-【统计】-【查找与筛选】——一行两格的紧凑下拉格，
 * 一格 = 标签 + 当前值 + 折叠箭头，点到即筛，没有【开始查找】这种二次确认。
 *
 * 只留「指标 / 范围」：排序方向挪到榜单卡头做成切换按钮，重置按钮与那枚 info 长按说明图标
 * 一并删除。格子里的标签已经自解释，筛选区不再需要独立的说明入口（非必要说明仍可在结果卡
 * 的长按卡头里查到）。
 */
@Composable
private fun RankFilterPanel(
    available: List<RankMetric>,
    activeMetric: RankMetric?,
    scope: RankScope,
    groupTag: String?,
    onMetric: (RankMetric) -> Unit,
    onScope: (RankScope) -> Unit,
) {
    val tokens = LocalAppTokens.current
    val shape = RoundedCornerShape(tokens.radii.container)

    val allLabel = stringResource(RankScope.ALL.labelRes)
    // groupTag 可空，先收窄再查标签资源：rankGroupLabelRes 只接非空 tag。
    val sameLabel = if (groupTag != null) {
        rankGroupLabelRes(groupTag)?.let { stringResource(it) }
            ?: stringResource(RankScope.SAME_CATEGORY.labelRes)
    } else {
        stringResource(RankScope.SAME_CATEGORY.labelRes)
    }
    val allOption = AppFilterOption(
        id = "all",
        text = allLabel,
        selected = scope == RankScope.ALL,
    ) { onScope(RankScope.ALL) }
    val sameOption = if (groupTag != null) {
        AppFilterOption(
            id = "same",
            text = sameLabel,
            selected = scope == RankScope.SAME_CATEGORY,
        ) { onScope(RankScope.SAME_CATEGORY) }
    } else {
        null
    }
    val scopeValue = if (scope == RankScope.ALL) allLabel else sameLabel

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(tokens.colors.panelRaised)
            .border(tokens.radii.stroke, tokens.colors.border, shape)
            .padding(tokens.spacing.s3),
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppFilterDropdown(
            label = stringResource(R.string.rank_filter_metric),
            valueText = activeMetric?.let { stringResource(it.labelRes) }
                ?: stringResource(R.string.rank_no_metric),
            active = activeMetric != null && activeMetric != RankMetric.ENERGY,
            options = available.map { item ->
                AppFilterOption(
                    id = item.name,
                    text = stringResource(item.labelRes),
                    selected = item == activeMetric,
                ) { onMetric(item) }
            },
            modifier = Modifier.weight(1f),
        )
        AppFilterDropdown(
            label = stringResource(R.string.rank_scope_label),
            valueText = scopeValue,
            active = scope != RankScope.ALL,
            pinned = allOption,
            options = listOfNotNull(sameOption),
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * 名次概览：左边金字塔回答「我在第几层」，右边说明栏回答「具体是多少」。
 *
 * 两个方案合并落地：默认就是左图右文（中位数与差距等数值全部挪到右侧）；金字塔每层仍可点，
 * 点中后该层轮廓加重、其余层淡出，右侧立刻切换成该档的名次区间 / 条目数 / 数值区间，
 * 并给一个【返回】出口。这样窄屏里金字塔不必再塞两行长文本，信息又不会丢。
 */
@Composable
private fun RankOverviewCard(
    ranking: FoodRanking?,
    pyramid: List<RankPyramidBand>,
    metric: RankMetric?,
    unit: String,
    barColor: Color,
    selectedTierId: String?,
    onTierSelect: (String?) -> Unit,
) {
    val tokens = LocalAppTokens.current
    val selected = pyramid.firstOrNull { it.id == selectedTierId }
    AppSectionCard(
        iconRes = R.drawable.ic_trophy,
        title = stringResource(R.string.rank_overview_title),
        tooltip = stringResource(R.string.rank_overview_tooltip),
    ) {
        if (ranking == null || pyramid.isEmpty()) {
            AppEmptyState(
                iconRes = R.drawable.ic_leaderboard,
                title = stringResource(R.string.rank_empty_title),
                description = stringResource(R.string.rank_empty_hint),
            )
            return@AppSectionCard
        }
        val bands = pyramid.map { band ->
            PyramidBand(
                id = band.id,
                label = stringResource(band.labelRes),
                // 紧凑模式：数值区间不再写进层内，交给右侧说明栏（默认态给中位数，点选态给该档区间）。
                valueLabel = "",
                weight = band.weight,
                // 当前层用已按两套主题校准过对比度的 primary/onPrimary，其余层退成指标色的淡底。
                color = if (band.isCurrent) tokens.colors.primary else barColor.copy(alpha = 0.18f),
                labelColor = if (band.isCurrent) tokens.colors.onPrimary else tokens.colors.text,
                // 「你在这里」挪到右侧说明栏，窄栏层内只留档位名，不被长文本挤断。
                marker = null,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppPyramidChart(
                bands = bands,
                highlightedId = pyramid.firstOrNull { it.isCurrent }?.id,
                selectedBandId = selectedTierId,
                onBandSelect = onTierSelect,
                modifier = Modifier.weight(0.56f),
                bandHeight = 46.dp,
                compact = true,
            )
            Spacer(Modifier.width(tokens.spacing.s3))
            RankOverviewDetail(
                ranking = ranking,
                metric = metric,
                unit = unit,
                accent = barColor,
                selected = selected,
                onClearSelection = { onTierSelect(null) },
                modifier = Modifier.weight(0.44f),
            )
        }
    }
}

/**
 * 金字塔右侧的说明栏：默认态是「这一条本身」的读数，点选某档后换成该档的读数。
 * 两态共用同一块位置，不额外开卡、不把说明堆到卡底。
 *
 * 默认态只留三行有效信息——当前指标值、中位数、与中位数的差距。
 * 「你在这里 · 档名」与「全部食材」已由金字塔高亮和上方筛选格表达，属重复信息，本轮删除。
 */
@Composable
private fun RankOverviewDetail(
    ranking: FoodRanking,
    metric: RankMetric?,
    unit: String,
    accent: Color,
    selected: RankPyramidBand?,
    onClearSelection: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalAppTokens.current
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.s2),
    ) {
        if (selected == null) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = ranking.currentRank?.let { "#$it" } ?: "—",
                    style = tokens.type.titleL,
                    color = tokens.colors.primary,
                    maxLines = 1,
                )
                Spacer(Modifier.width(tokens.spacing.s1))
                Text(
                    text = stringResource(R.string.rank_ring_total, ranking.total),
                    style = tokens.type.caption,
                    color = tokens.colors.textMuted,
                    maxLines = 1,
                    modifier = Modifier.padding(bottom = tokens.spacing.s1),
                )
            }
            if (ranking.currentRank != null) PercentileBadge(ranking.percentile)
            RankOverviewStat(
                label = metric?.let { stringResource(it.labelRes) }
                    ?: stringResource(R.string.rank_no_metric),
                value = ranking.currentValue?.let { formatCompareValue(it) + unitSuffix(unit) } ?: "—",
                accent = accent,
            )
            RankOverviewStat(
                label = stringResource(R.string.rank_median_label),
                value = ranking.median?.let { formatCompareValue(it) + unitSuffix(unit) } ?: "—",
                accent = accent,
            )
            val delta = ranking.medianDeltaPercent()
            if (delta != null) {
                RankOverviewStat(
                    label = stringResource(
                        when {
                            delta == 0 -> R.string.rank_vs_median_same
                            delta > 0 -> R.string.rank_vs_median_above_label
                            else -> R.string.rank_vs_median_below_label
                        },
                    ),
                    value = stringResource(R.string.rank_percent_value, abs(delta)),
                    accent = accent,
                )
            }
        } else {
            Text(
                text = stringResource(selected.labelRes),
                style = tokens.type.titleS,
                color = tokens.colors.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (selected.isCurrent) {
                Text(
                    text = stringResource(R.string.rank_pyramid_you),
                    style = tokens.type.caption,
                    color = tokens.colors.primary,
                    maxLines = 1,
                )
            }
            if (selected.count == 0) {
                Text(
                    text = stringResource(R.string.rank_detail_empty),
                    style = tokens.type.bodyS,
                    color = tokens.colors.textMuted,
                )
            } else {
                RankOverviewStat(
                    label = stringResource(R.string.rank_detail_rank_label),
                    value = stringResource(R.string.rank_detail_rank_value, selected.fromRank, selected.toRank),
                    accent = accent,
                )
                RankOverviewStat(
                    label = stringResource(R.string.rank_detail_count_label),
                    value = stringResource(R.string.rank_detail_count_value, selected.count),
                    accent = accent,
                )
                RankOverviewStat(
                    label = stringResource(R.string.rank_detail_value_label),
                    value = bandValueRange(selected, unit),
                    accent = accent,
                )
            }
            AppPillIconTextButton(
                text = stringResource(R.string.rank_detail_back),
                iconRes = R.drawable.ic_arrow_left,
                onClick = onClearSelection,
            )
        }
    }
}

/**
 * 概览右侧的「文字卡 + 数字卡」一行：全览的三条（指标值 / 中位数 / 与中位数的差距）
 * 与点选态的三条（名次区间 / 条目数 / 数值区间）共用同一套版式。
 *
 * 文字卡吃满剩余宽度，文字走**轮播**：右栏只有 0.44 权重，中文标签（如「低于中位数」）
 * 在窄屏上必然放不下；缩字号会跌破 12 sp 下限，截断又会丢掉「高 / 低」这个方向，
 * 放得下时静止、放不下时缓慢横向滚动是唯一不牺牲信息的做法。
 *
 * 数字卡用指标代表色做 14% 淡底与 45% 描边，颜色只做辅助，数值本身仍用正文色靠明度对比可读；
 * 高低关系刻意不用「绿 = 好 / 红 = 坏」，降序排脂肪时靠前反而是坏事（§9.4 图表不靠颜色单独表意）。
 */
@Composable
private fun RankOverviewStat(label: String, value: String, accent: Color) {
    val tokens = LocalAppTokens.current
    val shape = RoundedCornerShape(tokens.radii.label)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(tokens.spacing.s1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(shape)
                .background(tokens.colors.panel)
                .border(tokens.radii.highlight, tokens.colors.border, shape)
                .padding(horizontal = tokens.spacing.s1, vertical = tokens.spacing.s1),
        ) {
            TextOverflowText(
                text = label,
                style = tokens.type.caption,
                color = tokens.colors.textMuted,
                maxLines = 1,
                overflowMode = "marquee",
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Box(
            modifier = Modifier
                .widthIn(max = 96.dp)
                .clip(shape)
                .background(accent.copy(alpha = 0.14f))
                .border(tokens.radii.highlight, accent.copy(alpha = 0.45f), shape)
                .padding(horizontal = tokens.spacing.s1, vertical = tokens.spacing.s1),
        ) {
            TextOverflowText(
                text = value,
                style = tokens.type.bodyS,
                color = tokens.colors.text,
                maxLines = 1,
                overflowMode = "marquee",
            )
        }
    }
}

/** 该档数值区间：无论排序方向都写 min–max，读者结合顶部当前值自己能对上方向。 */
@Composable
private fun bandValueRange(band: RankPyramidBand, unit: String): String {
    val min = band.minValue
    val max = band.maxValue
    val suffix = unitSuffix(unit)
    return when {
        band.count == 0 || min == null || max == null -> stringResource(R.string.rank_detail_empty)
        abs(min - max) < 0.0001 -> formatCompareValue(min) + suffix
        else -> formatCompareValue(min) + "–" + formatCompareValue(max) + suffix
    }
}

private fun unitSuffix(unit: String): String = if (unit.isBlank()) "" else " $unit"

@Composable
private fun PercentileBadge(percentile: Int) {
    val tokens = LocalAppTokens.current
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(tokens.radii.pill))
            .background(tokens.colors.primary.copy(alpha = 0.16f))
            .padding(horizontal = tokens.spacing.s2, vertical = tokens.spacing.s1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(tokens.colors.primary))
        Spacer(Modifier.width(tokens.spacing.s1))
        Text(
            text = stringResource(R.string.rank_percentile, percentile),
            style = tokens.type.caption,
            color = tokens.colors.primary,
            maxLines = 1,
        )
    }
}

@Composable
private fun RankListCard(
    ranking: FoodRanking?,
    unit: String,
    barColor: Color,
    language: String,
    visibleCount: Int,
    descending: Boolean,
    onToggleDirection: () -> Unit,
    onShowMore: () -> Unit,
    onCollapse: () -> Unit,
    onOpen: (FoodItem) -> Unit,
) {
    val tokens = LocalAppTokens.current
    AppSectionCard(
        iconRes = R.drawable.ic_leaderboard,
        title = stringResource(R.string.rank_list_title),
        tooltip = stringResource(R.string.rank_list_note),
        action = {
            // 「N 项可比」是筛选结果计数，「高 → 低 / 低 → 高」是榜单排序方向：
            // 两者都属于榜单状态，同放在卡头右侧共行并列，不再一个在卡头、一个在卡内。
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(tokens.spacing.s1),
            ) {
                Text(
                    text = stringResource(R.string.rank_participants, ranking?.total ?: 0),
                    style = tokens.type.caption,
                    color = tokens.colors.textMuted,
                    maxLines = 1,
                )
                AppPillIconTextButton(
                    text = stringResource(if (descending) R.string.rank_sort_desc else R.string.rank_sort_asc),
                    iconRes = if (descending) R.drawable.ic_sort_desc else R.drawable.ic_sort_asc,
                    onClick = onToggleDirection,
                    modifier = Modifier.padding(end = tokens.spacing.s2),
                )
            }
        },
    ) {
        if (ranking == null || ranking.entries.isEmpty()) {
            AppEmptyState(
                iconRes = R.drawable.ic_leaderboard,
                title = stringResource(R.string.rank_empty_title),
                description = stringResource(R.string.rank_empty_hint),
            )
            return@AppSectionCard
        }
        val maxValue = ranking.entries.maxOf { abs(it.value) }.coerceAtLeast(0.0001)
        val visible = ranking.entries.take(visibleCount)
        // 相邻可点击行留白 ≥ 8 dp（移动规范 07 门禁 04）；行高不小于 56 dp（§4.6-3）。
        Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.s2)) {
            visible.forEach { entry ->
                RankRow(
                    entry = entry,
                    language = language,
                    maxValue = maxValue,
                    unit = unit,
                    barColor = barColor,
                    isCurrent = entry.rank == ranking.currentRank,
                    onClick = { onOpen(entry.food) },
                )
            }
        }
        // 当前项名次靠后时，单独补一行，不强迫用户一路展开到 195 项才找到自己。
        val currentEntry = ranking.entries.firstOrNull { it.rank == ranking.currentRank }
        if (currentEntry != null && currentEntry.rank > visibleCount) {
            Spacer(Modifier.height(tokens.spacing.s2))
            Text(
                text = stringResource(R.string.rank_current_section),
                style = tokens.type.caption,
                color = tokens.colors.textMuted,
            )
            Spacer(Modifier.height(tokens.spacing.s2))
            RankRow(
                entry = currentEntry,
                language = language,
                maxValue = maxValue,
                unit = unit,
                barColor = barColor,
                isCurrent = true,
                onClick = { onOpen(currentEntry.food) },
            )
        }
        if (ranking.entries.size > RankPageSize) {
            Spacer(Modifier.height(tokens.spacing.s2))
            AppPillIconTextButton(
                text = if (visibleCount >= ranking.entries.size) {
                    stringResource(R.string.rank_collapse)
                } else {
                    stringResource(R.string.rank_show_more, minOf(RankPageSize, ranking.entries.size - visibleCount))
                },
                iconRes = R.drawable.ic_arrow_drop_down,
                onClick = if (visibleCount >= ranking.entries.size) onCollapse else onShowMore,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun RankRow(
    entry: RankEntry,
    language: String,
    maxValue: Double,
    unit: String,
    barColor: Color,
    isCurrent: Boolean,
    onClick: () -> Unit,
) {
    val tokens = LocalAppTokens.current
    val shape = RoundedCornerShape(tokens.radii.label)
    val ratio by animateFloatAsState(
        targetValue = (abs(entry.value) / maxValue).toFloat().coerceIn(0.02f, 1f),
        animationSpec = tween(tokens.motion.baseMs, easing = AppEaseOut),
        label = "rank-bar-${entry.food.id}",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = tokens.spacing.rowMinHeight)
            .clip(shape)
            .background(if (isCurrent) tokens.colors.primary.copy(alpha = 0.10f) else Color.Transparent)
            .border(
                width = if (isCurrent) tokens.radii.highlight else 0.dp,
                color = if (isCurrent) tokens.colors.primary else Color.Transparent,
                shape = shape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = tokens.spacing.s2, vertical = tokens.spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RankBadge(entry.rank, barColor)
        Spacer(Modifier.width(tokens.spacing.s2))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = entry.food.displayName(language),
                    style = tokens.type.bodyS,
                    color = tokens.colors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (isCurrent) CurrentPill()
            }
            Spacer(Modifier.height(tokens.spacing.s1))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(tokens.radii.pill))
                    .background(tokens.colors.border),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(ratio)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(tokens.radii.pill))
                        .background(barColor),
                )
            }
        }
        Spacer(Modifier.width(tokens.spacing.s2))
        Text(
            text = formatCompareValue(entry.value) + unitSuffix(unit),
            style = tokens.type.numeric,
            color = tokens.colors.text,
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier.width(84.dp),
        )
    }
}

@Composable
private fun RankBadge(rank: Int, barColor: Color) {
    val tokens = LocalAppTokens.current
    val topThree = rank <= 3
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(if (topThree) barColor.copy(alpha = 0.18f) else Color.Transparent)
            .border(
                width = tokens.radii.highlight,
                color = if (topThree) barColor else tokens.colors.border,
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "$rank",
            style = tokens.type.caption,
            color = if (topThree) tokens.colors.text else tokens.colors.textMuted,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Composable
private fun CurrentPill() {
    val tokens = LocalAppTokens.current
    Text(
        text = stringResource(R.string.rank_current_item),
        style = tokens.type.caption,
        color = tokens.colors.primary,
        modifier = Modifier
            .clip(RoundedCornerShape(tokens.radii.pill))
            .background(tokens.colors.primary.copy(alpha = 0.16f))
            .padding(horizontal = tokens.spacing.s2, vertical = 2.dp),
        maxLines = 1,
    )
}

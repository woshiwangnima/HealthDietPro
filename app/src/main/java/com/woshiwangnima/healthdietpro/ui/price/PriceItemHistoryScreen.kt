package com.woshiwangnima.healthdietpro.ui.price

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.time.RecordTimePrecision
import com.woshiwangnima.healthdietpro.common.time.formatRecordTimestamp
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppSectionCard
import com.woshiwangnima.healthdietpro.common.ui.designsystem.LocalAppTokens
import com.woshiwangnima.healthdietpro.model.price.PriceGroup
import com.woshiwangnima.healthdietpro.model.price.PriceRecord
import com.woshiwangnima.healthdietpro.model.price.groupByPrice
import com.woshiwangnima.healthdietpro.model.price.historySeries

/**
 * 【物品历史】：从统计页点某个物品进来，只看这一个物品。
 *
 * 一屏两段，**上走势下图明细**：先看「贵了还是便宜了」，再往下对「哪几次、在哪儿、多少钱」。
 * 原来是两个 tab，得来回切才能把图和数对上；同一屏上下摆，图形和明细天然是同一条时间线的两种读法。
 *
 * 两段高度分工：走势固定高度（图不能跟着列表涨），历史记录吃掉剩下的屏幕并**内部滚动**。
 * 这样图始终停在原地，滚动明细时不会把图顶出屏幕——「看着图往下翻明细」才成立。
 *
 * 这一页**不重复摆筛选**：地区 / 年份 / 月份 / 搜索都从统计页继承（见 `PriceUiState.historyRecords`），
 * 页面上再放一套同样的控件，就会出现「我在这里筛了，回统计页怎么又变了」的双份状态。
 * 也因此右上角要写「X 条 / 共 M 条」：筛选藏掉了多少条，用户得在页面上看得见。
 */
@Composable
internal fun PriceItemHistoryScreen(
    itemName: String,
    records: List<PriceRecord>,
    /** 不受统计页筛选影响时这个物品的条目数，做「X 条 / 共 M 条」的分母。 */
    totalRowCount: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalAppTokens.current
    val colors = tokens.colors
    val spacing = tokens.spacing
    // 与记录页同一套合并规则：同一天同一地区同一单位的多条记录是一个区间，不是两行。
    val groups = remember(records) { records.groupByPrice() }
    // 色板顺序是「按色相拉开」挑的：energy（浅棕）与 carbs（琥珀）相邻时几乎分不出，
    // 所以三条线以上一定先走 carbs → fiber（青绿）→ organicAcid（紫），把这三种最容易混的放在最前面。
    val seriesColors = remember(tokens) {
        listOf(
            tokens.chart.carbs,
            tokens.chart.fiber,
            tokens.chart.organicAcid,
            tokens.chart.protein,
            tokens.chart.fat,
            tokens.chart.alcohol,
            tokens.chart.other,
        )
    }
    val series = remember(records, seriesColors) {
        records.historySeries().mapIndexed { index, item ->
            PriceChartSeries(
                label = item.label,
                color = seriesColors[index % seriesColors.size],
                points = item.points.map { point ->
                    PriceChartPoint(
                        recordedAt = point.recordedAt,
                        low = point.low,
                        high = point.high,
                        sampleCount = point.sampleCount,
                        regions = point.regions,
                    )
                },
            )
        }
    }

    BaseScreen(
        title = stringResource(R.string.price_history_title, itemName),
        onBack = onBack,
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = spacing.page, vertical = spacing.s3),
            verticalArrangement = Arrangement.spacedBy(spacing.s4),
        ) {
            AppSectionCard(
                iconRes = R.drawable.ic_chart,
                title = stringResource(R.string.price_history_chart_section),
            ) {
                if (series.isEmpty()) {
                    PriceEmptyState(
                        iconRes = R.drawable.ic_chart,
                        title = stringResource(R.string.price_history_empty),
                        description = stringResource(R.string.price_history_empty_hint),
                    )
                } else {
                    PriceHistoryChart(
                        series = series,
                        contentDescription = stringResource(
                            R.string.price_history_chart_desc,
                            series.size,
                            series.joinToString(", ") { it.label },
                        ),
                        modifier = Modifier.padding(
                            start = spacing.s3,
                            end = spacing.s3,
                            top = spacing.s2,
                        ),
                    )
                    // 说明画法 + 手势：折线取的是下限、竖线才是范围，不说清会让人以为图表少画了一半数据。
                    Text(
                        text = stringResource(R.string.price_history_chart_hint),
                        style = tokens.type.caption,
                        color = colors.textMuted,
                        modifier = Modifier.padding(
                            start = spacing.s3,
                            end = spacing.s3,
                            top = spacing.s2,
                            bottom = spacing.s3,
                        ),
                    )
                }
            }

            AppSectionCard(
                iconRes = R.drawable.ic_list,
                title = stringResource(R.string.price_history_records_section),
                // 吃掉剩余高度：图不动、明细在卡片内部滚，翻多久都还看得见图。
                modifier = Modifier.weight(1f),
                action = if (groups.isEmpty()) {
                    null
                } else {
                    {
                        Text(
                            text = stringResource(
                                R.string.price_history_records_count,
                                groups.size,
                                maxOf(totalRowCount, groups.size),
                            ),
                            style = tokens.type.bodyS,
                            color = colors.textMuted,
                            modifier = Modifier.padding(end = spacing.s2),
                        )
                    }
                },
                contentPadding = PaddingValues(bottom = spacing.s2),
            ) {
                if (groups.isEmpty()) {
                    PriceEmptyState(
                        iconRes = R.drawable.ic_search_empty,
                        title = stringResource(R.string.price_history_empty),
                        description = stringResource(R.string.price_history_empty_hint),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(bottom = spacing.s2),
                    ) {
                        itemsIndexed(groups, key = { _, group -> group.key }) { index, group ->
                            if (index > 0) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = spacing.s3),
                                    thickness = tokens.radii.highlight,
                                    color = colors.border,
                                )
                            }
                            PriceHistoryRow(group)
                        }
                    }
                }
            }
        }
    }
}

/** 一条历史记录：左边日期 + 地区（+ 备注），右边价格区间与单位。 */
@Composable
private fun PriceHistoryRow(group: PriceGroup) {
    val tokens = LocalAppTokens.current
    val colors = tokens.colors
    val spacing = tokens.spacing
    val meta = buildList {
        add(group.regionDisplay.ifBlank { stringResource(R.string.price_region_unset) })
        if (group.count > 1) add(stringResource(R.string.price_group_count, group.count))
    }.joinToString(" · ")

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
                text = formatRecordTimestamp(group.recordedAt, RecordTimePrecision.DATE),
                style = tokens.type.body,
                color = colors.text,
                maxLines = 1,
            )
            Text(
                text = meta,
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
        Spacer(Modifier.width(spacing.s3))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = group.priceRangeText,
                style = tokens.type.numeric,
                color = colors.text,
                maxLines = 1,
            )
            Text(
                text = group.priceUnit + "/" + group.sellUnit,
                style = tokens.type.caption,
                color = colors.textMuted,
                maxLines = 1,
            )
        }
    }
}

package com.woshiwangnima.healthdietpro.ui.price

import androidx.annotation.DrawableRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppFilterActionButton
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppSectionCard
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppSplitCardBody
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppSplitCardHead
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
    /**
     * 当前 tab 的 id（`statistics` / `records`）。
     *
     * 状态放在**路由层**而不是本页：从统计页点进【物品历史】再返回时，本页会先离开组合树，
     * `rememberSaveable` 存的是「进程重建」用的状态，救不回这种情况——用户会莫名其妙掉回【记录】页。
     */
    selectedTabId: String,
    onSelectTab: (String) -> Unit,
    onSelectItem: (String?) -> Unit,
    onToggleRegion: (String) -> Unit,
    onClearRegions: () -> Unit,
    onSelectYear: (Int?) -> Unit,
    onSelectMonth: (Int?) -> Unit,
    onClearFilters: () -> Unit,
    /** 点统计行进【物品历史】；参数是物品名，页面内部再归一化成物品键。 */
    onOpenItemHistory: (String) -> Unit,
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
    val selectedTab = tabs.indexOfFirst { it.id == selectedTabId }.coerceAtLeast(0)
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
                        onAdd = onAdd,
                        onEdit = onEdit,
                        onDeleteRequest = { pendingDelete = it },
                    )
                } else {
                    PriceStatisticsTab(
                        uiState = uiState,
                        onSelectItem = onSelectItem,
                        onToggleRegion = onToggleRegion,
                        onClearRegions = onClearRegions,
                        onSelectYear = onSelectYear,
                        onSelectMonth = onSelectMonth,
                        onClearFilters = onClearFilters,
                        onOpenItemHistory = onOpenItemHistory,
                    )
                }
            }
            DetailTabBar(items = tabs, selectedId = tabs[selectedTab].id) { item ->
                onSelectTab(item.id)
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
 * 这一页**只有主动作和流水**：筛选（地区 / 年份 / 月份 / 搜索）全部在【统计】页，因为那些条件
 * 是为「看汇总时收窄范围」服务的。记录页一旦也能筛，用户切回来看到条数变少，就会以为记录丢了。
 * 所以这里恒读 [PriceUiState.allGroups]，与统计页的筛选互不影响。
 */
@Composable
private fun PriceRecordsTab(
    uiState: PriceUiState,
    onAdd: () -> Unit,
    onEdit: (PriceRecord) -> Unit,
    onDeleteRequest: (PriceGroup) -> Unit,
) {
    val tokens = LocalAppTokens.current
    val colors = tokens.colors
    val spacing = tokens.spacing
    // 卡片是「合并后的区间」，不是单条记录：同条件多条合成一行（见 PriceGroup）。
    val groups = uiState.allGroups

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
            if (uiState.records.isEmpty()) {
                PriceEmptyState(
                    title = stringResource(R.string.price_empty),
                    description = stringResource(R.string.price_empty_hint),
                )
            } else {
                groups.forEachIndexed { index, group ->
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
 * 【统计】页：**只按物品聚合**的价格区间，筛选控件全部在这页。
 *
 * 筛选（搜索 / 年份 / 月份 / 地区）是为「看汇总时收窄范围」服务的，所以它们跟着汇总走，
 * 不留在【记录】页——那边是流水账，筛了只会让人以为记录丢了（见 [PriceRecordsTab]）。
 *
 * 同一个物品下可能出现多个单位（元/斤、元/个），**每个单位各占一行，全部列出**：
 * 不同售卖单位的价格不可比，合成一个区间只会得到一个不存在的数（详见 `PriceUnitRange` 的注释）；
 * 也不折叠成「另有 N 种单位」——单位数正是这里要回答的问题的一半，藏起来等于没答。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PriceStatisticsTab(
    uiState: PriceUiState,
    onSelectItem: (String?) -> Unit,
    onToggleRegion: (String) -> Unit,
    onClearRegions: () -> Unit,
    onSelectYear: (Int?) -> Unit,
    onSelectMonth: (Int?) -> Unit,
    onClearFilters: () -> Unit,
    onOpenItemHistory: (String) -> Unit,
) {
    val tokens = LocalAppTokens.current
    val colors = tokens.colors
    val spacing = tokens.spacing
    val stats = uiState.visibleStats

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        // 列表自己不加 item 间距、也不留顶部 contentPadding：筛选卡被切成「卡头（悬浮）+ 条件（滚动）」
        // 两片，只有间距为 0 它们才接成一张卡；而 contentPadding 的那条留白不裁内容，悬浮片吸顶后
        // 会从上方那条缝里漏出正在滚动的卡片（同一套处理见【记睡眠】-【统计】）。
        contentPadding = PaddingValues(
            start = spacing.page,
            end = spacing.page,
            bottom = spacing.s6,
        ),
    ) {
        // 没有记录时不摆一个筛不出东西的筛选项（§6.2）；但还留着筛选条件时保留卡片，
        // 否则用户看不到也清不掉它。
        if (uiState.records.isNotEmpty() || uiState.hasFilters) {
            // 【清除筛选】写在卡头里、紧贴卡标题下，并随列表吸顶：想把条件退回去，
            // 往往是滚过一屏统计行之后才想起来的事，埋在卡尾就得先滑回顶部（§1.3）。
            stickyHeader(key = "filter_head") {
                PriceFilterCardHead(
                    canClear = uiState.hasFilters,
                    onClear = onClearFilters,
                )
            }
            item(key = "filter_body") {
                PriceFilterGrid(
                    uiState = uiState,
                    onSelectItem = onSelectItem,
                    onToggleRegion = onToggleRegion,
                    onClearRegions = onClearRegions,
                    onSelectYear = onSelectYear,
                    onSelectMonth = onSelectMonth,
                )
            }
        }

        item(key = "stats") {
            AppSectionCard(
                modifier = Modifier.padding(top = spacing.s4),
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
                when {
                    uiState.records.isEmpty() -> PriceEmptyState(
                        iconRes = R.drawable.ic_chart,
                        title = stringResource(R.string.price_stats_empty),
                        description = stringResource(R.string.price_stats_empty_hint),
                    )

                    // 有记录但筛完什么都没有：说清楚是筛掉的，并留一个出口（§6.2「无结果」）。
                    stats.isEmpty() -> {
                        PriceEmptyState(
                            iconRes = R.drawable.ic_search_empty,
                            title = stringResource(R.string.price_filter_empty),
                            description = stringResource(R.string.price_filter_empty_hint),
                        )
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            TextButton(onClick = onClearFilters) {
                                Text(stringResource(R.string.price_filter_clear))
                            }
                        }
                    }

                    else -> stats.forEachIndexed { index, item ->
                        if (index > 0) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = spacing.s3),
                                thickness = tokens.radii.highlight,
                                color = colors.border,
                            )
                        }
                        PriceStatsRow(
                            item = item,
                            onClick = { onOpenItemHistory(item.itemName) },
                        )
                    }
                }
            }
        }

        // 说明统计口径，避免用户把「不同单位分行」误读成数据缺失。
        if (stats.isNotEmpty()) {
            item(key = "stats_hint") {
                Text(
                    text = stringResource(R.string.price_stats_hint),
                    style = tokens.type.caption,
                    color = colors.textMuted,
                    modifier = Modifier.padding(
                        start = spacing.s1,
                        end = spacing.s1,
                        top = spacing.s4,
                    ),
                )
            }
        }
    }
}

/**
 * 筛选卡的**上半片**：卡标题 + 【清除筛选】，由 LazyColumn 的 stickyHeader 承载。
 *
 * 只有列表直属的条目能吸顶，卡里的内容是随整张卡一起滚的，所以为了让【清除筛选】一直留在手边，
 * 整张筛选卡必须切成「头 + 体」两片：两片共用同一套卡面（只是各自负责两个圆角），
 * 列表里又不加 item 间距，静止时看上去仍是一张卡。
 *
 * 外层那圈是**页面底色**而不是透明：吸顶时它压在统计卡上，留白处不能漏出滚动的内容。
 *
 * 这里**没有**【开始查找】：四个格子改成点到即筛之后，「填完还得再点一次」只剩误导——
 * 和多选地区这种逐步收窄的用法尤其合不来，选中第一个地区就该看到列表收窄。
 */
@Composable
private fun PriceFilterCardHead(canClear: Boolean, onClear: () -> Unit) {
    AppSplitCardHead(
        iconRes = R.drawable.ic_search_empty,
        title = stringResource(R.string.price_filter_section),
    ) {
        // 占满卡宽：这一个区域里唯一要做的动作就是「把条件退回去」，没理由只给它一半宽。
        AppFilterActionButton(
            text = stringResource(R.string.price_filter_clear),
            iconRes = R.drawable.ic_refresh,
            enabled = canClear,
            onClick = onClear,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * 筛选卡的**下半片**：**两行四格**——商品 / 地区、年份 / 月份，每格都是下拉，跟着列表滚。
 *
 * 上一版是「一个搜索框 + 三行横滚 chip」。搜索框只能按关键词包含匹配，用户永远拿不到「只看苹果」
 * 这个精确条件；三行 chip 又把卡片撑到半个屏幕，年份一多就要横向划。改成下拉后：
 *
 * - 【商品】的选项就是记录里真实出现过的物品名（不是全量食材库），展开后第一行是搜索框——
 *   物品一多，靠划列表找比打字慢；选项里置顶「全部」。
 * - 【地区】保留多选（下拉里打勾，点空白收起），因为「北京 + 上海比一比」是这页的真实用法；
 *   置顶的「全部」＝清空多选。
 * - 【年份】【月份】是单选，选中即收起。
 *
 * 四格**点到即筛**：没有草稿层、也没有【开始查找】，每改一格列表立刻跟着变
 * （理由见 [PriceUiState.selectedItem]）。每一格都自带「全部」，清单项不用另外找入口。
 */
@Composable
private fun PriceFilterGrid(
    uiState: PriceUiState,
    onSelectItem: (String?) -> Unit,
    onToggleRegion: (String) -> Unit,
    onClearRegions: () -> Unit,
    onSelectYear: (Int?) -> Unit,
    onSelectMonth: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalAppTokens.current
    val spacing = tokens.spacing
    val all = stringResource(R.string.price_filter_all)
    // 地区是多选，格子里放不下三个以上的地名：两个以上收成「已选 N 个」。
    val regionSummary = when (uiState.selectedRegions.size) {
        0 -> all
        1 -> uiState.selectedRegions.first()
        else -> stringResource(R.string.price_filter_region_count, uiState.selectedRegions.size)
    }
    AppSplitCardBody(modifier = modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.s2)) {
            PriceFilterDropdown(
                label = stringResource(R.string.price_filter_item),
                valueText = uiState.selectedItem ?: all,
                active = uiState.selectedItem != null,
                pinned = PriceDropdownOption(
                    id = "__all",
                    text = all,
                    selected = uiState.selectedItem == null,
                    onClick = { onSelectItem(null) },
                ),
                options = uiState.itemOptions.map { name ->
                    PriceDropdownOption(
                        id = name,
                        text = name,
                        selected = uiState.selectedItem == name,
                        onClick = { onSelectItem(name) },
                    )
                },
                searchHint = stringResource(R.string.price_search_hint),
                searchEmptyText = stringResource(R.string.price_filter_item_empty),
                modifier = Modifier.weight(1f),
            )
            PriceFilterDropdown(
                label = stringResource(R.string.price_region),
                valueText = regionSummary,
                active = uiState.selectedRegions.isNotEmpty(),
                // 地区是**多选**，「全部」不能只靠「没选中任何一项」隐含表达：选了京沪时也要有一眼可见的出口。
                pinned = PriceDropdownOption(
                    id = "__all",
                    text = all,
                    selected = uiState.selectedRegions.isEmpty(),
                    onClick = onClearRegions,
                ),
                options = uiState.regionOptions.map { region ->
                    PriceDropdownOption(
                        id = region,
                        text = region,
                        selected = region in uiState.selectedRegions,
                        onClick = { onToggleRegion(region) },
                    )
                },
                multiSelect = true,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(spacing.s2))
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.s2)) {
            // 年份：只列记录里真实出现过的年份，避免给出一个永远筛不出东西的选项。
            PriceFilterDropdown(
                label = stringResource(R.string.price_filter_year),
                valueText = uiState.selectedYear?.toString() ?: all,
                active = uiState.selectedYear != null,
                pinned = PriceDropdownOption(
                    id = "__all",
                    text = all,
                    selected = uiState.selectedYear == null,
                    onClick = { onSelectYear(null) },
                ),
                options = uiState.yearOptions.map { year ->
                    PriceDropdownOption(
                        id = year.toString(),
                        text = year.toString(),
                        selected = uiState.selectedYear == year,
                        onClick = { onSelectYear(year) },
                    )
                },
                modifier = Modifier.weight(1f),
            )
            PriceFilterDropdown(
                label = stringResource(R.string.price_filter_month),
                valueText = uiState.selectedMonth
                    ?.let { stringResource(R.string.price_filter_month_option, it) }
                    ?: all,
                active = uiState.selectedMonth != null,
                pinned = PriceDropdownOption(
                    id = "__all",
                    text = all,
                    selected = uiState.selectedMonth == null,
                    onClick = { onSelectMonth(null) },
                ),
                options = (1..12).map { month ->
                    PriceDropdownOption(
                        id = month.toString(),
                        text = stringResource(R.string.price_filter_month_option, month),
                        selected = uiState.selectedMonth == month,
                        onClick = { onSelectMonth(month) },
                    )
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * 统计行：左列物品名 + 「N 个地区 · N 条记录」，右列**按单位逐行**显示区间。
 *
 * 单位不折叠：不同售卖单位的价格本来就不可比，用户看这一行就是要知道「这物品有哪几种卖法、各自什么价」，
 * 藏掉后面几种等于只答一半。单位多时行自然变高，比给一个「另有 N 种」让人再点一次划算。
 *
 * 整行可点，进【物品历史】看这个物品的逐条记录与走势；右端箭头是这条路由的唯一提示，
 * 不加就没人会去点一张看起来只是数据的行（§4.6 可点区域要有可视提示）。
 */
@Composable
private fun PriceStatsRow(
    item: PriceItemStats,
    onClick: () -> Unit,
) {
    val tokens = LocalAppTokens.current
    val colors = tokens.colors
    val spacing = tokens.spacing
    val meta = buildList {
        if (item.regionCount > 0) add(stringResource(R.string.price_stats_regions, item.regionCount))
        add(stringResource(R.string.price_stats_records, item.recordCount))
    }.joinToString(" · ")

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
            item.ranges.forEach { range ->
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = range.rangeText,
                        style = tokens.type.numeric,
                        color = colors.text,
                        maxLines = 1,
                    )
                    Spacer(Modifier.width(spacing.s2))
                    Text(
                        text = range.unitText,
                        style = tokens.type.caption,
                        color = colors.textMuted,
                        maxLines = 1,
                    )
                }
            }
        }
        Spacer(Modifier.width(spacing.s1))
        Icon(
            painter = painterResource(R.drawable.ic_arrow_right),
            contentDescription = null,
            modifier = Modifier.size(spacing.s5),
            tint = colors.textMuted,
        )
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
internal fun PriceEmptyState(
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

package com.woshiwangnima.healthdietpro.ui.nutrition

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen
import com.woshiwangnima.healthdietpro.common.ui.FoodImageStore
import com.woshiwangnima.healthdietpro.common.ui.ImageVariant
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppBottomSheet
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppChipSelectedAlpha
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppEmptyState
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppFilterChip
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppSearchField
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppSectionGroup
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppSheetHeight
import com.woshiwangnima.healthdietpro.common.ui.designsystem.LocalAppTokens
import com.woshiwangnima.healthdietpro.model.food.FoodItem
import com.woshiwangnima.healthdietpro.model.food.FoodKind
import com.woshiwangnima.healthdietpro.model.food.NutrientMeta
import com.woshiwangnima.healthdietpro.model.food.ResolvedNutrition

/**
 * 【食材 / 食物 / 菜肴对比】。
 *
 * 上一版是「标题 → 四段控件 → 搜索框 → 150 dp 候选表 → 结果表」：候选表和结果表都是
 * `AppDataTable`，窄屏上三列各 96 dp 起，表头里的食物名先被省略；结果区还要再滚一层；
 * 两边的数值只靠「更高就加粗」，既分不出哪边是谁，也读不出差多少。
 *
 * 这一版按移动端的信息节奏重排：
 *
 * 1. **对决卡在最上面**：左格是当前食物，右格是空的「选择对比对象」（虚线框 + 加号），中间 VS。
 *    对比对象是谁、现在比的是哪两个，从一进页面就不用猜。
 * 2. **选择对比对象进底部抽屉**：候选列表从页面里搬走，页面只留对比结果。抽屉里首行是搜索框、
 *    第二行是分类胶囊，下面是 64 dp 的行（缩略图 + 名称 + 分类 · 千卡），选中项打勾。
 *    这也是空间上唯一合理的解法——把几十项候选挤在页面上半屏，结果区就只剩一条缝。
 * 3. **结论卡给三个数**：谁更高、几项相同。刻意只写「更高」不写「更好」：钠、能量这些项
 *    低才更好，把「高」染成褒义会直接误导。
 * 4. **营养行改为双柱**：名称与单位一行，下面「左值 · 双向柱 · 右值」。两柱从中心向两侧生长，
 *    满量程取两侧较大值，所以较短那一柱的长度就是真实比例；两侧数值都用等宽数字右/左对齐。
 *    颜色只是辅助（蓝 / 琥珀，红绿色觉障碍下仍可分），每行都写了两个数值，去掉颜色也读得懂。
 */
@Composable
internal fun FoodComparisonScreen(
    food: FoodItem,
    foods: List<FoodItem>,
    nutrientMetas: List<NutrientMeta>,
    imageStore: FoodImageStore,
    resolve: (FoodItem) -> ResolvedNutrition,
    onBack: () -> Unit,
) {
    val tokens = LocalAppTokens.current
    val spacing = tokens.spacing
    val language = LocalConfiguration.current.locales[0]?.language ?: "en"
    // 两侧都能换：左边是「基准项」，右边是「对比对象」。两边都挂在 food.id 上——
    // 从 A 的对比页返回再进 B 的对比页，不该继承上一次选的人。
    // 用 rememberSaveable：切深色模式、转屏都会重建 Activity，好不容易选好的对象不该当场丢掉。
    var leftId by rememberSaveable(food.id) { mutableStateOf(food.id) }
    var opponentId by rememberSaveable(food.id) { mutableStateOf<String?>(null) }
    var pickingSide by remember(food.id) { mutableStateOf<CompareSide?>(null) }
    val left = remember(foods, leftId, food) { foods.firstOrNull { it.id == leftId } ?: food }
    val opponent = remember(foods, opponentId) { foods.firstOrNull { it.id == opponentId } }
    val sections = remember(left.id, opponent?.id, nutrientMetas) {
        buildCompareSections(left, opponent, nutrientMetas, resolve, language)
    }
    val summary = remember(sections) { compareSummary(sections) }
    // 有些菜肴只登记了配料、没有营养素表，解析出来是空表：这时整页「—」和 0/0/0 会让人以为功能坏了。
    // 两侧分开判断：缺数据的是哪一边，空态要说清楚。
    val leftHasData = sections.any { section -> section.rows.any { it.left != null } }
    val rightHasData = sections.any { section -> section.rows.any { it.right != null } }
    val leftName = remember(left.id, language) { left.displayName(language) }
    val rightName = opponent?.displayName(language).orEmpty()

    BaseScreen(
        // 换了基准项可能连类别一起换（食材 → 菜肴），标题跟着走，别让页面还说「食材对比」。
        title = stringResource(left.kind.comparisonTitleRes()),
        onBack = onBack,
        includeStatusBarPadding = false,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(
                start = spacing.page,
                end = spacing.page,
                top = spacing.s3,
                bottom = spacing.s6,
            ),
            verticalArrangement = Arrangement.spacedBy(spacing.s4),
        ) {
            item(key = "matchup") {
                CompareMatchupCard(
                    left = left,
                    opponent = opponent,
                    language = language,
                    imageStore = imageStore,
                    resolve = resolve,
                    onPickLeft = { pickingSide = CompareSide.LEFT },
                    onPickRight = { pickingSide = CompareSide.RIGHT },
                )
            }
            if (opponent == null) {
                item(key = "empty") {
                    // 还没挑对手时不留半屏黑：说明「下一步做什么」，并给出同一个入口（§6.2）。
                    AppSectionGroup {
                        AppEmptyState(
                            title = stringResource(R.string.nutrition_compare_empty_title),
                            description = stringResource(R.string.nutrition_compare_empty_desc),
                            iconRes = R.drawable.ic_vs,
                        )
                    }
                }
            } else if (!leftHasData || !rightHasData) {
                item(key = "no_data") {
                    val missingName = if (!leftHasData) leftName else rightName
                    AppSectionGroup {
                        AppEmptyState(
                            title = stringResource(R.string.nutrition_compare_no_data_title, missingName),
                            description = if (!leftHasData) {
                                stringResource(R.string.nutrition_compare_no_data_self_desc)
                            } else {
                                stringResource(R.string.nutrition_compare_no_data_desc)
                            },
                            iconRes = R.drawable.ic_nutrients,
                        )
                    }
                }
            } else {
                item(key = "verdict") {
                    CompareVerdictCard(
                        summary = summary,
                        leftName = leftName,
                        rightName = rightName,
                        leftColor = tokens.colors.primary,
                        rightColor = tokens.chart.carbs,
                    )
                }
                sections.forEach { section ->
                    item(key = "group_${section.group.name}") {
                        CompareGroupCard(section = section, leftName = leftName, rightName = rightName)
                    }
                }
            }
        }
    }

    pickingSide?.let { side ->
        CompareTargetSheet(
            titleRes = if (side == CompareSide.LEFT) {
                R.string.nutrition_compare_change_this
            } else {
                R.string.nutrition_compare_pick
            },
            foods = foods,
            language = language,
            selectedId = if (side == CompareSide.LEFT) left.id else opponentId,
            imageStore = imageStore,
            resolve = resolve,
            onPick = { picked ->
                pickingSide = null
                // 挑中的正好是对面那一项时**交换**两格，而不是让两侧变成同一样东西（整页「相同」）。
                // 这样「左边想换成右边那个」也不用先改右边，一次点击就完成。
                when (side) {
                    CompareSide.LEFT -> {
                        if (picked.id == opponentId) opponentId = left.id
                        leftId = picked.id
                    }
                    CompareSide.RIGHT -> {
                        if (picked.id == left.id) {
                            opponent?.let { current ->
                                leftId = current.id
                                opponentId = picked.id
                            }
                        } else {
                            opponentId = picked.id
                        }
                    }
                }
            },
            onDismiss = { pickingSide = null },
        )
    }
}

/** 从哪一格点开的选择器。两侧共用一套抽屉，只有标题、已选项和排除项不同。 */
private enum class CompareSide { LEFT, RIGHT }

/** 卡底与描边跟详情页同一套（`panelRaised` + 2 dp `border`），对比页里外都是「卡片」这一种材质。 */
@Composable
private fun CompareCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(LocalAppTokens.current.spacing.s3),
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = LocalAppTokens.current
    val shape = RoundedCornerShape(tokens.radii.container)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(tokens.colors.panelRaised)
            .border(tokens.radii.stroke, tokens.colors.border, shape)
            .padding(contentPadding),
        content = content,
    )
}

/**
 * 对决卡：左格是基准项，右格是「对比对象」。
 *
 * 两格都是整块可点（热区 ≥ 48 dp）：右格空了就挑一个、有值时点它换一个；
 * 左格同样可点——基准项也需要能换（比如想反过来看「牛奶 vs 南瓜」）。
 * 两格的「更换」提示都常驻，不靠用户猜哪边能点。
 */
@Composable
private fun CompareMatchupCard(
    left: FoodItem,
    opponent: FoodItem?,
    language: String,
    imageStore: FoodImageStore,
    resolve: (FoodItem) -> ResolvedNutrition,
    onPickLeft: () -> Unit,
    onPickRight: () -> Unit,
) {
    val tokens = LocalAppTokens.current
    val spacing = tokens.spacing
    CompareCard(contentPadding = PaddingValues(horizontal = spacing.s3, vertical = spacing.s2)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            CompareSlot(
                food = left,
                language = language,
                imageStore = imageStore,
                resolve = resolve,
                modifier = Modifier.weight(1f),
                onClick = onPickLeft,
            )
            VsBadge()
            Box(modifier = Modifier.weight(1f)) {
                if (opponent == null) {
                    CompareSlotPlaceholder(onClick = onPickRight)
                } else {
                    CompareSlot(
                        food = opponent,
                        language = language,
                        imageStore = imageStore,
                        resolve = resolve,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onPickRight,
                    )
                }
            }
        }
    }
}

@Composable
private fun CompareSlot(
    food: FoodItem,
    language: String,
    imageStore: FoodImageStore,
    resolve: (FoodItem) -> ResolvedNutrition,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val tokens = LocalAppTokens.current
    val spacing = tokens.spacing
    val shape = RoundedCornerShape(tokens.radii.label)
    val image = imageStore.rememberImage(food.imageKey(), ImageVariant.THUMB)
    val changeLabel = stringResource(R.string.nutrition_compare_change)
    Column(
        modifier = modifier
            .then(
                if (onClick != null) {
                    Modifier.clickable(role = Role.Button, onClickLabel = changeLabel, onClick = onClick)
                } else {
                    Modifier
                },
            )
            .padding(vertical = spacing.s2),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(shape)
                .border(tokens.radii.highlight, tokens.colors.border, shape),
        ) {
            FoodImage(image, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        Spacer(Modifier.height(spacing.s2))
        Text(
            text = food.displayName(language),
            style = tokens.type.body,
            color = tokens.colors.text,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(spacing.s1))
        Text(
            text = slotMeta(food, resolve),
            style = tokens.type.caption,
            color = tokens.colors.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        if (onClick != null) {
            Spacer(Modifier.height(spacing.s1))
            // 换对手这件事不靠猜：右格有值时也要明说自己还能点。
            Text(
                text = changeLabel,
                style = tokens.type.caption,
                color = tokens.colors.primary,
                maxLines = 1,
            )
        }
    }
}

/** 空对手位：虚线框 + 加号，与「已选」的实心缩略图形成对比，一眼看出这里还缺一半。 */
@Composable
private fun CompareSlotPlaceholder(onClick: () -> Unit) {
    val tokens = LocalAppTokens.current
    val spacing = tokens.spacing
    val dashed = tokens.colors.borderStrong
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = spacing.s2),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(tokens.radii.label))
                .dashedOutline(dashed, tokens.radii.label),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = tokens.colors.primary,
            )
        }
        Spacer(Modifier.height(spacing.s2))
        Text(
            text = stringResource(R.string.nutrition_compare_pick),
            style = tokens.type.body,
            color = tokens.colors.primary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(spacing.s1))
        Text(
            text = stringResource(R.string.nutrition_compare_pick_desc),
            style = tokens.type.caption,
            color = tokens.colors.textMuted,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun VsBadge() {
    val tokens = LocalAppTokens.current
    Box(
        modifier = Modifier
            .padding(top = 10.dp, start = tokens.spacing.s1, end = tokens.spacing.s1)
            .size(36.dp)
            .clip(CircleShape)
            .background(tokens.colors.panel)
            .border(tokens.radii.highlight, tokens.colors.border, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.nutrition_compare_vs),
            style = tokens.type.caption,
            fontWeight = FontWeight.Bold,
            color = tokens.colors.textMuted,
        )
    }
}

/** 结论卡：三个数字回答「差在哪」，脚注说明「差多少不代表好坏」。 */
@Composable
private fun CompareVerdictCard(
    summary: CompareSummary,
    leftName: String,
    rightName: String,
    leftColor: Color,
    rightColor: Color,
) {
    val tokens = LocalAppTokens.current
    val spacing = tokens.spacing
    CompareCard {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.nutrition_compare_verdict),
                style = tokens.type.bodyS,
                fontWeight = FontWeight.SemiBold,
                color = tokens.colors.text,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.nutrition_compare_basis),
                style = tokens.type.caption,
                color = tokens.colors.textMuted,
                maxLines = 1,
            )
        }
        Spacer(Modifier.height(spacing.s3))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.s2)) {
            VerdictStat(
                count = summary.leftHigher,
                label = stringResource(R.string.nutrition_compare_side_higher, leftName),
                accent = leftColor,
                modifier = Modifier.weight(1f),
            )
            VerdictStat(
                count = summary.same,
                label = stringResource(R.string.nutrition_compare_same),
                accent = null,
                modifier = Modifier.weight(1f),
            )
            VerdictStat(
                count = summary.rightHigher,
                label = stringResource(R.string.nutrition_compare_side_higher, rightName),
                accent = rightColor,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(spacing.s3))
        Text(
            text = stringResource(R.string.nutrition_compare_verdict_note, summary.comparable),
            style = tokens.type.caption,
            color = tokens.colors.textMuted,
        )
    }
}

@Composable
private fun VerdictStat(
    count: Int,
    label: String,
    accent: Color?,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalAppTokens.current
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = count.toString(),
            style = tokens.type.titleS,
            color = tokens.colors.text,
            maxLines = 1,
        )
        Spacer(Modifier.height(tokens.spacing.s1))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (accent != null) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(accent),
                )
                Spacer(Modifier.width(tokens.spacing.s1))
            }
            Text(
                text = label,
                style = tokens.type.caption,
                color = tokens.colors.textMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** 一组营养素一张卡：组名 `bodyS` 而非 `titleS`——7 张卡都按 20 sp 起标题，会和页面标题抢层级。 */
@Composable
private fun CompareGroupCard(section: CompareSection, leftName: String, rightName: String) {
    val tokens = LocalAppTokens.current
    val spacing = tokens.spacing
    CompareCard(contentPadding = PaddingValues(bottom = spacing.s1)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = spacing.s3, end = spacing.s3, top = spacing.s3, bottom = spacing.s2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(section.group.titleRes),
                style = tokens.type.bodyS,
                fontWeight = FontWeight.SemiBold,
                color = tokens.colors.text,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.nutrition_compare_group_count, section.rows.size),
                style = tokens.type.caption,
                color = tokens.colors.textMuted,
                maxLines = 1,
            )
        }
        section.rows.forEachIndexed { index, row ->
            if (index > 0) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = spacing.s3),
                    thickness = tokens.radii.highlight,
                    color = tokens.colors.border,
                )
            }
            CompareNutrientRow(row = row, leftName = leftName, rightName = rightName)
        }
    }
}

@Composable
private fun CompareNutrientRow(row: CompareRow, leftName: String, rightName: String) {
    val tokens = LocalAppTokens.current
    val spacing = tokens.spacing
    // 数值列只放数字：单位已经在行首右侧写过一次，再跟在每个数后面只是噪声，还挤窄了本就不宽的两列。
    val leftText = row.left?.let(::formatCompareValue).orEmpty()
    val rightText = row.right?.let(::formatCompareValue).orEmpty()
    val emptyText = stringResource(R.string.nutrition_metric_no_data)
    // 读屏一次读完一行：名称、两侧的「谁 · 多少 · 单位」，免得在双柱里迷路。
    val description = stringResource(
        R.string.nutrition_compare_row_desc,
        row.name,
        leftName,
        "${leftText.ifEmpty { emptyText }} ${row.unit}",
        rightName,
        "${rightText.ifEmpty { emptyText }} ${row.unit}",
    )
    val valueStyle = tokens.type.numeric.copy(fontSize = 14.sp, lineHeight = 20.sp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.s3, vertical = spacing.s2)
            .heightIn(min = 56.dp)
            .semantics(mergeDescendants = true) { contentDescription = description },
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = row.name,
                style = tokens.type.bodyS,
                color = tokens.colors.text,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(spacing.s2))
            Text(
                text = row.unit,
                style = tokens.type.caption,
                color = tokens.colors.textMuted,
                maxLines = 1,
            )
        }
        Spacer(Modifier.height(spacing.s1))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = leftText.ifEmpty { emptyText },
                style = valueStyle,
                color = if (row.left == null) tokens.colors.textMuted else tokens.colors.text,
                modifier = Modifier.width(64.dp),
                maxLines = 1,
                textAlign = TextAlign.End,
            )
            CompareDuelBar(
                row = row,
                leftColor = tokens.colors.primary,
                rightColor = tokens.chart.carbs,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = spacing.s2),
            )
            Text(
                text = rightText.ifEmpty { emptyText },
                style = valueStyle,
                color = if (row.right == null) tokens.colors.textMuted else tokens.colors.text,
                modifier = Modifier.width(64.dp),
                maxLines = 1,
                textAlign = TextAlign.Start,
            )
        }
    }
}

/**
 * 双向柱：以中缝为零点，左柱向左、右柱向右，长度 = 本侧值 / 两侧较大值。
 *
 * 满量程取两侧的较大值而不是各自拉满：两柱都满格时，读者读到的「一样多」是假的。
 * 值为 0 或缺失的那一侧不画柱，中缝照画，保证 49 行的中缝是一条直线。
 *
 * 只有一侧有数据（不可比）时整条柱区留空：早先在这里画过一条贯穿的基线，
 * 结果 20 多行氨基酸/维生素全是灰色长线，既像分隔线又像「两侧相等」，反而要多读一眼。
 * 「—」已经把「这边没有」说清楚了，柱区不必再表态。
 */
@Composable
private fun CompareDuelBar(
    row: CompareRow,
    leftColor: Color,
    rightColor: Color,
    modifier: Modifier = Modifier,
) {
    val divider = LocalAppTokens.current.colors.border
    val max = row.maxValue
    Canvas(modifier = modifier.height(8.dp)) {
        val mid = size.width / 2f
        val gap = 2.dp.toPx()
        val halfSpan = (mid - gap).coerceAtLeast(0f)
        val radius = CornerRadius(size.height / 2f, size.height / 2f)
        if (!row.comparable) {
            // 只有一侧有值时没有「比例」可言：不画柱，也不画基线。
            return@Canvas
        }
        // 值为 0 不画柱：留一个「最小长度」的圆点会被读成「有一点点」，而它其实是 0。
        row.left?.takeIf { it > 0.0 }?.let { value ->
            val length = (halfSpan * (value / max).toFloat()).coerceAtLeast(size.height * 0.5f)
            drawRoundRect(
                color = leftColor,
                topLeft = Offset(mid - gap - length, 0f),
                size = Size(length, size.height),
                cornerRadius = radius,
            )
        }
        row.right?.takeIf { it > 0.0 }?.let { value ->
            val length = (halfSpan * (value / max).toFloat()).coerceAtLeast(size.height * 0.5f)
            drawRoundRect(
                color = rightColor,
                topLeft = Offset(mid + gap, 0f),
                size = Size(length, size.height),
                cornerRadius = radius,
            )
        }
        drawLine(
            color = divider,
            start = Offset(mid, 0f),
            end = Offset(mid, size.height),
            strokeWidth = 1.dp.toPx(),
        )
    }
}

/**
 * 选择对比对象的底部抽屉。
 *
 * 候选是整库（几十到上百项），所以：首行固定搜索框、第二行分类胶囊、下面才是列表；
 * 抽屉传 `contentScrollable = false`，把滚动交给内部的 `LazyColumn`。
 */
@Composable
private fun CompareTargetSheet(
    @StringRes titleRes: Int,
    foods: List<FoodItem>,
    language: String,
    selectedId: String?,
    imageStore: FoodImageStore,
    resolve: (FoodItem) -> ResolvedNutrition,
    onPick: (FoodItem) -> Unit,
    onDismiss: () -> Unit,
) {
    val tokens = LocalAppTokens.current
    val spacing = tokens.spacing
    var query by remember { mutableStateOf("") }
    var kindFilter by remember { mutableStateOf<FoodKind?>(null) }
    // 两侧都列全库（含本侧当前项，用来打勾与置顶）：选到对面那一项时由调用方交换两格。
    val candidates = remember(foods, query, kindFilter) {
        val token = query.trim()
        foods.filter { candidate ->
            (kindFilter == null || candidate.kind == kindFilter) &&
                (token.isEmpty() || candidate.searchableNames().any { it.contains(token, ignoreCase = true) })
        }
    }

    // 重开时把已选中的那项滚到顶部：候选几十项，否则用户要先划一遍才知道现在比的是谁。
    val selectedIndex = remember(candidates, selectedId) {
        candidates.indexOfFirst { it.id == selectedId }.coerceAtLeast(0)
    }
    val listState = remember { LazyListState(firstVisibleItemIndex = selectedIndex, firstVisibleItemScrollOffset = 0) }
    var userNarrowed by remember { mutableStateOf(false) }
    LaunchedEffect(query, kindFilter) {
        // 首次组合不打断初始定位；之后每改一次条件回到顶部，否则会停在一个与结果无关的偏移上。
        if (userNarrowed) listState.scrollToItem(0) else userNarrowed = true
    }

    AppBottomSheet(
        title = stringResource(titleRes),
        onDismiss = onDismiss,
        initialSnap = AppSheetHeight.Full,
        contentScrollable = false,
    ) {
        Column(modifier = Modifier.fillMaxWidth().weight(1f)) {
            AppSearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = stringResource(R.string.nutrition_compare_search),
            )
            Spacer(Modifier.height(spacing.s2))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(spacing.s2),
            ) {
                AppFilterChip(
                    text = stringResource(R.string.nutrition_compare_all),
                    selected = kindFilter == null,
                    onClick = { kindFilter = null },
                )
                listOf(FoodKind.INGREDIENT, FoodKind.FOOD, FoodKind.DISH).forEach { kind ->
                    AppFilterChip(
                        text = stringResource(kind.compareKindRes()),
                        selected = kindFilter == kind,
                        onClick = { kindFilter = kind },
                    )
                }
            }
            Spacer(Modifier.height(spacing.s2))
            HorizontalDivider(thickness = tokens.radii.highlight, color = tokens.colors.border)
            if (candidates.isEmpty()) {
                AppEmptyState(
                    title = stringResource(R.string.nutrition_compare_no_match),
                    description = stringResource(R.string.nutrition_compare_no_match_desc),
                    iconRes = R.drawable.ic_search_empty,
                )
            } else {
                LazyColumn(state = listState, modifier = Modifier.fillMaxWidth().weight(1f)) {
                    items(candidates, key = { it.id }) { candidate ->
                        CompareTargetRow(
                            food = candidate,
                            language = language,
                            selected = candidate.id == selectedId,
                            imageStore = imageStore,
                            resolve = resolve,
                            onClick = { onPick(candidate) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompareTargetRow(
    food: FoodItem,
    language: String,
    selected: Boolean,
    imageStore: FoodImageStore,
    resolve: (FoodItem) -> ResolvedNutrition,
    onClick: () -> Unit,
) {
    val tokens = LocalAppTokens.current
    val spacing = tokens.spacing
    val shape = RoundedCornerShape(tokens.radii.label)
    val image = imageStore.rememberImage(food.imageKey(), ImageVariant.THUMB)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (selected) tokens.colors.primary.copy(alpha = AppChipSelectedAlpha) else Color.Transparent,
            )
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 64.dp)
            .padding(horizontal = spacing.s3, vertical = spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(shape)
                .border(tokens.radii.highlight, tokens.colors.border, shape),
        ) {
            FoodImage(image, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
        Spacer(Modifier.width(spacing.s3))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = food.displayName(language),
                style = tokens.type.body,
                color = tokens.colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = slotMeta(food, resolve),
                style = tokens.type.caption,
                color = tokens.colors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (selected) {
            Spacer(Modifier.width(spacing.s2))
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = stringResource(R.string.nutrition_compare_selected),
                modifier = Modifier.size(20.dp),
                tint = tokens.colors.primary,
            )
        }
    }
}

/**
 * 「分类 · 千卡」：对比列表里最需要一眼比的两个量就在这一行，省掉再点进详情。
 * 解析不出任何营养素的条目直接写「暂无营养数据」——否则用户挑中它，下一页只有一整屏「—」。
 */
@Composable
private fun slotMeta(food: FoodItem, resolve: (FoodItem) -> ResolvedNutrition): String {
    val kindLabel = stringResource(food.kind.compareKindRes())
    val resolved = remember(food.id) { runCatching { resolve(food) }.getOrNull() }
    val energy = resolved?.energyKilocalories()
    return when {
        resolved == null || resolved.nutrients.isEmpty() -> stringResource(
            R.string.nutrition_compare_slot_meta_text,
            kindLabel,
            stringResource(R.string.nutrition_compare_no_data_short),
        )
        energy != null -> stringResource(R.string.nutrition_compare_slot_meta, kindLabel, energy)
        else -> kindLabel
    }
}

@StringRes
private fun FoodKind.compareKindRes(): Int = when (this) {
    FoodKind.INGREDIENT -> R.string.nutrition_kind_ingredient
    FoodKind.FOOD -> R.string.nutrition_kind_food
    FoodKind.DISH -> R.string.nutrition_kind_dish
}

/** 虚线描边：用 `drawBehind` 画一圈 `PathEffect` 虚线，虚线框本身不承载任何点击。 */
private fun Modifier.dashedOutline(color: Color, radius: Dp): Modifier = drawBehind {
    val strokeWidth = 1.dp.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f),
        size = Size(size.width - strokeWidth, size.height - strokeWidth),
        cornerRadius = CornerRadius(radius.toPx(), radius.toPx()),
        style = Stroke(
            width = strokeWidth,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()), 0f),
        ),
    )
}

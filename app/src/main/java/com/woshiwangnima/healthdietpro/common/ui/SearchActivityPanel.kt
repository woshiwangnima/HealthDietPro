package com.woshiwangnima.healthdietpro.common.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppPillIconTextButton
import com.woshiwangnima.healthdietpro.common.ui.designsystem.LocalAppTokens

internal data class RecentSearchItem(val id: String, val title: String, @param:DrawableRes val iconRes: Int)

/** 面板最大高度（通用规范 §7.2：下拉最大高度 320 px，超过后滚动）。 */
internal val SearchPanelMaxHeight = 320.dp

/**
 * 搜索面板：搜索框聚焦且没有输入时，给出「搜索历史」与「最近打开」，替用户省掉一次输入。
 *
 * 结构按通用规范 §5.2「超长时只滚动内容区，标题与操作固定」：
 * - **出现时机**：调用方按「聚焦且输入为空」判定，所以光标一落到搜索框，面板与顶部那句说明就淡入；
 *   输入框一有内容，面板连同说明一起消失。
 * - **说明与收起键固定在边界**：说明在最上、收起键在最下（48 dp 行、带文字），只有中间的历史/最近滚动。
 * - **内容区自己滚动**：高度超过 [maxHeight] 时只有中间这段滚，头栏永远在边界上。
 * - **浮层而非占位**：调用方把它叠在页面之上（`Box` + `align`），面板展开不再把下方的功能网格 / 分类栏
 *   顶下去——下拉建议属于 §5.5.5 浮层气泡，非阻断、点外部关闭，不该改变页面布局。
 * - 历史与最近排成 chip 流式换行，不再用「小列表 + 假滚动条 + 上下滑动提示」；
 *   清空是破坏性操作，先弹二次确认，默认「取消」。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SearchActivityPanel(
    history: List<String>,
    recentItems: List<RecentSearchItem>,
    historyTitle: String,
    recentsTitle: String,
    onSelectHistory: (String) -> Unit,
    onRemoveHistory: (String) -> Unit,
    onClearHistory: () -> Unit,
    onOpenRecent: (RecentSearchItem) -> Unit,
    onRemoveRecent: (RecentSearchItem) -> Unit,
    onClearRecents: () -> Unit,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier,
    maxHeight: Dp = SearchPanelMaxHeight,
) {
    val tokens = LocalAppTokens.current
    val colors = tokens.colors
    val shape = RoundedCornerShape(tokens.radii.container)
    val scrollState = rememberScrollState()
    var clearing by remember { mutableStateOf<ClearTarget?>(null) }
    var hintVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { hintVisible = true }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = maxHeight)
            // 浮层要比它盖住的卡片更亮一档（页面卡片是 panel），再加 ≤8 dp 柔影，才读得出「浮在上面」（§2.7）。
            .shadow(elevation = 8.dp, shape = shape)
            .background(colors.panelRaised, shape)
            .border(tokens.radii.highlight, colors.border, shape),
    ) {
        // 说明文字：面板出现（= 光标落进搜索框）时淡入，排版位置提前占好，避免面板高度跳动。
        // 输入框一有内容，面板本身就隐藏了（调用方按「聚焦且为空」判定），这句随之消失。
        val hintAlpha by animateFloatAsState(
            targetValue = if (hintVisible) 1f else 0f,
            animationSpec = tween(durationMillis = 180),
            label = "search-hint-alpha",
        )
        Text(
            text = stringResource(R.string.search_activity_hint),
            style = tokens.type.caption,
            color = colors.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = tokens.spacing.s3, end = tokens.spacing.s3, top = tokens.spacing.s2)
                .alpha(hintAlpha),
        )

        // 只让中间这段滚：上方说明、下方收起键都停在面板边界上（§5.2）。
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(scrollState)
                .padding(horizontal = tokens.spacing.s3, vertical = tokens.spacing.s2),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.s1),
        ) {
            if (history.isEmpty() && recentItems.isEmpty()) {
                // 面板现在「聚焦即出现」，所以必须交代一句为什么是空的，而不是给一张空卡。
                Text(
                    text = stringResource(R.string.search_activity_empty),
                    style = tokens.type.bodyS,
                    color = colors.textMuted,
                    modifier = Modifier.padding(vertical = tokens.spacing.s2),
                )
            }
            if (history.isNotEmpty()) {
                SearchSectionHeader(title = historyTitle, onClear = { clearing = ClearTarget.History })
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(bottom = tokens.spacing.s1),
                    horizontalArrangement = Arrangement.spacedBy(tokens.spacing.s2),
                    verticalArrangement = Arrangement.spacedBy(tokens.spacing.s2),
                ) {
                    history.take(MAX_HISTORY_ENTRIES).forEach { entry ->
                        SearchTag(
                            title = entry,
                            onClick = { onSelectHistory(entry) },
                            onRemove = { onRemoveHistory(entry) },
                        )
                    }
                }
            }
            if (recentItems.isNotEmpty()) {
                SearchSectionHeader(title = recentsTitle, onClear = { clearing = ClearTarget.Recents })
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(bottom = tokens.spacing.s1),
                    horizontalArrangement = Arrangement.spacedBy(tokens.spacing.s2),
                    verticalArrangement = Arrangement.spacedBy(tokens.spacing.s2),
                ) {
                    recentItems.take(MAX_RECENT_ENTRIES).forEach { item ->
                        SearchTag(
                            title = item.title,
                            iconRes = item.iconRes,
                            onClick = { onOpenRecent(item) },
                            onRemove = { onRemoveRecent(item) },
                        )
                    }
                }
            }
        }

        HorizontalDivider(
            color = colors.border,
            thickness = tokens.radii.highlight,
            modifier = Modifier.padding(horizontal = tokens.spacing.s3),
        )
        // 收起键固定在面板下边界、居中，是「图标 + 文字」的描边胶囊；条目再多也停在边界上点得到。
        // 之前它是一个整行的透明文字 + 小箭头，读起来像说明文字而不是可点的东西。
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            AppPillIconTextButton(
                text = stringResource(R.string.search_activity_collapse),
                imageVector = Icons.Filled.KeyboardArrowUp,
                onClick = onCollapse,
            )
        }
    }

    clearing?.let { target ->
        val title = if (target == ClearTarget.History) historyTitle else recentsTitle
        ConfirmDialog(
            title = stringResource(R.string.search_activity_clear_title, title),
            message = stringResource(R.string.search_activity_clear_message),
            confirmText = stringResource(R.string.search_activity_clear),
            cancelText = stringResource(R.string.common_cancel),
            onConfirm = {
                if (target == ClearTarget.History) onClearHistory() else onClearRecents()
                clearing = null
            },
            onDismiss = { clearing = null },
        )
    }
}

/**
 * 面板下方的透明拦截层：点空白处收起（§5.5.5 浮层气泡「点外部关闭」）。
 *
 * 只覆盖搜索框下方的区域，点搜索框本身仍能继续输入。
 */
@Composable
internal fun SearchPanelDismissLayer(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(onDismiss) { detectTapGestures { onDismiss() } },
    )
}

private enum class ClearTarget { History, Recents }

@Composable
private fun SearchSectionHeader(title: String, onClear: () -> Unit) {
    val tokens = LocalAppTokens.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = tokens.type.bodyS,
            fontWeight = FontWeight.Medium,
            color = tokens.colors.text,
            modifier = Modifier.weight(1f),
        )
        TextButton(
            onClick = onClear,
            contentPadding = PaddingValues(horizontal = tokens.spacing.s2, vertical = 0.dp),
            modifier = Modifier.height(tokens.spacing.s6),
        ) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = tokens.colors.danger,
            )
            Spacer(Modifier.width(tokens.spacing.s1))
            Text(
                text = stringResource(R.string.search_activity_clear),
                style = tokens.type.bodyS,
                color = tokens.colors.danger,
            )
        }
    }
}

/** 一条可点选、可单独删除的搜索标签；标签本体 32 dp，胶囊圆角，删除键 28 dp。 */
@Composable
private fun SearchTag(
    title: String,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    @DrawableRes iconRes: Int? = null,
) {
    val tokens = LocalAppTokens.current
    val colors = tokens.colors
    val shape = RoundedCornerShape(tokens.radii.pill)
    Row(
        modifier = Modifier
            .widthIn(max = 220.dp)
            .height(32.dp)
            .clip(shape)
            .background(colors.panel)
            .border(tokens.radii.highlight, colors.border, shape)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (iconRes != null) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.padding(start = tokens.spacing.s2, end = tokens.spacing.s1).size(16.dp),
                tint = colors.primary,
            )
        } else {
            Spacer(Modifier.width(tokens.spacing.s3))
        }
        Text(
            text = title,
            style = tokens.type.bodyS,
            color = colors.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = stringResource(R.string.search_activity_remove),
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(tokens.radii.pill))
                .clickable(onClick = onRemove)
                .padding(6.dp),
            tint = colors.textMuted,
        )
        Spacer(Modifier.width(tokens.spacing.s1))
    }
}

private const val MAX_HISTORY_ENTRIES = 10
private const val MAX_RECENT_ENTRIES = 10

package com.woshiwangnima.healthdietpro.common.ui.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.woshiwangnima.healthdietpro.R
import kotlin.math.roundToInt

/** 下拉的一行数据：调用方决定文字、选中态与点击语义，控件只负责排版。 */
data class AppFilterOption(
    val id: String,
    val text: String,
    val selected: Boolean,
    val onClick: () -> Unit,
)

/**
 * 通用筛选格：一格 = 标签 + 当前值 + 折叠箭头，点开在下方弹出选项。
 *
 * 形态对齐【记物价】-【统计】-【查找与筛选】里的 `PriceFilterDropdown`（通用规范 §7.2）：
 * 高度压到 `hitMin`（48 dp）、标签留在格内左侧、有条件时值用主色并加亮描边，
 * 不采用 M3 `ExposedDropdownMenuBox` 的 56 dp 表单样式——两行并排会把筛选区撑成半屏。
 *
 * 与业务无关，因此能直接给别的页面复用：选项由 [options] 传入，[pinned] 可给一个钉在列表外的
 * 「全部」出口，[searchHint] 非空时展开后第一行是搜索框，[multiSelect] 打开多选（点选项不收起）。
 * 加一个筛选维度＝多放一格，不需要改控件本身。
 *
 * 自己写 [Popup] 而不是 `DropdownMenu`：M3 的实现把内容整体塞进 `verticalScroll` 的 `Column`，
 * 搜索框会跟着长列表一起滚走；这里搜索框钉在第一行、选项区 `LazyColumn` 自己滚。
 * 弹出宽度不小于 248 dp，并夹在窗口边距内；展开不改变触发格宽度（§7.2）。
 */
@Composable
fun AppFilterDropdown(
    label: String,
    valueText: String,
    active: Boolean,
    options: List<AppFilterOption>,
    modifier: Modifier = Modifier,
    /** 置顶的「全部」：永远在第一行，不随选项列表滚动。 */
    pinned: AppFilterOption? = null,
    /** 非空时展开后第一行是搜索框（选项很多才需要）。 */
    searchHint: String = "",
    /** 搜索无结果时的说明。 */
    searchEmptyText: String = "",
    /** 多选：点选项只打勾、不收起，点浮层外 / 返回键收起。 */
    multiSelect: Boolean = false,
) {
    val tokens = LocalAppTokens.current
    val colors = tokens.colors
    val spacing = tokens.spacing
    val shape = RoundedCornerShape(tokens.radii.label)
    var expanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var anchor by remember { mutableStateOf(Rect.Zero) }

    fun close() {
        expanded = false
        query = ""
    }

    Box(modifier = modifier.onGloballyPositioned { anchor = it.boundsInWindow() }) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = spacing.hitMin)
                .clip(shape)
                // 输入框底材比卡片低一层（卡片是 panelRaised），格子才读作「可填的槽」而不是又一张卡。
                .background(colors.panel)
                .border(
                    width = tokens.radii.highlight,
                    color = if (active) colors.primary else colors.border,
                    shape = shape,
                )
                .clickable(role = Role.DropdownList) { if (expanded) close() else expanded = true }
                .padding(horizontal = spacing.s3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = tokens.type.caption,
                color = colors.textMuted,
                maxLines = 1,
            )
            Spacer(Modifier.width(spacing.s2))
            Text(
                text = valueText,
                style = tokens.type.bodyS,
                // 有条件时值用主色：不靠箭头方向判断「这格筛过没有」，灰度下也分得出。
                color = if (active) colors.primary else colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(spacing.s1))
            Icon(
                painter = painterResource(R.drawable.ic_arrow_drop_down),
                contentDescription = null,
                modifier = Modifier.size(spacing.s4),
                tint = if (active) colors.primary else colors.textMuted,
            )
        }

        if (expanded && anchor.width > 0f) {
            val density = LocalDensity.current
            val configuration = LocalConfiguration.current
            val marginPx = with(density) { spacing.s2.toPx() }
            val gapPx = with(density) { spacing.s1.toPx() }
            val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
            val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
            val menuWidthPx = minOf(
                maxOf(anchor.width, with(density) { 248.dp.toPx() }),
                screenWidthPx - marginPx * 2,
            )
            val xPx = anchor.left.coerceIn(
                marginPx,
                (screenWidthPx - menuWidthPx - marginPx).coerceAtLeast(marginPx),
            )
            // 筛选区在页面顶部，正常情况下总是往下弹；最大高度按「到屏幕底还剩多少」和 §7.2 的 320 dp 取小。
            val belowPx = screenHeightPx - (anchor.bottom + gapPx) - marginPx
            val maxHeightPx = belowPx.coerceAtMost(with(density) { 320.dp.toPx() })
                .coerceAtLeast(with(density) { 160.dp.toPx() })
            val visibleOptions = if (query.isBlank()) {
                options
            } else {
                options.filter { it.text.contains(query.trim(), ignoreCase = true) }
            }

            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(
                    x = (xPx - anchor.left).roundToInt(),
                    y = (anchor.bottom + gapPx - anchor.top).roundToInt(),
                ),
                onDismissRequest = ::close,
                properties = PopupProperties(focusable = true),
            ) {
                Surface(
                    modifier = Modifier.width(with(density) { menuWidthPx.toDp() }),
                    shape = RoundedCornerShape(tokens.radii.container),
                    color = colors.panelRaised,
                    // 深色下 panelRaised 与卡片同色，只有描边能说明「这是浮在卡片上面的一层」。
                    border = BorderStroke(tokens.radii.highlight, colors.borderStrong),
                    shadowElevation = 8.dp,
                ) {
                    Column(
                        modifier = Modifier.heightIn(max = with(density) { maxHeightPx.toDp() }),
                    ) {
                        if (searchHint.isNotBlank()) {
                            AppSearchField(
                                value = query,
                                onValueChange = { query = it },
                                placeholder = searchHint,
                                modifier = Modifier.padding(spacing.s2),
                            )
                            HorizontalDivider(thickness = tokens.radii.highlight, color = colors.border)
                        }
                        if (pinned != null) {
                            AppFilterDropdownRow(
                                option = pinned,
                                onClick = {
                                    pinned.onClick()
                                    if (!multiSelect) close()
                                },
                            )
                            HorizontalDivider(thickness = tokens.radii.highlight, color = colors.border)
                        }
                        LazyColumn(
                            modifier = Modifier.weight(1f, fill = false),
                            // 重新展开时把选中项摆到顶部：选项按自然序排，选中的可能靠后。
                            state = rememberLazyListState(
                                initialFirstVisibleItemIndex = visibleOptions
                                    .indexOfFirst { it.selected }
                                    .coerceAtLeast(0),
                            ),
                        ) {
                            if (visibleOptions.isEmpty() && searchEmptyText.isNotBlank()) {
                                item(key = "__empty") {
                                    Text(
                                        text = searchEmptyText,
                                        style = tokens.type.caption,
                                        color = colors.textMuted,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = spacing.s3, vertical = spacing.s4),
                                    )
                                }
                            }
                            items(visibleOptions, key = { it.id }) { option ->
                                AppFilterDropdownRow(
                                    option = option,
                                    onClick = {
                                        option.onClick()
                                        if (!multiSelect) close()
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 一行选项：选中项打勾（§7.2「当前选中项必须有勾选标记」），文字右端留出勾位不跳。 */
@Composable
private fun AppFilterDropdownRow(
    option: AppFilterOption,
    onClick: () -> Unit,
) {
    val tokens = LocalAppTokens.current
    val colors = tokens.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = tokens.spacing.hitMin)
            .clickable(onClick = onClick)
            .padding(horizontal = tokens.spacing.s3, vertical = tokens.spacing.s1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = option.text,
            style = tokens.type.bodyS,
            color = if (option.selected) colors.primary else colors.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (option.selected) {
            Spacer(Modifier.width(tokens.spacing.s2))
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = colors.primary,
            )
        }
    }
}

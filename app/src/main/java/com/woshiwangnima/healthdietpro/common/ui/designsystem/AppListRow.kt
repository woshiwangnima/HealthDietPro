package com.woshiwangnima.healthdietpro.common.ui.designsystem

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.R

/**
 * 设置类列表行：前置图标 + 标题 [+ 副标题] + 尾值 / 尾图标 [+ 箭头]（通用规范 §2.2 热区、§2.6 信息层级）。
 *
 * 全站拉一次口径，解决此前两套并存：
 * - 旧 `SettingRow` 用 `typography.titleLarge`（24 sp）当标题，一行字比页面标题还抢眼；
 * - `我的` 页自绘的行是 `body` 16 sp + `caption` 副标题，两个页面的同一种行高矮差一截。
 *
 * 约定：标题 `body`、图标 20 dp 主色、行高 ≥ 56 dp。
 * - [tooltip]：**不恒显**的说明。行里只留标题与当前值，用户长按该行时才在行旁弹出文字提示
 *   （[AppTooltipPopup]），列表因此保持一行一项的节奏，不会因为每行带一句解释而变成两行。
 * - [showChevron] 只给「点了会进下一层」的行；[expanded] 非空时改用可旋转的下箭头，
 *   表示这一行就地展开内容（如「来源与参考」下面直接列出 DRIs / WHO）。
 * - [trailingValue] 是这一行当前的值；有值无箭头的那一行一定不切页面，而是就地改值或弹确认框。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    @DrawableRes iconRes: Int? = null,
    @DrawableRes trailingIconRes: Int? = null,
    trailingValue: String? = null,
    showChevron: Boolean = false,
    expanded: Boolean? = null,
    tooltip: String? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val tokens = LocalAppTokens.current
    val colors = tokens.colors
    val hasTooltip = !tooltip.isNullOrBlank()
    // 状态自带生命周期收尾（切页 / 进后台立即收起），行里不用再操心「回来会不会又亮一下」。
    val tooltipState = rememberAppTooltipState()
    val tooltipLabel = stringResource(R.string.common_tooltip_long_press)

    val interaction = when {
        hasTooltip -> Modifier.combinedClickable(
            enabled = enabled,
            onClickLabel = null,
            onLongClickLabel = tooltipLabel,
            onLongClick = { tooltipState.show() },
            onClick = { onClick?.invoke() },
        )
        onClick != null -> Modifier.clickable(enabled = enabled, onClick = onClick)
        else -> Modifier
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = tokens.spacing.rowMinHeight)
            .onGloballyPositioned { tooltipState.updateAnchor(it.boundsInWindow()) }
            .then(interaction)
            .padding(horizontal = tokens.spacing.s3, vertical = tokens.spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (iconRes != null) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (enabled) colors.primary else colors.disabled,
            )
            Spacer(Modifier.width(tokens.spacing.s3))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = tokens.type.body,
                color = if (enabled) colors.text else colors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrEmpty()) {
                Text(
                    text = subtitle,
                    style = tokens.type.caption,
                    color = colors.textMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (!trailingValue.isNullOrEmpty()) {
            Spacer(Modifier.width(tokens.spacing.s2))
            Text(
                text = trailingValue,
                style = tokens.type.bodyS,
                color = colors.textMuted,
                maxLines = 1,
                textAlign = TextAlign.End,
            )
        }
        if (trailingIconRes != null) {
            Spacer(Modifier.width(tokens.spacing.s2))
            Icon(
                painter = painterResource(trailingIconRes),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = colors.textMuted,
            )
        }
        if (expanded != null) {
            Spacer(Modifier.width(tokens.spacing.s2))
            val rotation by animateFloatAsState(
                targetValue = if (expanded) 180f else 0f,
                animationSpec = androidx.compose.animation.core.tween(
                    durationMillis = tokens.motion.baseMs,
                    easing = AppEaseOut,
                ),
                label = "appListRowExpandedChevron",
            )
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier
                    .size(18.dp)
                    .rotate(rotation),
                tint = colors.textMuted,
            )
        } else if (showChevron) {
            Spacer(Modifier.width(tokens.spacing.s2))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = colors.textMuted,
            )
        }
    }

    if (hasTooltip) {
        AppTooltipPopup(
            state = tooltipState,
            text = tooltip.orEmpty(),
        )
    }
}

/**
 * 单选行：整行可选、点按范围 ≥ 56 dp，选中色取主色（§2.2 / §4.5 选择控件）。
 *
 * [horizontalPadding] 默认取 `space.3`（贴在卡片里）；放进对话框等自带内容内边距的容器时传 `0.dp`，
 * 否则选项会比标题多缩一截。
 */
@Composable
fun AppRadioRow(
    title: String,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    horizontalPadding: Dp? = null,
) {
    val tokens = LocalAppTokens.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = tokens.spacing.rowMinHeight)
            .selectable(selected = selected, onClick = onSelect)
            .padding(
                horizontal = horizontalPadding ?: tokens.spacing.s3,
                vertical = tokens.spacing.s2,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = tokens.colors.primary,
                unselectedColor = tokens.colors.textMuted,
            ),
        )
        Spacer(Modifier.width(tokens.spacing.s2))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = tokens.type.body,
                color = tokens.colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrEmpty()) {
                Text(
                    text = subtitle,
                    style = tokens.type.caption,
                    color = tokens.colors.textMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * 行间分隔线，左缘与行内文字对齐（卡片左内边距 + 图标宽 + 间距）。
 *
 * 无前置图标的列表传 `leadingInset = false`，否则分隔线会从中间莫名其妙地开始。
 */
@Composable
fun AppListRowDivider(leadingInset: Boolean = true) {
    val tokens = LocalAppTokens.current
    HorizontalDivider(
        modifier = if (leadingInset) {
            Modifier.padding(start = tokens.spacing.s3 + 20.dp + tokens.spacing.s3)
        } else {
            Modifier
        },
        thickness = tokens.radii.highlight,
        color = tokens.colors.border,
    )
}

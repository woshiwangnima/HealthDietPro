package com.woshiwangnima.healthdietpro.common.ui.designsystem

import androidx.annotation.DrawableRes
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.R

/**
 * 开关行（通用规范 §4.5 开关、§2.2 热区）。
 *
 * 从软件设置的「功能启用」页里那一版抽出来：整行可点（≥ 56 dp）、右侧 `Switch` 只承担状态显示。
 * 抽出原因——睡眠编辑器的「开始计时 / 系统通知」、饮食设置的提醒、通知权限页各自手写过一遍同样的 Row，
 * 字号、行高、关闭态取色各写各的，同一类控件在不同页高矮差一截。
 *
 * - 关闭态文字转 `textMuted`、图标转 `disabled`，但**不整行变淡**：关闭后仍要能读清是什么功能。
 * - [enabled] 为 false 时（前置条件未满足）整行不可点，且必须由调用方在同区域说明原因（§5 状态矩阵）。
 * - [tooltip]：这个开关是做什么的。默认不显示，长按整行才在行旁弹出（[AppTooltipPopup]）；
 *   开关行本来就在列表里密集排列，每行多一行说明会让相邻两行的高低差一眼可见。
 *   [subtitle] 只留给「当前状态」这类必须随时可见的信息，不用它装解释文案。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    tooltip: String? = null,
    @DrawableRes iconRes: Int? = null,
    enabled: Boolean = true,
) {
    val tokens = LocalAppTokens.current
    val hasTooltip = !tooltip.isNullOrBlank()
    val tooltipState = rememberAppTooltipState()
    val tooltipLabel = stringResource(R.string.common_tooltip_long_press)
    val interaction = if (hasTooltip) {
        Modifier.combinedClickable(
            enabled = enabled,
            onLongClickLabel = tooltipLabel,
            onLongClick = { tooltipState.show() },
            onClick = { onCheckedChange(!checked) },
        )
    } else {
        Modifier.clickable(enabled = enabled) { onCheckedChange(!checked) }
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
                tint = when {
                    !enabled -> tokens.colors.disabled
                    checked -> tokens.colors.primary
                    else -> tokens.colors.disabled
                },
            )
            Spacer(Modifier.width(tokens.spacing.s3))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = tokens.type.body,
                color = when {
                    !enabled -> tokens.colors.disabled
                    checked -> tokens.colors.text
                    else -> tokens.colors.textMuted
                },
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
        Spacer(Modifier.width(tokens.spacing.s2))
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }

    if (hasTooltip) {
        AppTooltipPopup(state = tooltipState, text = tooltip.orEmpty())
    }
}

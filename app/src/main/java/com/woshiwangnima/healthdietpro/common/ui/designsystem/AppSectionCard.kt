package com.woshiwangnima.healthdietpro.common.ui.designsystem

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.woshiwangnima.healthdietpro.R

/**
 * 无标题的区块卡：只有底材与描边，用来把几行同类条目括成一组。
 *
 * 页面的分组标题已经写在页头、或这组只有两三行不值得再起标题时用它，
 * 避免为了套 [AppSectionCard] 硬编一个「设置」当卡标题。
 */
@Composable
fun AppSectionGroup(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(vertical = LocalAppTokens.current.spacing.s1),
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = LocalAppTokens.current
    val shape = RoundedCornerShape(tokens.radii.container)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(tokens.colors.panelRaised)
            .border(tokens.radii.highlight, tokens.colors.border, shape)
            .padding(contentPadding),
        content = content,
    )
}

/**
 * 区块卡：标题在卡内，内容随卡（通用规范 §2.4「卡片标题」、§2.7-1 底材分层）。
 *
 * 默认落在 `panelRaised`（对纯黑底 2 级明度差）。父容器本身已是 `panelRaised` 时
 * （例如模态浮层内部），把 [containerColor] 换成 `panel` 才能保持「底 → 面板 → 浮起」的方向。
 *
 * [tooltip]：卡内内容需要一句统一解释时用它，**不要**在标题下再加一行常显的 `caption`——
 * 那句解释会把卡标题与第一行内容隔开，读者会以为它是第一行的副标题。长按卡头的图标与标题区
 * 才弹出（[AppTooltipPopup]），卡内右侧的 [action] 按钮不受影响。
 */
@Composable
fun AppSectionCard(
    @DrawableRes iconRes: Int,
    title: String,
    modifier: Modifier = Modifier,
    containerColor: Color = LocalAppTokens.current.colors.panelRaised,
    tooltip: String? = null,
    action: (@Composable () -> Unit)? = null,
    contentPadding: PaddingValues? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = LocalAppTokens.current
    val shape = RoundedCornerShape(tokens.radii.container)
    val hasTooltip = !tooltip.isNullOrBlank()
    val tooltipState = rememberAppTooltipState()
    val tooltipLabel = stringResource(R.string.common_tooltip_long_press)
    val innerPadding = contentPadding ?: PaddingValues(
        start = tokens.spacing.s3,
        end = tokens.spacing.s3,
        bottom = tokens.spacing.s3,
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(containerColor)
            .border(tokens.radii.stroke, tokens.colors.border, shape),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { tooltipState.updateAnchor(it.boundsInWindow()) }
                // 卡头没有点击行为，长按只负责「念一遍这张卡是干什么的」。
                // 用原始手势而不是 combinedClickable：后者会给卡头加一层假的可点击涟漪与点击语义。
                .pointerInput(hasTooltip) {
                    if (hasTooltip) {
                        detectTapGestures(onLongPress = { tooltipState.show() })
                    }
                }
                .semantics {
                    if (hasTooltip) onLongClick(label = tooltipLabel) { tooltipState.show(); true }
                }
                .padding(
                    start = tokens.spacing.s3,
                    end = tokens.spacing.s1,
                    top = tokens.spacing.s3,
                    bottom = tokens.spacing.s2,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(tokens.spacing.s5),
                tint = tokens.colors.primary,
            )
            Spacer(Modifier.width(tokens.spacing.s2))
            Text(
                text = title,
                style = tokens.type.titleS,
                color = tokens.colors.text,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            action?.invoke()
        }
        Column(modifier = Modifier.fillMaxWidth().padding(innerPadding), content = content)
    }

    if (hasTooltip) {
        AppTooltipPopup(state = tooltipState, text = tooltip.orEmpty())
    }
}

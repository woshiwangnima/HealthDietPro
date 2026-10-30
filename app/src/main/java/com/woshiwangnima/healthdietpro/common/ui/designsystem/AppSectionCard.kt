package com.woshiwangnima.healthdietpro.common.ui.designsystem

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow

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
 */
@Composable
fun AppSectionCard(
    @DrawableRes iconRes: Int,
    title: String,
    modifier: Modifier = Modifier,
    containerColor: Color = LocalAppTokens.current.colors.panelRaised,
    action: (@Composable () -> Unit)? = null,
    contentPadding: PaddingValues? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val tokens = LocalAppTokens.current
    val shape = RoundedCornerShape(tokens.radii.container)
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
}

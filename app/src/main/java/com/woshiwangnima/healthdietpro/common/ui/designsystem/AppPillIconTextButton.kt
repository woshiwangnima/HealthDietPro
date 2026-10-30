package com.woshiwangnima.healthdietpro.common.ui.designsystem

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * 「图标 + 文字」描边胶囊按钮（通用规范 §2.2 热区、§5.1 次要按钮）。
 *
 * 用在页面角落与浮层边界的**低频入口**：右上角「设置」、搜索面板底部「收起」。
 * 这类入口原先都是纯图标按钮，图标本身就是唯一线索——图标猜不中就等于没有入口。
 *
 * - 视觉高 32 dp、`radii.pill`、1 dp 主色描边（透明度 [AppPillBorderAlpha]，只做「这是按钮」的提示，
 *   不跟标题抢注意力），图标 16 dp，文字 `bodyS`，两者同为主色。
 * - 热区另算：外层 `heightIn(min = 48 dp)` 且整块可点，胶囊只负责画，所以点按范围永远够（§2.2）；
 *   页面角落那种 48 dp 行里塞 32 dp 胶囊的位置，视觉不撑高、触摸不吃亏。
 * - 文字单行省略，避免长译文把胶囊顶到两行。
 */
@Composable
fun AppPillIconTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes iconRes: Int? = null,
    imageVector: ImageVector? = null,
    enabled: Boolean = true,
    containerColor: Color = Color.Transparent,
    contentColor: Color = LocalAppTokens.current.colors.primary,
) {
    val tokens = LocalAppTokens.current
    val shape = RoundedCornerShape(tokens.radii.pill)
    val tint = if (enabled) contentColor else tokens.colors.disabled

    Box(
        modifier = modifier
            .widthIn(min = tokens.spacing.hitMin)
            .heightIn(min = tokens.spacing.hitMin)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .height(tokens.spacing.s6)
                .clip(shape)
                .background(containerColor)
                .border(
                    width = tokens.radii.highlight,
                    color = if (enabled) contentColor.copy(alpha = AppPillBorderAlpha) else tokens.colors.border,
                    shape = shape,
                )
                .padding(horizontal = tokens.spacing.s3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when {
                iconRes != null -> Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = tint,
                )
                imageVector != null -> Icon(
                    imageVector = imageVector,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = tint,
                )
            }
            if (iconRes != null || imageVector != null) {
                Spacer(Modifier.width(tokens.spacing.s1))
            }
            Text(
                text = text,
                style = tokens.type.bodyS,
                color = tint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** 描边胶囊的边框透明度：主色压到 45%，与「切换用户」胶囊同一档，全站描边按钮才读成一套。 */
const val AppPillBorderAlpha = 0.45f

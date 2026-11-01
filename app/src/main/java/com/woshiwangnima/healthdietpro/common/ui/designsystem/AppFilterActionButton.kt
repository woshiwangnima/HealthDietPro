package com.woshiwangnima.healthdietpro.common.ui.designsystem

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow

/**
 * 筛选卡里的动作按钮（【开始查找】/【清除筛选】）：图标 + 文字 + 1 dp 描边。
 *
 * 从【记物价】-【统计】-【查找与筛选】抽出来的公共件，睡眠统计顶部筛选卡复用同一套尺寸，
 * 两处并排时不会一个高一个矮。
 *
 * 禁用态用 `textMuted` + `border` 而不是 M3 默认的 `onSurface` 38%：那层灰在深色卡片上几乎看不见，
 * 读起来像「按钮没画完」，分不清「暂时不能点」和「这里坏了」。文字再加一道删除线——灰字 + 灰框
 * 在浅色下仍像一个「画淡了的按钮」，删除线才是明确说「此刻按不动」（用户反馈）。
 * 删除线写进 `style` 的 copy 而不是 `Text` 的 `textDecoration` 参数：按钮内容会套上一层
 * `LocalTextStyle`，样式里的装饰在合并后稳定生效。
 *
 * 不禁用 `enabled` 也不用重色块：一个是「没改动」不该让按钮显得像主操作，另一个是重色块会盖过
 * 真正要看清的筛选条件（§1.3 一屏只有一个主操作）。
 */
@Composable
fun AppFilterActionButton(
    text: String,
    @DrawableRes iconRes: Int,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalAppTokens.current
    val colors = tokens.colors
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = tokens.spacing.hitMin),
        shape = RoundedCornerShape(tokens.radii.label),
        border = BorderStroke(
            width = tokens.radii.highlight,
            color = if (enabled) colors.primary else colors.border,
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = colors.primary,
            disabledContentColor = colors.textMuted,
        ),
        contentPadding = PaddingValues(horizontal = tokens.spacing.s2),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(tokens.spacing.s4),
        )
        Spacer(Modifier.width(tokens.spacing.s1))
        Text(
            text = text,
            style = if (enabled) {
                tokens.type.bodyS
            } else {
                tokens.type.bodyS.copy(textDecoration = TextDecoration.LineThrough)
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

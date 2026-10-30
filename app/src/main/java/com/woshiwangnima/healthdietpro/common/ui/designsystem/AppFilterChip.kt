package com.woshiwangnima.healthdietpro.common.ui.designsystem

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * 筛选胶囊（通用规范 §4.5 选择控件、§2.2 热区）。
 *
 * 全站列表的「全部 / 夜间睡眠 / 小憩」这类互斥过滤都用它，替代各屏自己配的
 * `MaterialTheme.material3.FilterChip` —— 后者会带 M3 默认 32 dp 高、`labelLarge` 与 tonal 选中底，
 * 与卡片的 4 dp 圆角语言、与 `AppPillIconTextButton` 的描边胶囊都不是一套。
 *
 * - 视觉高 32 dp（与 `AppPillIconTextButton` 同高，横排时基线一致），`radii.pill`，1 dp 描边。
 * - 选中：主色 12% 底 + 主色描边 + 主色文字；未选中：透明底 + `border` 描边 + `textMuted` 文字。
 * - 热区另算：外层 `heightIn(min = 48 dp)` 且整块可点（§2.2），胶囊只负责画，横排不撑高列表头。
 * - 选中态同时改描边与文字，不只靠底色——灰度下也能分出选中的是哪一个（§11.1）。
 */
@Composable
fun AppFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes iconRes: Int? = null,
    enabled: Boolean = true,
) {
    val tokens = LocalAppTokens.current
    val shape = RoundedCornerShape(tokens.radii.pill)
    val contentColor = when {
        !enabled -> tokens.colors.disabled
        selected -> tokens.colors.primary
        else -> tokens.colors.textMuted
    }
    val borderColor = when {
        !enabled -> tokens.colors.border
        selected -> tokens.colors.primary
        else -> tokens.colors.border
    }
    val containerColor = if (selected) tokens.colors.primary.copy(alpha = AppChipSelectedAlpha) else androidx.compose.ui.graphics.Color.Transparent

    Box(
        modifier = modifier
            .heightIn(min = tokens.spacing.hitMin)
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .height(tokens.spacing.s6)
                .clip(shape)
                .background(containerColor)
                .border(tokens.radii.highlight, borderColor, shape)
                .padding(horizontal = tokens.spacing.s3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (iconRes != null) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = contentColor,
                )
                Spacer(Modifier.width(tokens.spacing.s1))
            }
            Text(
                text = text,
                style = tokens.type.bodyS,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** 选中底的主色透明度：12% 在纯黑与白底上都能看出「这块是选中的」，又不至于盖过文字。 */
const val AppChipSelectedAlpha = 0.12f

package com.woshiwangnima.healthdietpro.common.ui.designsystem

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign

/**
 * 空状态（通用规范 §6.2）：图标 + 一句结论 + 一句怎么办，可选一个恢复操作。
 *
 * 只做「说明 + 出口」，不叠底色块——外层已在卡片里时再套一层灰底会把「底 → 面板 → 浮起」读乱。
 */
@Composable
fun AppEmptyState(
    title: String,
    description: String,
    @DrawableRes iconRes: Int,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    val tokens = LocalAppTokens.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = tokens.spacing.s3, vertical = tokens.spacing.s5),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.s2),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(tokens.spacing.s6),
            tint = tokens.colors.disabled,
        )
        Text(
            text = title,
            style = tokens.type.body,
            color = tokens.colors.text,
            textAlign = TextAlign.Center,
        )
        Text(
            text = description,
            style = tokens.type.caption,
            color = tokens.colors.textMuted,
            textAlign = TextAlign.Center,
        )
        action?.invoke()
    }
}

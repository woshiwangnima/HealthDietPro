package com.woshiwangnima.healthdietpro.common.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.ui.designsystem.LocalAppTokens

@Composable
internal fun AppIconTextButton(
    text: String,
    @DrawableRes iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
    ) {
        AppButtonIcon(iconRes)
        TextOverflowText(text = text, style = LocalTextStyle.current)
    }
}

/**
 * 描边按钮（基类形态：透明底 + 1 dp 描边）。
 *
 * [danger]：这一下点了会丢东西（删除、放弃修改）时改成 danger 色。**只在真正不可逆的动作上用**——
 * 描边 + 红字已经是这一屏里最刺眼的一块，若同一处还并列着别的红，
 * 读者就得先猜哪个才是「点了就回不去」的那个（§11.1 颜色不是唯一线索：这里仍保留图标与文案）。
 */
@Composable
internal fun AppOutlinedIconTextButton(
    text: String,
    @DrawableRes iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    danger: Boolean = false,
) {
    val tokens = LocalAppTokens.current
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(tokens.radii.label),
        border = BorderStroke(
            width = tokens.radii.highlight,
            color = when {
                !enabled -> tokens.colors.border
                danger -> tokens.colors.danger
                else -> tokens.colors.primary
            },
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = if (danger) tokens.colors.danger else tokens.colors.primary,
            disabledContentColor = tokens.colors.textMuted,
        ),
    ) {
        AppButtonIcon(iconRes)
        TextOverflowText(text = text, style = LocalTextStyle.current)
    }
}

@Composable
internal fun AppTextIconButton(
    text: String,
    @DrawableRes iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
    ) {
        AppButtonIcon(iconRes)
        Text(text)
    }
}

@Composable
internal fun AppDestructiveTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    iconSize: Dp = 18.dp,
    textStyle: TextStyle = LocalTextStyle.current,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(
            contentColor = androidx.compose.material3.MaterialTheme.colorScheme.error,
        ),
    ) {
        AppButtonIcon(R.drawable.ic_delete, iconSize)
        TextOverflowText(
            text = text,
            modifier = Modifier.weight(1f),
            style = textStyle,
        )
    }
}

@Composable
fun AppDataTableDeleteAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    iconSize: Dp = 18.dp,
    textStyle: TextStyle = LocalTextStyle.current,
) {
    AppDestructiveTextButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        iconSize = iconSize,
        textStyle = textStyle,
    )
}

@Composable
private fun AppButtonIcon(
    @DrawableRes iconRes: Int,
    iconSize: Dp = 18.dp,
) {
    Icon(
        painter = painterResource(iconRes),
        contentDescription = null,
        modifier = Modifier.size(iconSize),
    )
    Spacer(Modifier.width(8.dp))
}

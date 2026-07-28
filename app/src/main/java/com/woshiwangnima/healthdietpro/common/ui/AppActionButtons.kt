package com.woshiwangnima.healthdietpro.common.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
        Text(text)
    }
}

@Composable
internal fun AppOutlinedIconTextButton(
    text: String,
    @DrawableRes iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
    ) {
        AppButtonIcon(iconRes)
        Text(text)
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
        Text(text, style = textStyle)
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

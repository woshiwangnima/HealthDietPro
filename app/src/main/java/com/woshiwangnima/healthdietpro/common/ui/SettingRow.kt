package com.woshiwangnima.healthdietpro.common.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

@Composable
internal fun SettingRow(
    title: String,
    subtitle: String,
    leadingIconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingValue: String = "",
    trailingValueColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    clickable: Boolean = true,
) {
    val iconSize = with(LocalDensity.current) {
        MaterialTheme.typography.titleLarge.fontSize.toDp() + 6.dp
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (clickable) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(leadingIconRes),
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            tint = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.width(12.dp))
        SettingTextContent(
            title = title,
            subtitle = subtitle,
            modifier = Modifier.weight(1f),
        )
        if (trailingValue.isNotEmpty()) {
            Text(
                text = trailingValue,
                style = MaterialTheme.typography.bodyMedium,
                color = trailingValueColor,
                modifier = Modifier.padding(end = if (clickable) 4.dp else 0.dp),
            )
        }
        if (clickable) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(iconSize),
            )
        }
    }
}

@Composable
internal fun SettingTextContent(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    val alphaMid = rememberFontStyleAlphaMid()
    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (subtitle.isNotEmpty()) {
            Text(
                text = subtitle,
                style = TextStyle(fontSize = FontTokens.caption),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.alpha(alphaMid),
            )
        }
    }
}

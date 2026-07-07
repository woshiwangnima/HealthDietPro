package com.woshiwangnima.healthdietpro.common.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
    clickable: Boolean = true,
) {
    val alphaMid = rememberFontStyleAlphaMid()
    val iconSize = with(LocalDensity.current) {
        MaterialTheme.typography.titleLarge.fontSize.toDp()
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
            modifier = Modifier
                .size(iconSize)
                .padding(end = 12.dp),
            tint = MaterialTheme.colorScheme.onSurface,
        )
        Column(modifier = Modifier.weight(1f)) {
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
        if (trailingValue.isNotEmpty()) {
            Text(
                text = trailingValue,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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

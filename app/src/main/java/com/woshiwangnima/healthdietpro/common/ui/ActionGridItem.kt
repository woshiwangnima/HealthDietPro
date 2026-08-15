package com.woshiwangnima.healthdietpro.common.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun ActionGridItem(
    title: String,
    @DrawableRes iconRes: Int,
    enabled: Boolean,
    summary: String,
    onClick: () -> Unit,
    onAddClick: (() -> Unit)? = null,
    showSummary: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val alpha = if (enabled) 1f else 0.45f
    Surface(modifier = modifier.fillMaxWidth().alpha(alpha).then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier), shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = title,
                    modifier = Modifier.size(30.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                TextOverflowText(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                    maxLines = 1,
                )
                onAddClick?.let { onAdd ->
                    IconButton(onClick = onAdd, modifier = Modifier.size(30.dp)) {
                        Icon(
                            painter = painterResource(com.woshiwangnima.healthdietpro.R.drawable.ic_add),
                            contentDescription = stringResource(com.woshiwangnima.healthdietpro.R.string.record_add_action, title),
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            TextOverflowText(text = if (showSummary) summary else "", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth(), maxLines = 1)
        }
    }
}

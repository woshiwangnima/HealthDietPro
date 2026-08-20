package com.woshiwangnima.healthdietpro.common.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** 线性进度条基类；percentText 非空时在条中间居中常驻显示。 */
@Composable
internal fun LinearProgressWithPercent(
    progress: () -> Float,
    color: Color,
    trackColor: Color,
    modifier: Modifier = Modifier,
    percentText: String? = null,
    barHeight: Dp = 8.dp,
) {
    if (percentText == null) {
        LinearProgressIndicator(
            progress = progress,
            color = color,
            trackColor = trackColor,
            modifier = modifier.fillMaxWidth().height(barHeight),
        )
        return
    }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        LinearProgressIndicator(
            progress = progress,
            color = color,
            trackColor = trackColor,
            modifier = Modifier.fillMaxWidth().height(barHeight),
        )
        Text(
            text = percentText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}
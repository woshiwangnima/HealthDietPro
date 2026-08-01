package com.woshiwangnima.healthdietpro.common.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Horizontal track and thumb matching the search-history scroll indicator. */
@Composable
internal fun HorizontalScrollIndicator(state: ScrollState, modifier: Modifier = Modifier) {
    val progress by remember(state) {
        derivedStateOf { if (state.maxValue == 0) 0f else state.value.toFloat() / state.maxValue }
    }
    val visibleFraction by remember(state) {
        derivedStateOf {
            if (state.maxValue == 0) 1f else (1f - state.maxValue.toFloat() / (state.maxValue + 360f)).coerceIn(0.16f, 0.85f)
        }
    }
    BoxWithConstraints(
        modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp).height(4.dp)
            .background(MaterialTheme.colorScheme.outlineVariant, androidx.compose.foundation.shape.RoundedCornerShape(2.dp)),
    ) {
        val thumbWidth = maxWidth * visibleFraction
        val thumbOffset = (maxWidth - thumbWidth) * progress
        androidx.compose.foundation.layout.Box(
            Modifier.width(thumbWidth).offset(x = thumbOffset).height(4.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.72f), androidx.compose.foundation.shape.RoundedCornerShape(2.dp)),
        )
    }
}

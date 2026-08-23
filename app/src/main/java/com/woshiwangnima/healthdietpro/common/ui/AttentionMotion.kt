package com.woshiwangnima.healthdietpro.common.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp

@Composable
internal fun rememberAttentionShakeOffset(active: Boolean, label: String) =
    if (active) rememberAttentionShakeDistance(label) else 0.dp

@Composable
private fun rememberAttentionShakeDistance(label: String) = run {
    val transition = rememberInfiniteTransition(label = "${label}Transition")
    val offsetDp by transition.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1400
                0f at 0
                -5f at 50
                4f at 100
                -3f at 150
                2.2f at 195
                -1.4f at 240
                0.8f at 275
                0f at 300
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "${label}Offset",
    )
    offsetDp.dp
}

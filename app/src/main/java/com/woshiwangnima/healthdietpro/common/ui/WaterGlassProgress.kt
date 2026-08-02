package com.woshiwangnima.healthdietpro.common.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/** Transparent glass with a gently oscillating liquid surface. Progress is clamped to 0..1. */
@Composable
internal fun WaterGlassProgress(
    progress: Float,
    valueLabel: String,
    supportingLabel: String,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val fill by animateFloatAsState(progress.coerceIn(0f, 1f), tween(650), label = "glassFill")
    val transition = rememberInfiniteTransition(label = "glassWave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(tween(2_600, easing = LinearEasing), RepeatMode.Restart),
        label = "glassWavePhase",
    )
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(Modifier.size(width = 156.dp, height = 206.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.matchParentSize()) {
            val inset = 12.dp.toPx()
            val corner = 20.dp.toPx()
            val glass = androidx.compose.ui.graphics.Path().apply {
                addRoundRect(androidx.compose.ui.geometry.RoundRect(inset, inset, size.width - inset, size.height - inset, corner, corner))
            }
            clipPath(glass) {
                val liquidTop = size.height - inset - (size.height - inset * 2) * fill
                val amplitude = 5.dp.toPx() * (0.45f + fill * 0.55f)
                val wave = Path().apply {
                    moveTo(inset, liquidTop)
                    val step = 3.dp.toPx()
                    var x = inset
                    while (x <= size.width - inset + step) {
                        val y = liquidTop + sin((x / (size.width - inset * 2) * PI * 2.2 + phase).toDouble()).toFloat() * amplitude
                        lineTo(x, y)
                        x += step
                    }
                    lineTo(size.width - inset, size.height - inset)
                    lineTo(inset, size.height - inset)
                    close()
                }
                drawPath(wave, scheme.primary.copy(alpha = .72f))
                drawPath(wave, scheme.primary.copy(alpha = .16f), style = Stroke(1.dp.toPx()))
            }
            drawPath(glass, scheme.outline.copy(alpha = .75f), style = Stroke(2.dp.toPx()))
            drawPath(glass, scheme.surface.copy(alpha = .10f), style = Stroke(7.dp.toPx()))
            }
            androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(valueLabel, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                Text(supportingLabel, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
        }
    }
}

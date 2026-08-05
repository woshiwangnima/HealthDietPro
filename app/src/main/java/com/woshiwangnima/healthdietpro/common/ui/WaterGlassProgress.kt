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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** Transparent glass with a gently oscillating liquid surface and fill-responsive particles. */
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
    val currentFill by rememberUpdatedState(fill)
    var particles by remember { mutableStateOf<List<WaterParticle>>(emptyList()) }
    LaunchedEffect(Unit) {
        var lastFrameNanos = 0L
        var spawnCredit = 0f
        while (true) {
            withFrameNanos { frameNanos ->
                val elapsedSeconds = if (lastFrameNanos == 0L) 0f else ((frameNanos - lastFrameNanos) / 1_000_000_000f).coerceAtMost(.05f)
                lastFrameNanos = frameNanos
                val targetCount = (3 + currentFill.coerceIn(0f, 1f) * 25).toInt()
                val advanced = particles.map { it.copy(ageSeconds = it.ageSeconds + elapsedSeconds) }
                    .filter { it.ageSeconds < it.lifetimeSeconds }
                    .toMutableList()
                spawnCredit += elapsedSeconds * (4f + currentFill.coerceIn(0f, 1f) * 8f)
                while (advanced.size < targetCount && spawnCredit >= 1f) {
                    advanced += newWaterParticle()
                    spawnCredit -= 1f
                }
                particles = advanced.take(targetCount)
            }
        }
    }
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(Modifier.size(width = 184.dp, height = 232.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.matchParentSize()) {
            val inset = 22.dp.toPx()
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
                drawWaterSurfaceParticles(
                    liquidTop = liquidTop,
                    fill = fill,
                    color = scheme.primary,
                    particles = particles,
                )
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

private fun DrawScope.drawWaterSurfaceParticles(
    liquidTop: Float,
    fill: Float,
    color: androidx.compose.ui.graphics.Color,
    particles: List<WaterParticle>,
) {
    val intensity = fill.coerceIn(0f, 1f)
    val horizontalInset = 18.dp.toPx()
    val particleBandHeight = 14.dp.toPx() + intensity * 48.dp.toPx()
    val containerBottom = size.height - 22.dp.toPx()
    particles.forEach { particle ->
        val progress = particle.ageSeconds / particle.lifetimeSeconds
        val fadeIn = (particle.ageSeconds / particle.fadeInSeconds).coerceIn(0f, 1f)
        val fadeOut = ((particle.lifetimeSeconds - particle.ageSeconds) / particle.fadeOutSeconds).coerceIn(0f, 1f)
        val baseX = horizontalInset + particle.startXRatio * (size.width - horizontalInset * 2)
        val sway = sin((particle.ageSeconds * particle.swayFrequency + particle.pathPhase).toDouble()).toFloat()
        val drift = cos((particle.ageSeconds * particle.driftFrequency + particle.pathPhase * .63f).toDouble()).toFloat()
        val x = (baseX + sway * particle.swayDp.dp.toPx() + drift * 2.dp.toPx()).coerceIn(horizontalInset, size.width - horizontalInset)
        val startY = liquidTop + 5.dp.toPx() + particle.startDepthRatio * particleBandHeight
        val y = (startY + progress * particleBandHeight * particle.fallDistanceRatio).coerceAtMost(containerBottom)
        val radius = particle.radiusDp.dp.toPx()
        val alpha = (.18f + intensity * .42f) * fadeIn * fadeOut
        drawCircle(color.copy(alpha = alpha), radius, Offset(x, y))
        if (particle.hasGlow && alpha > 0f) drawCircle(color.copy(alpha = alpha * .45f), radius * 2.1f, Offset(x, y))
    }
}

private data class WaterParticle(
    val startXRatio: Float,
    val startDepthRatio: Float,
    val fallDistanceRatio: Float,
    val swayDp: Float,
    val swayFrequency: Float,
    val driftFrequency: Float,
    val pathPhase: Float,
    val radiusDp: Float,
    val fadeInSeconds: Float,
    val fadeOutSeconds: Float,
    val lifetimeSeconds: Float,
    val hasGlow: Boolean,
    val ageSeconds: Float = 0f,
)

private fun newWaterParticle() = WaterParticle(
    startXRatio = Random.nextFloat() * .84f + .08f,
    startDepthRatio = Random.nextFloat() * .22f,
    fallDistanceRatio = Random.nextFloat() * .42f + .58f,
    swayDp = Random.nextFloat() * 6f + 2f,
    swayFrequency = Random.nextFloat() * 1.8f + 1.2f,
    driftFrequency = Random.nextFloat() * 1.4f + .8f,
    pathPhase = Random.nextFloat() * (PI * 2).toFloat(),
    radiusDp = Random.nextFloat() * 1.6f + 1.1f,
    fadeInSeconds = Random.nextFloat() * .35f + .18f,
    fadeOutSeconds = Random.nextFloat() * .55f + .3f,
    lifetimeSeconds = Random.nextFloat() * 3.2f + 1.8f,
    hasGlow = Random.nextBoolean(),
)

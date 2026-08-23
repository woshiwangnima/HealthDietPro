package com.woshiwangnima.healthdietpro.common.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

/** Circular value container with level-driven particles: negative descends, positive ascends. */
@Composable
internal fun ParticleValueOrb(
    valueLabel: String,
    supportingLabel: String,
    level: Int,
    modifier: Modifier = Modifier,
    particleColor: Color = MaterialTheme.colorScheme.primary,
) {
    val scheme = MaterialTheme.colorScheme
    val currentLevel by rememberUpdatedState(level.coerceIn(-4, 4))
    val currentColor by rememberUpdatedState(particleColor)
    val edgeMagnitude = abs(currentLevel)
    val edgeWidth = 1.dp + (edgeMagnitude * .6f).dp
    val edgeAlpha = .60f + edgeMagnitude * .10f
    var particles by remember { mutableStateOf<List<ValueOrbParticle>>(emptyList()) }
    LaunchedEffect(Unit) {
        var previousFrameNanos = 0L
        var spawnCredit = 0f
        while (true) {
            withFrameNanos { frameNanos ->
                val elapsedSeconds = if (previousFrameNanos == 0L) 0f else {
                    ((frameNanos - previousFrameNanos) / 1_000_000_000f).coerceAtMost(.05f)
                }
                previousFrameNanos = frameNanos
                val magnitude = abs(currentLevel)
                val targetCount = 5 + magnitude * 2
                val spawnRate = 2.2f + magnitude * 1.15f
                val advanced = particles
                    .map { it.copy(ageSeconds = it.ageSeconds + elapsedSeconds) }
                    .filter { it.ageSeconds < it.lifetimeSeconds }
                    .toMutableList()
                spawnCredit += elapsedSeconds * spawnRate
                while (advanced.size < targetCount && spawnCredit >= 1f) {
                    advanced += newValueOrbParticle(currentLevel)
                    spawnCredit -= 1f
                }
                particles = advanced.take(targetCount)
            }
        }
    }
    Surface(
        color = scheme.surfaceVariant.copy(alpha = .48f),
        shape = androidx.compose.foundation.shape.CircleShape,
        border = androidx.compose.foundation.BorderStroke(edgeWidth, currentColor.copy(alpha = edgeAlpha)),
        modifier = modifier.clip(androidx.compose.foundation.shape.CircleShape),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(Modifier.matchParentSize()) {
                drawCircle(particleColor.copy(alpha = .08f))
                drawValueOrbParticles(particles, currentColor)
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            ) {
                TextOverflowText(
                    text = valueLabel,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                )
                TextOverflowText(
                    text = supportingLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawValueOrbParticles(
    particles: List<ValueOrbParticle>,
    color: Color,
) {
    particles.forEach { particle ->
        val progress = particle.ageSeconds / particle.lifetimeSeconds
        val fadeIn = (particle.ageSeconds / particle.fadeInSeconds).coerceIn(0f, 1f)
        val fadeOut = ((particle.lifetimeSeconds - particle.ageSeconds) / particle.fadeOutSeconds).coerceIn(0f, 1f)
        val wobble = sin((particle.ageSeconds * particle.wobbleFrequency + particle.phase).toDouble()).toFloat()
        val x = (particle.xRatio + wobble * .035f).coerceIn(.08f, .92f) * size.width
        val y = (particle.startYRatio + particle.direction * progress * particle.travelRatio).coerceIn(.06f, .94f) * size.height
        val alpha = (.20f + abs(particle.direction) * .09f) * fadeIn * fadeOut
        val radius = particle.radiusRatio * size.minDimension
        drawCircle(color.copy(alpha = alpha), radius, androidx.compose.ui.geometry.Offset(x, y))
        if (particle.glow) drawCircle(color.copy(alpha = alpha * .35f), radius * 2.2f, androidx.compose.ui.geometry.Offset(x, y))
    }
}

private data class ValueOrbParticle(
    val xRatio: Float,
    val startYRatio: Float,
    val direction: Float,
    val travelRatio: Float,
    val wobbleFrequency: Float,
    val phase: Float,
    val radiusRatio: Float,
    val fadeInSeconds: Float,
    val fadeOutSeconds: Float,
    val lifetimeSeconds: Float,
    val glow: Boolean,
    val ageSeconds: Float = 0f,
)

private fun newValueOrbParticle(level: Int): ValueOrbParticle {
    val magnitude = abs(level)
    val direction = when {
        level < 0 -> 1f
        level > 0 -> -1f
        else -> 0f
    }
    val lifetime = if (level < 0) {
        2.25f - magnitude * .28f + Random.nextFloat() * .35f
    } else {
        2.15f - magnitude * .16f + Random.nextFloat() * .55f
    }.coerceAtLeast(.65f)
    return ValueOrbParticle(
        xRatio = Random.nextFloat() * .78f + .11f,
        startYRatio = when {
            level < 0 -> Random.nextFloat() * .22f + .08f
            level > 0 -> Random.nextFloat() * .22f + .70f
            else -> Random.nextFloat() * .68f + .16f
        },
        direction = direction,
        travelRatio = if (level == 0) 0f else .38f + magnitude * .10f + Random.nextFloat() * .12f,
        wobbleFrequency = 1.2f + magnitude * .30f + Random.nextFloat() * .8f,
        phase = Random.nextFloat() * 6.283185f,
        radiusRatio = Random.nextFloat() * .020f + .013f,
        fadeInSeconds = Random.nextFloat() * .22f + .12f,
        fadeOutSeconds = if (level < 0) Random.nextFloat() * .20f + .16f else Random.nextFloat() * .35f + .24f,
        lifetimeSeconds = lifetime,
        glow = Random.nextBoolean(),
    )
}

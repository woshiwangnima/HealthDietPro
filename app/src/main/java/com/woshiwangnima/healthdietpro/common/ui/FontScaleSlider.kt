package com.woshiwangnima.healthdietpro.common.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

const val FONT_SCALE_MIN = 0.8f
const val FONT_SCALE_MAX = 1.5f
const val FONT_SCALE_STEP = 0.01f
private const val RANGE = FONT_SCALE_MAX - FONT_SCALE_MIN

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FontScaleSlider(
    currentScale: Float,
    onScaleChangeStopped: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    var progress by remember(currentScale) {
        mutableFloatStateOf(((currentScale - FONT_SCALE_MIN) / RANGE).coerceIn(0f, 1f))
    }
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val labelSp = FontTokens.label
    val visibleScale = (FONT_SCALE_MIN + progress * RANGE).toSteppedScale()
    val percentText = visibleScale.toPercentText()
    val minLabel = "${(FONT_SCALE_MIN * 100).roundToInt()}%"
    val maxLabel = "${(FONT_SCALE_MAX * 100).roundToInt()}%"

    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FontScaleStepButton(
            label = "A-",
            percent = minLabel,
            enabled = visibleScale > FONT_SCALE_MIN,
            onClick = {
                val next = (visibleScale - FONT_SCALE_STEP).toSteppedScale()
                progress = next.toProgress()
                onScaleChangeStopped(next)
            },
        )

        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            Slider(
                value = progress,
                onValueChange = { progress = it },
                onValueChangeFinished = {
                    val stepped = (FONT_SCALE_MIN + progress * RANGE).toSteppedScale()
                    progress = stepped.toProgress()
                    onScaleChangeStopped(stepped)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = Color.Transparent,
                    activeTrackColor = primary,
                    inactiveTrackColor = primary.copy(alpha = 0.24f),
                ),
                thumb = {
                    Box(
                        modifier = Modifier
                            .layout { measurable, constraints ->
                                val placeable = measurable.measure(constraints)
                                val w = placeable.width.coerceAtLeast(56.dp.roundToPx())
                                val h = placeable.height.coerceAtLeast(32.dp.roundToPx())
                                layout(w, h) {
                                    placeable.placeRelative(
                                        x = (w - placeable.width) / 2,
                                        y = (h - placeable.height) / 2,
                                    )
                                }
                            }
                            .clip(RoundedCornerShape(8.dp))
                            .background(primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = percentText,
                            style = TextStyle(
                                fontSize = labelSp,
                                fontWeight = FontWeight.Medium,
                            ),
                            color = onPrimary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                },
            )
        }

        FontScaleStepButton(
            label = "A+",
            percent = maxLabel,
            enabled = visibleScale < FONT_SCALE_MAX,
            onClick = {
                val next = (visibleScale + FONT_SCALE_STEP).toSteppedScale()
                progress = next.toProgress()
                onScaleChangeStopped(next)
            },
        )
    }
}

@Composable
private fun FontScaleStepButton(
    label: String,
    percent: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
        modifier = Modifier.widthIn(min = 44.dp, max = 54.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                style = TextStyle(fontSize = FontTokens.micro, fontWeight = FontWeight.Medium),
            )
            Text(
                text = percent,
                style = TextStyle(fontSize = FontTokens.micro),
            )
        }
    }
}

private fun Float.toSteppedScale(): Float =
    ((this / FONT_SCALE_STEP).roundToInt() * FONT_SCALE_STEP)
        .coerceIn(FONT_SCALE_MIN, FONT_SCALE_MAX)

private fun Float.toProgress(): Float =
    ((this - FONT_SCALE_MIN) / RANGE).coerceIn(0f, 1f)

private fun Float.toPercentText(): String =
    "${(this * 100).roundToInt()}%"

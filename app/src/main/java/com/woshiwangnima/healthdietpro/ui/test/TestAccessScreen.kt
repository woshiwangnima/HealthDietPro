package com.woshiwangnima.healthdietpro.ui.test

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.ui.AppOutlinedIconTextButton

@Composable
internal fun TestAccessScreen(
    onCancel: () -> Unit,
    onVerify: (List<Int>) -> Boolean,
    modifier: Modifier = Modifier,
) {
    var showError by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.test_access_title),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.test_access_description),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
        )
        if (showError) {
            Text(
                text = stringResource(R.string.test_access_error),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
            )
        }
        GesturePasswordPad(
            onPatternComplete = { completedPattern ->
                onVerify(completedPattern).also { accepted ->
                    if (!accepted) showError = true
                }
            },
            modifier = Modifier
                .padding(top = 20.dp)
                .fillMaxWidth(0.78f),
        )
        AppOutlinedIconTextButton(
            text = stringResource(R.string.test_access_cancel),
            iconRes = R.drawable.ic_cancel,
            onClick = onCancel,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        )
    }
}

@Composable
private fun GesturePasswordPad(
    onPatternComplete: (List<Int>) -> Boolean,
    modifier: Modifier = Modifier,
) {
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.outline
    val padDescription = stringResource(R.string.test_access_gesture_pad)
    var pattern by remember { mutableStateOf(emptyList<Int>()) }
    var pointerPosition by remember { mutableStateOf<Offset?>(null) }
    Canvas(
        modifier = modifier
            .aspectRatio(1f)
            .semantics { contentDescription = padDescription }
            .pointerInput(Unit) {
                var currentPattern = emptyList<Int>()
                var previousPosition: Offset? = null
                detectDragGestures(
                    onDragStart = { offset ->
                        previousPosition = offset
                        pointerPosition = offset
                        gesturePointAt(offset.toGestureCoordinate(), size.width.toFloat())?.let { point ->
                            currentPattern = listOf(point)
                            pattern = currentPattern
                        }
                    },
                    onDrag = { change, _ ->
                        val currentPosition = change.position
                        pointerPosition = currentPosition
                        previousPosition?.let { previous ->
                            val updatedPattern = gesturePointsCrossed(
                                from = previous.toGestureCoordinate(),
                                to = currentPosition.toGestureCoordinate(),
                                size = size.width.toFloat(),
                                excluded = currentPattern.toSet(),
                            ).fold(currentPattern) { selected, point ->
                                normalizeGesturePattern(selected + point)
                            }
                            if (updatedPattern != currentPattern) {
                                currentPattern = updatedPattern
                                pattern = updatedPattern
                            }
                        }
                        previousPosition = currentPosition
                    },
                    onDragEnd = {
                        pointerPosition = null
                        if (!onPatternComplete(currentPattern)) pattern = emptyList()
                    },
                    onDragCancel = {
                        pointerPosition = null
                        pattern = emptyList()
                    },
                )
            },
    ) {
        val spacing = size.width / 4f
        val centers = (1..9).associateWith { point ->
            Offset(spacing * ((point - 1) % 3 + 1), spacing * ((point - 1) / 3 + 1))
        }
        pattern.zipWithNext().forEach { (from, to) ->
            drawLine(activeColor, centers.getValue(from), centers.getValue(to), 8.dp.toPx(), StrokeCap.Round)
        }
        pointerPosition?.let { pointer ->
            pattern.lastOrNull()?.let { point ->
                drawLine(activeColor, centers.getValue(point), pointer, 8.dp.toPx(), StrokeCap.Round)
            }
        }
        centers.forEach { (point, center) ->
            val selected = point in pattern
            drawCircle(if (selected) activeColor else inactiveColor, 24.dp.toPx(), center, style = Stroke(3.dp.toPx()))
            if (selected) drawCircle(activeColor, 10.dp.toPx(), center)
        }
    }
}

private fun Offset.toGestureCoordinate() = GestureCoordinate(x, y)

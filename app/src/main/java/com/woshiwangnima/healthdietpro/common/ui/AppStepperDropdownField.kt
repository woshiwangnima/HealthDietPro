package com.woshiwangnima.healthdietpro.common.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AppStepperDropdownField(
    label: String,
    value: String,
    options: List<AppDropdownOption>,
    onSelect: (AppDropdownOption) -> Unit,
    numericValue: Double,
    minimum: Double,
    maximum: Double,
    step: Double,
    onValueChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        AppDropdownField(
            label = label,
            value = value,
            options = options,
            onSelect = onSelect,
            modifier = Modifier.width(180.dp),
        )
        Column(
            modifier = Modifier.height(56.dp),
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            AppRepeatAdjustButton(
                icon = Icons.Filled.KeyboardArrowUp,
                enabled = numericValue < maximum,
                onAdjust = { onValueChange((numericValue + step).coerceAtMost(maximum)) },
            )
            AppRepeatAdjustButton(
                icon = Icons.Filled.KeyboardArrowDown,
                enabled = numericValue > minimum,
                onAdjust = { onValueChange((numericValue - step).coerceAtLeast(minimum)) },
            )
        }
    }
}

@Composable
fun AppRepeatAdjustButton(
    icon: ImageVector,
    enabled: Boolean = true,
    onAdjust: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnAdjust = rememberUpdatedState(onAdjust)
    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = modifier
            .size(24.dp)
            .then(
                if (enabled) {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                currentOnAdjust.value()
                                val repeatJob = CoroutineScope(currentCoroutineContext()).launch {
                                    delay(400)
                                    while (true) {
                                        currentOnAdjust.value()
                                        delay(80)
                                    }
                                }
                                tryAwaitRelease()
                                repeatJob.cancel()
                            },
                        )
                    }
                } else {
                    Modifier
                },
            ),
        tint = if (enabled) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
        },
    )
}

package com.woshiwangnima.healthdietpro.common.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.util.Locale
import kotlin.math.round

@Composable
fun AppNumericStepperField(
    label: String,
    value: Float,
    unit: String,
    step: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    minimum: Float = step,
    maximum: Float = Float.MAX_VALUE,
) {
    var text by remember(value) {
        mutableStateOf(String.format(Locale.getDefault(), "%.2f", value).trimEnd('0').trimEnd('.'))
    }
    Row(modifier = modifier, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        OutlinedTextField(
            value = text,
            onValueChange = { input ->
                text = input
                input.toFloatOrNull()?.let { entered ->
                    onValueChange((round(entered / step) * step).coerceIn(minimum, maximum))
                }
            },
            label = { Text(label) },
            suffix = { Text(unit) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        Column(modifier = Modifier.width(32.dp).height(56.dp)) {
            AppRepeatAdjustButton(
                icon = Icons.Filled.KeyboardArrowUp,
                enabled = value < maximum,
                onAdjust = { onValueChange((value + step).coerceAtMost(maximum)) },
            )
            AppRepeatAdjustButton(
                icon = Icons.Filled.KeyboardArrowDown,
                enabled = value > minimum,
                onAdjust = { onValueChange((value - step).coerceAtLeast(minimum)) },
            )
        }
    }
}

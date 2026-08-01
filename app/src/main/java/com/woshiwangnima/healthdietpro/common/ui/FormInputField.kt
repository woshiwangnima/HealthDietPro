package com.woshiwangnima.healthdietpro.common.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.common.range.Range
import java.math.BigDecimal
import java.math.RoundingMode

internal enum class NumericInputKind { Integer, Decimal }

internal data class NumericInputSpec(
    val kind: NumericInputKind,
    val allowNegative: Boolean = false,
    val range: Range<Double>? = null,
    val example: String? = null,
    val decimalPlaces: Int? = null,
    val step: Double? = null,
    val tooltip: String? = null,
    val showSupportingText: Boolean = true,
)

/** Text-only field. Numeric constraints intentionally do not exist on this API. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun TextInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    tooltip: String? = null,
    modifier: Modifier = Modifier,
) {
    FormInputContainer(tooltip, modifier, onClear = { onValueChange("") }) { fieldModifier, focused, clear ->
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            trailingIcon = if (value.isNotEmpty()) clear else null,
            minLines = 3,
            modifier = fieldModifier.onFocusChanged { focused(it.isFocused) },
        )
    }
}

/** Numeric-only field with syntax, precision, range, and optional step semantics. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun NumericInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    spec: NumericInputSpec,
    modifier: Modifier = Modifier,
) {
    val number = value.toDoubleOrNull()
    val syntaxValid = value.isBlank() || numericPattern(spec).matches(value)
    val rangeValid = number == null || spec.range?.contains(number) != false
    val valid = syntaxValid && rangeValid
    FormInputContainer(spec.tooltip, modifier, onClear = { onValueChange("") }) { fieldModifier, focused, clear ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = value,
                onValueChange = { onValueChange(filterNumericInput(it, spec)) },
                label = { Text(label) },
                isError = !valid,
                trailingIcon = if (value.isNotEmpty()) clear else null,
                supportingText = if (spec.showSupportingText && (!valid || spec.example != null)) {
                    {
                        Text(
                            when {
                                !syntaxValid -> "Input format is not valid"
                                !rangeValid -> "Allowed range: ${spec.range?.formatFor(spec)}"
                                else -> "Example: ${spec.example}"
                            },
                        )
                    }
                } else null,
                keyboardOptions = KeyboardOptions(keyboardType = if (spec.kind == NumericInputKind.Integer) KeyboardType.Number else KeyboardType.Decimal),
                singleLine = true,
                modifier = fieldModifier.onFocusChanged { focused(it.isFocused) },
            )
            spec.step?.let { step -> NumericStepControls(value, step, spec, onValueChange) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FormInputContainer(
    tooltip: String?,
    modifier: Modifier,
    onClear: () -> Unit,
    field: @Composable androidx.compose.foundation.layout.RowScope.(fieldModifier: Modifier, onFocusChanged: (Boolean) -> Unit, clear: @Composable () -> Unit) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val requester = remember { BringIntoViewRequester() }
    LaunchedEffect(focused) { if (focused) requester.bringIntoView() }
    InlineTooltip(message = tooltip.orEmpty(), modifier = modifier.fillMaxWidth().bringIntoViewRequester(requester)) { tooltipExpanded, onTooltipClick ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            field(
                Modifier.weight(1f),
                { focused = it },
                {
                    IconButton(onClick = onClear) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear", tint = androidx.compose.material3.MaterialTheme.colorScheme.error)
                    }
                },
            )
            tooltip?.let { description ->
                // Tooltip remains the rightmost action even when step controls are present.
                FormAuxiliaryIconButton(
                    onClick = onTooltipClick,
                    contentDescription = description,
                    selected = tooltipExpanded,
                ) { tint -> Icon(Icons.Filled.Info, contentDescription = null, tint = tint) }
            }
        }
    }
}

@Composable
private fun NumericStepControls(value: String, step: Double, spec: NumericInputSpec, onValueChange: (String) -> Unit) {
    Column(
        modifier = Modifier.width(48.dp).padding(start = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        StepButton(onClick = { changeByStep(value, step, spec)?.let(onValueChange) }, increase = true)
        StepButton(onClick = { changeByStep(value, -step, spec)?.let(onValueChange) }, increase = false)
    }
}

@Composable
private fun StepButton(onClick: () -> Unit, increase: Boolean) {
    FormAuxiliaryIconButton(
        onClick = onClick,
        contentDescription = if (increase) "Increase" else "Decrease",
    ) { tint ->
        if (increase) {
            Icon(Icons.Filled.Add, contentDescription = null, tint = tint)
        } else {
            Text("−", style = androidx.compose.material3.MaterialTheme.typography.titleMedium, color = tint)
        }
    }
}

@Composable
private fun FormAuxiliaryIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    selected: Boolean = false,
    content: @Composable (androidx.compose.ui.graphics.Color) -> Unit,
) {
    val colors = androidx.compose.material3.MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        color = if (selected) colors.secondaryContainer else colors.surface,
        contentColor = if (selected) colors.onSecondaryContainer else colors.onSurface,
        shape = androidx.compose.foundation.shape.CircleShape,
        shadowElevation = 2.dp,
        modifier = Modifier.size(40.dp),
    ) {
        androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
            content(if (selected) colors.onSecondaryContainer else colors.onSurface)
        }
    }
}

private fun filterNumericInput(value: String, spec: NumericInputSpec): String = when (spec.kind) {
    NumericInputKind.Integer -> value.filterIndexed { index, char -> char.isDigit() || spec.allowNegative && char == '-' && index == 0 }
    NumericInputKind.Decimal -> value.filterIndexed { index, char -> char.isDigit() || spec.allowNegative && char == '-' && index == 0 || char == '.' && value.indexOf('.') == index }
        .let { filtered -> spec.decimalPlaces?.let { places -> filtered.indexOf('.').let { point -> if (point < 0) filtered else filtered.take(point + places + 1) } } ?: filtered }
}

private fun numericPattern(spec: NumericInputSpec): Regex = when (spec.kind) {
    NumericInputKind.Integer -> Regex(if (spec.allowNegative) "-?\\d+" else "\\d+")
    NumericInputKind.Decimal -> Regex(if (spec.allowNegative) "-?(?:\\d+\\.?\\d*|\\.\\d+)" else "(?:\\d+\\.?\\d*|\\.\\d+)")
}

private fun Range<Double>.formatFor(spec: NumericInputSpec): String =
    "${if (minInclusive) '[' else '('}${formatEndpoint(min, spec, isLower = true)}, ${formatEndpoint(max, spec, isLower = false)}${if (maxInclusive) ']' else ')'}"

private fun formatEndpoint(value: Double?, spec: NumericInputSpec, isLower: Boolean): String = value?.let {
    val places = if (spec.kind == NumericInputKind.Integer) 0 else spec.decimalPlaces
    if (places == null) it.toString() else "%1$.${places}f".format(it)
} ?: if (isLower) "-∞" else "∞"

private fun changeByStep(value: String, step: Double, spec: NumericInputSpec): String? {
    var next = (value.toDoubleOrNull() ?: 0.0) + step
    spec.range?.min?.let { if (next < it || next == it && !spec.range.minInclusive) next = if (spec.range.minInclusive) it else Math.nextUp(it) }
    spec.range?.max?.let { if (next > it || next == it && !spec.range.maxInclusive) next = if (spec.range.maxInclusive) it else Math.nextDown(it) }
    val scale = spec.decimalPlaces ?: if (spec.kind == NumericInputKind.Integer) 0 else 2
    return BigDecimal.valueOf(next).setScale(scale, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
}

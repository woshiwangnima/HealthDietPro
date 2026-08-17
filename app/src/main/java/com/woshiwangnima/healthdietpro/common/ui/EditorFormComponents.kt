package com.woshiwangnima.healthdietpro.common.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.onFocusChanged
import com.woshiwangnima.healthdietpro.R

@Composable
internal fun FoodSearchField(value: String, onValueChange: (String) -> Unit, placeholder: String, onSearch: (() -> Unit)? = null, onFocusChanged: ((Boolean) -> Unit)? = null) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        trailingIcon = {
            Row {
                value.takeIf { it.isNotBlank() }?.let { IconButton(onClick = { onValueChange("") }) { Icon(Icons.Filled.Close, contentDescription = null) } }
                onSearch?.let { search -> IconButton(onClick = search) { Icon(Icons.Filled.Search, contentDescription = null) } }
            }
        },
        placeholder = { Text(placeholder) },
        textStyle = TextStyle.Default,
        modifier = Modifier.fillMaxWidth().onFocusChanged { onFocusChanged?.invoke(it.isFocused) },
    )
}

@Composable
internal fun AppCheckboxRow(checked: Boolean, label: String, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(label)
    }
}

internal data class NumericInputRange(val minimum: Double? = null, val maximum: Double? = null) {
    fun accepts(value: String): Boolean = value.isBlank() || value.toDoubleOrNull()?.let { number ->
        (minimum == null || number >= minimum) && (maximum == null || number <= maximum)
    } == true
}

@Composable
internal fun EditorSectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 4.dp))
}

@Composable
internal fun EditorTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    required: Boolean = false,
    showRequirementMarker: Boolean = true,
    numeric: Boolean = false,
    range: NumericInputRange? = null,
    singleLine: Boolean = true,
    suffix: @Composable (() -> Unit)? = null,
    supportingTextOverride: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val valid = range?.accepts(value) ?: true
    val decoratedLabel = when {
        !showRequirementMarker -> label
        required -> stringResource(R.string.nutrition_editor_field_required, label)
        else -> stringResource(R.string.nutrition_editor_field_optional, label)
    }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(decoratedLabel) },
        isError = !valid,
        suffix = suffix,
        supportingText = supportingTextOverride ?: if (range != null) {
            {
                val rangeText = when {
                    range.minimum != null && range.maximum != null -> stringResource(R.string.nutrition_editor_input_range, range.minimum, range.maximum)
                    range.minimum != null -> stringResource(R.string.nutrition_editor_input_minimum, range.minimum)
                    else -> stringResource(R.string.nutrition_editor_input_maximum, requireNotNull(range.maximum))
                }
                Text(rangeText)
            }
        } else null,
        singleLine = singleLine,
        keyboardOptions = if (numeric) KeyboardOptions(keyboardType = KeyboardType.Decimal) else KeyboardOptions.Default,
        modifier = modifier.fillMaxWidth(),
    )
}

/** Defers composition of long optional forms until the user explicitly expands the section. */
@Composable
internal fun ExpandableEditorSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Surface(
        modifier = modifier.fillMaxWidth().clickable { expanded = !expanded },
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
            Icon(if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, contentDescription = null)
        }
    }
    if (expanded) content(title)
}

@Composable
internal fun <T> LazyOptionalFields(items: List<T>, key: (T) -> String, field: @Composable (T) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth().height(420.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(items, key = key) { field(it) }
    }
}

/** Shared compact category filter backed by the repository-provided tree. */
@Composable
internal fun FoodCategoryFilterRows(
    roots: List<com.woshiwangnima.healthdietpro.model.food.FoodCategory>,
    childrenFor: (String) -> List<com.woshiwangnima.healthdietpro.model.food.FoodCategory>,
    selectedRoot: String?,
    selectedChild: String?,
    onRootSelected: (String?) -> Unit,
    onChildSelected: (String?) -> Unit,
) {
    val children = selectedRoot?.let(childrenFor).orEmpty()
    CategoryFilterRow(stringResource(R.string.nutrition_editor_category_level_one)) {
        roots.forEach { root ->
            FilterChip(
                selected = selectedRoot == root.tag,
                onClick = { onRootSelected(if (selectedRoot == root.tag) null else root.tag) },
                label = { Text(stringResource(root.labelRes)) },
            )
        }
    }
    if (children.isNotEmpty()) {
        CategoryFilterRow(stringResource(R.string.nutrition_editor_category_level_two)) {
            children.forEach { child ->
                FilterChip(
                    selected = selectedChild == child.tag,
                    onClick = { onChildSelected(if (selectedChild == child.tag) null else child.tag) },
                    label = { Text(stringResource(child.labelRes)) },
                )
            }
        }
    }
}

@Composable
private fun CategoryFilterRow(label: String, chips: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.width(56.dp).padding(end = 8.dp), style = MaterialTheme.typography.labelMedium)
        Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) { chips() }
    }
}

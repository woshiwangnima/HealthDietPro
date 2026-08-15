package com.woshiwangnima.healthdietpro.ui.container

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.range.RangeBand
import com.woshiwangnima.healthdietpro.common.ui.AppDropdownField
import com.woshiwangnima.healthdietpro.common.ui.AppDropdownOption
import com.woshiwangnima.healthdietpro.common.ui.AppIconTextButton
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen
import com.woshiwangnima.healthdietpro.common.ui.EditorSectionTitle
import com.woshiwangnima.healthdietpro.common.ui.NumericInputField
import com.woshiwangnima.healthdietpro.common.ui.NumericInputKind
import com.woshiwangnima.healthdietpro.common.ui.NumericInputSpec
import com.woshiwangnima.healthdietpro.common.ui.SettingRow
import com.woshiwangnima.healthdietpro.model.container.ContainerRecord
import com.woshiwangnima.healthdietpro.model.container.ContainerCapacityMode
import com.woshiwangnima.healthdietpro.model.container.capacityMlAtHeightPercent
import com.woshiwangnima.healthdietpro.model.prefs.AppPrefs
import com.woshiwangnima.healthdietpro.model.unit.UnitCategoryType

/** 记容器设置：容器使用场景标签管理与容量查询入口。 */
@Composable
internal fun ContainerSettingsScreen(
    scenarioTags: List<String>,
    onSaveScenarioTags: (List<String>) -> Unit,
    onCapacityLookup: () -> Unit,
    onBack: () -> Unit,
) {
    var localTags by remember(scenarioTags) { mutableStateOf(scenarioTags) }
    var addingTag by remember { mutableStateOf(false) }
    var deletingTag by remember { mutableStateOf<String?>(null) }
    BackHandler(onBack = onBack)
    BaseScreen(title = stringResource(R.string.container_settings_title), onBack = onBack) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { EditorSectionTitle(stringResource(R.string.container_scenario_tags)) }
            item {
                Text(
                    stringResource(R.string.container_scenario_tags_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            if (localTags.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.container_scenario_tag_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            } else {
                items(localTags, key = { it }) { tag ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    ) {
                        Row(
                            Modifier.padding(start = 12.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(tag, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            IconButton(onClick = { deletingTag = tag }) {
                                Icon(
                                    painterResource(R.drawable.ic_delete),
                                    contentDescription = stringResource(R.string.container_scenario_tag_delete, tag),
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
            }
            item {
                AppIconTextButton(
                    text = stringResource(R.string.container_scenario_tag_add),
                    iconRes = R.drawable.ic_add,
                    onClick = { addingTag = true },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                )
            }
            item {
                SettingRow(
                    title = stringResource(R.string.container_capacity_lookup),
                    subtitle = stringResource(R.string.container_capacity_lookup_description),
                    leadingIconRes = R.drawable.ic_volume,
                    onClick = onCapacityLookup,
                )
            }
        }
    }
    if (addingTag) {
        AddScenarioTagDialog(onDismiss = { addingTag = false }) { label ->
            val trimmed = label.trim()
            if (trimmed.isNotEmpty() && trimmed !in localTags) {
                localTags = localTags + trimmed
                onSaveScenarioTags(localTags)
            }
            addingTag = false
        }
    }
    deletingTag?.let { tag ->
        AlertDialog(
            onDismissRequest = { deletingTag = null },
            title = { Text(stringResource(R.string.container_scenario_tag_delete_confirm_title)) },
            text = { Text(stringResource(R.string.container_scenario_tag_delete_confirm_message, tag)) },
            confirmButton = {
                TextButton(onClick = {
                    localTags = localTags.filterNot { it == tag }
                    onSaveScenarioTags(localTags)
                    deletingTag = null
                }) { Text(stringResource(R.string.body_record_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deletingTag = null }) { Text(stringResource(R.string.compose_confirm_dialog_cancel)) }
            },
        )
    }
}

@Composable
private fun AddScenarioTagDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var label by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.container_scenario_tag_add)) },
        text = {
            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text(stringResource(R.string.container_scenario_tag_name)) },
            )
        },
        confirmButton = {
            TextButton(onClick = { onAdd(label) }) { Text(stringResource(R.string.body_record_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.body_record_cancel)) }
        },
    )
}

/**
 * 容量查询：选择一个已有容器，输入高度百分比，两种容量方式返回对应容量
 * （手动输入按线性增长，截面方式按截面系统积分）。
 */
@Composable
internal fun ContainerCapacityLookupScreen(
    containers: List<ContainerRecord>,
    onBack: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val volumeUnit = AppPrefs.getUnit(context, UnitCategoryType.Volume.id, UnitCategoryType.Volume.defaultUnitId)
    val volumeUnitLabel = volumeUnitSymbol(volumeUnit)
    var selectedId by remember { mutableStateOf(containers.firstOrNull()?.id.orEmpty()) }
    var percentText by remember { mutableStateOf("100") }
    val selected = containers.firstOrNull { it.id == selectedId }
    val percent = percentText.toDoubleOrNull()?.coerceIn(0.0, 100.0)
    val resultMl = selected?.let { container -> percent?.let { container.capacityMlAtHeightPercent(it) } }
    val unavailable = selected != null && selected.capacityMode == ContainerCapacityMode.CROSS_SECTION && selected.crossSections == null
    BackHandler(onBack = onBack)
    BaseScreen(title = stringResource(R.string.container_capacity_lookup), onBack = onBack) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AppDropdownField(
                label = stringResource(R.string.container_capacity_lookup_select_container),
                value = selected?.name.orEmpty(),
                options = containers.map { container ->
                    AppDropdownOption(
                        container.id,
                        container.name,
                        stringResource(R.string.container_capacity_lookup_capacity_hint, fromMl(container.capacityMl, volumeUnit), volumeUnitLabel),
                    )
                },
                onSelect = { selectedId = it.id },
            )
            NumericInputField(
                label = stringResource(R.string.container_capacity_lookup_height_percent),
                value = percentText,
                onValueChange = { percentText = it },
                spec = NumericInputSpec(
                    kind = NumericInputKind.Decimal,
                    range = RangeBand(min = 0.0, minInclusive = true, max = 100.0, maxInclusive = true, value = Unit),
                    decimalPlaces = 1,
                    example = "50",
                ),
            )
            when {
                selected == null -> Text(stringResource(R.string.container_capacity_lookup_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                unavailable -> Text(stringResource(R.string.container_capacity_lookup_unavailable), color = MaterialTheme.colorScheme.onSurfaceVariant)
                resultMl == null -> Text(stringResource(R.string.container_capacity_lookup_invalid), color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> Text(
                    stringResource(
                        R.string.container_capacity_lookup_result,
                        percent ?: 0.0,
                        fromMl(resultMl, volumeUnit),
                        volumeUnitLabel,
                    ),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}
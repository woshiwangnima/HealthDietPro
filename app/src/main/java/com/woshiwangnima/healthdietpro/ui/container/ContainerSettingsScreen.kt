package com.woshiwangnima.healthdietpro.ui.container

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.ui.AppDropdownField
import com.woshiwangnima.healthdietpro.common.ui.AppDropdownOption
import com.woshiwangnima.healthdietpro.common.ui.AppIconTextButton
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen
import com.woshiwangnima.healthdietpro.common.ui.SettingRow
import com.woshiwangnima.healthdietpro.model.container.ContainerRecord
import com.woshiwangnima.healthdietpro.model.container.ContainerCapacityMode
import com.woshiwangnima.healthdietpro.model.container.capacityMlAtHeightPercent
import com.woshiwangnima.healthdietpro.model.container.toDomain
import com.woshiwangnima.healthdietpro.model.prefs.AppPrefs
import com.woshiwangnima.healthdietpro.model.unit.UnitCategoryType

/** 记容器设置：自定义标签设置与容量查询入口。 */
@Composable
internal fun ContainerSettingsScreen(
    onCustomTags: () -> Unit,
    onCapacityLookup: () -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    BaseScreen(title = stringResource(R.string.container_settings_title), onBack = onBack) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            SettingRow(
                title = stringResource(R.string.custom_tag_settings_title),
                subtitle = stringResource(R.string.container_scenario_tags),
                leadingIconRes = R.drawable.ic_preferences,
                onClick = onCustomTags,
            )
            SettingRow(
                title = stringResource(R.string.container_capacity_lookup),
                subtitle = stringResource(R.string.container_capacity_lookup_description),
                leadingIconRes = R.drawable.ic_volume,
                onClick = onCapacityLookup,
            )
        }
    }
}

/** 自定义标签设置：管理容器使用场景标签（添加/删除，删除同步清理容器引用）。 */
@Composable
internal fun CustomTagSettingsScreen(
    scenarioTags: List<String>,
    onSaveScenarioTags: (List<String>) -> Unit,
    onBack: () -> Unit,
) {
    var localTags by remember(scenarioTags) { mutableStateOf(scenarioTags) }
    var addingTag by remember { mutableStateOf(false) }
    var deletingTag by remember { mutableStateOf<String?>(null) }
    BackHandler(onBack = onBack)
    BaseScreen(title = stringResource(R.string.custom_tag_settings_title), onBack = onBack) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
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
 * 容量查询：选择一个已有容器（下拉框选项复用记容器卡片），用固定高度进度条上下调整
 * 高度百分比，左侧实时显示当前高度、当前高度处容量与总容量。
 *
 * 手动输入模式按线性增长返回容量；按截面计算模式按截面系统在对应高度的容积返回。
 */
@Composable
internal fun ContainerCapacityLookupScreen(
    containers: List<ContainerRecord>,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val volumeUnit = AppPrefs.getUnit(context, UnitCategoryType.Volume.id, UnitCategoryType.Volume.defaultUnitId)
    val volumeUnitLabel = volumeUnitSymbol(volumeUnit)
    val lengthUnit = AppPrefs.getUnit(context, UnitCategoryType.Length.id, UnitCategoryType.Length.defaultUnitId)
    var selectedId by remember { mutableStateOf(containers.firstOrNull()?.id.orEmpty()) }
    var percent by remember { mutableStateOf(100.0) }
    val selected = containers.firstOrNull { it.id == selectedId }
    val resultMl = selected?.let { container -> container.capacityMlAtHeightPercent(percent) }
    val totalMl = selected?.let { container -> container.capacityMlAtHeightPercent(100.0) ?: container.capacityMl }
    val totalHeightCm = selected?.crossSections?.toDomain()?.totalHeightCm
    val unavailable = selected != null && selected.capacityMode == ContainerCapacityMode.CROSS_SECTION && selected.crossSections == null
    BackHandler(onBack = onBack)
    BaseScreen(title = stringResource(R.string.container_capacity_lookup), onBack = onBack) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (containers.isEmpty()) {
                Text(stringResource(R.string.container_capacity_lookup_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                AppDropdownField(
                    label = stringResource(R.string.container_capacity_lookup_select_container),
                    value = selected?.let { it.name.ifBlank { context.getString(it.category.labelRes()) } }.orEmpty(),
                    options = containers.map { container ->
                        AppDropdownOption(
                            container.id,
                            container.name.ifBlank { context.getString(container.category.labelRes()) },
                            stringResource(
                                R.string.container_capacity_lookup_capacity_hint,
                                fromMl(container.capacityMlAtHeightPercent(100.0) ?: container.capacityMl, volumeUnit),
                                volumeUnitLabel,
                            ),
                        )
                    },
                    onSelect = { selectedId = it.id },
                    showOptionDividers = true,
                    optionContent = { option ->
                        val container = containers.first { it.id == option.id }
                        Surface(
                            Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                        ) {
                            ContainerCardRow(container, showDelete = false)
                        }
                    },
                )
                if (unavailable) {
                    Text(stringResource(R.string.container_capacity_lookup_unavailable), color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Row(
                        Modifier.fillMaxWidth().height(220.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                stringResource(R.string.container_capacity_lookup_height, percent),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            if (totalHeightCm != null) {
                                Text(
                                    stringResource(
                                        R.string.container_capacity_lookup_height_value,
                                        fromBaseCm(totalHeightCm * percent / 100.0, lengthUnit),
                                        lengthUnitSymbol(lengthUnit),
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            } else {
                                Text(
                                    stringResource(R.string.container_capacity_lookup_linear_hint),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                stringResource(R.string.container_capacity_lookup_capacity_at_height, fromMl(resultMl ?: 0.0, volumeUnit), volumeUnitLabel),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                stringResource(R.string.container_capacity_lookup_total_capacity, fromMl(totalMl ?: 0.0, volumeUnit), volumeUnitLabel),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        VerticalPercentSlider(
                            value = (percent / 100.0).toFloat(),
                            onValueChange = { percent = it.toDouble() * 100.0 },
                            modifier = Modifier.width(48.dp).fillMaxHeight(),
                        )
                    }
                }
            }
        }
    }
}
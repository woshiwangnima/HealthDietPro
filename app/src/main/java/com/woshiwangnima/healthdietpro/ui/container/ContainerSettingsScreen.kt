package com.woshiwangnima.healthdietpro.ui.container

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppEmptyState
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppListRow
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppListRowDivider
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppSectionGroup
import com.woshiwangnima.healthdietpro.common.ui.designsystem.LocalAppTokens
import com.woshiwangnima.healthdietpro.model.container.ContainerCapacityMode
import com.woshiwangnima.healthdietpro.model.container.ContainerRecord
import com.woshiwangnima.healthdietpro.model.container.capacityMlAtHeightPercent
import com.woshiwangnima.healthdietpro.model.container.toDomain
import com.woshiwangnima.healthdietpro.model.prefs.AppPrefs
import com.woshiwangnima.healthdietpro.model.unit.UnitCategoryType

/**
 * 记容器设置：自定义标签与容量查询两个入口。
 *
 * 旧版两条 `SettingRow` 直接贴在页面上，没有分组容器——这与「我的」「软件设置」已统一的
 * 「设置项一律进分组卡」不一致。现在收进 [AppSectionGroup] + [AppListRow]。
 */
@Composable
internal fun ContainerSettingsScreen(
    onCustomTags: () -> Unit,
    onCapacityLookup: () -> Unit,
    onBack: () -> Unit,
) {
    val tokens = LocalAppTokens.current
    BackHandler(onBack = onBack)
    BaseScreen(title = stringResource(R.string.container_settings_title), onBack = onBack) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .background(tokens.colors.bg)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(
                    start = tokens.spacing.page,
                    end = tokens.spacing.page,
                    top = tokens.spacing.s4,
                    bottom = tokens.spacing.s4,
                ),
        ) {
            AppSectionGroup {
                AppListRow(
                    title = stringResource(R.string.custom_tag_settings_title),
                    tooltip = stringResource(R.string.container_scenario_tags_hint),
                    iconRes = R.drawable.ic_preferences,
                    showChevron = true,
                    onClick = onCustomTags,
                )
                AppListRowDivider()
                AppListRow(
                    title = stringResource(R.string.container_capacity_lookup),
                    tooltip = stringResource(R.string.container_capacity_lookup_description),
                    iconRes = R.drawable.ic_volume,
                    showChevron = true,
                    onClick = onCapacityLookup,
                )
            }
        }
    }
}

/**
 * 自定义标签：说明 → 标签列表 → 添加按钮。
 *
 * 标签行从「每行一块灰底 Surface」改成同一张分组卡里的行 + 分隔线：标签是同一类项的集合，
 * 一卡一行会把长列表读成一堆彼此无关的碎片。
 */
@Composable
internal fun CustomTagSettingsScreen(
    scenarioTags: List<String>,
    onSaveScenarioTags: (List<String>) -> Unit,
    onBack: () -> Unit,
) {
    val tokens = LocalAppTokens.current
    var localTags by remember(scenarioTags) { mutableStateOf(scenarioTags) }
    var addingTag by remember { mutableStateOf(false) }
    var deletingTag by remember { mutableStateOf<String?>(null) }
    BackHandler(onBack = onBack)
    BaseScreen(title = stringResource(R.string.custom_tag_settings_title), onBack = onBack) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .background(tokens.colors.bg)
                .padding(padding),
            contentPadding = PaddingValues(
                start = tokens.spacing.page,
                end = tokens.spacing.page,
                top = tokens.spacing.s4,
                bottom = tokens.spacing.s4,
            ),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.s4),
        ) {
            item {
                Text(
                    text = stringResource(R.string.container_scenario_tags_hint),
                    style = tokens.type.caption,
                    color = tokens.colors.textMuted,
                )
            }
            if (localTags.isEmpty()) {
                item {
                    AppEmptyState(
                        title = stringResource(R.string.container_scenario_tag_empty),
                        description = stringResource(R.string.container_scenario_tags_hint),
                        iconRes = R.drawable.ic_event,
                    )
                }
            } else {
                item {
                    AppSectionGroup {
                        localTags.forEachIndexed { index, tag ->
                            if (index > 0) AppListRowDivider(leadingInset = false)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = tokens.spacing.rowMinHeight)
                                    .padding(start = tokens.spacing.s3, end = tokens.spacing.s1),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = tag,
                                    style = tokens.type.body,
                                    color = tokens.colors.text,
                                    modifier = Modifier.weight(1f),
                                )
                                IconButton(onClick = { deletingTag = tag }) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_delete),
                                        contentDescription = stringResource(
                                            R.string.container_scenario_tag_delete,
                                            tag,
                                        ),
                                        tint = tokens.colors.danger,
                                    )
                                }
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
                    modifier = Modifier.fillMaxWidth(),
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
                }) { Text(stringResource(R.string.body_record_delete), color = tokens.colors.danger) }
            },
            dismissButton = {
                TextButton(onClick = { deletingTag = null }) {
                    Text(stringResource(R.string.compose_confirm_dialog_cancel))
                }
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
 * 容量查询：选容器 → 拖高度 → 读当前高度处的容量。
 *
 * 读数区按「主数值 + 三条说明」排列：高度百分比用 `numeric` + 主色（唯一的主读数），
 * 其余三行（对应高度、该高度容量、总容量）降为 `caption`，一眼能分出哪个数是「结果」。
 */
@Composable
internal fun ContainerCapacityLookupScreen(
    containers: List<ContainerRecord>,
    onBack: () -> Unit,
) {
    val tokens = LocalAppTokens.current
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
    val unavailable = selected != null &&
        selected.capacityMode == ContainerCapacityMode.CROSS_SECTION &&
        selected.crossSections == null
    BackHandler(onBack = onBack)
    BaseScreen(title = stringResource(R.string.container_capacity_lookup), onBack = onBack) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .background(tokens.colors.bg)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(
                    start = tokens.spacing.page,
                    end = tokens.spacing.page,
                    top = tokens.spacing.s4,
                    bottom = tokens.spacing.s4,
                ),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.s4),
        ) {
            if (containers.isEmpty()) {
                AppEmptyState(
                    title = stringResource(R.string.container_capacity_lookup_empty),
                    description = stringResource(R.string.container_empty_hint),
                    iconRes = R.drawable.ic_container,
                )
            } else {
                AppSectionGroup {
                    Column(Modifier.padding(tokens.spacing.s3)) {
                        AppDropdownField(
                            label = stringResource(R.string.container_capacity_lookup_select_container),
                            value = selected?.let {
                                it.name.ifBlank { context.getString(it.category.labelRes()) }
                            }.orEmpty(),
                            options = containers.map { container ->
                                AppDropdownOption(
                                    container.id,
                                    container.name.ifBlank { context.getString(container.category.labelRes()) },
                                    stringResource(
                                        R.string.container_capacity_lookup_capacity_hint,
                                        fromMl(
                                            container.capacityMlAtHeightPercent(100.0) ?: container.capacityMl,
                                            volumeUnit,
                                        ),
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
                                    shape = RoundedCornerShape(tokens.radii.container),
                                    color = LocalAppTokens.current.colors.panelRaised.copy(alpha = 0.2f),
                                ) {
                                    ContainerCardRow(container, showDelete = false)
                                }
                            },
                        )
                    }
                }
                if (unavailable) {
                    AppEmptyState(
                        title = stringResource(R.string.container_capacity_lookup_unavailable),
                        description = stringResource(R.string.container_capacity_lookup_linear_hint),
                        iconRes = R.drawable.ic_vs,
                    )
                } else {
                    AppSectionGroup {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                // 220 dp 是滑轨的最小可操作高度（拇指能沿轨上下拖 100% 行程），
                                // 属自绘控件的几何量，不走间距令牌。
                                .height(CapacitySliderHeight)
                                .padding(tokens.spacing.s3),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.s4),
                        ) {
                            Column(
                                Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(tokens.spacing.s2),
                            ) {
                                Text(
                                    text = stringResource(R.string.container_capacity_lookup_height, percent),
                                    style = tokens.type.numeric,
                                    color = tokens.colors.primary,
                                )
                                Text(
                                    text = if (totalHeightCm != null) {
                                        stringResource(
                                            R.string.container_capacity_lookup_height_value,
                                            fromBaseCm(totalHeightCm * percent / 100.0, lengthUnit),
                                            lengthUnitSymbol(lengthUnit),
                                        )
                                    } else {
                                        stringResource(R.string.container_capacity_lookup_linear_hint)
                                    },
                                    style = tokens.type.caption,
                                    color = tokens.colors.textMuted,
                                )
                                Text(
                                    text = stringResource(
                                        R.string.container_capacity_lookup_capacity_at_height,
                                        fromMl(resultMl ?: 0.0, volumeUnit),
                                        volumeUnitLabel,
                                    ),
                                    style = tokens.type.caption,
                                    color = tokens.colors.textMuted,
                                )
                                Text(
                                    text = stringResource(
                                        R.string.container_capacity_lookup_total_capacity,
                                        fromMl(totalMl ?: 0.0, volumeUnit),
                                        volumeUnitLabel,
                                    ),
                                    style = tokens.type.caption,
                                    color = tokens.colors.textMuted,
                                )
                            }
                            VerticalPercentSlider(
                                value = (percent / 100.0).toFloat(),
                                onValueChange = { percent = it.toDouble() * 100.0 },
                                modifier = Modifier.width(tokens.spacing.s7).fillMaxHeight(),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 容量查询滑轨的最小高度：低于这个值拇指拖不满 0–100% 的行程。 */
private val CapacitySliderHeight = 220.dp

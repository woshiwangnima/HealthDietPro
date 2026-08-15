package com.woshiwangnima.healthdietpro.ui.container

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.ui.AppIconTextButton
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen
import com.woshiwangnima.healthdietpro.model.container.ContainerCategory
import com.woshiwangnima.healthdietpro.model.container.ContainerRecord
import com.woshiwangnima.healthdietpro.model.container.ContainerRepository
import com.woshiwangnima.healthdietpro.model.container.capacityMlAtHeightPercent
import com.woshiwangnima.healthdietpro.model.prefs.AppPrefs
import com.woshiwangnima.healthdietpro.model.unit.UnitCategoryType
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private const val CONTAINER_IMAGE_ROTATION_MS = 3_000L

@Composable
internal fun ContainerListScreen(
    containers: List<ContainerRecord>,
    scenarioTags: List<String>,
    selectedCategories: Set<ContainerCategory>,
    selectedScenarioTags: Set<String>,
    onToggleCategory: (ContainerCategory) -> Unit,
    onToggleScenarioTag: (String) -> Unit,
    onAdd: () -> Unit,
    onEdit: (ContainerRecord) -> Unit,
    onDelete: (String) -> Unit,
    onSettings: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pendingDelete by remember { mutableStateOf<ContainerRecord?>(null) }
    val context = LocalContext.current
    val filtered = remember(containers, selectedCategories, selectedScenarioTags) {
        containers.filter { container ->
            (selectedCategories.isEmpty() || container.category in selectedCategories) &&
                (selectedScenarioTags.isEmpty() || container.scenarioTags.any { it in selectedScenarioTags })
        }
    }
    BaseScreen(
        title = stringResource(R.string.container_title),
        onBack = onBack,
        actions = { IconButton(onClick = onSettings) { Icon(painterResource(R.drawable.ic_settings), stringResource(R.string.container_settings_title)) } },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AppIconTextButton(stringResource(R.string.container_add), R.drawable.ic_add, onAdd, Modifier.fillMaxWidth())
            val categoryTags = ContainerCategory.entries
                .filter { it != ContainerCategory.CUSTOM }
                .map { it.name to stringResource(it.labelRes()) }
            ContainerFilterChipRow(
                title = stringResource(R.string.container_system_categories),
                tags = categoryTags,
                selected = selectedCategories.map { it.name }.toSet(),
                onToggle = { onToggleCategory(ContainerCategory.valueOf(it)) },
            )
            if (scenarioTags.isNotEmpty()) {
                ContainerFilterChipRow(
                    title = stringResource(R.string.container_scenario_tags),
                    tags = scenarioTags.map { it to it },
                    selected = selectedScenarioTags,
                    onToggle = onToggleScenarioTag,
                )
            }
            if (filtered.isEmpty()) {
                Text(
                    text = stringResource(if (containers.isEmpty()) R.string.container_empty else R.string.container_filter_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filtered, key = { it.id }) { container ->
                        ContainerListCard(container, onEdit, { pendingDelete = container })
                    }
                }
            }
        }
    }
    pendingDelete?.let { container ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.container_delete_confirm_title)) },
            text = { Text(stringResource(R.string.container_delete_confirm_message, container.name.ifBlank { context.getString(container.category.labelRes()) })) },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(container.id)
                    pendingDelete = null
                }) { Text(stringResource(R.string.body_record_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.compose_confirm_dialog_cancel)) }
            },
        )
    }
}

@Composable
private fun ContainerFilterChipRow(
    title: String,
    tags: List<Pair<String, String>>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxWidth().height(40.dp), shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxHeight().padding(horizontal = 4.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                tags.forEach { (id, label) ->
                    ContainerFilterChip(selected = id in selected, onClick = { onToggle(id) }, label = { Text(label, style = MaterialTheme.typography.bodySmall) })
                }
            }
        }
    }
}

@Composable
private fun ContainerFilterChip(selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, label: @Composable () -> Unit) {
    Surface(
        modifier = modifier.height(28.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(6.dp),
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant),
    ) {
        Box(modifier = Modifier.padding(horizontal = 2.dp), contentAlignment = Alignment.Center) { label() }
    }
}

@Composable
private fun ContainerListCard(
    container: ContainerRecord,
    onEdit: (ContainerRecord) -> Unit,
    onRequestDelete: (ContainerRecord) -> Unit,
) {
    val context = LocalContext.current
    val volumeUnit = AppPrefs.getUnit(context, UnitCategoryType.Volume.id, UnitCategoryType.Volume.defaultUnitId)
    val weightUnit = AppPrefs.getUnit(context, UnitCategoryType.Weight.id, UnitCategoryType.Weight.defaultUnitId)
    val capacityMl = container.capacityMlAtHeightPercent(100.0) ?: container.capacityMl
    Card(onClick = { onEdit(container) }, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ContainerThumbnail(container)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(container.name.ifBlank { context.getString(container.category.labelRes()) }, style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(container.category.labelRes()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    stringResource(R.string.container_capacity_value, fromMl(capacityMl, volumeUnit), volumeUnitSymbol(volumeUnit)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                container.emptyMassGrams?.let { grams ->
                    Text(
                        stringResource(R.string.container_empty_mass_value, fromGrams(grams, weightUnit), weightUnitSymbol(weightUnit)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (container.scenarioTags.isNotEmpty()) {
                    Text(
                        container.scenarioTags.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            IconButton(onClick = { onRequestDelete(container) }) {
                Icon(painterResource(R.drawable.ic_delete), contentDescription = stringResource(R.string.body_record_delete), tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun ContainerThumbnail(container: ContainerRecord) {
    val paths = container.imagePaths
    var index by remember(container.id, paths) { mutableIntStateOf(0) }
    LaunchedEffect(container.id, paths) {
        if (paths.size > 1) {
            while (isActive) {
                delay(CONTAINER_IMAGE_ROTATION_MS)
                index = (index + 1) % paths.size
            }
        }
    }
    if (paths.isEmpty()) {
        Surface(Modifier.size(56.dp), color = MaterialTheme.colorScheme.surfaceVariant) {}
        return
    }
    val currentPath = paths[index.coerceIn(0, paths.size - 1)]
    val context = LocalContext.current
    val repository = remember { ContainerRepository.fromContext(context) }
    val bitmap = remember(currentPath) { repository.loadImage(currentPath) }
    Box(Modifier.size(56.dp)) {
        if (bitmap == null) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceVariant) {}
        } else {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

internal fun ContainerCategory.labelRes(): Int = when (this) {
    ContainerCategory.CUP -> R.string.container_category_cup
    ContainerCategory.BOWL -> R.string.container_category_bowl
    ContainerCategory.PLATE -> R.string.container_category_plate
    ContainerCategory.SPOON -> R.string.container_category_spoon
    ContainerCategory.BOTTLE -> R.string.container_category_bottle
    ContainerCategory.CUSTOM -> R.string.container_category_custom
}

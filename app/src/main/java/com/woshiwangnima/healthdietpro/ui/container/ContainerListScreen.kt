package com.woshiwangnima.healthdietpro.ui.container

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.woshiwangnima.healthdietpro.model.prefs.AppPrefs
import com.woshiwangnima.healthdietpro.model.unit.UnitCategoryType

@Composable
internal fun ContainerListScreen(
    containers: List<ContainerRecord>,
    onAdd: () -> Unit,
    onEdit: (ContainerRecord) -> Unit,
    onDelete: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pendingDelete by remember { mutableStateOf<ContainerRecord?>(null) }
    BaseScreen(title = stringResource(R.string.container_title), onBack = onBack) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AppIconTextButton(stringResource(R.string.container_add), R.drawable.ic_add, onAdd, Modifier.fillMaxWidth())
            if (containers.isEmpty()) {
                Text(
                    text = stringResource(R.string.container_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(containers, key = { it.id }) { container ->
                        ContainerListCard(container, onEdit)
                    }
                }
            }
        }
    }
    pendingDelete?.let { container ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.container_delete_confirm_title)) },
            text = { Text(stringResource(R.string.container_delete_confirm_message, container.name)) },
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
private fun ContainerListCard(container: ContainerRecord, onEdit: (ContainerRecord) -> Unit) {
    val context = LocalContext.current
    val volumeUnit = AppPrefs.getUnit(context, UnitCategoryType.Volume.id, UnitCategoryType.Volume.defaultUnitId)
    val weightUnit = AppPrefs.getUnit(context, UnitCategoryType.Weight.id, UnitCategoryType.Weight.defaultUnitId)
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ContainerThumbnail(container)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(container.name, style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(container.category.labelRes()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    stringResource(R.string.container_capacity_value, fromMl(container.capacityMl, volumeUnit), volumeUnitSymbol(volumeUnit)),
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
            }
            IconButton(onClick = { onEdit(container) }) {
                Icon(painterResource(R.drawable.ic_edit), contentDescription = stringResource(R.string.container_edit))
            }
        }
    }
}

@Composable
private fun ContainerThumbnail(container: ContainerRecord) {
    val firstImage = container.imagePaths.firstOrNull()
    val context = LocalContext.current
    val repository = remember { ContainerRepository.fromContext(context) }
    val bitmap = remember(firstImage) { firstImage?.let { repository.loadImage(it) } }
    if (bitmap == null) {
        Surface(Modifier.size(56.dp), color = MaterialTheme.colorScheme.surfaceVariant) {}
    } else {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(56.dp),
        )
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

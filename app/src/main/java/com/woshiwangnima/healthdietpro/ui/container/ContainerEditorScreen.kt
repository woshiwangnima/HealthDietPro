package com.woshiwangnima.healthdietpro.ui.container

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.ui.AppDropdownField
import com.woshiwangnima.healthdietpro.common.ui.AppDropdownOption
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen
import com.woshiwangnima.healthdietpro.common.ui.DiscardChangesDialog
import com.woshiwangnima.healthdietpro.common.ui.EditorSectionTitle
import com.woshiwangnima.healthdietpro.common.ui.EditorTextField
import com.woshiwangnima.healthdietpro.common.ui.FormSaveBar
import com.woshiwangnima.healthdietpro.common.ui.HorizontalImageEditor
import com.woshiwangnima.healthdietpro.model.container.CircleShape
import com.woshiwangnima.healthdietpro.model.container.ContainerCapacityMode
import com.woshiwangnima.healthdietpro.model.container.ContainerCategory
import com.woshiwangnima.healthdietpro.model.container.ContainerRecord
import com.woshiwangnima.healthdietpro.model.container.CrossSection
import com.woshiwangnima.healthdietpro.model.container.CrossSectionProfile
import com.woshiwangnima.healthdietpro.model.container.toDomain
import com.woshiwangnima.healthdietpro.model.container.toDto
import com.woshiwangnima.healthdietpro.model.prefs.AppPrefs
import com.woshiwangnima.healthdietpro.model.unit.UnitCategoryType
import java.util.UUID
import kotlinx.coroutines.launch

@Composable
internal fun ContainerEditorScreen(
    existing: ContainerRecord?,
    scenarioTags: List<String>,
    viewModel: ContainerViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val defaultVolumeUnit = AppPrefs.getUnit(context, UnitCategoryType.Volume.id, UnitCategoryType.Volume.defaultUnitId)
    val defaultWeightUnit = AppPrefs.getUnit(context, UnitCategoryType.Weight.id, UnitCategoryType.Weight.defaultUnitId)

    var name by rememberSaveable(existing?.id) { mutableStateOf(existing?.name.orEmpty()) }
    var category by rememberSaveable(existing?.id) { mutableStateOf(existing?.category ?: ContainerCategory.CUSTOM) }
    var capacityMode by rememberSaveable(existing?.id) { mutableStateOf(existing?.capacityMode ?: ContainerCapacityMode.MANUAL) }
    var capacityText by rememberSaveable(existing?.id) { mutableStateOf(existing?.let { "%.1f".format(fromMl(it.capacityMl, defaultVolumeUnit)) }.orEmpty()) }
    var volumeUnitId by rememberSaveable(existing?.id) { mutableStateOf(defaultVolumeUnit) }
    var emptyMassText by rememberSaveable(existing?.id) { mutableStateOf(existing?.emptyMassGrams?.let { "%.2f".format(fromGrams(it, defaultWeightUnit)) }.orEmpty()) }
    var weightUnitId by rememberSaveable(existing?.id) { mutableStateOf(defaultWeightUnit) }
    var note by rememberSaveable(existing?.id) { mutableStateOf(existing?.note.orEmpty()) }
    var imagePaths by remember(existing?.id) { mutableStateOf(existing?.imagePaths.orEmpty()) }
    var imageBitmaps by remember(existing?.id) { mutableStateOf(emptyList<Bitmap>()) }
    var crossSectionProfile by remember(existing?.id) { mutableStateOf(existing?.crossSections?.toDomain()) }
    var selectedScenarioTags by remember(existing?.id) { mutableStateOf(existing?.scenarioTags.orEmpty()) }
    var showDiscardDialog by rememberSaveable { mutableStateOf(false) }

    // Load existing thumbnails off the main thread.
    LaunchedEffect(existing?.id, imagePaths) {
        if (imageBitmaps.isEmpty() && imagePaths.isNotEmpty()) {
            imageBitmaps = imagePaths.mapNotNull { viewModel.loadImage(it) }
        }
    }
    LaunchedEffect(capacityMode) {
        if (capacityMode == ContainerCapacityMode.CROSS_SECTION && crossSectionProfile == null) {
            crossSectionProfile = defaultCrossSectionProfile()
        }
    }

    val manualCapacityMl = capacityText.toDoubleOrNull()?.let { toMl(it, volumeUnitId) }
    val derivedCapacityMl = crossSectionProfile?.totalVolumeMl()
    val capacityMl = when (capacityMode) {
        ContainerCapacityMode.MANUAL -> manualCapacityMl
        ContainerCapacityMode.CROSS_SECTION -> derivedCapacityMl
    }
    val emptyMassGrams = emptyMassText.toDoubleOrNull()?.let { toGrams(it, weightUnitId) }
    val valid = capacityMl != null && capacityMl > 0.0 &&
        (capacityMode != ContainerCapacityMode.CROSS_SECTION || crossSectionProfile != null)
    val current = if (valid) ContainerRecord(
        id = existing?.id.orEmpty(),
        name = name.trim(),
        category = category,
        capacityMode = capacityMode,
        capacityMl = requireNotNull(capacityMl),
        emptyMassGrams = emptyMassGrams?.takeIf { it > 0.0 },
        note = note.trim(),
        imagePaths = imagePaths,
        crossSections = crossSectionProfile?.toDto(),
        scenarioTags = selectedScenarioTags.distinct().filter { it in scenarioTags },
        createdAtMillis = existing?.createdAtMillis ?: System.currentTimeMillis(),
        updatedAtMillis = System.currentTimeMillis(),
    ) else null
    val hasChanges = current != existing
    val saveEnabled = valid && hasChanges

    fun save() {
        current?.let { record ->
            viewModel.upsert(record.copy(id = record.id.ifEmpty { UUID.randomUUID().toString() }))
            onBack()
        }
    }
    fun requestBack() { if (hasChanges) showDiscardDialog = true else onBack() }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        uris.forEach { uri ->
            scope.launch {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                }.getOrNull()?.let { bitmap ->
                    viewModel.saveImage(bitmap)?.let { path ->
                        imagePaths = imagePaths + path
                        imageBitmaps = imageBitmaps + bitmap
                    }
                }
            }
        }
    }

    BackHandler(onBack = ::requestBack)
    BaseScreen(title = stringResource(if (existing == null) R.string.container_add else R.string.container_edit), onBack = ::requestBack) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { EditorSectionTitle(stringResource(R.string.container_section_basic)) }
                item { EditorTextField(stringResource(R.string.container_name), name, { name = it }, required = false) }
                item {
                    AppDropdownField(
                        label = stringResource(R.string.container_category),
                        value = stringResource(category.labelRes()),
                        options = ContainerCategory.entries.map { AppDropdownOption(it.name, stringResource(it.labelRes())) },
                        onSelect = { category = ContainerCategory.valueOf(it.id) },
                    )
                }
                item {
                    Surface(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    ) {
                        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                                ContainerCapacityMode.entries.forEachIndexed { index, mode ->
                                    SegmentedButton(
                                        selected = capacityMode == mode,
                                        onClick = { capacityMode = mode },
                                        shape = SegmentedButtonDefaults.itemShape(index, ContainerCapacityMode.entries.size),
                                        label = { Text(stringResource(if (mode == ContainerCapacityMode.MANUAL) R.string.container_capacity_manual else R.string.container_capacity_cross_section)) },
                                    )
                                }
                            }
                            if (capacityMode == ContainerCapacityMode.MANUAL) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    Column(Modifier.weight(1f)) {
                                        EditorTextField(
                                            label = stringResource(R.string.container_capacity),
                                            value = capacityText,
                                            onValueChange = { capacityText = it },
                                            required = true,
                                            numeric = true,
                                        )
                                    }
                                    AppDropdownField(
                                        label = stringResource(R.string.container_unit),
                                        value = volumeUnitSymbol(volumeUnitId),
                                        options = PRACTICAL_VOLUME_UNITS.map { AppDropdownOption(it, volumeUnitSymbol(it)) },
                                        onSelect = { volumeUnitId = it.id },
                                        modifier = Modifier.weight(0.8f),
                                    )
                                }
                            } else {
                                Text(
                                    stringResource(R.string.container_capacity_cross_section_derived, derivedCapacityMl?.let { "%.1f %s".format(fromMl(it, volumeUnitId), volumeUnitSymbol(volumeUnitId)) } ?: "-"),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                CrossSectionProfileEditor(
                                    initial = crossSectionProfile ?: defaultCrossSectionProfile(),
                                    onProfileChanged = { crossSectionProfile = it },
                                )
                            }
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            EditorTextField(
                                label = stringResource(R.string.container_empty_mass),
                                value = emptyMassText,
                                onValueChange = { emptyMassText = it },
                                required = false,
                                numeric = true,
                            )
                        }
                        AppDropdownField(
                            label = stringResource(R.string.container_unit),
                            value = weightUnitSymbol(weightUnitId),
                            options = PRACTICAL_WEIGHT_UNITS.map { AppDropdownOption(it, weightUnitSymbol(it)) },
                            onSelect = { weightUnitId = it.id },
                            modifier = Modifier.weight(0.8f),
                        )
                    }
                }
                if (scenarioTags.isNotEmpty()) {
                    item { EditorSectionTitle(stringResource(R.string.container_scenario_tags)) }
                    item {
                        Row(
                            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            scenarioTags.forEach { tag ->
                                val selected = tag in selectedScenarioTags
                                Surface(
                                    modifier = Modifier.height(32.dp).clickable { selectedScenarioTags = if (selected) selectedScenarioTags - tag else selectedScenarioTags + tag },
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant),
                                ) {
                                    Box(Modifier.padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
                                        Text(tag, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
                item { EditorSectionTitle(stringResource(R.string.container_section_notes)) }
                item {
                    EditorTextField(
                        label = stringResource(R.string.container_note),
                        value = note,
                        onValueChange = { note = it },
                        required = false,
                        singleLine = false,
                    )
                }
                item { EditorSectionTitle(stringResource(R.string.container_section_images)) }
                item {
                    HorizontalImageEditor(
                        bitmaps = imageBitmaps,
                        onAdd = { galleryLauncher.launch("image/*") },
                        onRemove = { index ->
                            viewModel.deleteImage(imagePaths[index])
                            imagePaths = imagePaths.filterIndexed { i, _ -> i != index }
                            imageBitmaps = imageBitmaps.filterIndexed { i, _ -> i != index }
                        },
                    )
                }
            }
            FormSaveBar(stringResource(R.string.container_save), saveEnabled, ::save)
        }
    }
    if (showDiscardDialog) {
        DiscardChangesDialog(
            onDiscard = onBack,
            onSave = ::save,
            onDismiss = { showDiscardDialog = false },
            saveEnabled = saveEnabled,
        )
    }
}

private fun defaultCrossSectionProfile(): CrossSectionProfile = CrossSectionProfile(
    points = listOf(CrossSection(0.0, CircleShape(10.0))),
    totalHeightCm = 10.0,
)

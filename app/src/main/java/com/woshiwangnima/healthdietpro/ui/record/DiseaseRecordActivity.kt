package com.woshiwangnima.healthdietpro.ui.record

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.base.BaseActivity
import com.woshiwangnima.healthdietpro.common.ui.AnimatedPageContent
import com.woshiwangnima.healthdietpro.common.ui.AppDataTable
import com.woshiwangnima.healthdietpro.common.ui.AppDataTableColumn
import com.woshiwangnima.healthdietpro.common.ui.AppDataTableDeleteAction
import com.woshiwangnima.healthdietpro.common.ui.AppDataTableHeaderText
import com.woshiwangnima.healthdietpro.common.ui.AppDataTableText
import com.woshiwangnima.healthdietpro.common.ui.AppDropdownField
import com.woshiwangnima.healthdietpro.common.ui.AppDropdownOption
import com.woshiwangnima.healthdietpro.common.ui.AppIconTextButton
import com.woshiwangnima.healthdietpro.common.ui.AliasListEditor
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen
import com.woshiwangnima.healthdietpro.common.ui.ColumnWidth
import com.woshiwangnima.healthdietpro.common.ui.ComposeDatePickerDialog
import com.woshiwangnima.healthdietpro.common.ui.RecordTimePickerField
import com.woshiwangnima.healthdietpro.common.time.RecordTimePrecision
import com.woshiwangnima.healthdietpro.common.ui.DetailTabBar
import com.woshiwangnima.healthdietpro.common.ui.DetailTabItem
import com.woshiwangnima.healthdietpro.common.ui.EditorTextField
import com.woshiwangnima.healthdietpro.common.ui.FoodSearchField
import com.woshiwangnima.healthdietpro.common.ui.FormSaveBar
import com.woshiwangnima.healthdietpro.common.ui.HealthDietProTheme
import com.woshiwangnima.healthdietpro.common.ui.TextOverflowText
import com.woshiwangnima.healthdietpro.model.disease.Disease

import com.woshiwangnima.healthdietpro.model.disease.DiseaseCourse
import com.woshiwangnima.healthdietpro.model.disease.DiseaseSourceKind
import com.woshiwangnima.healthdietpro.model.disease.DiseaseStatus
import com.woshiwangnima.healthdietpro.model.disease.DiseaseSubjectType
import com.woshiwangnima.healthdietpro.model.disease.DiseaseReference
import com.woshiwangnima.healthdietpro.model.disease.UserDiseaseRecord
import com.woshiwangnima.healthdietpro.model.disease.UserCustomDisease
import com.woshiwangnima.healthdietpro.model.disease.curatedId
import com.woshiwangnima.healthdietpro.model.disease.customId
import com.woshiwangnima.healthdietpro.model.disease.toCustomDiseaseId
import com.woshiwangnima.healthdietpro.model.profile.Gender
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import java.util.UUID

private enum class DiseaseRoute { HOME, DETAIL, PICKER, EDITOR, CUSTOM_EDITOR }
private enum class DiseaseEditorMode { RECORD, CUSTOM }

class DiseaseRecordActivity : BaseActivity() {
    private val viewModel: DiseaseRecordViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { HealthDietProTheme { DiseaseRecordScreen(viewModel, ::finish) } }
    }
}

@Composable
private fun DiseaseRecordScreen(viewModel: DiseaseRecordViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var route by rememberSaveable { mutableStateOf(DiseaseRoute.HOME) }
    var editing by remember { mutableStateOf<UserDiseaseRecord?>(null) }
    var selectedDisease by remember { mutableStateOf<Disease?>(null) }
    var selectedCustomName by remember { mutableStateOf("") }
    var selectedCustomDiseaseId by remember { mutableStateOf<String?>(null) }
    var editorMode by rememberSaveable { mutableStateOf(DiseaseEditorMode.RECORD) }
    var editingCustom by remember { mutableStateOf<UserCustomDisease?>(null) }
    var detailCustom by remember { mutableStateOf<UserCustomDisease?>(null) }
    var detailDisease by remember { mutableStateOf<Disease?>(null) }
    fun navigateBack() {
        route = when (route) {
            DiseaseRoute.PICKER -> DiseaseRoute.EDITOR
            DiseaseRoute.HOME -> DiseaseRoute.HOME
            DiseaseRoute.DETAIL,
            DiseaseRoute.EDITOR,
            DiseaseRoute.CUSTOM_EDITOR -> DiseaseRoute.HOME
        }
    }
    BackHandler(enabled = route != DiseaseRoute.HOME) { navigateBack() }

    when (route) {
        DiseaseRoute.DETAIL -> {
            detailDisease?.let { DiseaseDetailScreen(it, state.categoryLabels, state.departmentLabels, onBack = { route = DiseaseRoute.HOME }) }
            detailCustom?.let { CustomDiseaseDetailScreen(it, state.categoryLabels, state.departmentLabels, onBack = { route = DiseaseRoute.HOME }, onEdit = { editingCustom = it; route = DiseaseRoute.CUSTOM_EDITOR }) }
        }
        DiseaseRoute.PICKER -> DiseasePickerScreen(
            state = state,
            onBack = ::navigateBack,
            onSelect = { disease -> selectedDisease = disease; selectedCustomName = ""; selectedCustomDiseaseId = null; route = DiseaseRoute.EDITOR },
            onSelectCustom = { id -> selectedDisease = null; selectedCustomName = state.customDiseaseById()[id]?.name.orEmpty(); selectedCustomDiseaseId = id; route = DiseaseRoute.EDITOR },
            onQueryChange = viewModel::setQuery,
            onCategorySelect = viewModel::toggleCategory,
            onCustomToggle = viewModel::toggleCustomOnly,
            onDepartmentToggle = viewModel::toggleDepartment,
            onStatusToggle = viewModel::toggleStatus,
        )
        DiseaseRoute.EDITOR -> DiseaseEditorScreen(
            record = editing,
            selectedDisease = selectedDisease,
            initialCustomName = selectedCustomName,
            selectedCustomDiseaseId = selectedCustomDiseaseId,
            mode = editorMode,
            onSelectDisease = { route = DiseaseRoute.PICKER },
            onBack = { route = DiseaseRoute.HOME },
            onSave = { viewModel.upsert(it) { route = DiseaseRoute.HOME } },
        )
        DiseaseRoute.CUSTOM_EDITOR -> CustomDiseaseEditorScreen(
            disease = editingCustom,
            categoryLabels = state.categoryLabels,
            departmentLabels = state.departmentLabels,
            existingCodes = state.customDiseases.filterNot { it.id == editingCustom?.id }.map { it.code }.toSet(),
            onBack = { route = DiseaseRoute.HOME },
            onSave = { viewModel.upsertCustomDisease(it); route = DiseaseRoute.HOME },
        )
        DiseaseRoute.HOME -> DiseaseHomeScreen(
            state = state,
            selectedTab = tab,
            onTabSelected = { tab = it },
            onBack = onBack,
            onAddEmpty = { editing = null; selectedDisease = null; selectedCustomName = ""; selectedCustomDiseaseId = null; editorMode = DiseaseEditorMode.RECORD; route = DiseaseRoute.EDITOR },
            onEdit = { record -> editing = record; selectedDisease = record.disease.curatedId()?.let { id -> state.diseases.firstOrNull { id in it.referenceIds() } }; selectedCustomDiseaseId = record.disease.customId(); selectedCustomName = record.disease.customId()?.let { state.customDiseaseById()[it]?.name }.orEmpty(); editorMode = DiseaseEditorMode.RECORD; route = DiseaseRoute.EDITOR },
            onDelete = viewModel::delete,
            onAddCustom = { editingCustom = null; route = DiseaseRoute.CUSTOM_EDITOR },
            onOpenDetail = { detailDisease = it; detailCustom = null; route = DiseaseRoute.DETAIL },
            onOpenCustomDetail = { detailCustom = it; detailDisease = null; route = DiseaseRoute.DETAIL },
            onAddDisease = { disease -> editing = null; selectedDisease = disease; selectedCustomName = ""; selectedCustomDiseaseId = null; editorMode = DiseaseEditorMode.RECORD; route = DiseaseRoute.EDITOR },
            onAddCustomRecord = { custom -> editing = null; selectedDisease = null; selectedCustomDiseaseId = custom.id; selectedCustomName = custom.name; editorMode = DiseaseEditorMode.RECORD; route = DiseaseRoute.EDITOR },
            onQueryChange = viewModel::setQuery,
            onCategorySelect = viewModel::toggleCategory,
            onCustomToggle = viewModel::toggleCustomOnly,
            onDepartmentToggle = viewModel::toggleDepartment,
            onStatusToggle = viewModel::toggleStatus,
        )
    }
}

@Composable
private fun DiseaseHomeScreen(
    state: DiseaseRecordUiState,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onBack: () -> Unit,
    onAddEmpty: () -> Unit,
    onEdit: (UserDiseaseRecord) -> Unit,
    onDelete: (String) -> Unit,
    onAddCustom: () -> Unit,
    onOpenDetail: (Disease) -> Unit,
    onOpenCustomDetail: (UserCustomDisease) -> Unit,
    onAddDisease: (Disease) -> Unit,
    onAddCustomRecord: (UserCustomDisease) -> Unit,
    onQueryChange: (String) -> Unit,
    onCategorySelect: (String) -> Unit,
    onCustomToggle: () -> Unit,
    onDepartmentToggle: (String) -> Unit,
    onStatusToggle: (DiseaseStatus) -> Unit,
) {
    val tabs = remember { listOf(DetailTabItem("records", R.string.disease_record_tab_records, R.drawable.ic_list), DetailTabItem("catalog", R.string.disease_record_tab_catalog, R.drawable.ic_medical_history)) }
    BaseScreen(title = stringResource(R.string.disease_record_title), onBack = onBack, includeNavigationBarPadding = false) { padding ->
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(padding)) {
            AnimatedPageContent(selectedTab, Modifier.weight(1f), direction = { from, to -> to - from }) { tab ->
                if (tab == 0) DiseaseRecordsPage(state.records, state.diseases, state.customDiseaseById(), onAddEmpty, onEdit, onDelete)
                else DiseaseBrowsePanel(state, state.userGender, onQueryChange, onCategorySelect, onCustomToggle, onDepartmentToggle, onStatusToggle, onAddCustom, onOpenDetail, onOpenCustomDetail, onAddDisease, onAddCustomRecord = onAddCustomRecord)
            }
            DetailTabBar(tabs, tabs[selectedTab].id) { onTabSelected(tabs.indexOf(it)) }
        }
    }
}

@Composable
private fun DiseaseRecordsPage(records: List<UserDiseaseRecord>, diseases: List<Disease>, customById: Map<String, UserCustomDisease>, onAdd: () -> Unit, onEdit: (UserDiseaseRecord) -> Unit, onDelete: (String) -> Unit) {
    val byId = remember(diseases) { diseases.flatMap { disease -> disease.referenceIds().map { it to disease } }.toMap() }
    var deleting by remember { mutableStateOf<UserDiseaseRecord?>(null) }
    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AppIconTextButton(stringResource(R.string.disease_record_add), R.drawable.ic_add, onAdd)
        if (records.isEmpty()) Text(stringResource(R.string.disease_record_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
        else AppDataTable(
            rows = records,
            rowKey = { _, it -> it.id },
            columns = listOf(
                AppDataTableColumn("disease", { AppDataTableHeaderText(stringResource(R.string.disease_record_disease)) }, ColumnWidth.Flex(1f, 150.dp)) { record -> AppDataTableText(record.disease.curatedId()?.let(byId::get)?.displayName(Locale.getDefault()) ?: record.disease.customId()?.let { customById[it]?.name }.orEmpty()) },
                AppDataTableColumn("subject", { AppDataTableHeaderText(stringResource(R.string.disease_record_subject_type)) }, ColumnWidth.Fixed(110.dp)) { AppDataTableText(stringResource(it.subjectType.labelRes())) },
                AppDataTableColumn("status", { AppDataTableHeaderText(stringResource(R.string.disease_record_status)) }, ColumnWidth.Fixed(110.dp)) { AppDataTableText(stringResource(it.status.labelRes()), color = it.status.color()) },
                AppDataTableColumn("date", { AppDataTableHeaderText(stringResource(R.string.disease_record_diagnosed_on)) }, ColumnWidth.Fixed(120.dp)) { AppDataTableText(it.startedOn ?: "-") },
                AppDataTableColumn("note", { AppDataTableHeaderText(stringResource(R.string.disease_record_note)) }, ColumnWidth.Flex(1f, 120.dp)) { AppDataTableText(it.note.orEmpty()) },
            ),
            actionsHeader = { AppDataTableHeaderText(stringResource(R.string.body_record_delete)) },
            rowActions = { AppDataTableDeleteAction(stringResource(R.string.body_record_delete), onClick = { deleting = it }) },
            onRowClick = onEdit,
            modifier = Modifier.weight(1f),
        )
    }
    deleting?.let { record -> DeleteDiseaseDialog({ deleting = null }) { onDelete(record.id); deleting = null } }
}

@Composable
private fun DiseasePickerScreen(
    state: DiseaseRecordUiState,
    onBack: () -> Unit,
    onSelect: (Disease) -> Unit,
    onSelectCustom: (String) -> Unit,
    onQueryChange: (String) -> Unit,
    onCategorySelect: (String) -> Unit,
    onCustomToggle: () -> Unit,
    onDepartmentToggle: (String) -> Unit,
    onStatusToggle: (DiseaseStatus) -> Unit,
) {
    BaseScreen(title = stringResource(R.string.disease_record_select_disease), onBack = onBack) { padding ->
        DiseaseBrowsePanel(state, state.userGender, onQueryChange, onCategorySelect, onCustomToggle, onDepartmentToggle, onStatusToggle, onAddCustom = {}, onOpenDetail = {}, onOpenCustomDetail = {}, onAddDisease = onSelect, onSelectCustom = onSelectCustom, modifier = Modifier.padding(padding), pickerMode = true)
    }
}

@Composable
internal fun DiseaseBrowsePanel(
    state: DiseaseRecordUiState,
    userGender: Gender,
    onQueryChange: (String) -> Unit,
    onCategorySelect: (String) -> Unit,
    onCustomToggle: () -> Unit,
    onDepartmentToggle: (String) -> Unit,
    onStatusToggle: (DiseaseStatus) -> Unit,
    onAddCustom: () -> Unit,
    onOpenDetail: (Disease) -> Unit,
    onOpenCustomDetail: (UserCustomDisease) -> Unit,
    onAddDisease: (Disease) -> Unit,
    onAddCustomRecord: (UserCustomDisease) -> Unit = {},
    onSelectCustom: (String) -> Unit = {},
    onSelectReference: ((DiseaseReference) -> Unit)? = null,
    excludedReferences: Set<DiseaseReference> = emptySet(),
    modifier: Modifier = Modifier,
    pickerMode: Boolean = false,
) {
    val categories = state.categoryLabels.entries.sortedBy { it.value }
    val departments = state.departmentLabels.entries.sortedBy { it.value }
    val diseases = state.filteredCatalog().filter { disease -> disease.referenceIds().none { id -> DiseaseReference(DiseaseSourceKind.CURATED, id) in excludedReferences } }
    val customDiseases = state.customDiseases.takeIf { state.customOnly }.orEmpty()
        .filter { DiseaseReference(DiseaseSourceKind.CUSTOM, it.id.toCustomDiseaseId()) !in excludedReferences }
    Column(modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FoodSearchField(state.query, onQueryChange, stringResource(R.string.disease_record_search), onSearch = { })
        DiseaseChipRow(stringResource(R.string.disease_record_department), departments.map { it.key to it.value }, state.selectedDepartmentIds, onDepartmentToggle)
        DiseaseChipRow(stringResource(R.string.disease_record_status), DiseaseStatus.entries.map { it.name to stringResource(it.labelRes()) }, state.selectedStatuses.map { it.name }.toSet()) { onStatusToggle(DiseaseStatus.valueOf(it)) }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.width(104.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (!pickerMode) AddCustomDiseaseButton(onAddCustom)
                Surface(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .35f),
                ) {
                    LazyColumn(Modifier.padding(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        item { DiseaseSidebarTag(state.customOnly, onCustomToggle, stringResource(R.string.disease_record_custom_disease_filter)) }
                        item { androidx.compose.foundation.layout.Spacer(Modifier.height(6.dp)) }
                        items(categories, key = { it.key }) { (id, label) -> DiseaseSidebarTag(id in state.selectedCategoryIds, { onCategorySelect(id) }, label) }
                    }
                }
            }
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (state.customOnly && customDiseases.isEmpty()) item { Text(stringResource(R.string.disease_record_no_custom), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                if (!state.customOnly && diseases.isEmpty()) item { Text(stringResource(R.string.disease_record_no_catalog_match), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                items(customDiseases, key = { it.id }) { custom ->
                    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .35f)).clickable { if (pickerMode) onSelectReference?.invoke(DiseaseReference(DiseaseSourceKind.CUSTOM, custom.id.toCustomDiseaseId())) ?: onSelectCustom(custom.id) else onOpenCustomDetail(custom) }.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(Modifier.fillMaxWidth()) {
                            Text(custom.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            if (!pickerMode) IconButton(onClick = { onAddCustomRecord(custom) }, enabled = custom.allows(userGender)) { Icon(painterResource(R.drawable.ic_add), contentDescription = stringResource(R.string.disease_record_add)) }
                        }
                        Text(custom.code, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        if (!pickerMode && custom.description.isNotBlank()) Text(custom.description, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                    }
                }
                items(diseases, key = { it.id }) { disease ->
                    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .35f)).clickable { if (pickerMode) onSelectReference?.invoke(DiseaseReference(DiseaseSourceKind.CURATED, requireNotNull(disease.referenceId()))) ?: onAddDisease(disease) else onOpenDetail(disease) }.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(Modifier.fillMaxWidth()) {
                            Text(disease.displayName(Locale.getDefault()), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            if (!pickerMode) IconButton(onClick = { onAddDisease(disease) }, enabled = disease.applicability.allows(userGender)) { Icon(painterResource(R.drawable.ic_add), contentDescription = stringResource(R.string.disease_record_add)) }
                        }
                        disease.icd11References.firstOrNull()?.let { Text(it.code, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
                        if (!pickerMode) Text(disease.localizedDescription(), style = MaterialTheme.typography.bodySmall, maxLines = 2)
                    }
                }
            }
        }
    }
}

@Composable
private fun AddCustomDiseaseButton(onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(30.dp).clickable(onClick = onClick),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_add),
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            TextOverflowText(
                text = stringResource(R.string.disease_record_add_custom),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun DiseaseSidebarTag(selected: Boolean, onClick: () -> Unit, label: String) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(28.dp).clickable(onClick = onClick),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
    ) {
        Box(Modifier.padding(horizontal = 2.dp), contentAlignment = Alignment.Center) {
            TextOverflowText(label, style = MaterialTheme.typography.labelSmall, color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface, maxLines = 1)
        }
    }
}

@Composable
private fun DiseaseChipRow(title: String, items: List<Pair<String, String>>, selected: Set<String>, onToggle: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 10.dp))
        items.forEach { (id, label) -> FilterChip(id in selected, { onToggle(id) }, label = { Text(label) }) }
    }
}

@Composable
private fun DiseaseDetailScreen(disease: Disease, categoryLabels: Map<String, String>, departmentLabels: Map<String, String>, onBack: () -> Unit) {
    BaseScreen(title = disease.displayName(Locale.getDefault()), onBack = onBack) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { Text(stringResource(R.string.disease_detail_icd11), style = MaterialTheme.typography.titleSmall) }
            items(disease.icd11References) { reference -> Text("${reference.code}  ${reference.title[Locale.getDefault().language] ?: reference.title["en"].orEmpty()}") }
            item { Text(stringResource(R.string.disease_detail_description), style = MaterialTheme.typography.titleSmall) }
            item { Text(disease.localizedDescription(), style = MaterialTheme.typography.bodyLarge) }
            disease.localizedAliases().takeIf { it.isNotEmpty() }?.let { aliases -> item { Text(stringResource(R.string.disease_detail_aliases), style = MaterialTheme.typography.titleSmall); Text(aliases.joinToString(" / ")) } }
            disease.categoryIds.takeIf { it.isNotEmpty() }?.let { ids -> item { Text(stringResource(R.string.disease_record_category), style = MaterialTheme.typography.titleSmall); Text(ids.map { categoryLabels[it] ?: it }.joinToString(" / ")) } }
            disease.careDepartmentIds.takeIf { it.isNotEmpty() }?.let { ids -> item { Text(stringResource(R.string.disease_detail_departments), style = MaterialTheme.typography.titleSmall); Text(ids.map { departmentLabels[it] ?: it }.joinToString(" / ")) } }
        }
    }
}

@Composable
private fun CustomDiseaseDetailScreen(
    disease: UserCustomDisease,
    categoryLabels: Map<String, String>,
    departmentLabels: Map<String, String>,
    onBack: () -> Unit,
    onEdit: () -> Unit,
) {
    BaseScreen(
        title = disease.name,
        onBack = onBack,
        actions = {
            IconButton(onClick = onEdit) {
                Icon(painterResource(R.drawable.ic_edit), contentDescription = stringResource(R.string.disease_record_edit))
            }
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { Text(stringResource(R.string.disease_custom_code), style = MaterialTheme.typography.titleSmall) }
            item { Text(disease.code, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary) }
            disease.description.takeIf { it.isNotBlank() }?.let { description ->
                item { Text(stringResource(R.string.disease_detail_description), style = MaterialTheme.typography.titleSmall) }
                item { Text(description, style = MaterialTheme.typography.bodyLarge) }
            }
            disease.aliases.takeIf { it.isNotEmpty() }?.let { aliases ->
                item { Text(stringResource(R.string.disease_detail_aliases), style = MaterialTheme.typography.titleSmall) }
                item { Text(aliases.joinToString(" / ")) }
            }
            disease.categoryIds.takeIf { it.isNotEmpty() }?.let { ids ->
                item { Text(stringResource(R.string.disease_record_category), style = MaterialTheme.typography.titleSmall) }
                item { Text(ids.map { categoryLabels[it] ?: it }.joinToString(" / ")) }
            }
            disease.careDepartmentIds.takeIf { it.isNotEmpty() }?.let { ids ->
                item { Text(stringResource(R.string.disease_detail_departments), style = MaterialTheme.typography.titleSmall) }
                item { Text(ids.map { departmentLabels[it] ?: it }.joinToString(" / ")) }
            }
            disease.note.takeIf { it.isNotBlank() }?.let { note ->
                item { Text(stringResource(R.string.disease_record_note), style = MaterialTheme.typography.titleSmall) }
                item { Text(note) }
            }
        }
    }
}

@Composable
private fun DiseaseEditorScreen(record: UserDiseaseRecord?, selectedDisease: Disease?, initialCustomName: String, selectedCustomDiseaseId: String?, mode: DiseaseEditorMode, onSelectDisease: () -> Unit, onBack: () -> Unit, onSave: (UserDiseaseRecord) -> Unit) {
    var subjectType by remember(record?.id) { mutableStateOf(record?.subjectType ?: DiseaseSubjectType.SELF) }
    var status by remember(record?.id) { mutableStateOf(record?.status ?: DiseaseStatus.HISTORY_ONLY) }
    var course by remember(record?.id) { mutableStateOf(record?.course ?: DiseaseCourse.UNKNOWN) }
    var startedOn by remember(record?.id) { mutableStateOf(record?.startedOn) }
    var endedOn by remember(record?.id) { mutableStateOf(record?.endedOn) }
    var subjectNote by remember(record?.id) { mutableStateOf(record?.subjectNote.orEmpty()) }
    var institution by remember(record?.id) { mutableStateOf(record?.medicalInstitution.orEmpty()) }
    var doctor by remember(record?.id) { mutableStateOf(record?.doctorName.orEmpty()) }
    var phone by remember(record?.id) { mutableStateOf(record?.doctorPhone.orEmpty()) }
    var note by remember(record?.id) { mutableStateOf(record?.note.orEmpty()) }
    var pickingStarted by remember { mutableStateOf(false) }
    var pickingEnded by remember { mutableStateOf(false) }
    var dateWarning by remember { mutableStateOf(false) }
    val curatedId = selectedDisease?.referenceId() ?: record?.disease?.curatedId()
    val customId = selectedCustomDiseaseId?.toCustomDiseaseId() ?: record?.disease?.customId()
    val valid = curatedId != null || customId != null
    val dirty = record == null || subjectType != record.subjectType || status != record.status || course != record.course || startedOn != record.startedOn || endedOn != record.endedOn || subjectNote != record.subjectNote.orEmpty() || institution != record.medicalInstitution.orEmpty() || doctor != record.doctorName.orEmpty() || phone != record.doctorPhone.orEmpty() || note != record.note.orEmpty()
    fun commit() {
        val now = System.currentTimeMillis()
        val reference = curatedId?.let { DiseaseReference(DiseaseSourceKind.CURATED, it) } ?: DiseaseReference(DiseaseSourceKind.CUSTOM, requireNotNull(customId))
        onSave(UserDiseaseRecord(record?.id ?: UUID.randomUUID().toString(), reference, subjectType, subjectNote.trim().ifBlank { null }.takeIf { subjectType != DiseaseSubjectType.SELF }, status, course, startedOn, endedOn, institution.trim().ifBlank { null }, doctor.trim().ifBlank { null }, phone.trim().ifBlank { null }, note.trim().ifBlank { null }, record?.createdAt ?: now, now))
    }
    fun save() {
        if (startedOn != null && endedOn != null && endedOn!! < startedOn!!) dateWarning = true else commit()
    }
    BaseScreen(title = stringResource(if (record == null) R.string.disease_record_add else R.string.disease_record_edit), onBack = onBack) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (curatedId != null) item { AppDropdownField(stringResource(R.string.disease_record_selected_disease), selectedDisease?.displayName(Locale.getDefault()) ?: curatedId, emptyList(), onSelect = {}, enabled = false) }
                if (customId != null) item { AppDropdownField(stringResource(R.string.disease_record_selected_disease), initialCustomName, emptyList(), onSelect = {}, enabled = false) }
                if (!valid) item { AppIconTextButton(stringResource(R.string.disease_record_select_disease), R.drawable.ic_medical_history, onSelectDisease, Modifier.fillMaxWidth()) }
                item { AppDropdownField(stringResource(R.string.disease_record_subject_type), stringResource(subjectType.labelRes()), DiseaseSubjectType.entries.map { AppDropdownOption(it.name, stringResource(it.labelRes())) }, { subjectType = DiseaseSubjectType.valueOf(it.id) }) }
                if (subjectType != DiseaseSubjectType.SELF) item { EditorTextField(stringResource(R.string.disease_record_subject_note), subjectNote, { subjectNote = it }) }
                item { AppDropdownField(stringResource(R.string.disease_record_status), stringResource(status.labelRes()), DiseaseStatus.entries.map { AppDropdownOption(it.name, stringResource(it.labelRes())) }, { status = DiseaseStatus.valueOf(it.id) }) }
                item { AppDropdownField(stringResource(R.string.disease_record_duration), stringResource(course.labelRes()), DiseaseCourse.entries.map { AppDropdownOption(it.name, stringResource(it.labelRes())) }, { course = DiseaseCourse.valueOf(it.id) }) }
                item { DateField(stringResource(R.string.disease_record_diagnosed_on), startedOn) { pickingStarted = true } }
                item { DateField(stringResource(R.string.disease_record_resolved_on), endedOn) { pickingEnded = true } }
                item { EditorTextField(stringResource(R.string.disease_record_facility), institution, { institution = it }) }
                item { EditorTextField(stringResource(R.string.disease_record_clinician), doctor, { doctor = it }) }
                item { EditorTextField(stringResource(R.string.disease_record_doctor_phone), phone, { phone = it }) }
                item { EditorTextField(stringResource(R.string.disease_record_note), note, { note = it }, singleLine = false) }
            }
            FormSaveBar(stringResource(R.string.disease_record_save), valid && dirty, onSave = ::save)
        }
    }
    if (pickingStarted) ComposeDatePickerDialog((startedOn?.let(LocalDate::parse) ?: LocalDate.now()).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(), { pickingStarted = false }) { startedOn = it.toString(); pickingStarted = false }
    if (pickingEnded) ComposeDatePickerDialog((endedOn?.let(LocalDate::parse) ?: LocalDate.now()).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(), { pickingEnded = false }) { endedOn = it.toString(); pickingEnded = false }
    if (dateWarning) AlertDialog(onDismissRequest = { dateWarning = false }, title = { Text(stringResource(R.string.disease_record_invalid_date_order_title)) }, text = { Text(stringResource(R.string.disease_record_invalid_date_order)) }, confirmButton = { TextButton(onClick = { dateWarning = false; commit() }) { Text(stringResource(R.string.compose_confirm_dialog_ok)) } }, dismissButton = { TextButton(onClick = { dateWarning = false }) { Text(stringResource(R.string.compose_confirm_dialog_cancel)) } })
}
@Composable
private fun CustomDiseaseEditorScreen(
    disease: UserCustomDisease?, categoryLabels: Map<String, String>, departmentLabels: Map<String, String>, existingCodes: Set<String>, onBack: () -> Unit, onSave: (UserCustomDisease) -> Unit,
) {
    var name by remember(disease?.id) { mutableStateOf(disease?.name.orEmpty()) }
    val generatedCode = remember(disease?.id) { disease?.code ?: "CUSTOM-${UUID.randomUUID().toString().take(8).uppercase(Locale.ROOT)}" }
    var aliases by remember(disease?.id) { mutableStateOf(disease?.aliases ?: emptyList()) }
    var description by remember(disease?.id) { mutableStateOf(disease?.description.orEmpty()) }
    var applicableGenders by remember(disease?.id) { mutableStateOf(disease?.applicableGenders ?: emptyList()) }
    var categoryIds by remember(disease?.id) { mutableStateOf(disease?.categoryIds ?: emptyList()) }
    var departmentIds by remember(disease?.id) { mutableStateOf(disease?.careDepartmentIds ?: emptyList()) }
    var note by remember(disease?.id) { mutableStateOf(disease?.note.orEmpty()) }
    var discardDialog by remember { mutableStateOf(false) }
    val codeDuplicate = existingCodes.any { it.equals(generatedCode, ignoreCase = true) }
    val dirty = disease == null || name != disease.name || aliases != disease.aliases || description != disease.description || applicableGenders != disease.applicableGenders || categoryIds != disease.categoryIds || departmentIds != disease.careDepartmentIds || note != disease.note
    fun requestBack() { if (dirty) discardDialog = true else onBack() }
    BackHandler { requestBack() }
    BaseScreen(title = stringResource(if (disease == null) R.string.disease_record_add_custom else R.string.disease_record_edit), onBack = ::requestBack) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { EditorTextField(stringResource(R.string.disease_record_custom_disease), name, { name = it }, required = true) }
                item { AppDropdownField(stringResource(R.string.disease_custom_code), generatedCode, emptyList(), onSelect = {}, enabled = false) }
                if (codeDuplicate) item { Text(stringResource(R.string.disease_custom_code_duplicate), color = MaterialTheme.colorScheme.error) }
                item { AliasListEditor(aliases, stringResource(R.string.disease_custom_aliases), stringResource(R.string.disease_custom_add_alias), { aliases = it }) }
                item { EditorTextField(stringResource(R.string.disease_custom_description), description, { description = it }, singleLine = false) }
                item { CustomDiseaseApplicabilitySelector(applicableGenders) { applicableGenders = it } }
                item { CustomDiseaseMultiSelect(stringResource(R.string.disease_record_category), categoryLabels, categoryIds) { id -> categoryIds = categoryIds.toggle(id) } }
                item { CustomDiseaseMultiSelect(stringResource(R.string.disease_record_department), departmentLabels, departmentIds) { id -> departmentIds = departmentIds.toggle(id) } }
                item { EditorTextField(stringResource(R.string.disease_record_note), note, { note = it }, singleLine = false) }
            }
            FormSaveBar(stringResource(R.string.disease_record_save), name.isNotBlank() && !codeDuplicate && dirty, onSave = {
                val now = System.currentTimeMillis()
                onSave(UserCustomDisease(disease?.id ?: "CUSTOM-${UUID.randomUUID().toString().take(8).uppercase(Locale.ROOT)}", name.trim(), generatedCode, aliases, description.trim(), applicableGenders, categoryIds, departmentIds, note.trim(), disease?.createdAt ?: now, now))
            })
        }
    }
    if (discardDialog) AlertDialog(onDismissRequest = { discardDialog = false }, title = { Text(stringResource(R.string.form_discard_changes_title)) }, text = { Text(stringResource(R.string.form_discard_changes_message)) }, confirmButton = { TextButton(onClick = { discardDialog = false; onBack() }) { Text(stringResource(R.string.form_discard_changes_confirm)) } }, dismissButton = { TextButton(onClick = { discardDialog = false }) { Text(stringResource(R.string.form_discard_changes_cancel)) } })
}

@Composable
private fun CustomDiseaseApplicabilitySelector(selected: List<Gender>, onChange: (List<Gender>) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.disease_applicability), style = MaterialTheme.typography.labelLarge)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(selected.isEmpty(), { onChange(emptyList()) }, label = { Text(stringResource(R.string.disease_applicability_all)) })
            Gender.entries.forEach { gender -> FilterChip(gender in selected, { onChange(selected.toggle(gender)) }, label = { Text(stringResource(gender.labelRes())) }) }
        }
    }
}
@Composable
private fun CustomDiseaseMultiSelect(title: String, labels: Map<String, String>, selected: List<String>, onToggle: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            labels.entries.sortedBy { it.value }.forEach { (id, label) -> FilterChip(id in selected, { onToggle(id) }, label = { Text(label) }) }
        }
    }
}
@Composable private fun DateField(label: String, value: String?, onClick: () -> Unit) {
    val timestamp = value?.let { runCatching { LocalDate.parse(it) }.getOrNull() }?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
    RecordTimePickerField(
        title = label,
        valueMillis = timestamp,
        precision = RecordTimePrecision.DATE,
        emptyText = stringResource(R.string.disease_record_date_not_set),
        onClick = onClick,
    )
}
@Composable private fun DeleteDiseaseDialog(onDismiss: () -> Unit, onDelete: () -> Unit) { AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(R.string.body_record_delete_confirm_title)) }, text = { Text(stringResource(R.string.body_record_delete_confirm_message)) }, confirmButton = { TextButton(onClick = onDelete) { Text(stringResource(R.string.body_record_delete)) } }, dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.compose_confirm_dialog_cancel)) } }) }
private fun Disease.localizedDescription(): String = i18n[Locale.getDefault().language]?.description ?: i18n["zh"]?.description.orEmpty()
private fun Disease.localizedAliases(): List<String> = i18n[Locale.getDefault().language]?.aliases ?: i18n["zh"]?.aliases.orEmpty()
@Composable private fun DiseaseSubjectType.labelRes(): Int = when (this) { DiseaseSubjectType.SELF -> R.string.disease_subject_self; DiseaseSubjectType.FATHER -> R.string.disease_subject_father; DiseaseSubjectType.MOTHER -> R.string.disease_subject_mother; DiseaseSubjectType.OLDER_BROTHER -> R.string.disease_subject_older_brother; DiseaseSubjectType.YOUNGER_BROTHER -> R.string.disease_subject_younger_brother; DiseaseSubjectType.OLDER_SISTER -> R.string.disease_subject_older_sister; DiseaseSubjectType.YOUNGER_SISTER -> R.string.disease_subject_younger_sister; DiseaseSubjectType.HUSBAND -> R.string.disease_subject_husband; DiseaseSubjectType.WIFE -> R.string.disease_subject_wife; DiseaseSubjectType.FRIEND -> R.string.disease_subject_friend; DiseaseSubjectType.CLASSMATE -> R.string.disease_subject_classmate; DiseaseSubjectType.OTHER -> R.string.disease_subject_other }
@Composable private fun DiseaseStatus.labelRes(): Int = when (this) { DiseaseStatus.ACTIVE -> R.string.disease_status_active; DiseaseStatus.RESOLVED -> R.string.disease_status_resolved; DiseaseStatus.ONGOING_RISK -> R.string.disease_status_ongoing_risk; DiseaseStatus.HISTORY_ONLY -> R.string.disease_status_history_only }
@Composable private fun DiseaseCourse.labelRes(): Int = when (this) { DiseaseCourse.ACUTE -> R.string.disease_course_acute; DiseaseCourse.CHRONIC -> R.string.disease_course_chronic; DiseaseCourse.EPISODIC -> R.string.disease_course_episodic; DiseaseCourse.UNKNOWN -> R.string.disease_course_unknown }
private fun DiseaseStatus.color() = when (this) { DiseaseStatus.ACTIVE -> androidx.compose.ui.graphics.Color(0xFFE53935); DiseaseStatus.RESOLVED -> androidx.compose.ui.graphics.Color(0xFF43A047); DiseaseStatus.ONGOING_RISK -> androidx.compose.ui.graphics.Color(0xFFF57C00); DiseaseStatus.HISTORY_ONLY -> androidx.compose.ui.graphics.Color(0xFF607D8B) }
private fun <T> List<T>.toggle(value: T): List<T> = if (value in this) this - value else this + value
private fun UserCustomDisease.allows(gender: Gender): Boolean = applicableGenders.isEmpty() || gender in applicableGenders
@Composable private fun Gender.labelRes(): Int = when (this) { Gender.MALE -> R.string.profile_gender_male; Gender.FEMALE -> R.string.profile_gender_female }







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

import com.woshiwangnima.healthdietpro.model.disease.DiseaseDurationKind
import com.woshiwangnima.healthdietpro.model.disease.DiseaseHistoryType
import com.woshiwangnima.healthdietpro.model.disease.DiseaseRecordStatus
import com.woshiwangnima.healthdietpro.model.disease.DiseaseReference
import com.woshiwangnima.healthdietpro.model.disease.FamilyRelation
import com.woshiwangnima.healthdietpro.model.disease.UserDiseaseRecord
import com.woshiwangnima.healthdietpro.model.disease.UserCustomDisease
import com.woshiwangnima.healthdietpro.model.disease.allowedStatuses
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
            onSave = { viewModel.upsert(it); route = DiseaseRoute.HOME },
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
            onEdit = { record -> editing = record; selectedDisease = record.disease.curatedDiseaseId?.let { id -> state.diseases.firstOrNull { it.id == id } }; selectedCustomDiseaseId = record.disease.customDiseaseId; selectedCustomName = record.disease.customDiseaseId?.let { state.customDiseaseById()[it]?.name }.orEmpty(); editorMode = DiseaseEditorMode.RECORD; route = DiseaseRoute.EDITOR },
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
    onStatusToggle: (DiseaseRecordStatus) -> Unit,
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
    val byId = remember(diseases) { diseases.associateBy { it.id } }
    var deleting by remember { mutableStateOf<UserDiseaseRecord?>(null) }
    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AppIconTextButton(stringResource(R.string.disease_record_add), R.drawable.ic_add, onAdd)
        if (records.isEmpty()) Text(stringResource(R.string.disease_record_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
        else AppDataTable(
            rows = records,
            rowKey = { _, it -> it.id },
            columns = listOf(
                AppDataTableColumn("disease", { AppDataTableHeaderText(stringResource(R.string.disease_record_disease)) }, ColumnWidth.Flex(1f, 150.dp)) { record -> AppDataTableText(record.disease.curatedDiseaseId?.let(byId::get)?.displayName(Locale.getDefault()) ?: record.disease.customDiseaseId?.let { customById[it]?.name }.orEmpty()) },
                AppDataTableColumn("history", { AppDataTableHeaderText(stringResource(R.string.disease_record_history_type)) }, ColumnWidth.Fixed(110.dp)) { AppDataTableText(stringResource(it.historyType.labelRes())) },
                AppDataTableColumn("status", { AppDataTableHeaderText(stringResource(R.string.disease_record_status)) }, ColumnWidth.Fixed(110.dp)) { AppDataTableText(stringResource(it.status.labelRes()), color = it.status.color()) },
                AppDataTableColumn("date", { AppDataTableHeaderText(stringResource(R.string.disease_record_diagnosed_on)) }, ColumnWidth.Fixed(120.dp)) { AppDataTableText(it.diagnosedOn ?: "-") },
                AppDataTableColumn("note", { AppDataTableHeaderText(stringResource(R.string.disease_record_note)) }, ColumnWidth.Flex(1f, 120.dp)) { AppDataTableText(it.note) },
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
    onStatusToggle: (DiseaseRecordStatus) -> Unit,
) {
    BaseScreen(title = stringResource(R.string.disease_record_select_disease), onBack = onBack) { padding ->
        DiseaseBrowsePanel(state, state.userGender, onQueryChange, onCategorySelect, onCustomToggle, onDepartmentToggle, onStatusToggle, onAddCustom = {}, onOpenDetail = {}, onOpenCustomDetail = {}, onAddDisease = onSelect, onSelectCustom = onSelectCustom, modifier = Modifier.padding(padding), pickerMode = true)
    }
}

@Composable
private fun DiseaseBrowsePanel(
    state: DiseaseRecordUiState,
    userGender: Gender,
    onQueryChange: (String) -> Unit,
    onCategorySelect: (String) -> Unit,
    onCustomToggle: () -> Unit,
    onDepartmentToggle: (String) -> Unit,
    onStatusToggle: (DiseaseRecordStatus) -> Unit,
    onAddCustom: () -> Unit,
    onOpenDetail: (Disease) -> Unit,
    onOpenCustomDetail: (UserCustomDisease) -> Unit,
    onAddDisease: (Disease) -> Unit,
    onAddCustomRecord: (UserCustomDisease) -> Unit = {},
    onSelectCustom: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    pickerMode: Boolean = false,
) {
    val categories = state.categoryLabels.entries.sortedBy { it.value }
    val departments = state.departmentLabels.entries.sortedBy { it.value }
    val diseases = state.filteredCatalog()
    Column(modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FoodSearchField(state.query, onQueryChange, stringResource(R.string.disease_record_search), onSearch = { })
        DiseaseChipRow(stringResource(R.string.disease_record_department), departments.map { it.key to it.value }, state.selectedDepartmentIds, onDepartmentToggle)
        DiseaseChipRow(stringResource(R.string.disease_record_status), DiseaseRecordStatus.entries.map { it.name to stringResource(it.labelRes()) }, state.selectedStatuses.map { it.name }.toSet()) { onStatusToggle(DiseaseRecordStatus.valueOf(it)) }
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
                if (state.customOnly && state.customDiseases.isEmpty()) item { Text(stringResource(R.string.disease_record_no_custom), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                if (!state.customOnly && diseases.isEmpty()) item { Text(stringResource(R.string.disease_record_no_catalog_match), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                items(state.customDiseases.takeIf { state.customOnly }.orEmpty(), key = { it.id }) { custom ->
                    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .35f)).clickable { if (pickerMode) onSelectCustom(custom.id) else onOpenCustomDetail(custom) }.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(Modifier.fillMaxWidth()) {
                            Text(custom.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            if (!pickerMode) IconButton(onClick = { onAddCustomRecord(custom) }, enabled = custom.allows(userGender)) { Icon(painterResource(R.drawable.ic_add), contentDescription = stringResource(R.string.disease_record_add)) }
                        }
                        Text(custom.code, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        if (!pickerMode && custom.description.isNotBlank()) Text(custom.description, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                    }
                }
                items(diseases, key = { it.id }) { disease ->
                    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .35f)).clickable { if (pickerMode) onAddDisease(disease) else onOpenDetail(disease) }.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
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
    var historyType by rememberSaveable(record?.id) { mutableStateOf(record?.historyType ?: DiseaseHistoryType.SELF) }
    var status by rememberSaveable(record?.id) { mutableStateOf(record?.status ?: DiseaseRecordStatus.ACTIVE) }
    var duration by rememberSaveable(record?.id) { mutableStateOf(record?.durationKind ?: DiseaseDurationKind.UNKNOWN) }
    var diagnosedOn by rememberSaveable(record?.id) { mutableStateOf(record?.diagnosedOn ?: if (record == null) LocalDate.now().toString() else null) }
    var resolvedOn by rememberSaveable(record?.id) { mutableStateOf(record?.resolvedOn) }
    var relation by rememberSaveable(record?.id) { mutableStateOf(record?.familyRelation) }
    var facility by rememberSaveable(record?.id) { mutableStateOf(record?.careFacility.orEmpty()) }
    var clinician by rememberSaveable(record?.id) { mutableStateOf(record?.clinicianName.orEmpty()) }
    var note by rememberSaveable(record?.id) { mutableStateOf(record?.note.orEmpty()) }
    var pickingDiagnosis by rememberSaveable { mutableStateOf(false) }
    var pickingResolution by rememberSaveable { mutableStateOf(false) }
    val curatedId = selectedDisease?.id ?: record?.disease?.curatedDiseaseId
    val customDiseaseId = selectedCustomDiseaseId ?: record?.disease?.customDiseaseId
    val allowed = allowedStatuses(historyType)
    if (status !in allowed) status = allowed.first()
    val valid = (curatedId != null || customDiseaseId != null) && (status != DiseaseRecordStatus.RESOLVED || resolvedOn != null) && (historyType != DiseaseHistoryType.FAMILY || relation != null)
    val dirty = record == null || historyType != record.historyType || status != record.status || duration != record.durationKind || diagnosedOn != record.diagnosedOn || resolvedOn != record.resolvedOn || relation != record.familyRelation || facility != record.careFacility || clinician != record.clinicianName || note != record.note || curatedId != record.disease.curatedDiseaseId
    BaseScreen(title = stringResource(if (record == null) R.string.disease_record_add else R.string.disease_record_edit), onBack = onBack) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (curatedId != null) item { AppDropdownField(stringResource(R.string.disease_record_selected_disease), selectedDisease?.displayName(Locale.getDefault()) ?: curatedId, emptyList(), onSelect = {}, enabled = false) }
                if (customDiseaseId != null) item { AppDropdownField(stringResource(R.string.disease_record_selected_disease), initialCustomName, emptyList(), onSelect = {}, enabled = false) }
                if (curatedId == null && customDiseaseId == null) item { AppIconTextButton(stringResource(R.string.disease_record_select_disease), R.drawable.ic_medical_history, onSelectDisease, Modifier.fillMaxWidth()) }
                item { AppDropdownField(stringResource(R.string.disease_record_history_type), stringResource(historyType.labelRes()), DiseaseHistoryType.entries.map { AppDropdownOption(it.name, stringResource(it.labelRes())) }, { historyType = DiseaseHistoryType.valueOf(it.id) }) }
                item { AppDropdownField(stringResource(R.string.disease_record_status), stringResource(status.labelRes()), allowed.map { AppDropdownOption(it.name, stringResource(it.labelRes())) }, { status = DiseaseRecordStatus.valueOf(it.id) }) }
                item { AppDropdownField(stringResource(R.string.disease_record_duration), stringResource(duration.labelRes()), DiseaseDurationKind.entries.map { AppDropdownOption(it.name, stringResource(it.labelRes())) }, { duration = DiseaseDurationKind.valueOf(it.id) }) }
                if (historyType == DiseaseHistoryType.FAMILY) item { AppDropdownField(stringResource(R.string.disease_record_family_relation), relation?.let { stringResource(it.labelRes()) }.orEmpty(), FamilyRelation.entries.map { AppDropdownOption(it.name, stringResource(it.labelRes())) }, { relation = FamilyRelation.valueOf(it.id) }) }
                if (historyType != DiseaseHistoryType.FAMILY) { item { DateField(stringResource(R.string.disease_record_diagnosed_on), diagnosedOn) { pickingDiagnosis = true } }; item { EditorTextField(stringResource(R.string.disease_record_facility), facility, { facility = it }) }; item { EditorTextField(stringResource(R.string.disease_record_clinician), clinician, { clinician = it }) } }
                if (status == DiseaseRecordStatus.RESOLVED) item { DateField(stringResource(R.string.disease_record_resolved_on), resolvedOn) { pickingResolution = true } }
                item { EditorTextField(stringResource(R.string.disease_record_note), note, { note = it }, singleLine = false) }
            }
            FormSaveBar(stringResource(R.string.disease_record_save), valid && dirty, onSave = { val now = System.currentTimeMillis(); onSave(UserDiseaseRecord(record?.id ?: UUID.randomUUID().toString(), DiseaseReference(curatedId, customDiseaseId), historyType, status, duration, diagnosedOn, resolvedOn, if (historyType == DiseaseHistoryType.FAMILY) relation else null, facility.trim(), clinician.trim(), note.trim(), record?.createdAt ?: now, now)) })
        }
    }
    if (pickingDiagnosis) ComposeDatePickerDialog((diagnosedOn?.let(LocalDate::parse) ?: LocalDate.now()).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(), { pickingDiagnosis = false }) { diagnosedOn = it.toString(); pickingDiagnosis = false }
    if (pickingResolution) ComposeDatePickerDialog((resolvedOn?.let(LocalDate::parse) ?: LocalDate.now()).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(), { pickingResolution = false }) { resolvedOn = it.toString(); pickingResolution = false }
}
@Composable
private fun CustomDiseaseEditorScreen(
    disease: UserCustomDisease?, categoryLabels: Map<String, String>, departmentLabels: Map<String, String>, existingCodes: Set<String>, onBack: () -> Unit, onSave: (UserCustomDisease) -> Unit,
) {
    var name by rememberSaveable(disease?.id) { mutableStateOf(disease?.name.orEmpty()) }
    val generatedCode = remember(disease?.id) { disease?.code ?: "CUSTOM-${UUID.randomUUID().toString().take(8).uppercase(Locale.ROOT)}" }
    var aliases by rememberSaveable(disease?.id) { mutableStateOf(disease?.aliases ?: emptyList()) }
    var description by rememberSaveable(disease?.id) { mutableStateOf(disease?.description.orEmpty()) }
    var applicableGenders by rememberSaveable(disease?.id) { mutableStateOf(disease?.applicableGenders ?: emptyList()) }
    var categoryIds by rememberSaveable(disease?.id) { mutableStateOf(disease?.categoryIds ?: emptyList()) }
    var departmentIds by rememberSaveable(disease?.id) { mutableStateOf(disease?.careDepartmentIds ?: emptyList()) }
    var note by rememberSaveable(disease?.id) { mutableStateOf(disease?.note.orEmpty()) }
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
                onSave(UserCustomDisease(disease?.id ?: "custom:${UUID.randomUUID()}", name.trim(), generatedCode, aliases, description.trim(), applicableGenders, categoryIds, departmentIds, note.trim(), disease?.createdAt ?: now, now))
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
    val timestamp = value?.let { LocalDate.parse(it).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() }
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
@Composable private fun DiseaseHistoryType.labelRes(): Int = when (this) { DiseaseHistoryType.SELF -> R.string.disease_history_self; DiseaseHistoryType.FAMILY -> R.string.disease_history_family; DiseaseHistoryType.PAST -> R.string.disease_history_past; DiseaseHistoryType.RISK -> R.string.disease_history_risk }
@Composable private fun DiseaseRecordStatus.labelRes(): Int = when (this) { DiseaseRecordStatus.ACTIVE -> R.string.disease_status_active; DiseaseRecordStatus.RESOLVED -> R.string.disease_status_resolved; DiseaseRecordStatus.ONGOING_RISK -> R.string.disease_status_ongoing_risk; DiseaseRecordStatus.HISTORY_ONLY -> R.string.disease_status_history_only }
@Composable private fun DiseaseDurationKind.labelRes(): Int = when (this) { DiseaseDurationKind.SHORT_TERM -> R.string.disease_duration_short; DiseaseDurationKind.LONG_TERM -> R.string.disease_duration_long; DiseaseDurationKind.UNKNOWN -> R.string.disease_duration_unknown }
@Composable private fun FamilyRelation.labelRes(): Int = when (this) { FamilyRelation.PARENT -> R.string.disease_relation_parent; FamilyRelation.SIBLING -> R.string.disease_relation_sibling; FamilyRelation.CHILD -> R.string.disease_relation_child; FamilyRelation.GRANDPARENT -> R.string.disease_relation_grandparent; FamilyRelation.OTHER -> R.string.disease_relation_other }
private fun DiseaseRecordStatus.color() = when (this) { DiseaseRecordStatus.ACTIVE -> androidx.compose.ui.graphics.Color(0xFFE53935); DiseaseRecordStatus.RESOLVED -> androidx.compose.ui.graphics.Color(0xFF43A047); DiseaseRecordStatus.ONGOING_RISK -> androidx.compose.ui.graphics.Color(0xFFF57C00); DiseaseRecordStatus.HISTORY_ONLY -> androidx.compose.ui.graphics.Color(0xFF607D8B) }
private fun <T> List<T>.toggle(value: T): List<T> = if (value in this) this - value else this + value
private fun UserCustomDisease.allows(gender: Gender): Boolean = applicableGenders.isEmpty() || gender in applicableGenders
@Composable private fun Gender.labelRes(): Int = when (this) { Gender.MALE -> R.string.profile_gender_male; Gender.FEMALE -> R.string.profile_gender_female }







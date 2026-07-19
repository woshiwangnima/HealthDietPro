package com.woshiwangnima.healthdietpro.ui.record

import android.app.Application
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.base.BaseActivity
import com.woshiwangnima.healthdietpro.common.ui.HealthDietProTheme
import com.woshiwangnima.healthdietpro.model.medication.MedicationPrefs
import com.woshiwangnima.healthdietpro.model.medication.MedicationCatalogItem
import com.woshiwangnima.healthdietpro.model.medication.MedicationRecord
import com.woshiwangnima.healthdietpro.model.medication.removeRecordById
import com.woshiwangnima.healthdietpro.model.prefs.AppPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MedicationListActivity : BaseActivity() {

    private val viewModel: MedicationListViewModel by viewModels {
        MedicationListViewModel.Factory(application)
    }

    private val medicationLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            viewModel.refresh()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HealthDietProTheme {
                MedicationListRoute(
                    viewModel = viewModel,
                    onBack = ::finish,
                    onAddRecord = { openRecord(null) },
                    onEditRecord = ::openRecord,
                    onAddCatalogItem = { openCatalogItem(null) },
                    onEditCatalogItem = ::openCatalogItem,
                    onDeleteCatalogItem = viewModel::deleteCatalogItem,
                )
            }
        }
    }

    private fun openRecord(record: MedicationRecord?) {
        medicationLauncher.launch(
            Intent(this, MedicationRecordActivity::class.java).apply {
                record?.let { putExtra(MedicationRecordActivity.EXTRA_RECORD_ID, it.id) }
            },
        )
    }

    private fun openCatalogItem(item: MedicationCatalogItem?) {
        medicationLauncher.launch(
            Intent(this, MedicationCatalogActivity::class.java).apply {
                item?.let { putExtra(MedicationCatalogActivity.EXTRA_CATALOG_ID, it.id) }
            },
        )
    }
}

@Composable
private fun MedicationListRoute(
    viewModel: MedicationListViewModel,
    onBack: () -> Unit,
    onAddRecord: () -> Unit,
    onEditRecord: (MedicationRecord) -> Unit,
    onAddCatalogItem: () -> Unit,
    onEditCatalogItem: (MedicationCatalogItem) -> Unit,
    onDeleteCatalogItem: (MedicationCatalogItem) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MedicationListScreen(
        uiState = uiState,
        title = stringResource(R.string.medication_record_title),
        onBack = onBack,
        onTabSelected = viewModel::selectTab,
        onAddRecord = onAddRecord,
        onEditRecord = onEditRecord,
        onDeleteRecord = viewModel::deleteRecord,
        onAddCatalogItem = onAddCatalogItem,
        onEditCatalogItem = onEditCatalogItem,
        onDeleteCatalogItem = onDeleteCatalogItem,
    )
}

internal data class MedicationListUiState(
    val selectedTab: Int = 0,
    val records: List<MedicationRecord> = emptyList(),
    val catalog: List<MedicationCatalogItem> = emptyList(),
)

internal class MedicationListViewModel(application: Application) : ViewModel() {

    private val app = application
    private val _uiState = MutableStateFlow(
        MedicationListUiState(selectedTab = AppPrefs.getMedicationTab(app).coerceIn(0, 2)),
    )
    val uiState: StateFlow<MedicationListUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun selectTab(tab: Int) {
        val selectedTab = tab.coerceIn(0, 2)
        _uiState.value = _uiState.value.copy(selectedTab = selectedTab)
        AppPrefs.setMedicationTab(app, selectedTab)
    }

    fun refresh() {
        viewModelScope.launch {
            val records = withContext(Dispatchers.IO) {
                MedicationPrefs.getRecords(app).sortedByDescending { it.timestamp }
            }
            val catalog = withContext(Dispatchers.IO) { MedicationPrefs.getCatalog(app) }
            _uiState.value = _uiState.value.copy(records = records, catalog = catalog)
        }
    }

    fun deleteRecord(record: MedicationRecord) {
        viewModelScope.launch {
            val records = withContext(Dispatchers.IO) {
                MedicationPrefs.getRecords(app)
                    .removeRecordById(record.id)
                    .also { MedicationPrefs.saveRecords(app, it) }
                    .sortedByDescending { it.timestamp }
            }
            _uiState.value = _uiState.value.copy(records = records)
        }
    }

    fun deleteCatalogItem(item: MedicationCatalogItem) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { MedicationPrefs.deleteCatalogItem(app, item.id) }
            refresh()
        }
    }

    class Factory(
        private val application: Application,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MedicationListViewModel(application) as T
        }
    }
}

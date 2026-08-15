package com.woshiwangnima.healthdietpro.ui.container

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.base.BaseActivity
import com.woshiwangnima.healthdietpro.common.ui.HealthDietProTheme
import com.woshiwangnima.healthdietpro.model.container.ContainerRecord
import com.woshiwangnima.healthdietpro.util.UnitConverter
import com.woshiwangnima.healthdietpro.ui.container.ContainerViewModel

/** 记容器：记录家庭常用容器（名称、分类、容量、可选空容器质量、备注、图片与 2D 截面）。 */
class ContainerRecordActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UnitConverter.init(this)
        setContent { HealthDietProTheme { ContainerRoute(::finish) } }
    }
}

private enum class ContainerRoute { LIST, EDITOR, SETTINGS, CAPACITY_LOOKUP }

@Composable
private fun ContainerRoute(onFinish: () -> Unit) {
    val viewModel: ContainerViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var route by rememberSaveable { mutableStateOf(ContainerRoute.LIST) }
    var editingId by remember { mutableStateOf<String?>(null) }

    fun navigateBack() {
        route = when (route) {
            ContainerRoute.EDITOR -> ContainerRoute.LIST
            ContainerRoute.SETTINGS -> ContainerRoute.LIST
            ContainerRoute.CAPACITY_LOOKUP -> ContainerRoute.SETTINGS
            ContainerRoute.LIST -> ContainerRoute.LIST
        }
    }
    BackHandler(enabled = route != ContainerRoute.LIST) { navigateBack() }

    when (route) {
        ContainerRoute.LIST -> ContainerListScreen(
            containers = uiState.containers,
            scenarioTags = uiState.scenarioTags,
            selectedCategories = uiState.selectedCategories,
            selectedScenarioTags = uiState.selectedScenarioTags,
            onToggleCategory = viewModel::toggleCategoryFilter,
            onToggleScenarioTag = viewModel::toggleScenarioTagFilter,
            onAdd = { editingId = null; route = ContainerRoute.EDITOR },
            onEdit = { editingId = it.id; route = ContainerRoute.EDITOR },
            onDelete = viewModel::delete,
            onSettings = { route = ContainerRoute.SETTINGS },
            onBack = onFinish,
            modifier = Modifier,
        )
        ContainerRoute.EDITOR -> {
            val editingRecord = editingId?.let { id -> uiState.containers.firstOrNull { it.id == id } }
            ContainerEditorScreen(
                existing = editingRecord,
                scenarioTags = uiState.scenarioTags,
                viewModel = viewModel,
                onBack = { route = ContainerRoute.LIST },
                modifier = Modifier,
            )
        }
        ContainerRoute.SETTINGS -> ContainerSettingsScreen(
            scenarioTags = uiState.scenarioTags,
            onSaveScenarioTags = viewModel::saveScenarioTags,
            onCapacityLookup = { route = ContainerRoute.CAPACITY_LOOKUP },
            onBack = { route = ContainerRoute.LIST },
        )
        ContainerRoute.CAPACITY_LOOKUP -> ContainerCapacityLookupScreen(
            containers = uiState.containers,
            onBack = { route = ContainerRoute.SETTINGS },
        )
    }
}
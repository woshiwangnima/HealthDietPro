package com.woshiwangnima.healthdietpro.ui.price

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.woshiwangnima.healthdietpro.base.BaseActivity
import com.woshiwangnima.healthdietpro.common.ui.HealthDietProTheme

/** 记物价：记录某地某天的物品单价（元/斤、元/个……）。 */
class PriceRecordActivity : BaseActivity() {
    companion object {
        const val EXTRA_OPEN_EDITOR = "open_editor"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HealthDietProTheme {
                PriceRoute(
                    onFinish = ::finish,
                    openEditorInitially = intent.getBooleanExtra(EXTRA_OPEN_EDITOR, false),
                )
            }
        }
    }
}

private enum class PriceRoute { LIST, EDITOR }

@Composable
private fun PriceRoute(onFinish: () -> Unit, openEditorInitially: Boolean) {
    val viewModel: PriceViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var route by rememberSaveable { mutableStateOf(if (openEditorInitially) PriceRoute.EDITOR else PriceRoute.LIST) }
    var editingId by remember { mutableStateOf<String?>(null) }

    fun openEditor(id: String?) {
        editingId = id
        route = PriceRoute.EDITOR
    }

    BackHandler(enabled = route == PriceRoute.EDITOR) {
        if (openEditorInitially && route == PriceRoute.EDITOR) onFinish() else route = PriceRoute.LIST
    }

    when (route) {
        PriceRoute.LIST -> PriceListScreen(
            uiState = uiState,
            onQueryChange = viewModel::setQuery,
            onToggleRegion = viewModel::toggleRegion,
            onClearRegions = viewModel::clearRegionFilters,
            onAdd = { openEditor(null) },
            onEdit = { openEditor(it.id) },
            onDeleteGroup = viewModel::deleteGroup,
            onBack = onFinish,
        )

        PriceRoute.EDITOR -> PriceEditorScreen(
            existing = editingId?.let { id -> uiState.records.firstOrNull { it.id == id } },
            viewModel = viewModel,
            onBack = {
                route = PriceRoute.LIST
                editingId = null
            },
        )
    }
}

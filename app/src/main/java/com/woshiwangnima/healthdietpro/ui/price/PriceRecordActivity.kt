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
import com.woshiwangnima.healthdietpro.model.price.priceItemKey

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

private enum class PriceRoute { LIST, EDITOR, HISTORY }

@Composable
private fun PriceRoute(onFinish: () -> Unit, openEditorInitially: Boolean) {
    val viewModel: PriceViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var route by rememberSaveable { mutableStateOf(if (openEditorInitially) PriceRoute.EDITOR else PriceRoute.LIST) }
    var editingId by remember { mutableStateOf<String?>(null) }
    // 物品历史只记住物品名：物品键由 `priceItemKey` 现场算，两处各存一份迟早会不一致。
    var historyItemName by rememberSaveable { mutableStateOf("") }
    // 记物价首页停在哪个 tab 也放这里：进过【物品历史】再回来，应该回到当初那一页。
    var priceTabId by rememberSaveable { mutableStateOf("records") }

    fun openEditor(id: String?) {
        editingId = id
        route = PriceRoute.EDITOR
    }

    BackHandler(enabled = route != PriceRoute.LIST) {
        when {
            route == PriceRoute.EDITOR && openEditorInitially -> onFinish()
            else -> route = PriceRoute.LIST
        }
    }

    when (route) {
        PriceRoute.LIST -> PriceListScreen(
            uiState = uiState,
            selectedTabId = priceTabId,
            onSelectTab = { priceTabId = it },
            onSelectItem = viewModel::selectItem,
            onToggleRegion = viewModel::toggleRegion,
            onClearRegions = viewModel::clearRegionFilters,
            onSelectYear = viewModel::selectYear,
            onSelectMonth = viewModel::selectMonth,
            onApplyFilters = viewModel::applyFilters,
            onClearFilters = viewModel::clearFilters,
            onOpenItemHistory = { name ->
                historyItemName = name
                route = PriceRoute.HISTORY
            },
            onAdd = { openEditor(null) },
            onEdit = { openEditor(it.id) },
            onDeleteGroup = viewModel::deleteGroup,
            onBack = onFinish,
        )

        PriceRoute.HISTORY -> PriceItemHistoryScreen(
            itemName = historyItemName,
            records = uiState.historyRecords(priceItemKey(historyItemName)),
            totalRowCount = uiState.historyItemRowTotal(priceItemKey(historyItemName)),
            onBack = { route = PriceRoute.LIST },
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

@file:OptIn(ExperimentalLayoutApi::class)

package com.woshiwangnima.healthdietpro.ui.record

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.common.ui.ActionGridItem
import com.woshiwangnima.healthdietpro.common.ui.ActionSectionCard

@Composable
fun RecordScreen(
    uiState: RecordUiState,
    onActionClick: (RecordActionId) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        uiState.sections.forEach { section ->
            ActionSectionCard(
                title = stringResource(section.titleRes),
                titleIconRes = section.titleIconRes,
            ) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    maxItemsInEachRow = 4,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    section.items.forEach { item ->
                        ActionGridItem(
                            title = stringResource(item.titleRes),
                            iconRes = item.iconRes,
                            enabled = item.enabled,
                            onClick = { onActionClick(item.id) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

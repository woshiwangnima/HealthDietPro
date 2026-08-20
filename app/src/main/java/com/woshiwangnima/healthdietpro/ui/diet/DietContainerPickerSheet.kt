package com.woshiwangnima.healthdietpro.ui.diet

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.model.container.ContainerRecord
import com.woshiwangnima.healthdietpro.ui.container.ContainerCardRow

/** Pick a recorded container for tare subtraction. Uses the same card as 记容器, display-only. */
@Composable
internal fun DietContainerPickerSheet(
    containers: List<ContainerRecord>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var pendingId by remember(selectedId) { mutableStateOf(selectedId) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.diet_select_container),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    pendingId?.let(onSelect)
                    onDismiss()
                },
                enabled = pendingId != null,
            ) { Text(stringResource(R.string.compose_confirm_dialog_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.compose_confirm_dialog_cancel)) }
        },
        text = {
            if (containers.isEmpty()) {
                Text(stringResource(R.string.diet_containers_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(420.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(containers, key = ContainerRecord::id) { container ->
                        val selected = container.id == pendingId
                        Surface(
                            onClick = { pendingId = container.id },
                            color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface,
                            border = if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            ContainerCardRow(container, showDelete = false)
                        }
                    }
                }
            }
        },
    )
}

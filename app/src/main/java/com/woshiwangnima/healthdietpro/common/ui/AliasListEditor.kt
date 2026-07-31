package com.woshiwangnima.healthdietpro.common.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.R

@Composable
internal fun AliasListEditor(
    aliases: List<String>,
    label: String,
    addLabel: String,
    onAliasesChange: (List<String>) -> Unit,
) {
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIconTextButton(addLabel, R.drawable.ic_add, { editingIndex = -1 })
        aliases.forEachIndexed { index, alias ->
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.clickable { editingIndex = index },
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        alias,
                        modifier = Modifier.padding(start = 8.dp, top = 5.dp, bottom = 5.dp),
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        style = MaterialTheme.typography.labelMedium,
                    )
                    IconButton(onClick = { onAliasesChange(aliases.filterIndexed { itemIndex, _ -> itemIndex != index }) }) {
                        Icon(painterResource(R.drawable.ic_cancel), contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                    }
                }
            }
        }
    }
    editingIndex?.let { index ->
        var value by remember(index) { mutableStateOf(aliases.getOrNull(index).orEmpty()) }
        val normalized = value.trim()
        val changed = normalized.isNotEmpty() && normalized != aliases.getOrNull(index) && aliases.filterIndexed { itemIndex, _ -> itemIndex != index }.none { it.equals(normalized, true) }
        AlertDialog(
            onDismissRequest = { editingIndex = null },
            title = { Text(label) },
            text = { OutlinedTextField(value, { value = it }, singleLine = true, modifier = Modifier.fillMaxWidth()) },
            confirmButton = {
                AppIconTextButton(stringResource(R.string.compose_confirm_dialog_ok), R.drawable.ic_confirm, {
                    onAliasesChange(if (index < 0) aliases + normalized else aliases.toMutableList().apply { set(index, normalized) })
                    editingIndex = null
                }, enabled = changed)
            },
            dismissButton = { AppIconTextButton(stringResource(R.string.compose_confirm_dialog_cancel), R.drawable.ic_cancel, { editingIndex = null }) },
        )
    }
}

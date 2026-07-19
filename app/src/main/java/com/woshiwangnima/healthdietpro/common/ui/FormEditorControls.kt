package com.woshiwangnima.healthdietpro.common.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.R

@Composable
fun FormSaveBar(
    text: String,
    enabled: Boolean,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppIconTextButton(
        text = text,
        iconRes = R.drawable.ic_save,
        onClick = onSave,
        modifier = modifier.fillMaxWidth().padding(16.dp),
        enabled = enabled,
    )
}

@Composable
fun DiscardChangesDialog(
    onDiscard: () -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.form_discard_changes_title))
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.form_discard_changes_cancel))
                }
            }
        },
        text = { Text(stringResource(R.string.form_discard_changes_message)) },
        confirmButton = {
            AppTextIconButton(
                text = stringResource(R.string.form_discard_changes_save),
                iconRes = R.drawable.ic_save,
                onClick = onSave,
            )
        },
        dismissButton = {
            AppTextIconButton(
                text = stringResource(R.string.form_discard_changes_confirm),
                iconRes = R.drawable.ic_cancel,
                onClick = onDiscard,
            )
        },
    )
}

package com.woshiwangnima.healthdietpro.common.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.R

@Composable
fun AppInfoDialog(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = content,
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.compose_confirm_dialog_ok)) } },
    )
}

data class AppInfoSection(
    val title: String,
    val body: String,
)

@Composable
fun AppInfoDialog(
    title: String,
    sections: List<AppInfoSection>,
    onDismiss: () -> Unit,
) {
    AppInfoDialog(title = title, onDismiss = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            sections.forEach { section ->
                Column {
                    Text(section.title, style = androidx.compose.material3.MaterialTheme.typography.titleSmall)
                    Text(section.body, modifier = Modifier.padding(top = 2.dp))
                }
            }
        }
    }
}

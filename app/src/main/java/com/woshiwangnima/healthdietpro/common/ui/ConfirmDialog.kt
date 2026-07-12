package com.woshiwangnima.healthdietpro.common.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.woshiwangnima.healthdietpro.R

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    cancelText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            AppTextIconButton(
                text = confirmText,
                iconRes = R.drawable.ic_check,
                onClick = onConfirm,
            )
        },
        dismissButton = {
            AppTextIconButton(
                text = cancelText,
                iconRes = R.drawable.ic_cancel,
                onClick = onDismiss,
            )
        },
    )
}

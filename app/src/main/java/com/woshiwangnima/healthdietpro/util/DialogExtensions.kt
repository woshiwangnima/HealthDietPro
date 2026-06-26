package com.woshiwangnima.healthdietpro.util

import android.content.Context
import androidx.appcompat.app.AlertDialog

fun Context.showConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "确认",
    cancelText: String = "取消",
    onConfirm: () -> Unit
) {
    AlertDialog.Builder(this)
        .setTitle(title)
        .setMessage(message)
        .setPositiveButton(confirmText) { _, _ -> onConfirm() }
        .setNegativeButton(cancelText, null)
        .show()
}

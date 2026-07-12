package com.woshiwangnima.healthdietpro.util

import android.content.Context
import androidx.appcompat.app.AlertDialog
import com.woshiwangnima.healthdietpro.R

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
        .also { dialog ->
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setStartIcon(R.drawable.ic_check)
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setStartIcon(R.drawable.ic_cancel)
        }
}

private fun android.widget.Button.setStartIcon(iconRes: Int) {
    setCompoundDrawablesRelativeWithIntrinsicBounds(iconRes, 0, 0, 0)
    compoundDrawablePadding = (8 * resources.displayMetrics.density).toInt()
}

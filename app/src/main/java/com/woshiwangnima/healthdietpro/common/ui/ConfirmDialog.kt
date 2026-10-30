package com.woshiwangnima.healthdietpro.common.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.woshiwangnima.healthdietpro.R

/**
 * 全站统一的二次确认对话框。
 *
 * 搜索面板的「清空搜索历史 / 清空点击历史 / 清空使用历史」、设置页的「清除缓存」共用它，
 * 所以**按钮只能在里面改一次**：两个动作都走「图标 + 文字 + 描边」（§5.1 次要按钮）。
 * 早先是 `TextButton`，在浮层上只有一行字，读不出「哪个能按」，也不跟页面上的动作按钮同款。
 */
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
            AppOutlinedIconTextButton(
                text = confirmText,
                iconRes = R.drawable.ic_check,
                onClick = onConfirm,
            )
        },
        dismissButton = {
            AppOutlinedIconTextButton(
                text = cancelText,
                iconRes = R.drawable.ic_cancel,
                onClick = onDismiss,
            )
        },
    )
}

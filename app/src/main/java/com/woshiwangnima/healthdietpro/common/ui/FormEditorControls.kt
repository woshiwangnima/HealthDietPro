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
    saveEnabled: Boolean = true,
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
        // 两个动作都做成按钮而不是文字按钮：这一屏只有两件事可做，其中一件不可逆，
        // 一排灰字会让「放弃」和「保存」读起来一样轻。真正的出口另有右上角那个关闭。
        confirmButton = {
            // 保存＝实心主按钮（这一屏唯一的主操作）；放弃＝danger 描边按钮：
            // 它同样会让人关掉这一屏，但代价是丢内容，按钮的形状与颜色必须同时说明这一点。
            AppIconTextButton(
                text = stringResource(R.string.form_discard_changes_save),
                iconRes = R.drawable.ic_save,
                onClick = onSave,
                enabled = saveEnabled,
            )
        },
        dismissButton = {
            AppOutlinedIconTextButton(
                text = stringResource(R.string.form_discard_changes_confirm),
                iconRes = R.drawable.ic_cancel,
                onClick = onDiscard,
                danger = true,
            )
        },
    )
}

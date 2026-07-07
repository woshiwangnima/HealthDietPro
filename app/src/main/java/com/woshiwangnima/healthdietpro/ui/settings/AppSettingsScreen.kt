package com.woshiwangnima.healthdietpro.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen
import com.woshiwangnima.healthdietpro.common.ui.SettingRow
@Composable
fun AppSettingsScreen(
    onBack: () -> Unit,
    viewModel: AppSettingsViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showClearCacheConfirm by remember { mutableStateOf(false) }

    val toastMbTemplate = stringResource(R.string.settings_cache_cleared_mb)
    val toastKbTemplate = stringResource(R.string.settings_cache_cleared_kb)

    LaunchedEffect(Unit) {
        viewModel.toastMessage.collect { toast ->
            val msg = when (toast) {
                is AppSettingsViewModel.Toast.Mb -> toastMbTemplate.format(toast.value)
                is AppSettingsViewModel.Toast.Kb -> toastKbTemplate.format(toast.value)
            }
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    if (showClearCacheConfirm) {
        AlertDialog(
            onDismissRequest = { showClearCacheConfirm = false },
            title = { Text(stringResource(R.string.settings_clear_cache_confirm_title)) },
            text = { Text(stringResource(R.string.settings_clear_cache_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showClearCacheConfirm = false
                    viewModel.clearCache()
                }) {
                    Text(stringResource(R.string.settings_clear_cache_confirm_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheConfirm = false }) {
                    Text(stringResource(R.string.settings_clear_cache_cancel))
                }
            },
        )
    }

    BaseScreen(title = stringResource(R.string.settings_app_title), onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            SettingRow(
                title = stringResource(R.string.settings_message_notification),
                subtitle = stringResource(R.string.settings_message_notification_desc),
                leadingIconRes = R.drawable.ic_notification,
                onClick = {
                    val uid = try {
                        context.packageManager.getApplicationInfo(context.packageName, 0).uid
                    } catch (_: Exception) {
                        -1
                    }
                    context.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            putExtra("android.provider.extra.APP_UID", uid)
                        }
                    )
                },
            )
            HorizontalDivider()
            SettingRow(
                title = stringResource(R.string.settings_permission),
                subtitle = stringResource(R.string.settings_permission_desc),
                leadingIconRes = R.drawable.ic_shield,
                onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                    )
                },
            )
            HorizontalDivider()
            SettingRow(
                title = stringResource(R.string.settings_clear_cache),
                subtitle = stringResource(R.string.settings_clear_cache_desc),
                leadingIconRes = R.drawable.ic_broom,
                trailingValue = uiState.cacheSizeText,
                onClick = { showClearCacheConfirm = true },
            )
        }
    }
}

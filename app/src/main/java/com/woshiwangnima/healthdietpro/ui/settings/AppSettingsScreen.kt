package com.woshiwangnima.healthdietpro.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen
import com.woshiwangnima.healthdietpro.common.ui.ConfirmDialog
import com.woshiwangnima.healthdietpro.common.ui.SettingRow

@Composable
fun AppSettingsScreen(
    onBack: () -> Unit,
    onOpenTextDisplay: () -> Unit,
    viewModel: AppSettingsViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showClearCacheConfirm by remember { mutableStateOf(false) }
    var firstDayOfWeek by remember { mutableStateOf(com.woshiwangnima.healthdietpro.model.prefs.AppPrefs.getFirstDayOfWeek(context)) }
    var darkMode by remember { mutableStateOf(com.woshiwangnima.healthdietpro.model.prefs.AppPrefs.getDarkMode(context)) }

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
        ConfirmDialog(
            title = stringResource(R.string.settings_clear_cache_confirm_title),
            message = stringResource(R.string.settings_clear_cache_confirm_message),
            confirmText = stringResource(R.string.settings_clear_cache_confirm_ok),
            cancelText = stringResource(R.string.settings_clear_cache_cancel),
            onConfirm = {
                showClearCacheConfirm = false
                viewModel.clearCache()
            },
            onDismiss = { showClearCacheConfirm = false },
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
                leadingIconRes = R.drawable.ic_cleaning_services,
                trailingValue = uiState.cacheSizeText,
                onClick = { showClearCacheConfirm = true },
            )
            HorizontalDivider()
            SettingRow("文字显示设置", "字体大小与文字溢出处理", R.drawable.ic_font_size, onClick = onOpenTextDisplay)
            HorizontalDivider()
            SettingRow(
                title = stringResource(R.string.settings_first_day_of_week),
                subtitle = stringResource(R.string.settings_first_day_of_week_desc),
                leadingIconRes = R.drawable.ic_preferences,
                trailingValue = if (firstDayOfWeek == "MONDAY") stringResource(R.string.settings_monday) else stringResource(R.string.settings_sunday),
                onClick = {
                    val next = if (firstDayOfWeek == "MONDAY") "SUNDAY" else "MONDAY"
                    com.woshiwangnima.healthdietpro.model.prefs.AppPrefs.setFirstDayOfWeek(context, next)
                    firstDayOfWeek = next
                },
            )
            HorizontalDivider()
            SettingRow(
                title = stringResource(R.string.settings_dark_mode),
                subtitle = stringResource(R.string.settings_dark_mode_desc),
                leadingIconRes = R.drawable.ic_settings,
                trailingValue = darkModeLabel(darkMode),
                onClick = {
                    val modes = listOf("FOLLOW_SYSTEM", "YES", "NO")
                    val next = modes[(modes.indexOf(darkMode) + 1) % modes.size]
                    com.woshiwangnima.healthdietpro.model.prefs.AppPrefs.setDarkMode(context, next)
                    darkMode = next
                    AppCompatDelegate.setDefaultNightMode(when (next) { "YES" -> AppCompatDelegate.MODE_NIGHT_YES; "NO" -> AppCompatDelegate.MODE_NIGHT_NO; else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM })
                },
            )
        }
    }
}

@Composable
private fun darkModeLabel(mode: String): String = when (mode) {
    "YES" -> stringResource(R.string.settings_dark_mode_on)
    "NO" -> stringResource(R.string.settings_dark_mode_off)
    else -> stringResource(R.string.settings_dark_mode_system)
}

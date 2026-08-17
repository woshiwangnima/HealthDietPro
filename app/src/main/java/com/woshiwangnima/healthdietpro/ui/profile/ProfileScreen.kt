package com.woshiwangnima.healthdietpro.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.activity.compose.BackHandler
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.ui.SettingRow
import com.woshiwangnima.healthdietpro.common.ui.AppDropdownField
import com.woshiwangnima.healthdietpro.common.ui.AppDropdownOption
import com.woshiwangnima.healthdietpro.common.ui.AppCheckboxRow
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen
import com.woshiwangnima.healthdietpro.common.ui.PlainTextPreviewScreen

@Composable
internal fun ProfileScreen(
    state: ProfileUserInfoUiState,
    onOpenAppSettings: () -> Unit,
    onOpenBmi: () -> Unit,
    onOpenUserSettings: () -> Unit,
    onEditProfile: () -> Unit,
    onOpenUserSwitch: () -> Unit,
    onArchiveAction: (export: Boolean, encrypted: Boolean, password: String) -> Unit,
    onArchivePreview: ((Result<String>) -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showArchiveDialog by remember { mutableStateOf(false) }
    var pendingImport by remember { mutableStateOf<ArchiveRequest?>(null) }
    var archivePreview by remember { mutableStateOf<String?>(null) }
    var previewLoading by remember { mutableStateOf(false) }
    var previewRequestId by remember { mutableStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            IconButton(onClick = onOpenAppSettings) {
                Icon(
                    painter = painterResource(R.drawable.ic_settings),
                    contentDescription = stringResource(R.string.settings_app_title),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        ProfileUserInfoCard(
            state = state,
            onEditProfile = onEditProfile,
            onSwitchUser = onOpenUserSwitch,
            modifier = Modifier.padding(top = 8.dp),
        )

        Spacer(Modifier.height(24.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
        ) {
            Column {
                SettingRow(
                    title = stringResource(R.string.bmi_title),
                    subtitle = stringResource(R.string.bmi_entry_desc),
                    leadingIconRes = R.drawable.ic_chart,
                    onClick = onOpenBmi,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingRow(
                    title = stringResource(R.string.profile_user_settings),
                    subtitle = stringResource(R.string.profile_user_settings_desc),
                    leadingIconRes = R.drawable.ic_preferences,
                    onClick = onOpenUserSettings,
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingRow(
                    title = stringResource(R.string.profile_archive_data),
                    subtitle = stringResource(R.string.profile_archive_data_desc),
                    leadingIconRes = R.drawable.ic_export,
                    onClick = { showArchiveDialog = true },
                )
            }
        }
    }

    if (previewLoading) {
        BackHandler(enabled = true) {
            previewRequestId++
            previewLoading = false
            showArchiveDialog = true
        }
        BaseScreen(
            title = stringResource(R.string.profile_archive_preview),
            onBack = {
                previewRequestId++
                previewLoading = false
                showArchiveDialog = true
            },
            includeStatusBarPadding = false,
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    CircularProgressIndicator()
                    Text(stringResource(R.string.profile_archive_preview_loading), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    if (showArchiveDialog) {
        var encrypted by remember { mutableStateOf(false) }
        var password by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showArchiveDialog = false },
            title = { Text(stringResource(R.string.profile_archive_data)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppDropdownField(stringResource(R.string.profile_archive_format), stringResource(R.string.profile_archive_format_json), listOf(AppDropdownOption("json", stringResource(R.string.profile_archive_format_json))), {}, Modifier.fillMaxWidth())
                    AppDropdownField(stringResource(R.string.profile_archive_compression), stringResource(R.string.profile_archive_compression_gzip_short), listOf(AppDropdownOption("gzip", stringResource(R.string.profile_archive_compression_gzip_short))), {}, Modifier.fillMaxWidth())
                    AppCheckboxRow(checked = encrypted, label = stringResource(R.string.profile_archive_encrypt), onCheckedChange = { encrypted = it })
                    if (encrypted) OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text(stringResource(R.string.profile_archive_password)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    if (!encrypted) Text(stringResource(R.string.profile_archive_unencrypted_warning), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = {
                        val requestId = ++previewRequestId
                        previewLoading = true
                        showArchiveDialog = false
                        onArchivePreview { result ->
                            if (previewRequestId != requestId) return@onArchivePreview
                            previewLoading = false
                            result.onSuccess { archivePreview = it }.onFailure { showArchiveDialog = true }
                        }
                    }) { Text(stringResource(R.string.profile_archive_preview)) }
                }
            },
            confirmButton = {
                Row {
                    TextButton(onClick = {
                        if (archivePreview == null) onArchivePreview { result -> result.onSuccess { archivePreview = it } }
                        else { showArchiveDialog = false; onArchiveAction(true, encrypted, password) }
                    }, enabled = !encrypted || password.isNotBlank()) { Icon(painterResource(R.drawable.ic_export), null); Text(stringResource(R.string.profile_plain_json_export_action)) }
                    TextButton(onClick = { showArchiveDialog = false; pendingImport = ArchiveRequest(encrypted, password) }, enabled = !encrypted || password.isNotBlank()) { Icon(painterResource(R.drawable.ic_import), null); Text(stringResource(R.string.profile_plain_json_import_action)) }
                }
            },
            dismissButton = { TextButton(onClick = { showArchiveDialog = false }) { Text(stringResource(R.string.profile_plain_json_cancel)) } },
        )
    }
    archivePreview?.let { preview -> PlainTextPreviewScreen(stringResource(R.string.profile_archive_preview), preview, onBack = { archivePreview = null; showArchiveDialog = true }) }
    pendingImport?.let { request ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text(stringResource(R.string.profile_plain_json_import_confirm_title)) },
            text = { Text(stringResource(R.string.profile_plain_json_import_confirm_message)) },
            confirmButton = { TextButton(onClick = { pendingImport = null; onArchiveAction(false, request.encrypted, request.password) }) { Text(stringResource(R.string.profile_plain_json_import_action)) } },
            dismissButton = { TextButton(onClick = { pendingImport = null }) { Text(stringResource(R.string.profile_plain_json_cancel)) } },
        )
    }
}

private data class ArchiveRequest(val encrypted: Boolean, val password: String)

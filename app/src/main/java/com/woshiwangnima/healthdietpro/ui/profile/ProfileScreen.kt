package com.woshiwangnima.healthdietpro.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.woshiwangnima.healthdietpro.common.ui.AppCheckboxRow
import com.woshiwangnima.healthdietpro.common.ui.AppDropdownField
import com.woshiwangnima.healthdietpro.common.ui.AppDropdownOption
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen
import com.woshiwangnima.healthdietpro.common.ui.PlainTextPreviewScreen
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppListRow
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppListRowDivider
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppPillIconTextButton
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppSectionGroup
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppTheme
import com.woshiwangnima.healthdietpro.common.ui.designsystem.LocalAppTokens
import com.woshiwangnima.healthdietpro.common.ui.designsystem.rememberAppDarkTheme

/**
 * 「我的」页。
 *
 * 本轮只做与规范对齐的细调（§2.5 圆角、§2.7-1 底材、§2.2 热区）：
 * - 顶部设置入口单独一行，热区 48 dp，不再和卡片挤在一张 `MaterialTheme.surface` 上；
 * - 卡片统一 `radii.container` + 1 dp 描边、去掉阴影与 tonal 叠加（旧版 8/16 dp 圆角 + tonal + shadow 三套并存）；
 * - 设置项行高 ≥ 56 dp，图标 20 dp，分隔线用 `border`；标题/副标题字号收敛到 `body` / `caption` 两级。
 */
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
    AppTheme(darkTheme = rememberAppDarkTheme()) {
        ProfileContent(
            state = state,
            onOpenAppSettings = onOpenAppSettings,
            onOpenBmi = onOpenBmi,
            onOpenUserSettings = onOpenUserSettings,
            onEditProfile = onEditProfile,
            onOpenUserSwitch = onOpenUserSwitch,
            onArchiveAction = onArchiveAction,
            onArchivePreview = onArchivePreview,
            modifier = modifier,
        )
    }
}

@Composable
private fun ProfileContent(
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
    val tokens = LocalAppTokens.current
    var showArchiveDialog by remember { mutableStateOf(false) }
    var pendingImport by remember { mutableStateOf<ArchiveRequest?>(null) }
    var archivePreview by remember { mutableStateOf<String?>(null) }
    var previewLoading by remember { mutableStateOf(false) }
    var previewRequestId by remember { mutableStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(tokens.colors.bg),
    ) {
        // 顶栏：只有设置入口。固定不随之滚动，滚到底也还能进设置。
        // 图标必须是「图标 + 文字」的描边胶囊：右上角一个纯齿轮图标，用户得先猜它是什么才能点（§5.1 次要按钮）。
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = tokens.spacing.page, end = tokens.spacing.s3, top = tokens.spacing.s1),
            horizontalArrangement = Arrangement.End,
        ) {
            AppPillIconTextButton(
                text = stringResource(R.string.profile_settings_button),
                iconRes = R.drawable.ic_settings,
                onClick = onOpenAppSettings,
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = tokens.spacing.page, end = tokens.spacing.page, bottom = tokens.spacing.s4),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.s4),
        ) {
            ProfileUserInfoCard(
                state = state,
                onEditProfile = onEditProfile,
                onSwitchUser = onOpenUserSwitch,
            )

            AppSectionGroup {
                AppListRow(
                    title = stringResource(R.string.bmi_title),
                    iconRes = R.drawable.ic_bmi,
                    tooltip = stringResource(R.string.bmi_entry_desc),
                    showChevron = true,
                    onClick = onOpenBmi,
                )
                AppListRowDivider()
                AppListRow(
                    title = stringResource(R.string.profile_user_settings),
                    iconRes = R.drawable.ic_preferences,
                    tooltip = stringResource(R.string.profile_user_settings_desc),
                    showChevron = true,
                    onClick = onOpenUserSettings,
                )
                AppListRowDivider()
                AppListRow(
                    title = stringResource(R.string.profile_archive_data),
                    iconRes = R.drawable.ic_import_export,
                    tooltip = stringResource(R.string.profile_archive_data_desc),
                    showChevron = true,
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
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(tokens.spacing.s4),
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = stringResource(R.string.profile_archive_preview_loading),
                        style = tokens.type.body,
                        color = tokens.colors.textMuted,
                    )
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
                Column(verticalArrangement = Arrangement.spacedBy(tokens.spacing.s2)) {
                    AppDropdownField(
                        stringResource(R.string.profile_archive_format),
                        stringResource(R.string.profile_archive_format_json),
                        listOf(AppDropdownOption("json", stringResource(R.string.profile_archive_format_json))),
                        {},
                        Modifier.fillMaxWidth(),
                    )
                    AppDropdownField(
                        stringResource(R.string.profile_archive_compression),
                        stringResource(R.string.profile_archive_compression_gzip_short),
                        listOf(AppDropdownOption("gzip", stringResource(R.string.profile_archive_compression_gzip_short))),
                        {},
                        Modifier.fillMaxWidth(),
                    )
                    AppCheckboxRow(
                        checked = encrypted,
                        label = stringResource(R.string.profile_archive_encrypt),
                        onCheckedChange = { encrypted = it },
                    )
                    if (encrypted) {
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text(stringResource(R.string.profile_archive_password)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                    }
                    if (!encrypted) {
                        Text(
                            text = stringResource(R.string.profile_archive_unencrypted_warning),
                            style = tokens.type.caption,
                            color = tokens.colors.danger,
                        )
                    }
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
                    TextButton(
                        onClick = {
                            if (archivePreview == null) {
                                onArchivePreview { result -> result.onSuccess { archivePreview = it } }
                            } else {
                                showArchiveDialog = false
                                onArchiveAction(true, encrypted, password)
                            }
                        },
                        enabled = !encrypted || password.isNotBlank(),
                    ) {
                        Icon(painterResource(R.drawable.ic_export), null)
                        Text(stringResource(R.string.profile_plain_json_export_action))
                    }
                    TextButton(
                        onClick = {
                            showArchiveDialog = false
                            pendingImport = ArchiveRequest(encrypted, password)
                        },
                        enabled = !encrypted || password.isNotBlank(),
                    ) {
                        Icon(painterResource(R.drawable.ic_import), null)
                        Text(stringResource(R.string.profile_plain_json_import_action))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showArchiveDialog = false }) {
                    Text(stringResource(R.string.profile_plain_json_cancel))
                }
            },
        )
    }
    archivePreview?.let { preview ->
        PlainTextPreviewScreen(
            stringResource(R.string.profile_archive_preview),
            preview,
            onBack = {
                archivePreview = null
                showArchiveDialog = true
            },
        )
    }
    pendingImport?.let { request ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text(stringResource(R.string.profile_plain_json_import_confirm_title)) },
            text = { Text(stringResource(R.string.profile_plain_json_import_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    pendingImport = null
                    onArchiveAction(false, request.encrypted, request.password)
                }) { Text(stringResource(R.string.profile_plain_json_import_action)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingImport = null }) {
                    Text(stringResource(R.string.profile_plain_json_cancel))
                }
            },
        )
    }
}

private data class ArchiveRequest(val encrypted: Boolean, val password: String)

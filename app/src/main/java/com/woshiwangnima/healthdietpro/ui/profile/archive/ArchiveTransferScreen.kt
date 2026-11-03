package com.woshiwangnima.healthdietpro.ui.profile.archive

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.ui.AppIconTextButton
import com.woshiwangnima.healthdietpro.common.ui.AppOutlinedIconTextButton
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen
import com.woshiwangnima.healthdietpro.common.ui.ConfirmDialog
import com.woshiwangnima.healthdietpro.common.ui.EqualWidthSegmentedTabs
import com.woshiwangnima.healthdietpro.common.ui.EqualWidthTab
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppModal
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppModalSize
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppRadioRow
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppSectionCard
import com.woshiwangnima.healthdietpro.common.ui.designsystem.LocalAppTokens

/**
 * 「导入/导出用户数据」整页。
 *
 * 旧版是「我的」页里弹一个 `AlertDialog`：格式与压缩是两个只有一个选项的假下拉、
 * 导出按钮第一次点只生成预览、密码只有一个输入框（打错就直接导不回来）。
 * 现在拆成独立页面，把**导出格式、加密、预览、导入前预览**都摆成可操作的真实选项。
 */
@Composable
internal fun ArchiveTransferScreen(controller: ArchiveTransferController) {
    val state = controller.uiState
    val tokens = LocalAppTokens.current
    var password by remember { mutableStateOf("") }
    var passwordConfirm by remember { mutableStateOf("") }
    var pendingImportConfirm by remember { mutableStateOf(false) }

    state.preview?.let { preview ->
        BackHandler { controller.dismissPreview() }
        ArchivePreviewScreen(
            preview = preview,
            busy = state.busy,
            onBack = controller::dismissPreview,
            onExport = {
                if (state.exportFormat.encrypted) {
                    controller.export(password)
                } else {
                    controller.export("")
                }
            },
            onImport = { pendingImportConfirm = true },
        )
        if (pendingImportConfirm) {
            val newUser = preview.importMode == ArchiveImportMode.NEW_USER
            ConfirmDialog(
                title = stringResource(
                    if (newUser) R.string.archive_new_user_title else R.string.archive_overwrite_title,
                ),
                message = stringResource(
                    if (newUser) R.string.archive_new_user_message else R.string.archive_overwrite_message,
                ),
                confirmText = stringResource(
                    if (newUser) R.string.archive_import_new_confirm else R.string.archive_import_confirm,
                ),
                cancelText = stringResource(R.string.common_cancel),
                onConfirm = {
                    pendingImportConfirm = false
                    controller.confirmImport()
                },
                onDismiss = { pendingImportConfirm = false },
            )
        }
        if (state.passwordPrompt) {
            ImportPasswordDialog(controller, rejected = state.passwordRejected)
        }
        state.failure?.let { FailureDialog(it, controller::consumeFailure) }
        return
    }

    BackHandler { controller.close() }
    BaseScreen(
        title = stringResource(R.string.archive_transfer_title),
        onBack = controller::close,
        includeStatusBarPadding = false,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(tokens.colors.bg),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 顶部固定：提示条 + 分区切换栏。导出与导入互斥，同一时刻只显示一件。
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = tokens.spacing.page, vertical = tokens.spacing.s2),
                    verticalArrangement = Arrangement.spacedBy(tokens.spacing.s2),
                ) {
                    state.notice?.let { notice ->
                        ArchiveNoticeBanner(notice, controller::consumeNotice)
                    }
                    ArchiveStatusCard(
                        state = state,
                        onReload = controller::reload,
                    )
                    EqualWidthSegmentedTabs(
                        tabs = listOf(
                            EqualWidthTab.text(stringResource(R.string.archive_export_title)),
                            EqualWidthTab.text(stringResource(R.string.archive_import_title)),
                        ),
                        selectedIndex = if (state.section == ArchiveSection.IMPORT) 1 else 0,
                        onSelected = { index ->
                            controller.selectSection(
                                if (index == 1) ArchiveSection.IMPORT else ArchiveSection.EXPORT,
                            )
                        },
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = tokens.spacing.page, vertical = tokens.spacing.s2),
                    verticalArrangement = Arrangement.spacedBy(tokens.spacing.s4),
                ) {
                    when (state.section) {
                        ArchiveSection.EXPORT -> {
                            ArchiveExportCard(
                                state = state,
                                password = password,
                                passwordConfirm = passwordConfirm,
                                onPasswordChange = { password = it },
                                onPasswordConfirmChange = { passwordConfirm = it },
                                onSelectFormat = controller::selectFormat,
                                onPreview = controller::previewExport,
                                onExport = {
                                    controller.export(password)
                                    password = ""
                                    passwordConfirm = ""
                                },
                            )
                        }
                        ArchiveSection.IMPORT -> {
                            ArchiveImportCard(
                                state = state,
                                onPickFile = controller::beginImport,
                                onSelectMode = controller::selectImportMode,
                            )
                        }
                    }
                }
            }
            if (state.busy || state.loading) {
                ArchiveBusyOverlay()
            }
        }
    }

    if (state.passwordPrompt) {
        ImportPasswordDialog(controller, rejected = state.passwordRejected)
    }
    state.failure?.let { FailureDialog(it, controller::consumeFailure) }
}

@Composable
private fun ArchiveBusyOverlay() {
    val tokens = LocalAppTokens.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(tokens.colors.scrim.copy(alpha = 0.35f))
            // 吞掉所有点击：加载中再从底下点一次「导出」会发起第二次落盘。
            .pointerInput(Unit) { detectTapGestures { } },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.s2),
        ) {
            CircularProgressIndicator()
            Text(
                text = stringResource(R.string.archive_busy),
                style = tokens.type.bodyS,
                color = tokens.colors.text,
            )
        }
    }
}

@Composable
private fun ArchiveNoticeBanner(notice: ArchiveNotice, onDismiss: () -> Unit) {
    val tokens = LocalAppTokens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(tokens.radii.container))
            .background(tokens.colors.primaryContainer)
            .padding(horizontal = tokens.spacing.s3, vertical = tokens.spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(
                if (notice is ArchiveNotice.ExportCancelled) R.drawable.ic_info else R.drawable.ic_check,
            ),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = tokens.colors.onPrimaryContainer,
        )
        Spacer(Modifier.width(tokens.spacing.s2))
        Text(
            text = notice.text(),
            style = tokens.type.bodyS,
            color = tokens.colors.onPrimaryContainer,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onDismiss) {
            Text(stringResource(R.string.common_close), style = tokens.type.caption)
        }
    }
}

@Composable
private fun ArchiveExportCard(
    state: ArchiveTransferUiState,
    password: String,
    passwordConfirm: String,
    onPasswordChange: (String) -> Unit,
    onPasswordConfirmChange: (String) -> Unit,
    onSelectFormat: (ArchiveExportFormat) -> Unit,
    onPreview: () -> Unit,
    onExport: () -> Unit,
) {
    val tokens = LocalAppTokens.current
    val format = state.exportFormat
    val mismatch = format.encrypted && passwordConfirm.isNotEmpty() && password != passwordConfirm
    val canSubmit = !state.busy && state.snapshot != null &&
        (!format.encrypted || (password.isNotBlank() && password == passwordConfirm))
    AppSectionCard(
        iconRes = R.drawable.ic_export,
        title = stringResource(R.string.archive_export_section_title),
    ) {
        ArchiveExportFormat.entries.forEach { option ->
            AppRadioRow(
                title = stringResource(option.labelRes()),
                subtitle = stringResource(option.descriptionRes()),
                selected = format == option,
                onSelect = { onSelectFormat(option) },
                horizontalPadding = 0.dp,
            )
        }
        if (format.encrypted) {
            Spacer(Modifier.height(tokens.spacing.s2))
            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = { Text(stringResource(R.string.archive_password)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(tokens.spacing.s2))
            OutlinedTextField(
                value = passwordConfirm,
                onValueChange = onPasswordConfirmChange,
                label = { Text(stringResource(R.string.archive_password_confirm)) },
                singleLine = true,
                isError = mismatch,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )
            if (mismatch) {
                Text(
                    text = stringResource(R.string.archive_password_mismatch),
                    style = tokens.type.caption,
                    color = tokens.colors.danger,
                )
            }
        } else {
            Spacer(Modifier.height(tokens.spacing.s2))
            Text(
                text = stringResource(R.string.archive_plain_warning),
                style = tokens.type.caption,
                color = tokens.colors.danger,
            )
        }
        Spacer(Modifier.height(tokens.spacing.s3))
        Row(horizontalArrangement = Arrangement.spacedBy(tokens.spacing.s2)) {
            AppOutlinedIconTextButton(
                text = stringResource(R.string.archive_preview_action),
                iconRes = R.drawable.ic_list,
                onClick = onPreview,
                modifier = Modifier.weight(1f),
                enabled = state.snapshot != null && !state.loading,
            )
            AppIconTextButton(
                text = stringResource(R.string.archive_export_action),
                iconRes = R.drawable.ic_export,
                onClick = onExport,
                modifier = Modifier.weight(1f),
                enabled = canSubmit,
            )
        }
    }
}

@Composable
private fun ArchiveImportCard(
    state: ArchiveTransferUiState,
    onPickFile: () -> Unit,
    onSelectMode: (ArchiveImportMode) -> Unit,
) {
    val tokens = LocalAppTokens.current
    // 用单选列表而不是分段控件：顶部的「导出 / 导入」已经是分段控件，
    // 再叠一个同款会让人分不清哪一层在切换什么。这里的两个选项与导出页的三种格式同级。
    AppSectionCard(
        iconRes = R.drawable.ic_import,
        title = stringResource(R.string.archive_import_section_title),
    ) {
        AppRadioRow(
            title = stringResource(R.string.archive_import_mode_replace),
            subtitle = stringResource(R.string.archive_import_mode_replace_desc),
            selected = state.importMode == ArchiveImportMode.REPLACE_CURRENT,
            onSelect = { onSelectMode(ArchiveImportMode.REPLACE_CURRENT) },
            horizontalPadding = 0.dp,
        )
        AppRadioRow(
            title = stringResource(R.string.archive_import_mode_new),
            subtitle = stringResource(R.string.archive_import_mode_new_desc),
            selected = state.importMode == ArchiveImportMode.NEW_USER,
            onSelect = { onSelectMode(ArchiveImportMode.NEW_USER) },
            horizontalPadding = 0.dp,
        )
        Spacer(Modifier.height(tokens.spacing.s2))
        Text(
            text = stringResource(R.string.archive_import_desc),
            style = tokens.type.caption,
            color = tokens.colors.textMuted,
        )
        Spacer(Modifier.height(tokens.spacing.s3))
        AppOutlinedIconTextButton(
            text = stringResource(R.string.archive_import_action),
            iconRes = R.drawable.ic_import,
            onClick = onPickFile,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.loading && !state.busy,
        )
    }
}

@Composable
private fun ImportPasswordDialog(controller: ArchiveTransferController, rejected: Boolean) {
    var password by remember { mutableStateOf("") }
    AppModal(
        title = stringResource(R.string.archive_password_prompt_title),
        subtitle = stringResource(R.string.archive_password_prompt_message),
        size = AppModalSize.S,
        dismissOnScrimClick = false,
        onDismiss = controller::cancelImportPassword,
        actions = {
            AppOutlinedIconTextButton(
                text = stringResource(R.string.common_cancel),
                iconRes = R.drawable.ic_cancel,
                onClick = controller::cancelImportPassword,
            )
            Spacer(Modifier.width(LocalAppTokens.current.spacing.s2))
            AppIconTextButton(
                text = stringResource(R.string.archive_continue),
                iconRes = R.drawable.ic_check,
                onClick = { controller.submitImportPassword(password.toCharArray()) },
                enabled = password.isNotEmpty(),
            )
        },
    ) {
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.archive_password)) },
            singleLine = true,
            isError = rejected,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
        )
        if (rejected) {
            Text(
                text = stringResource(R.string.archive_wrong_password),
                style = LocalAppTokens.current.type.caption,
                color = LocalAppTokens.current.colors.danger,
                modifier = Modifier.padding(top = LocalAppTokens.current.spacing.s1),
            )
        }
    }
}

@Composable
private fun FailureDialog(failure: ArchiveFailure, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.archive_failure_title)) },
        text = { Text(stringResource(failure.messageRes())) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_close))
            }
        },
    )
}

private fun ArchiveFailure.messageRes(): Int = when (this) {
    ArchiveFailure.LOAD_FAILED -> R.string.archive_failure_load
    ArchiveFailure.EXPORT_FAILED -> R.string.archive_failure_export
    ArchiveFailure.IMPORT_FAILED -> R.string.archive_failure_import
    ArchiveFailure.UNREADABLE_FILE -> R.string.archive_failure_unreadable
    ArchiveFailure.NOT_AN_ARCHIVE -> R.string.archive_failure_not_archive
    ArchiveFailure.NEWER_SCHEMA -> R.string.archive_import_newer_schema
    ArchiveFailure.WRONG_PASSWORD -> R.string.archive_wrong_password
    ArchiveFailure.PASSWORD_REQUIRED -> R.string.archive_failure_password
}

private fun ArchiveExportFormat.labelRes(): Int = when (this) {
    ArchiveExportFormat.PLAIN_JSON -> R.string.archive_format_plain
    ArchiveExportFormat.PLAIN_JSON_GZIP -> R.string.archive_format_plain_gzip
    ArchiveExportFormat.ENCRYPTED_HDP -> R.string.archive_format_encrypted
}

private fun ArchiveExportFormat.descriptionRes(): Int = when (this) {
    ArchiveExportFormat.PLAIN_JSON -> R.string.archive_format_plain_desc
    ArchiveExportFormat.PLAIN_JSON_GZIP -> R.string.archive_format_plain_gzip_desc
    ArchiveExportFormat.ENCRYPTED_HDP -> R.string.archive_format_encrypted_desc
}

@Composable
private fun ArchiveNotice.text(): String = when (this) {
    ArchiveNotice.Exported -> stringResource(R.string.archive_notice_exported)
    ArchiveNotice.Imported -> stringResource(R.string.archive_notice_imported)
    is ArchiveNotice.ImportedAsNewUser -> stringResource(R.string.archive_notice_imported_new_user, userName)
    ArchiveNotice.ExportCancelled -> stringResource(R.string.archive_notice_export_cancelled)
}

package com.woshiwangnima.healthdietpro.ui.memo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.ui.AppOutlinedIconTextButton
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen
import com.woshiwangnima.healthdietpro.common.ui.EqualWidthSegmentedTabs
import com.woshiwangnima.healthdietpro.common.ui.EqualWidthTab
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppEmptyState
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppSectionCard
import com.woshiwangnima.healthdietpro.common.ui.designsystem.LocalAppTokens
import com.woshiwangnima.healthdietpro.model.memo.MAX_MEMO_HISTORY_DEPTH
import com.woshiwangnima.healthdietpro.model.memo.MIN_MEMO_HISTORY_DEPTH
import com.woshiwangnima.healthdietpro.model.memo.MemoArchive
import com.woshiwangnima.healthdietpro.model.memo.MemoFolder
import com.woshiwangnima.healthdietpro.model.memo.flattened

/**
 * 便签设置：顶部分【笔记】【待办】两栏。
 *
 * 历史版本与文件夹都是**笔记**的属性（待办不压历史、也不进文件夹），
 * 把它们和「待办」并排在一页里，用户会以为待办也吃这两个设置；分栏之后每一栏名下的东西都对得上。
 * 待办目前没有可调项，那一栏就说清楚这件事，不摆一排点了没反应的开关。
 *
 * 文件夹的行尾菜单与对话框不在本页私有：它们住在 [FolderActions.kt]，
 * 【移动到】弹窗里每个文件夹用的是同一份操作。
 */
@Composable
internal fun MemoSettingsScreen(
    uiState: MemoUiState,
    onBack: () -> Unit,
) {
    val tokens = LocalAppTokens.current
    val viewModel: MemoViewModel = viewModel()
    val archive = uiState.archive
    var tabIndex by rememberSaveable { mutableStateOf(0) }
    var folderAction by remember { mutableStateOf<FolderAction?>(null) }

    BaseScreen(title = stringResource(R.string.memo_settings_title), onBack = onBack) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            EqualWidthSegmentedTabs(
                tabs = listOf(
                    EqualWidthTab(R.string.memo_tab_notes),
                    EqualWidthTab(R.string.memo_tab_todos),
                ),
                selectedIndex = tabIndex,
                onSelected = { tabIndex = it },
                modifier = Modifier.padding(horizontal = tokens.spacing.page, vertical = tokens.spacing.s2),
            )
            if (tabIndex == 0) {
                NoteSettings(
                    archive = archive,
                    onHistoryDepthChange = viewModel::setHistoryDepth,
                    onCreateFolder = { parentId -> folderAction = FolderAction.Name(parentId, null) },
                    onFolderAction = { folderAction = it },
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    AppEmptyState(
                        title = stringResource(R.string.memo_settings_todo_empty_title),
                        description = stringResource(R.string.memo_settings_todo_empty_desc),
                        iconRes = R.drawable.ic_todo,
                    )
                }
            }
        }
    }

    FolderActionDialogs(
        action = folderAction,
        folders = archive.folders,
        ops = viewModel.folderOps,
        onDismiss = { folderAction = null },
    )
}

@Composable
private fun NoteSettings(
    archive: MemoArchive,
    onHistoryDepthChange: (Int) -> Unit,
    onCreateFolder: (String?) -> Unit,
    onFolderAction: (FolderAction) -> Unit,
) {
    val tokens = LocalAppTokens.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = tokens.spacing.page, vertical = tokens.spacing.s2),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.s3),
    ) {
        AppSectionCard(
            iconRes = R.drawable.ic_history,
            title = stringResource(R.string.memo_settings_history_title),
            tooltip = stringResource(R.string.memo_settings_history_tooltip),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = tokens.spacing.rowMinHeight),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.memo_settings_history_depth, archive.historyDepth),
                    style = tokens.type.body,
                    color = tokens.colors.text,
                    modifier = Modifier.weight(1f),
                )
                StepperButton(
                    iconRes = R.drawable.ic_minus,
                    enabled = archive.historyDepth > MIN_MEMO_HISTORY_DEPTH,
                    onClick = { onHistoryDepthChange(archive.historyDepth - 1) },
                )
                Spacer(Modifier.width(tokens.spacing.s2))
                StepperButton(
                    iconRes = R.drawable.ic_add,
                    enabled = archive.historyDepth < MAX_MEMO_HISTORY_DEPTH,
                    onClick = { onHistoryDepthChange(archive.historyDepth + 1) },
                )
            }
        }

        AppSectionCard(
            iconRes = R.drawable.ic_folder,
            title = stringResource(R.string.memo_settings_folders_title),
            tooltip = stringResource(R.string.memo_settings_folders_tooltip),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(tokens.radii.container))
                    .background(tokens.colors.panel)
                    .border(
                        tokens.radii.highlight,
                        tokens.colors.border,
                        RoundedCornerShape(tokens.radii.container),
                    ),
            ) {
                val rows = archive.folders.flattened()
                if (rows.isEmpty()) {
                    AppEmptyState(
                        title = stringResource(R.string.memo_folder_empty_title),
                        description = stringResource(R.string.memo_folder_empty_desc),
                        iconRes = R.drawable.ic_folder,
                    )
                }
                rows.forEach { row ->
                    FolderRow(
                        archive = archive,
                        folder = row.folder,
                        depth = row.depth,
                        onFolderAction = onFolderAction,
                    )
                }
            }
            Spacer(Modifier.size(tokens.spacing.s2))
            AppOutlinedIconTextButton(
                text = stringResource(R.string.memo_folder_new_root),
                iconRes = R.drawable.ic_add,
                onClick = { onCreateFolder(null) },
            )
        }

        Spacer(Modifier.size(tokens.spacing.s6))
    }
}

@Composable
private fun FolderRow(
    archive: MemoArchive,
    folder: MemoFolder,
    depth: Int,
    onFolderAction: (FolderAction) -> Unit,
) {
    val tokens = LocalAppTokens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = tokens.spacing.rowMinHeight)
            .padding(start = tokens.spacing.s3 + tokens.spacing.s4 * depth, end = tokens.spacing.s1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_folder),
            contentDescription = null,
            tint = tokens.colors.primary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(tokens.spacing.s2))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = folder.name,
                style = tokens.type.body,
                color = tokens.colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(
                    R.string.memo_folder_note_count,
                    archive.notes.count { it.folderId == folder.id },
                    archive.folders.count { it.parentId == folder.id },
                ),
                style = tokens.type.caption,
                color = tokens.colors.textMuted,
            )
        }
        FolderMoreMenu(folder = folder, onRequest = onFolderAction)
    }
}

@Composable
private fun StepperButton(iconRes: Int, enabled: Boolean, onClick: () -> Unit) {
    val tokens = LocalAppTokens.current
    IconButton(onClick = onClick, enabled = enabled) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = if (enabled) tokens.colors.primary else tokens.colors.disabled,
            modifier = Modifier.size(20.dp),
        )
    }
}

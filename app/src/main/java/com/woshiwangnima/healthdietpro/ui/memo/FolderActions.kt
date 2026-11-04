package com.woshiwangnima.healthdietpro.ui.memo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.ui.ConfirmDialog
import com.woshiwangnima.healthdietpro.common.ui.AppOutlinedIconTextButton
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppBottomSheet
import com.woshiwangnima.healthdietpro.common.ui.designsystem.LocalAppTokens
import com.woshiwangnima.healthdietpro.model.memo.MemoFolder
import com.woshiwangnima.healthdietpro.model.memo.canMoveFolder
import com.woshiwangnima.healthdietpro.model.memo.flattened

/** 文件夹管理操作集：【便签设置】、【移动到】弹窗、笔记编辑器共用同一份，保证三处行为永远一致。 */
internal data class FolderOps(
    val create: (name: String, parentId: String?) -> Unit,
    val rename: (folderId: String, name: String) -> Unit,
    val move: (folderId: String, newParentId: String?) -> Unit,
    val delete: (folderId: String) -> Unit,
)

internal val MemoViewModel.folderOps: FolderOps
    get() = FolderOps(::createFolder, ::renameFolder, ::moveFolder, ::deleteFolder)

/** 由文件夹行尾菜单发起的请求：弹名字对话框 / 移动目标表 / 删除确认。 */
internal sealed interface FolderAction {
    data class Name(val parentId: String?, val existing: MemoFolder?) : FolderAction
    data class Move(val folder: MemoFolder) : FolderAction
    data class Delete(val folder: MemoFolder) : FolderAction
}

/**
 * 文件夹行尾的【更多】菜单（竖排省略号）。
 *
 * 只负责发起请求，不负责弹对话框：这样【便签设置】和【移动到】弹窗可以各自决定
 * 把对话框叠在哪一层，菜单项本身却永远是同一份。
 */
@Composable
internal fun FolderMoreMenu(
    folder: MemoFolder,
    onRequest: (FolderAction) -> Unit,
) {
    val tokens = LocalAppTokens.current
    var menuOpen by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { menuOpen = true }) {
            Icon(
                painter = painterResource(R.drawable.ic_more_vert),
                contentDescription = stringResource(R.string.memo_note_more),
                tint = tokens.colors.textMuted,
            )
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.memo_folder_new_child)) },
                onClick = {
                    menuOpen = false
                    onRequest(FolderAction.Name(folder.id, null))
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.memo_folder_rename)) },
                onClick = {
                    menuOpen = false
                    onRequest(FolderAction.Name(folder.parentId, folder))
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.memo_folder_move)) },
                onClick = {
                    menuOpen = false
                    onRequest(FolderAction.Move(folder))
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.memo_delete), color = tokens.colors.danger) },
                onClick = {
                    menuOpen = false
                    onRequest(FolderAction.Delete(folder))
                },
            )
        }
    }
}

/**
 * 文件夹管理对话框宿主：渲染 [FolderAction] 对应的弹窗并接上操作。
 *
 * `onFolderDeleted` 给需要感知删除后果的调用方（笔记编辑器要把被上提的笔记
 * 归属改到上一级，否则退出编辑时会把已删除的文件夹写回去）。
 */
@Composable
internal fun FolderActionDialogs(
    action: FolderAction?,
    folders: List<MemoFolder>,
    ops: FolderOps,
    onDismiss: () -> Unit,
    onFolderDeleted: ((MemoFolder) -> Unit)? = null,
) {
    when (action) {
        is FolderAction.Name -> FolderNameDialog(
            action = action,
            onDismiss = onDismiss,
            onConfirm = { name ->
                val existing = action.existing
                if (existing == null) {
                    ops.create(name, action.parentId)
                } else {
                    ops.rename(existing.id, name)
                }
                onDismiss()
            },
        )

        is FolderAction.Move -> FolderMoveSheet(
            folders = folders,
            moving = action.folder,
            onDismiss = onDismiss,
            onSelect = { target ->
                ops.move(action.folder.id, target)
                onDismiss()
            },
        )

        is FolderAction.Delete -> ConfirmDialog(
            title = stringResource(R.string.memo_folder_delete_title),
            message = stringResource(R.string.memo_folder_delete_message, action.folder.name),
            confirmText = stringResource(R.string.memo_delete),
            cancelText = stringResource(R.string.common_cancel),
            onConfirm = {
                ops.delete(action.folder.id)
                onFolderDeleted?.invoke(action.folder)
                onDismiss()
            },
            onDismiss = onDismiss,
        )

        null -> Unit
    }
}

@Composable
private fun FolderNameDialog(
    action: FolderAction.Name,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf(action.existing?.name.orEmpty()) }
    val trimmed = name.trim()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (action.existing == null) R.string.memo_folder_create_title else R.string.memo_folder_rename,
                ),
            )
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                placeholder = { Text(stringResource(R.string.memo_folder_name_hint)) },
            )
        },
        confirmButton = {
            AppOutlinedIconTextButton(
                text = stringResource(R.string.memo_confirm),
                iconRes = R.drawable.ic_check,
                enabled = trimmed.isNotEmpty(),
                onClick = { onConfirm(trimmed) },
            )
        },
        dismissButton = {
            AppOutlinedIconTextButton(
                text = stringResource(R.string.common_cancel),
                iconRes = R.drawable.ic_cancel,
                onClick = onDismiss,
            )
        },
    )
}

@Composable
private fun FolderMoveSheet(
    folders: List<MemoFolder>,
    moving: MemoFolder,
    onDismiss: () -> Unit,
    onSelect: (String?) -> Unit,
) {
    val tokens = LocalAppTokens.current
    AppBottomSheet(
        title = stringResource(R.string.memo_folder_move_title),
        onDismiss = onDismiss,
    ) {
        MoveTargetRow(
            title = stringResource(R.string.memo_folder_root_target),
            onClick = { onSelect(null) },
        )
        folders.flattened().forEach { row ->
            if (folders.canMoveFolder(moving.id, row.folder.id)) {
                Box(modifier = Modifier.padding(start = tokens.spacing.s4 * row.depth)) {
                    MoveTargetRow(
                        title = row.folder.name,
                        onClick = { onSelect(row.folder.id) },
                    )
                }
            }
        }
        Spacer(Modifier.size(tokens.spacing.s3))
    }
}

@Composable
internal fun MoveTargetRow(title: String, onClick: () -> Unit) {
    val tokens = LocalAppTokens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = tokens.spacing.hitMin)
            .clickable(onClick = onClick)
            .padding(horizontal = tokens.spacing.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_folder),
            contentDescription = null,
            tint = tokens.colors.textMuted,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(tokens.spacing.s2))
        Text(text = title, style = tokens.type.body, color = tokens.colors.text)
    }
}

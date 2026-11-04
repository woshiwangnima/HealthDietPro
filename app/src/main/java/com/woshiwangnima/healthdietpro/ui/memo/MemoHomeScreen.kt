package com.woshiwangnima.healthdietpro.ui.memo

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.woshiwangnima.healthdietpro.R
import com.woshiwangnima.healthdietpro.common.ui.AnimatedPageContent
import com.woshiwangnima.healthdietpro.common.ui.BaseScreen
import com.woshiwangnima.healthdietpro.common.ui.ConfirmDialog
import com.woshiwangnima.healthdietpro.common.ui.DetailTabBar
import com.woshiwangnima.healthdietpro.common.ui.DetailTabItem
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppBottomSheet
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppChipSelectedAlpha
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppEmptyState
import com.woshiwangnima.healthdietpro.common.ui.designsystem.AppListRow
import com.woshiwangnima.healthdietpro.common.ui.designsystem.LocalAppTokens
import com.woshiwangnima.healthdietpro.model.memo.MemoArchive
import com.woshiwangnima.healthdietpro.model.memo.MemoFolder
import com.woshiwangnima.healthdietpro.model.memo.MemoNote
import com.woshiwangnima.healthdietpro.model.memo.MemoTodo
import com.woshiwangnima.healthdietpro.model.memo.MemoTodoItem
import com.woshiwangnima.healthdietpro.model.memo.childrenOf
import com.woshiwangnima.healthdietpro.model.memo.displayItems
import com.woshiwangnima.healthdietpro.model.memo.doneCount
import com.woshiwangnima.healthdietpro.model.memo.flattened
import com.woshiwangnima.healthdietpro.model.memo.isCompleted
import com.woshiwangnima.healthdietpro.model.memo.matchesQuery
import com.woshiwangnima.healthdietpro.model.memo.pathOf
import com.woshiwangnima.healthdietpro.model.memo.pathStringOf
import com.woshiwangnima.healthdietpro.model.memo.previewText
import com.woshiwangnima.healthdietpro.model.memo.sortedForDisplay
import com.woshiwangnima.healthdietpro.model.memo.voiceCount
import com.woshiwangnima.healthdietpro.model.memo.withPinnedFirst
import com.woshiwangnima.healthdietpro.model.memo.wordCount
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal enum class MemoTab(val id: String) {
    NOTES("notes"),
    TODOS("todos"),
    ;

    companion object {
        fun ofId(id: String): MemoTab = entries.firstOrNull { it.id == id } ?: NOTES
    }
}

/** 文件夹筛选的持久化键：`all` / `unfiled` / `f:<folderId>`。 */
internal const val MEMO_FILTER_ALL = "all"
internal const val MEMO_FILTER_UNFILED = "unfiled"
private const val MEMO_FILTER_FOLDER_PREFIX = "f:"

internal fun memoFolderKey(folderId: String?): String =
    if (folderId == null) MEMO_FILTER_ALL else "$MEMO_FILTER_FOLDER_PREFIX$folderId"

internal fun memoFolderIdOf(key: String): String? =
    key.takeIf { it.startsWith(MEMO_FILTER_FOLDER_PREFIX) }?.removePrefix(MEMO_FILTER_FOLDER_PREFIX)

internal fun MemoArchive.notesFor(key: String): List<MemoNote> = when {
    key == MEMO_FILTER_ALL -> notes
    key == MEMO_FILTER_UNFILED -> notes.filter { it.folderId == null }
    else -> {
        val folderId = memoFolderIdOf(key)
        if (folderId == null) notes else notes.filter { it.folderId == folderId }
    }
}

/**
 * 记便签首页：【笔记】【待办】两个同级板块 + 文件夹层级导航 + 全库搜索。
 *
 * 版面自上而下是「新建按钮 → 搜索框 → 列表 → 板块切换栏」：切换栏放底部是为了跟【记物价】的
 * 【统计】/【记录】、睡眠的【统计】/【记录】同一套语法（`DetailTabBar`），
 * 让「这一屏属于哪个板块」在拇指够得到位置，而不是顶端一行只在刚进来时看一眼。
 * 新建按钮反过来常驻顶部（参考记睡眠的两个记录入口）：它回答的是「现在记什么」，
 * 滚到第 20 条笔记时也还得点得到；搜索框紧挨在它下方，同样不随列表滚走。
 *
 * 搜索是**全局**的（不限当前文件夹）：笔记散落在多层文件夹里时，用户想找的是「那条笔记」，
 * 不是「当前文件夹里的那条」。结果卡上带出文件夹路径，点开即读；把【复制文件夹路径】
 * 复制来的路径粘进搜索框，也能直达那个文件夹下的全部笔记。
 *
 * 文件夹不做抽屉：笔记的层级通常只有两三层，用「面包屑 + 当前层子文件夹横滑」
 * 比抽屉少两次点击，也不会把整页内容挤成一条；面包屑跟着列表一起滚走，把纵向空间留给内容。
 */
@Composable
internal fun MemoHomeScreen(
    uiState: MemoUiState,
    tab: MemoTab,
    onSelectTab: (MemoTab) -> Unit,
    folderKey: String,
    onSelectFolder: (String) -> Unit,
    folderOps: FolderOps,
    onCreateNote: () -> Unit,
    onOpenNote: (String) -> Unit,
    onDeleteNote: (MemoNote) -> Unit,
    onMoveNote: (MemoNote, String?) -> Unit,
    onTogglePin: (MemoNote) -> Unit,
    onCreateTodo: () -> Unit,
    onOpenTodo: (String) -> Unit,
    onToggleTodoItem: (String, String) -> Unit,
    onDeleteTodo: (MemoTodo) -> Unit,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit,
) {
    val tokens = LocalAppTokens.current
    val archive = uiState.archive
    var movingNote by remember { mutableStateOf<MemoNote?>(null) }
    var deletingNote by remember { mutableStateOf<MemoNote?>(null) }
    var deletingTodo by remember { mutableStateOf<MemoTodo?>(null) }
    var query by rememberSaveable { mutableStateOf("") }

    val tabs = remember {
        listOf(
            DetailTabItem(MemoTab.NOTES.id, R.string.memo_tab_notes, R.drawable.ic_memo),
            DetailTabItem(MemoTab.TODOS.id, R.string.memo_tab_todos, R.drawable.ic_todo),
        )
    }
    val selectedIndex = tabs.indexOfFirst { it.id == tab.id }.coerceAtLeast(0)

    BaseScreen(
        title = stringResource(R.string.memo_title),
        onBack = onBack,
        // 底部让给 DetailTabBar，由它自己处理系统导航栏安全区。
        includeNavigationBarPadding = false,
        actions = {
            IconButton(onClick = onOpenSettings) {
                Icon(
                    painter = painterResource(R.drawable.ic_settings),
                    contentDescription = stringResource(R.string.memo_settings_title),
                    tint = tokens.colors.primary,
                )
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            MemoCreateBar(
                tab = tab,
                onCreateNote = onCreateNote,
                onCreateTodo = onCreateTodo,
            )
            if (tab == MemoTab.NOTES) {
                MemoSearchField(
                    query = query,
                    onQueryChange = { query = it },
                )
            }
            AnimatedPageContent(
                selectedIndex,
                Modifier.weight(1f),
                direction = { initial, target -> target - initial },
            ) { index ->
                if (tabs[index].id == MemoTab.TODOS.id) {
                    TodoTab(
                        todos = archive.todos.sortedForDisplay(),
                        onOpenTodo = onOpenTodo,
                        onToggleTodoItem = onToggleTodoItem,
                        onDeleteTodo = { deletingTodo = it },
                    )
                } else {
                    NoteTab(
                        archive = archive,
                        folderKey = folderKey,
                        query = query,
                        onSelectFolder = onSelectFolder,
                        onOpenNote = onOpenNote,
                        onMoveNote = { movingNote = it },
                        onTogglePin = onTogglePin,
                        onDeleteNote = { deletingNote = it },
                    )
                }
            }
            DetailTabBar(items = tabs, selectedId = tabs[selectedIndex].id) { item ->
                onSelectTab(MemoTab.ofId(item.id))
            }
        }
    }

    movingNote?.let { note ->
        MoveNoteSheet(
            folders = archive.folders,
            ops = folderOps,
            onDismiss = { movingNote = null },
            onSelect = { folderId ->
                onMoveNote(note, folderId)
                movingNote = null
            },
        )
    }

    deletingNote?.let { note ->
        ConfirmDialog(
            title = stringResource(R.string.memo_delete_note_title),
            message = stringResource(R.string.memo_delete_note_message),
            confirmText = stringResource(R.string.memo_delete),
            cancelText = stringResource(R.string.common_cancel),
            onConfirm = {
                onDeleteNote(note)
                deletingNote = null
            },
            onDismiss = { deletingNote = null },
        )
    }

    deletingTodo?.let { todo ->
        ConfirmDialog(
            title = stringResource(R.string.memo_delete_todo_title),
            message = stringResource(R.string.memo_delete_todo_message),
            confirmText = stringResource(R.string.memo_delete),
            cancelText = stringResource(R.string.common_cancel),
            onConfirm = {
                onDeleteTodo(todo)
                deletingTodo = null
            },
            onDismiss = { deletingTodo = null },
        )
    }
}

/**
 * 顶部常驻的新建入口（参考记睡眠页顶部两个记录按钮）。
 *
 * 用「淡底 + 同色描边 + 同色字」而不是实心：这一页的主色块已经给了底部选中态，
 * 顶部再来一块实心会把列表压成背景；淡底既点得明白，也不抢内容。
 */
@Composable
private fun MemoCreateBar(
    tab: MemoTab,
    onCreateNote: () -> Unit,
    onCreateTodo: () -> Unit,
) {
    val tokens = LocalAppTokens.current
    val isNotes = tab == MemoTab.NOTES
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = tokens.spacing.page,
                end = tokens.spacing.page,
                top = tokens.spacing.s2,
                bottom = if (isNotes) tokens.spacing.s1 else tokens.spacing.s2,
            ),
    ) {
        MemoPillAction(
            text = stringResource(if (isNotes) R.string.memo_new_note else R.string.memo_new_todo),
            iconRes = R.drawable.ic_add,
            onClick = if (isNotes) onCreateNote else onCreateTodo,
            modifier = Modifier.weight(1f),
        )
    }
}

/** 页内主操作药丸（新建 / 语音 / 导入音频共用），颜色只走主题主色。 */
@Composable
internal fun MemoPillAction(
    text: String,
    iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalAppTokens.current
    val shape = RoundedCornerShape(tokens.radii.pill)
    Row(
        modifier = modifier
            .heightIn(min = tokens.spacing.hitMin)
            .clip(shape)
            .background(tokens.colors.primary.copy(alpha = AppChipSelectedAlpha))
            .border(tokens.radii.highlight, tokens.colors.primary.copy(alpha = 0.5f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = tokens.spacing.s2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = tokens.colors.primary,
        )
        Spacer(Modifier.width(tokens.spacing.s1))
        Text(
            text = text,
            style = tokens.type.bodyS,
            color = tokens.colors.primary,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 搜索框：药丸形容器 + 前置放大镜 + 尾部清除，与新建按钮同宽同距、视觉上连成一组。 */
@Composable
private fun MemoSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    val tokens = LocalAppTokens.current
    val shape = RoundedCornerShape(tokens.radii.pill)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = tokens.spacing.page,
                end = tokens.spacing.page,
                bottom = tokens.spacing.s2,
            )
            .heightIn(min = 44.dp)
            .clip(shape)
            .background(tokens.colors.panel)
            .border(tokens.radii.highlight, tokens.colors.border, shape)
            .padding(horizontal = tokens.spacing.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_search),
            contentDescription = null,
            tint = tokens.colors.textMuted,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(tokens.spacing.s2))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = tokens.type.bodyS.copy(color = tokens.colors.text),
            cursorBrush = SolidColor(tokens.colors.primary),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box {
                    if (query.isEmpty()) {
                        Text(
                            text = stringResource(R.string.memo_search_hint),
                            style = tokens.type.bodyS,
                            color = tokens.colors.textMuted,
                        )
                    }
                    inner()
                }
            },
        )
        if (query.isNotEmpty()) {
            Icon(
                painter = painterResource(R.drawable.ic_cancel),
                contentDescription = stringResource(R.string.memo_search_clear),
                tint = tokens.colors.textMuted,
                modifier = Modifier
                    .clip(RoundedCornerShape(tokens.radii.pill))
                    .clickable { onQueryChange("") }
                    .padding(tokens.spacing.s1)
                    .size(16.dp),
            )
        }
    }
}

@Composable
private fun NoteTab(
    archive: MemoArchive,
    folderKey: String,
    query: String,
    onSelectFolder: (String) -> Unit,
    onOpenNote: (String) -> Unit,
    onMoveNote: (MemoNote) -> Unit,
    onTogglePin: (MemoNote) -> Unit,
    onDeleteNote: (MemoNote) -> Unit,
) {
    val tokens = LocalAppTokens.current
    val searching = query.isNotBlank()
    val notes = (
        if (searching) {
            archive.notes.filter { it.matchesQuery(query, archive.folders.pathStringOf(it.folderId)) }
        } else {
            archive.notesFor(folderKey)
        }
        ).withPinnedFirst()

    LazyColumn(
        modifier = Modifier.fillMaxSize().fillMaxWidth(),
        contentPadding = PaddingValues(
            start = tokens.spacing.page,
            end = tokens.spacing.page,
            top = tokens.spacing.s2,
            bottom = tokens.spacing.s6,
        ),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.s3),
    ) {
        if (!searching) {
            item(key = "folder-bar") {
                FolderBar(
                    archive = archive,
                    folderKey = folderKey,
                    onSelectFolder = onSelectFolder,
                )
            }
        } else {
            item(key = "search-count") {
                Text(
                    text = stringResource(R.string.memo_search_result_count, notes.size),
                    style = tokens.type.caption,
                    color = tokens.colors.textMuted,
                )
            }
        }
        if (notes.isEmpty()) {
            item {
                AppEmptyState(
                    title = stringResource(
                        if (searching) R.string.memo_search_empty_title else R.string.memo_empty_notes_title,
                    ),
                    description = stringResource(
                        if (searching) R.string.memo_search_empty_desc else R.string.memo_empty_notes_desc,
                    ),
                    iconRes = if (searching) R.drawable.ic_search else R.drawable.ic_memo,
                )
            }
        }
        items(notes, key = MemoNote::id) { note ->
            val folderPath = archive.folders.pathStringOf(note.folderId)
            NoteCard(
                note = note,
                folderPath = folderPath,
                // 搜索是全局的，每张卡都要带出「它住在哪」；浏览态只在看全库时带文件夹名。
                folderLabel = if (searching) {
                    folderPath.ifEmpty { stringResource(R.string.memo_filter_unfiled) }
                } else {
                    archive.folderName(note.folderId)
                },
                query = query,
                onOpen = { onOpenNote(note.id) },
                onMove = { onMoveNote(note) },
                onTogglePin = { onTogglePin(note) },
                onDelete = { onDeleteNote(note) },
            )
        }
    }
}

@Composable
private fun TodoTab(
    todos: List<MemoTodo>,
    onOpenTodo: (String) -> Unit,
    onToggleTodoItem: (String, String) -> Unit,
    onDeleteTodo: (MemoTodo) -> Unit,
) {
    val tokens = LocalAppTokens.current
    LazyColumn(
        modifier = Modifier.fillMaxSize().fillMaxWidth(),
        contentPadding = PaddingValues(
            start = tokens.spacing.page,
            end = tokens.spacing.page,
            top = tokens.spacing.s2,
            bottom = tokens.spacing.s6,
        ),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.s3),
    ) {
        if (todos.isEmpty()) {
            item {
                AppEmptyState(
                    title = stringResource(R.string.memo_empty_todos_title),
                    description = stringResource(R.string.memo_empty_todos_desc),
                    iconRes = R.drawable.ic_todo,
                )
            }
        }
        items(todos, key = MemoTodo::id) { todo ->
            TodoCard(
                todo = todo,
                onOpen = { onOpenTodo(todo.id) },
                onToggle = { item -> onToggleTodoItem(todo.id, item.id) },
                onDelete = { onDeleteTodo(todo) },
            )
        }
    }
}

@Composable
private fun FolderBar(
    archive: MemoArchive,
    folderKey: String,
    onSelectFolder: (String) -> Unit,
) {
    val tokens = LocalAppTokens.current
    val currentFolderId = memoFolderIdOf(folderKey)
    val path = archive.folders.pathOf(currentFolderId)
    val children = archive.folders.childrenOf(currentFolderId)
    val rootMode = currentFolderId == null

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(tokens.spacing.s2),
    ) {
        if (path.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.memo_filter_all),
                    style = tokens.type.caption,
                    color = tokens.colors.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(tokens.radii.label))
                        .clickable { onSelectFolder(MEMO_FILTER_ALL) }
                        .padding(horizontal = tokens.spacing.s1, vertical = tokens.spacing.s1),
                )
                path.forEachIndexed { index, folder ->
                    Text(
                        text = " / ",
                        style = tokens.type.caption,
                        color = tokens.colors.textMuted,
                    )
                    Text(
                        text = folder.name,
                        style = tokens.type.caption,
                        color = if (index == path.lastIndex) tokens.colors.text else tokens.colors.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(tokens.radii.label))
                            .then(
                                if (index == path.lastIndex) {
                                    Modifier
                                } else {
                                    Modifier.clickable { onSelectFolder(memoFolderKey(folder.id)) }
                                },
                            )
                            .padding(horizontal = tokens.spacing.s1, vertical = tokens.spacing.s1),
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(tokens.spacing.s2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!rootMode) {
                FolderChip(
                    text = stringResource(R.string.memo_folder_up),
                    selected = false,
                    onClick = {
                        val parentId = path.dropLast(1).lastOrNull()?.id
                        onSelectFolder(parentId?.let(::memoFolderKey) ?: MEMO_FILTER_ALL)
                    },
                )
            } else {
                FolderChip(
                    text = stringResource(R.string.memo_filter_all),
                    selected = folderKey == MEMO_FILTER_ALL,
                    onClick = { onSelectFolder(MEMO_FILTER_ALL) },
                )
                FolderChip(
                    text = stringResource(R.string.memo_filter_unfiled),
                    selected = folderKey == MEMO_FILTER_UNFILED,
                    onClick = { onSelectFolder(MEMO_FILTER_UNFILED) },
                )
            }
            children.forEach { folder ->
                FolderChip(
                    text = folderChipLabel(archive, folder),
                    selected = currentFolderId == folder.id,
                    onClick = { onSelectFolder(memoFolderKey(folder.id)) },
                )
            }
        }
    }
}

@Composable
private fun folderChipLabel(archive: MemoArchive, folder: MemoFolder): String {
    val count = archive.notes.count { it.folderId == folder.id }
    return if (count > 0) "${folder.name} $count" else folder.name
}

@Composable
private fun FolderChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val tokens = LocalAppTokens.current
    val shape = RoundedCornerShape(tokens.radii.pill)
    Text(
        text = text,
        style = tokens.type.caption,
        color = if (selected) tokens.colors.primary else tokens.colors.textMuted,
        maxLines = 1,
        modifier = Modifier
            .clip(shape)
            .background(if (selected) tokens.colors.primaryContainer else tokens.colors.panel)
            .border(tokens.radii.highlight, if (selected) tokens.colors.primary else tokens.colors.border, shape)
            .clickable(onClick = onClick)
            .heightIn(min = 32.dp)
            .padding(horizontal = tokens.spacing.s3, vertical = tokens.spacing.s1),
    )
}

/**
 * 笔记卡。
 *
 * 行尾菜单收着四个动作：移动到 / 置顶 / 复制文件夹路径 / 删除——入口统一用省略号图标，
 * 与文件夹行尾的【更多】同一套语法。置顶只改排序标记，不碰内容也不压历史版本；
 * 复制文件夹路径是为了之后的搜索：把路径粘进搜索框就能直达那个文件夹下的笔记。
 * 搜索态下标题与摘要里的关键词用主色加粗高亮，扫一眼就知道为什么这条会出现在结果里。
 */
@Composable
private fun NoteCard(
    note: MemoNote,
    folderPath: String,
    folderLabel: String?,
    query: String,
    onOpen: () -> Unit,
    onMove: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit,
) {
    val tokens = LocalAppTokens.current
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val shape = RoundedCornerShape(tokens.radii.container)
    var menuOpen by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(tokens.colors.panelRaised)
            .border(tokens.radii.highlight, tokens.colors.border, shape)
            .clickable(onClick = onOpen)
            .padding(start = tokens.spacing.s3, end = tokens.spacing.s1, top = tokens.spacing.s3, bottom = tokens.spacing.s2),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (note.pinned) {
                Icon(
                    painter = painterResource(R.drawable.ic_pin),
                    contentDescription = stringResource(R.string.memo_note_pin),
                    tint = tokens.colors.primary,
                    modifier = Modifier.padding(end = tokens.spacing.s1).size(14.dp),
                )
            }
            HighlightedText(
                text = note.title.ifBlank { stringResource(R.string.memo_untitled_note) },
                query = query,
                style = tokens.type.body.copy(fontWeight = FontWeight.SemiBold),
                color = if (note.title.isBlank()) tokens.colors.textMuted else tokens.colors.text,
                maxLines = 2,
                modifier = Modifier.weight(1f),
            )
            Box {
                IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(tokens.spacing.hitMin)) {
                    Icon(
                        painter = painterResource(R.drawable.ic_more_vert),
                        contentDescription = stringResource(R.string.memo_note_more),
                        tint = tokens.colors.textMuted,
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.memo_move_to)) },
                        onClick = {
                            menuOpen = false
                            onMove()
                        },
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(
                                    if (note.pinned) R.string.memo_note_unpin else R.string.memo_note_pin,
                                ),
                            )
                        },
                        onClick = {
                            menuOpen = false
                            onTogglePin()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.memo_note_copy_path)) },
                        onClick = {
                            menuOpen = false
                            if (folderPath.isBlank()) {
                                Toast.makeText(context, R.string.memo_note_copy_path_none, Toast.LENGTH_SHORT).show()
                            } else {
                                clipboard.setText(AnnotatedString(folderPath))
                                Toast.makeText(context, R.string.memo_note_copy_path_done, Toast.LENGTH_SHORT).show()
                            }
                        },
                    )
                    HorizontalDivider(color = tokens.colors.border)
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.memo_delete), color = tokens.colors.danger) },
                        onClick = {
                            menuOpen = false
                            onDelete()
                        },
                    )
                }
            }
        }
        val preview = note.previewText()
        if (preview.isNotBlank()) {
            HighlightedText(
                text = preview,
                query = query,
                style = tokens.type.bodyS,
                color = tokens.colors.textMuted,
                maxLines = 2,
                modifier = Modifier.padding(end = tokens.spacing.s2, top = tokens.spacing.s1),
            )
        }
        MemoMetaLine(
            parts = buildList {
                add(formatMemoTime(note.updatedAtMillis))
                add(stringResource(R.string.memo_meta_words, note.wordCount))
                if (note.voiceCount > 0) add(stringResource(R.string.memo_meta_voices, note.voiceCount))
                folderLabel?.let { add(it) }
            },
            modifier = Modifier.padding(top = tokens.spacing.s2, end = tokens.spacing.s2),
        )
    }
}

/**
 * 命中高亮：把 [query] 在 [text] 里的所有出现（不区分大小写）用主色加粗标出。
 * 查询为空时退化为普通 [Text]，零额外开销。
 */
@Composable
private fun HighlightedText(
    text: String,
    query: String,
    style: TextStyle,
    color: Color,
    maxLines: Int = 1,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalAppTokens.current
    val keyword = query.trim()
    if (keyword.isEmpty()) {
        Text(
            text = text,
            style = style,
            color = color,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier,
        )
        return
    }
    val highlight = SpanStyle(color = tokens.colors.primary, fontWeight = FontWeight.Bold)
    val annotated = remember(text, keyword) {
        buildAnnotatedString {
            append(text)
            var index = text.indexOf(keyword, ignoreCase = true)
            while (index >= 0) {
                addStyle(highlight, index, (index + keyword.length).coerceAtMost(text.length))
                index = text.indexOf(keyword, startIndex = index + keyword.length, ignoreCase = true)
            }
        }
    }
    Text(
        text = annotated,
        style = style,
        color = color,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

/**
 * 待办卡：一项待办就是一个可勾选的清单。
 *
 * - **只有一项时不显示标题**：标题只会把那一项的文字重说一遍，卡片就该是一行可勾选的文字；
 * - **多项时标题固定【待办清单】**：这里不再是「给这条待办起名字」，而是「这一摞事」，
 *   名字随内容变会让同一张卡每次刷新都换个抬头；
 * - 多项卡右侧才有 `完成/总数` 与展开收起箭头：单项时没有可折叠的内容，摆一个箭头是骗点击；
 * - 点卡片进编辑，长按出菜单（目前只有删除），勾选只动勾选框本身——
 *   三种手势各自的落点写在卡片上，不用再额外摆一排图标。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TodoCard(
    todo: MemoTodo,
    onOpen: () -> Unit,
    onToggle: (MemoTodoItem) -> Unit,
    onDelete: () -> Unit,
) {
    val tokens = LocalAppTokens.current
    val shape = RoundedCornerShape(tokens.radii.container)
    var menuOpen by remember { mutableStateOf(false) }
    var expanded by remember(todo.id) { mutableStateOf(true) }
    val isList = todo.items.size > 1

    Box {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(tokens.colors.panelRaised)
                .border(tokens.radii.highlight, tokens.colors.border, shape)
                .combinedClickable(onClick = onOpen, onLongClick = { menuOpen = true })
                .padding(vertical = if (isList) tokens.spacing.s1 else 0.dp),
        ) {
            if (isList) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = tokens.spacing.hitMin)
                        .padding(start = tokens.spacing.s3, end = tokens.spacing.s1),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.memo_todo_list_title),
                        style = tokens.type.body.copy(fontWeight = FontWeight.SemiBold),
                        color = tokens.colors.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = stringResource(R.string.memo_todo_progress, todo.doneCount, todo.items.size),
                        style = tokens.type.caption,
                        color = if (todo.isCompleted) tokens.colors.success else tokens.colors.textMuted,
                    )
                    IconButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.size(tokens.spacing.hitMin),
                    ) {
                        Icon(
                            painter = painterResource(
                                if (expanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more,
                            ),
                            contentDescription = stringResource(
                                if (expanded) R.string.memo_todo_collapse else R.string.memo_todo_expand,
                            ),
                            tint = tokens.colors.textMuted,
                        )
                    }
                }
            }
            val visibleItems = if (isList && !expanded) emptyList() else todo.displayItems()
            visibleItems.forEach { item ->
                TodoItemRow(item = item, onToggle = onToggle)
            }
            if (!isList && todo.items.isEmpty()) {
                Text(
                    text = stringResource(R.string.memo_untitled_todo),
                    style = tokens.type.bodyS,
                    color = tokens.colors.textMuted,
                    modifier = Modifier.padding(
                        start = tokens.spacing.s3,
                        end = tokens.spacing.s3,
                        top = tokens.spacing.s3,
                        bottom = tokens.spacing.s3,
                    ),
                )
            }
            if (isList) Spacer(Modifier.height(tokens.spacing.s1))
        }
        DropdownMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false },
            modifier = Modifier.align(Alignment.TopEnd),
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.memo_delete)) },
                onClick = {
                    menuOpen = false
                    onDelete()
                },
            )
        }
    }
}

@Composable
private fun TodoItemRow(item: MemoTodoItem, onToggle: (MemoTodoItem) -> Unit) {
    val tokens = LocalAppTokens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = tokens.spacing.hitMin)
            .padding(start = tokens.spacing.s2, end = tokens.spacing.s3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = item.done,
            onCheckedChange = { onToggle(item) },
            colors = CheckboxDefaults.colors(
                checkedColor = tokens.colors.primary,
                uncheckedColor = tokens.colors.textMuted,
            ),
        )
        Text(
            text = item.text,
            style = tokens.type.bodyS,
            color = if (item.done) tokens.colors.disabled else tokens.colors.text,
            textDecoration = if (item.done) TextDecoration.LineThrough else null,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MemoMetaLine(parts: List<String>, modifier: Modifier = Modifier) {
    val tokens = LocalAppTokens.current
    Text(
        text = parts.joinToString(" · "),
        style = tokens.type.caption,
        color = tokens.colors.textMuted,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

internal fun formatMemoTime(millis: Long): String =
    SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(millis))

/**
 * 移动到文件夹。首页卡片菜单与笔记编辑器右上角菜单共用同一份。
 *
 * 每个文件夹行尾挂的是与【便签设置】完全相同的【更多】菜单（新建子文件夹 / 重命名 / 移动 / 删除）：
 * 移动笔记的路上发现文件夹本身要整理，不必退出去设置页再回来。
 */
@Composable
internal fun MoveNoteSheet(
    folders: List<MemoFolder>,
    ops: FolderOps,
    onDismiss: () -> Unit,
    onSelect: (String?) -> Unit,
    onFolderDeleted: ((MemoFolder) -> Unit)? = null,
) {
    val tokens = LocalAppTokens.current
    var folderAction by remember { mutableStateOf<FolderAction?>(null) }
    val rows = folders.flattened()
    AppBottomSheet(
        title = stringResource(R.string.memo_move_to),
        onDismiss = onDismiss,
    ) {
        AppListRow(
            title = stringResource(R.string.memo_folder_unfiled_option),
            iconRes = R.drawable.ic_folder,
            onClick = { onSelect(null) },
        )
        rows.forEach { row ->
            Box(modifier = Modifier.padding(start = tokens.spacing.s4 * row.depth)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = tokens.spacing.hitMin)
                        .clickable { onSelect(row.folder.id) }
                        .padding(start = tokens.spacing.s3, end = tokens.spacing.s1),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_folder),
                        contentDescription = null,
                        tint = tokens.colors.textMuted,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(tokens.spacing.s2))
                    Text(
                        text = row.folder.name,
                        style = tokens.type.body,
                        color = tokens.colors.text,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    FolderMoreMenu(folder = row.folder, onRequest = { folderAction = it })
                }
            }
        }
        Spacer(Modifier.height(tokens.spacing.s3))
    }

    FolderActionDialogs(
        action = folderAction,
        folders = folders,
        ops = ops,
        onDismiss = { folderAction = null },
        onFolderDeleted = onFolderDeleted,
    )
}

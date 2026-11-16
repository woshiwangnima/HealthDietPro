package com.woshiwangnima.healthdietpro.ui.memo

import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
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
import com.woshiwangnima.healthdietpro.common.ui.adaptiveNavigationBarWindowInsets
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
 *
 * **多选**：长按任一卡片进入多选，底部切换栏整条换成批量操作栏（全选 / 移动 / 置顶 / 删除）。
 * 单条操作仍然走卡片行尾的省略号菜单——长按不再抢它，两种手势各管一件事：
 * 长按 = 「这批要一起处理」，省略号 = 「就这一条」。
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
    onDeleteNotes: (Set<String>) -> Unit,
    onMoveNotes: (Set<String>, String?) -> Unit,
    onPinNotes: (Set<String>, Boolean) -> Unit,
    onCreateTodo: () -> Unit,
    onOpenTodo: (String) -> Unit,
    onToggleTodoItem: (String, String) -> Unit,
    onDeleteTodos: (Set<String>) -> Unit,
    onCompleteTodos: (Set<String>, Boolean) -> Unit,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit,
) {
    val archive = uiState.archive

    // 两个板块各存一份选中集：切换板块时清空，避免「在笔记里选了两条，切到待办还亮着批量栏」。
    var noteSelection by remember { mutableStateOf(emptySet<String>()) }
    var todoSelection by remember { mutableStateOf(emptySet<String>()) }
    // 多选**态**与选中集分开：全选后再点「取消全选」选中集会被清空，但用户还在多选里，
    // 批量栏必须留着（否则一取消全选就整条消失，用户得重新长按才能继续）。
    var selectMode by remember { mutableStateOf(false) }
    var movingNotes by remember { mutableStateOf<Set<String>?>(null) }
    var deletingNotes by remember { mutableStateOf<Set<String>?>(null) }
    var deletingTodos by remember { mutableStateOf<Set<String>?>(null) }
    // 笔记与待办各存一份搜索词：从笔记切到待办时，不该拿「标题/正文/路径」去筛待办项。
    var noteQuery by rememberSaveable { mutableStateOf("") }
    var todoQuery by rememberSaveable { mutableStateOf("") }

    fun clearSelection() {
        noteSelection = emptySet()
        todoSelection = emptySet()
        selectMode = false
    }

    val tabs = remember {
        listOf(
            DetailTabItem(MemoTab.NOTES.id, R.string.memo_tab_notes, R.drawable.ic_memo),
            DetailTabItem(MemoTab.TODOS.id, R.string.memo_tab_todos, R.drawable.ic_todo),
        )
    }
    val selectedIndex = tabs.indexOfFirst { it.id == tab.id }.coerceAtLeast(0)

    val query = if (tab == MemoTab.NOTES) noteQuery else todoQuery
    val searching = query.isNotBlank()
    val visibleNotes = (
        if (searching && tab == MemoTab.NOTES) {
            archive.notes.filter { it.matchesQuery(query, archive.folders.pathStringOf(it.folderId)) }
        } else {
            archive.notesFor(folderKey)
        }
        ).withPinnedFirst()
    val visibleTodos = (
        if (searching && tab == MemoTab.TODOS) {
            archive.todos.filter { it.matchesQuery(query) }
        } else {
            archive.todos
        }
        ).sortedForDisplay()

    val selection = if (tab == MemoTab.NOTES) noteSelection else todoSelection
    val selecting = selectMode
    val visibleIds = if (tab == MemoTab.NOTES) {
        visibleNotes.mapTo(LinkedHashSet()) { it.id }
    } else {
        visibleTodos.mapTo(LinkedHashSet()) { it.id }
    }
    val allSelected = visibleIds.isNotEmpty() && visibleIds.all { it in selection }

    BackHandler(enabled = selecting) { clearSelection() }

    BaseScreen(
        title = stringResource(R.string.memo_title),
        // 多选态下返回先退多选：顶栏返回与系统返回手势行为保持一致。
        onBack = { if (selecting) clearSelection() else onBack() },
        // 底部让给 DetailTabBar / 批量栏，由它们自己处理系统导航栏安全区。
        includeNavigationBarPadding = false,
        actions = {
            IconButton(onClick = onOpenSettings) {
                Icon(
                    painter = painterResource(R.drawable.ic_settings),
                    contentDescription = stringResource(R.string.memo_settings_title),
                    tint = LocalAppTokens.current.colors.primary,
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
            MemoSearchField(
                query = query,
                hint = stringResource(
                    if (tab == MemoTab.NOTES) R.string.memo_search_hint else R.string.memo_search_todo_hint,
                ),
                onQueryChange = { next ->
                    if (tab == MemoTab.NOTES) noteQuery = next else todoQuery = next
                },
            )
            AnimatedPageContent(
                selectedIndex,
                Modifier.weight(1f),
                direction = { initial, target -> target - initial },
            ) { index ->
                if (tabs[index].id == MemoTab.TODOS.id) {
                    TodoTab(
                        todos = visibleTodos,
                        searching = searching,
                        selectionMode = selecting,
                        selection = todoSelection,
                        onOpenTodo = onOpenTodo,
                        onToggleTodoItem = onToggleTodoItem,
                        onToggleSelect = { todoId ->
                            selectMode = true
                            todoSelection = todoSelection.toggle(todoId)
                        },
                        onDeleteTodo = { todo -> deletingTodos = setOf(todo.id) },
                    )
                } else {
                    NoteTab(
                        archive = archive,
                        notes = visibleNotes,
                        searching = searching,
                        query = query,
                        folderKey = folderKey,
                        selectionMode = selecting,
                        selection = noteSelection,
                        onSelectFolder = onSelectFolder,
                        onOpenNote = onOpenNote,
                        onToggleSelect = { noteId ->
                            selectMode = true
                            noteSelection = noteSelection.toggle(noteId)
                        },
                        onMoveNote = { note -> movingNotes = setOf(note.id) },
                        onTogglePin = { note -> onPinNotes(setOf(note.id), !note.pinned) },
                        onDeleteNote = { note -> deletingNotes = setOf(note.id) },
                    )
                }
            }
            if (selecting) {
                if (tab == MemoTab.NOTES) {
                    MemoBatchBar(
                        count = noteSelection.size,
                        allSelected = allSelected,
                        onToggleSelectAll = {
                            noteSelection = if (allSelected) emptySet() else visibleIds
                        },
                        onExit = { clearSelection() },
                    ) {
                        val hasSelection = noteSelection.isNotEmpty()
                        BatchAction(
                            text = stringResource(R.string.memo_batch_move),
                            iconRes = R.drawable.ic_folder,
                            enabled = hasSelection,
                            onClick = { movingNotes = noteSelection },
                        )
                        val allPinned = noteSelection.all { id ->
                            archive.notes.firstOrNull { it.id == id }?.pinned == true
                        }
                        BatchAction(
                            text = stringResource(
                                if (allPinned) R.string.memo_note_unpin else R.string.memo_note_pin,
                            ),
                            iconRes = R.drawable.ic_pin,
                            enabled = hasSelection,
                            onClick = { onPinNotes(noteSelection, !allPinned) },
                        )
                        BatchAction(
                            text = stringResource(R.string.memo_delete),
                            iconRes = R.drawable.ic_delete,
                            enabled = hasSelection,
                            danger = true,
                            onClick = { deletingNotes = noteSelection },
                        )
                    }
                } else {
                    MemoBatchBar(
                        count = todoSelection.size,
                        allSelected = allSelected,
                        onToggleSelectAll = {
                            todoSelection = if (allSelected) emptySet() else visibleIds
                        },
                        onExit = { clearSelection() },
                    ) {
                        val hasSelection = todoSelection.isNotEmpty()
                        BatchAction(
                            text = stringResource(R.string.memo_batch_todo_done),
                            iconRes = R.drawable.ic_check,
                            enabled = hasSelection,
                            onClick = {
                                onCompleteTodos(todoSelection, true)
                                clearSelection()
                            },
                        )
                        BatchAction(
                            text = stringResource(R.string.memo_batch_todo_undo),
                            iconRes = R.drawable.ic_undo,
                            enabled = hasSelection,
                            onClick = {
                                onCompleteTodos(todoSelection, false)
                                clearSelection()
                            },
                        )
                        BatchAction(
                            text = stringResource(R.string.memo_delete),
                            iconRes = R.drawable.ic_delete,
                            enabled = hasSelection,
                            danger = true,
                            onClick = { deletingTodos = todoSelection },
                        )
                    }
                }
            } else {
                DetailTabBar(items = tabs, selectedId = tabs[selectedIndex].id) { item ->
                    clearSelection()
                    onSelectTab(MemoTab.ofId(item.id))
                }
            }
        }
    }

    movingNotes?.let { ids ->
        MoveNoteSheet(
            folders = archive.folders,
            ops = folderOps,
            onDismiss = { movingNotes = null },
            onSelect = { folderId ->
                onMoveNotes(ids, folderId)
                movingNotes = null
                clearSelection()
            },
        )
    }

    deletingNotes?.let { ids ->
        ConfirmDialog(
            title = if (ids.size > 1) {
                stringResource(R.string.memo_batch_delete_note_title, ids.size)
            } else {
                stringResource(R.string.memo_delete_note_title)
            },
            message = stringResource(R.string.memo_delete_note_message),
            confirmText = stringResource(R.string.memo_delete),
            cancelText = stringResource(R.string.common_cancel),
            onConfirm = {
                onDeleteNotes(ids)
                deletingNotes = null
                clearSelection()
            },
            onDismiss = { deletingNotes = null },
        )
    }

    deletingTodos?.let { ids ->
        ConfirmDialog(
            title = if (ids.size > 1) {
                stringResource(R.string.memo_batch_delete_todo_title, ids.size)
            } else {
                stringResource(R.string.memo_delete_todo_title)
            },
            message = stringResource(R.string.memo_delete_todo_message),
            confirmText = stringResource(R.string.memo_delete),
            cancelText = stringResource(R.string.common_cancel),
            onConfirm = {
                onDeleteTodos(ids)
                deletingTodos = null
                clearSelection()
            },
            onDismiss = { deletingTodos = null },
        )
    }
}

/** 选中集合的开关：点一下进、再点一下出，空了就自动退出多选（`selecting` 由它派生）。 */
private fun Set<String>.toggle(id: String): Set<String> =
    if (id in this) this - id else this + id

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
    hint: String,
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
                            text = hint,
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
    notes: List<MemoNote>,
    searching: Boolean,
    query: String,
    folderKey: String,
    selectionMode: Boolean,
    selection: Set<String>,
    onSelectFolder: (String) -> Unit,
    onOpenNote: (String) -> Unit,
    onToggleSelect: (String) -> Unit,
    onMoveNote: (MemoNote) -> Unit,
    onTogglePin: (MemoNote) -> Unit,
    onDeleteNote: (MemoNote) -> Unit,
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
                selectionMode = selectionMode,
                selected = note.id in selection,
                onOpen = { onOpenNote(note.id) },
                onToggleSelect = { onToggleSelect(note.id) },
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
    searching: Boolean,
    selectionMode: Boolean,
    selection: Set<String>,
    onOpenTodo: (String) -> Unit,
    onToggleTodoItem: (String, String) -> Unit,
    onToggleSelect: (String) -> Unit,
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
        if (searching) {
            item(key = "search-count") {
                Text(
                    text = stringResource(R.string.memo_search_todo_result_count, todos.size),
                    style = tokens.type.caption,
                    color = tokens.colors.textMuted,
                )
            }
        }
        if (todos.isEmpty()) {
            item {
                AppEmptyState(
                    title = stringResource(
                        if (searching) {
                            R.string.memo_search_todo_empty_title
                        } else {
                            R.string.memo_empty_todos_title
                        },
                    ),
                    description = stringResource(
                        if (searching) {
                            R.string.memo_search_todo_empty_desc
                        } else {
                            R.string.memo_empty_todos_desc
                        },
                    ),
                    iconRes = if (searching) R.drawable.ic_search else R.drawable.ic_todo,
                )
            }
        }
        items(todos, key = MemoTodo::id) { todo ->
            TodoCard(
                todo = todo,
                selectionMode = selectionMode,
                selected = todo.id in selection,
                onOpen = { onOpenTodo(todo.id) },
                onToggleSelect = { onToggleSelect(todo.id) },
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
                    name = stringResource(R.string.memo_folder_up),
                    count = null,
                    selected = false,
                    onClick = {
                        val parentId = path.dropLast(1).lastOrNull()?.id
                        onSelectFolder(parentId?.let(::memoFolderKey) ?: MEMO_FILTER_ALL)
                    },
                )
            } else {
                FolderChip(
                    name = stringResource(R.string.memo_filter_all),
                    count = archive.notes.size,
                    selected = folderKey == MEMO_FILTER_ALL,
                    onClick = { onSelectFolder(MEMO_FILTER_ALL) },
                )
                FolderChip(
                    name = stringResource(R.string.memo_filter_unfiled),
                    count = archive.notes.count { it.folderId == null },
                    selected = folderKey == MEMO_FILTER_UNFILED,
                    onClick = { onSelectFolder(MEMO_FILTER_UNFILED) },
                )
            }
            children.forEach { folder ->
                FolderChip(
                    name = folder.name,
                    count = archive.notes.count { it.folderId == folder.id },
                    selected = currentFolderId == folder.id,
                    onClick = { onSelectFolder(memoFolderKey(folder.id)) },
                )
            }
        }
    }
}

/**
 * 文件夹胶囊：名称与笔记数分成两段文字。
 *
 * 数字单独一色是因为它跟名称同色时，「工作 12」会被读成一个词；分色后名称是主体、
 * 数字是计数。两态的取色都保证与名称形成对比：未选中名称是次要灰、数字用主色；
 * 选中时胶囊底是主色容器，名称保留主色、数字换成容器上的高对比前景色。
 */
@Composable
private fun FolderChip(
    name: String,
    count: Int?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val tokens = LocalAppTokens.current
    val shape = RoundedCornerShape(tokens.radii.pill)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(if (selected) tokens.colors.primaryContainer else tokens.colors.panel)
            .border(tokens.radii.highlight, if (selected) tokens.colors.primary else tokens.colors.border, shape)
            .clickable(onClick = onClick)
            .heightIn(min = 32.dp)
            .padding(horizontal = tokens.spacing.s3, vertical = tokens.spacing.s1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = name,
            style = tokens.type.caption,
            color = if (selected) tokens.colors.primary else tokens.colors.textMuted,
            maxLines = 1,
        )
        if (count != null && count > 0) {
            Spacer(Modifier.width(tokens.spacing.s1))
            Text(
                text = count.toString(),
                style = tokens.type.caption.copy(fontWeight = FontWeight.SemiBold),
                color = if (selected) tokens.colors.onPrimaryContainer else tokens.colors.primary,
                maxLines = 1,
            )
        }
    }
}

/**
 * 笔记卡。
 *
 * 行尾菜单收着四个动作：移动到 / 置顶 / 复制文件夹路径 / 删除——入口统一用省略号图标，
 * 与文件夹行尾的【更多】同一套语法。置顶只改排序标记，不碰内容也不压历史版本；
 * 复制文件夹路径是为了之后的搜索：把路径粘进搜索框就能直达那个文件夹下的笔记。
 * 搜索态下标题与摘要里的关键词用主色加粗高亮，扫一眼就知道为什么这条会出现在结果里。
 *
 * 多选时行尾换成勾选圈、点卡片改为选中：这时候「打开这条笔记」不是用户想做的事。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NoteCard(
    note: MemoNote,
    folderPath: String,
    folderLabel: String?,
    query: String,
    selectionMode: Boolean,
    selected: Boolean,
    onOpen: () -> Unit,
    onToggleSelect: () -> Unit,
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
            .background(if (selected) tokens.colors.primary.copy(alpha = 0.08f) else tokens.colors.panelRaised)
            .border(
                if (selected) 1.5.dp else tokens.radii.highlight,
                if (selected) tokens.colors.primary else tokens.colors.border,
                shape,
            )
            .combinedClickable(
                onClick = { if (selectionMode) onToggleSelect() else onOpen() },
                onLongClick = { onToggleSelect() },
            )
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
            if (selectionMode) {
                SelectionMark(selected = selected, onToggle = onToggleSelect)
            } else {
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
        // 页脚分两角：左下是「时间 + 字数 + 语音数」（时间带年份，跨年翻旧笔记不会误读），
        // 右下是分类。分类靠右是为了让同类卡片的归属对齐成一列，扫一眼就能按文件夹分组看。
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = tokens.spacing.s2, end = tokens.spacing.s2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = buildList {
                    add(formatMemoTimeFull(note.updatedAtMillis))
                    add(stringResource(R.string.memo_meta_words, note.wordCount))
                    if (note.voiceCount > 0) add(stringResource(R.string.memo_meta_voices, note.voiceCount))
                }.joinToString(" · "),
                style = tokens.type.caption,
                color = tokens.colors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (!folderLabel.isNullOrBlank()) {
                Spacer(Modifier.width(tokens.spacing.s2))
                Icon(
                    painter = painterResource(R.drawable.ic_folder),
                    contentDescription = null,
                    tint = tokens.colors.textMuted,
                    modifier = Modifier.size(12.dp),
                )
                Spacer(Modifier.width(tokens.spacing.s1))
                Text(
                    text = folderLabel,
                    style = tokens.type.caption,
                    color = tokens.colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 150.dp),
                )
            }
        }
    }
}

/** 多选勾选圈：20dp 圆形，选中时填主色 + 对勾；点击区域仍是整行高的 48dp。 */
@Composable
private fun SelectionMark(selected: Boolean, onToggle: () -> Unit) {
    val tokens = LocalAppTokens.current
    Box(
        modifier = Modifier
            .size(tokens.spacing.hitMin)
            .clip(CircleShape)
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(if (selected) tokens.colors.primary else Color.Transparent)
                .border(
                    width = if (selected) 0.dp else 1.5.dp,
                    color = if (selected) tokens.colors.primary else tokens.colors.borderStrong,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    tint = tokens.colors.onPrimary,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
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
 * - 行尾固定一个省略号【更多】给单条操作（目前只有删除），长按留给多选——
 *   与笔记卡同一套手势语法：长按 = 批处理，省略号 = 就这一条。
 * - 多选态下待办项的勾选框**暂时收起**：那个勾是「这一项做完了」，卡片的选中是「批处理这条」，
 *   两个勾同屏时会点错也看不出错在哪。整行的点击此时统一归「选中这条」，要勾选先退出多选。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TodoCard(
    todo: MemoTodo,
    selectionMode: Boolean,
    selected: Boolean,
    onOpen: () -> Unit,
    onToggleSelect: () -> Unit,
    onToggle: (MemoTodoItem) -> Unit,
    onDelete: () -> Unit,
) {
    val tokens = LocalAppTokens.current
    val shape = RoundedCornerShape(tokens.radii.container)
    var expanded by remember(todo.id) { mutableStateOf(true) }
    val isList = todo.items.size > 1

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) tokens.colors.primary.copy(alpha = 0.08f) else tokens.colors.panelRaised)
            .border(
                if (selected) 1.5.dp else tokens.radii.highlight,
                if (selected) tokens.colors.primary else tokens.colors.border,
                shape,
            )
            .combinedClickable(
                onClick = { if (selectionMode) onToggleSelect() else onOpen() },
                onLongClick = { onToggleSelect() },
            )
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
                TodoTrailing(
                    selectionMode = selectionMode,
                    selected = selected,
                    onToggleSelect = onToggleSelect,
                    onDelete = onDelete,
                )
            }
            if (expanded) {
                todo.displayItems().forEach { item ->
                    TodoItemRow(item = item, onToggle = onToggle, selectionMode = selectionMode)
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = tokens.spacing.hitMin),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val item = todo.items.firstOrNull()
                if (item == null) {
                    Text(
                        text = stringResource(R.string.memo_untitled_todo),
                        style = tokens.type.bodyS,
                        color = tokens.colors.textMuted,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = tokens.spacing.s3, end = tokens.spacing.s3),
                    )
                } else {
                    TodoItemRow(
                        item = item,
                        onToggle = onToggle,
                        modifier = Modifier.weight(1f),
                        selectionMode = selectionMode,
                    )
                }
                TodoTrailing(
                    selectionMode = selectionMode,
                    selected = selected,
                    onToggleSelect = onToggleSelect,
                    onDelete = onDelete,
                )
            }
        }
    }
}

/** 卡片行尾槽位：平常是省略号菜单，多选时换成勾选圈。 */
@Composable
private fun TodoTrailing(
    selectionMode: Boolean,
    selected: Boolean,
    onToggleSelect: () -> Unit,
    onDelete: () -> Unit,
) {
    val tokens = LocalAppTokens.current
    var menuOpen by remember { mutableStateOf(false) }
    if (selectionMode) {
        SelectionMark(selected = selected, onToggle = onToggleSelect)
        return
    }
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
                text = { Text(stringResource(R.string.memo_delete), color = tokens.colors.danger) },
                onClick = {
                    menuOpen = false
                    onDelete()
                },
            )
        }
    }
}

@Composable
private fun TodoItemRow(
    item: MemoTodoItem,
    onToggle: (MemoTodoItem) -> Unit,
    modifier: Modifier = Modifier,
    selectionMode: Boolean = false,
) {
    val tokens = LocalAppTokens.current
    Row(
        modifier = modifier
            .heightIn(min = tokens.spacing.hitMin)
            .padding(start = tokens.spacing.s2, end = tokens.spacing.s1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 多选时勾选框暂时收起：这里的勾是「这一项做完了」，卡片的选中是「批处理这条待办」，
        // 两个勾并排出现时点错也看不出错的是哪一个。只留同宽空位，文字不左右跳。
        if (selectionMode) {
            Spacer(Modifier.width(48.dp))
        } else {
            Checkbox(
                checked = item.done,
                onCheckedChange = { onToggle(item) },
                colors = CheckboxDefaults.colors(
                    checkedColor = tokens.colors.primary,
                    uncheckedColor = tokens.colors.textMuted,
                ),
            )
        }
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

/**
 * 多选操作栏：占据底部切换栏的位置。
 *
 * 顶行是「已选 N 项 + 全选 / 退出」，下面一行是批量动作——批量动作与切换栏同一个高度带，
 * 拇指不用移动就能从「看列表」切到「处理这批」。删掉的动作不做二次确认以外的手势保护：
 * 批量删除已经有一次确认弹窗，伤害面比误触一次勾选大得多。
 */
@Composable
private fun MemoBatchBar(
    count: Int,
    allSelected: Boolean,
    onToggleSelectAll: () -> Unit,
    onExit: () -> Unit,
    actions: @Composable RowScope.() -> Unit,
) {
    val tokens = LocalAppTokens.current
    Surface(
        color = tokens.colors.panel,
        shadowElevation = 12.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(adaptiveNavigationBarWindowInsets())
                .padding(
                    start = tokens.spacing.page,
                    end = tokens.spacing.page,
                    top = tokens.spacing.s1,
                    bottom = tokens.spacing.s2,
                ),
            verticalArrangement = Arrangement.spacedBy(tokens.spacing.s1),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.memo_batch_selected, count),
                    style = tokens.type.bodyS.copy(fontWeight = FontWeight.SemiBold),
                    color = tokens.colors.primary,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(
                        if (allSelected) R.string.memo_batch_clear_all else R.string.memo_batch_select_all,
                    ),
                    style = tokens.type.caption,
                    color = tokens.colors.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(tokens.radii.pill))
                        .clickable(onClick = onToggleSelectAll)
                        .padding(horizontal = tokens.spacing.s2, vertical = tokens.spacing.s1),
                )
                IconButton(onClick = onExit, modifier = Modifier.size(tokens.spacing.hitMin)) {
                    Icon(
                        painter = painterResource(R.drawable.ic_cancel),
                        contentDescription = stringResource(R.string.memo_batch_exit),
                        tint = tokens.colors.textMuted,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(tokens.spacing.s2),
                content = actions,
            )
        }
    }
}

/** 批量动作按钮：图标 + 文案等分一行，危险动作换成红字淡红底；没有选中项时置灰不可点。 */
@Composable
private fun RowScope.BatchAction(
    text: String,
    iconRes: Int,
    enabled: Boolean = true,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    val tokens = LocalAppTokens.current
    val shape = RoundedCornerShape(tokens.radii.pill)
    val contentColor = when {
        !enabled -> tokens.colors.disabled
        danger -> tokens.colors.danger
        else -> tokens.colors.text
    }
    val containerColor = when {
        !enabled -> tokens.colors.panel
        danger -> tokens.colors.danger.copy(alpha = 0.12f)
        else -> tokens.colors.panelRaised
    }
    val borderColor = when {
        !enabled -> tokens.colors.border
        danger -> tokens.colors.danger.copy(alpha = 0.5f)
        else -> tokens.colors.border
    }
    Row(
        modifier = Modifier
            .weight(1f)
            .heightIn(min = 44.dp)
            .clip(shape)
            .background(containerColor)
            .border(tokens.radii.highlight, borderColor, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = tokens.spacing.s2),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = contentColor,
        )
        Spacer(Modifier.width(tokens.spacing.s1))
        Text(
            text = text,
            style = tokens.type.bodyS,
            color = contentColor,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

internal fun formatMemoTime(millis: Long): String =
    SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(millis))

/** 带年份的完整时刻：列表页脚与历史版本用，跨年翻旧内容时不会把 2025 的当成今年的。 */
internal fun formatMemoTimeFull(millis: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(millis))

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

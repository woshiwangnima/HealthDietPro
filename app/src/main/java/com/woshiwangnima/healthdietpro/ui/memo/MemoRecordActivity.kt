package com.woshiwangnima.healthdietpro.ui.memo

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.woshiwangnima.healthdietpro.base.BaseActivity
import com.woshiwangnima.healthdietpro.common.ui.HealthDietProTheme
import com.woshiwangnima.healthdietpro.model.memo.MemoTodo

/** 记便签：【笔记】（可分层文件夹）与【待办】两个板块，右上角进便签设置。 */
class MemoRecordActivity : BaseActivity() {

    companion object {
        /** 从记录中心的「快速添加」进来时直接打新建笔记。 */
        const val EXTRA_NEW_NOTE = "new_note"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HealthDietProTheme {
                MemoRoute(
                    onFinish = ::finish,
                    openNewNote = intent.getBooleanExtra(EXTRA_NEW_NOTE, false),
                )
            }
        }
    }
}

private enum class MemoRoute { HOME, NOTE_EDITOR, TODO_EDITOR, SETTINGS }

@Composable
private fun MemoRoute(onFinish: () -> Unit, openNewNote: Boolean) {
    val viewModel: MemoViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var routeName by rememberSaveable {
        mutableStateOf(if (openNewNote) MemoRoute.NOTE_EDITOR.name else MemoRoute.HOME.name)
    }
    var editingNoteId by rememberSaveable { mutableStateOf<String?>(null) }
    var editingTodoId by rememberSaveable { mutableStateOf<String?>(null) }
    var tabId by rememberSaveable { mutableStateOf(MemoTab.NOTES.id) }
    var folderKey by rememberSaveable { mutableStateOf(MEMO_FILTER_ALL) }

    val route = runCatching { MemoRoute.valueOf(routeName) }.getOrDefault(MemoRoute.HOME)
    val tab = MemoTab.ofId(tabId)

    when (route) {
        MemoRoute.HOME -> MemoHomeScreen(
            uiState = uiState,
            tab = tab,
            onSelectTab = { tabId = it.id },
            folderKey = folderKey,
            onSelectFolder = { folderKey = it },
            folderOps = viewModel.folderOps,
            onCreateNote = {
                editingNoteId = null
                routeName = MemoRoute.NOTE_EDITOR.name
            },
            onOpenNote = { noteId ->
                editingNoteId = noteId
                routeName = MemoRoute.NOTE_EDITOR.name
            },
            onDeleteNote = viewModel::deleteNote,
            onMoveNote = { note, folderId -> viewModel.moveNote(note.id, folderId) },
            onTogglePin = { note -> viewModel.setNotePinned(note.id, !note.pinned) },
            onCreateTodo = {
                editingTodoId = null
                routeName = MemoRoute.TODO_EDITOR.name
            },
            onOpenTodo = { todoId ->
                editingTodoId = todoId
                routeName = MemoRoute.TODO_EDITOR.name
            },
            onToggleTodoItem = viewModel::toggleTodoItem,
            onDeleteTodo = { todo -> viewModel.deleteTodo(todo.id) },
            onOpenSettings = { routeName = MemoRoute.SETTINGS.name },
            onBack = onFinish,
        )

        MemoRoute.NOTE_EDITOR -> {
            // 进程被回收后恢复到编辑页时，存档还没读回来：先等一次，否则会用一个刚生成的 id
            // 重新开一条空笔记，用户看起来像「刚才写的内容没了」。
            if (editingNoteId != null && !uiState.loaded) {
                MemoLoading()
            } else {
                NoteEditorScreen(
                    existing = editingNoteId?.let { id -> uiState.archive.notes.firstOrNull { it.id == id } },
                    initialFolderId = memoFolderIdOf(folderKey),
                    folders = uiState.archive.folders,
                    historyDepth = uiState.archive.historyDepth,
                    onMove = viewModel::moveNote,
                    onDelete = viewModel::deleteNote,
                    onBack = {
                        routeName = MemoRoute.HOME.name
                        editingNoteId = null
                    },
                )
            }
        }

        MemoRoute.TODO_EDITOR -> {
            if (editingTodoId != null && !uiState.loaded) {
                MemoLoading()
            } else {
                val existing: MemoTodo? = editingTodoId?.let { id -> uiState.archive.todos.firstOrNull { it.id == id } }
                TodoEditorScreen(
                    existing = existing,
                    onBack = {
                        routeName = MemoRoute.HOME.name
                        editingTodoId = null
                    },
                )
            }
        }

        MemoRoute.SETTINGS -> MemoSettingsScreen(
            uiState = uiState,
            onBack = { routeName = MemoRoute.HOME.name },
        )
    }
}

@Composable
private fun MemoLoading() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

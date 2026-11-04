package com.woshiwangnima.healthdietpro.ui.memo

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.woshiwangnima.healthdietpro.model.memo.ImportedVoice
import com.woshiwangnima.healthdietpro.model.memo.MemoArchive
import com.woshiwangnima.healthdietpro.model.memo.MemoArchiveStore
import com.woshiwangnima.healthdietpro.model.memo.MemoBlockKind
import com.woshiwangnima.healthdietpro.model.memo.MemoNote
import com.woshiwangnima.healthdietpro.model.memo.MemoRepository
import com.woshiwangnima.healthdietpro.model.memo.MemoTodo
import com.woshiwangnima.healthdietpro.model.memo.MemoVoiceStore
import com.woshiwangnima.healthdietpro.model.memo.VoiceAdoptResult
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal data class MemoUiState(
    val archive: MemoArchive = MemoArchive(),
    /** 首次读盘完成前不渲染列表，避免「先闪一下空态再出内容」。 */
    val loaded: Boolean = false,
)

internal class MemoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MemoRepository(MemoArchiveStore.current(application))
    val voices: MemoVoiceStore = MemoVoiceStore.current(application)

    private val _uiState = MutableStateFlow(MemoUiState())
    val uiState: StateFlow<MemoUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val archive = withContext(Dispatchers.IO) { repository.load() }
            _uiState.value = MemoUiState(archive, loaded = true)
        }
    }

    /**
     * 所有写操作都在 IO 线程跑完再把整份存档推回 UI。
     *
     * 编辑器退出时会连着触发两次保存（返回手势 + onDispose 兜底），
     * 这里不做去重，靠 `mergedWith` 的「内容没变就不产生历史版本」保证幂等。
     */
    private fun mutate(transform: (MemoRepository) -> MemoArchive) {
        viewModelScope.launch {
            val archive = withContext(Dispatchers.IO) { transform(repository) }
            _uiState.value = MemoUiState(archive, loaded = true)
        }
    }

    fun saveNote(note: MemoNote) = mutate { it.saveNote(note) }

    fun deleteNote(note: MemoNote) = mutate { repo ->
        val archive = repo.deleteNote(note.id)
        // 语音文件只在附件目录里，删掉笔记后把没人引用的清掉，别让档案背着孤儿音频。
        pruneOrphanVoices(archive)
        archive
    }

    fun moveNote(noteId: String, folderId: String?) = mutate { it.moveNote(noteId, folderId) }

    fun setNotePinned(noteId: String, pinned: Boolean) = mutate { it.setNotePinned(noteId, pinned) }

    fun saveTodo(todo: MemoTodo) = mutate { it.saveTodo(todo) }

    fun deleteTodo(todoId: String) = mutate { it.deleteTodo(todoId) }

    fun toggleTodoItem(todoId: String, itemId: String) = mutate { it.toggleTodoItem(todoId, itemId) }

    fun createFolder(name: String, parentId: String?) = mutate { it.createFolder(name, parentId) }

    fun renameFolder(folderId: String, name: String) = mutate { it.renameFolder(folderId, name) }

    fun deleteFolder(folderId: String) = mutate { it.deleteFolder(folderId) }

    fun moveFolder(folderId: String, newParentId: String?) = mutate { it.moveFolder(folderId, newParentId) }

    fun setHistoryDepth(depth: Int) = mutate { it.setHistoryDepth(depth) }

    /** 录音结束：把缓存文件搬进用户附件目录，返回可写进笔记的裸文件名。 */
    suspend fun adoptRecording(temp: File): VoiceAdoptResult = withContext(Dispatchers.IO) {
        voices.adoptRecording(temp)
    }

    /** 从系统选择器导入音频：复制一份进档案，原文件不动。 */
    suspend fun importVoice(uri: Uri): ImportedVoice? = withContext(Dispatchers.IO) {
        voices.importAudio(uri).getOrNull()
    }

    /**
     * 清理没人引用的语音文件。
     *
     * 引用面要算上历史版本：5 个旧版本可能还引用着当前内容里已经删掉的录音，
     * 按「当前块」清理会把历史版本播放变成哑的。
     */
    private fun pruneOrphanVoices(archive: MemoArchive) {
        val referenced = archive.notes.flatMap { note ->
            note.blocks + note.versions.flatMap { it.blocks }
        }.filter { it.kind == MemoBlockKind.VOICE }
            .mapTo(mutableSetOf()) { it.voiceFile }
        voices.directory().listFiles()?.forEach { file ->
            if (file.name !in referenced) file.delete()
        }
    }
}

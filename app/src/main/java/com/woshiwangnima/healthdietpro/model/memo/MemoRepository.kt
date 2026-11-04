package com.woshiwangnima.healthdietpro.model.memo

import kotlin.math.abs
import kotlin.random.Random

/**
 * 便签的全部写操作。
 *
 * 所有方法都返回**写完后的整份存档**，调用方直接用它刷新 UI——
 * 不让 ViewModel 自己改内存副本，避免界面显示与磁盘不一致。
 */
internal class MemoRepository(private val store: MemoArchiveStore) {

    fun load(): MemoArchive = store.load()

    fun saveNote(note: MemoNote): MemoArchive = store.update { archive ->
        val existing = archive.notes.firstOrNull { it.id == note.id }
        val merged = if (existing == null) note else note.copy(
            // 历史版本与创建时间由存档侧保留，编辑器只负责标题和内容。
            versions = note.versions.ifEmpty { existing.versions },
            createdAtMillis = existing.createdAtMillis,
        )
        archive.copy(
            notes = (archive.notes.filterNot { it.id == merged.id } + merged)
                .sortedByDescending(MemoNote::updatedAtMillis),
        )
    }

    fun deleteNote(noteId: String): MemoArchive = store.update { archive ->
        archive.copy(notes = archive.notes.filterNot { it.id == noteId })
    }

    fun moveNote(noteId: String, folderId: String?): MemoArchive = store.update { archive ->
        archive.copy(
            notes = archive.notes.map { note ->
                if (note.id == noteId) note.copy(folderId = folderId) else note
            },
        )
    }

    /**
     * 置顶/取消置顶：直接改 `pinned` 标记，不动 `updatedAtMillis`、不压历史版本——
     * 它是列表元数据，不是内容；跟着内容排序走（置顶组内仍按更新时间）即可。
     */
    fun setNotePinned(noteId: String, pinned: Boolean): MemoArchive = store.update { archive ->
        archive.copy(
            notes = archive.notes.map { note ->
                if (note.id == noteId && note.pinned != pinned) note.copy(pinned = pinned) else note
            },
        )
    }

    fun saveTodo(todo: MemoTodo): MemoArchive = store.update { archive ->
        val existing = archive.todos.firstOrNull { it.id == todo.id }
        val merged = if (existing == null) todo else todo.copy(createdAtMillis = existing.createdAtMillis)
        archive.copy(todos = archive.todos.filterNot { it.id == merged.id } + merged)
    }

    fun deleteTodo(todoId: String): MemoArchive = store.update { archive ->
        archive.copy(todos = archive.todos.filterNot { it.id == todoId })
    }

    fun toggleTodoItem(todoId: String, itemId: String, now: Long = System.currentTimeMillis()): MemoArchive =
        store.update { archive ->
            archive.copy(
                todos = archive.todos.map { todo ->
                    if (todo.id == todoId) todo.toggledItem(itemId, now) else todo
                },
            )
        }

    fun createFolder(name: String, parentId: String?, now: Long = System.currentTimeMillis()): MemoArchive =
        store.update { archive ->
            val parent = parentId?.takeIf { id -> archive.folders.any { it.id == id } }
            val folder = MemoFolder(
                id = newMemoId("folder"),
                name = name.trim(),
                parentId = parent,
                createdAtMillis = now,
            )
            archive.copy(folders = archive.folders + folder)
        }

    fun renameFolder(folderId: String, name: String): MemoArchive = store.update { archive ->
        archive.copy(
            folders = archive.folders.map { folder ->
                if (folder.id == folderId) folder.copy(name = name.trim()) else folder
            },
        )
    }

    /**
     * 删除文件夹：子文件夹与其中的笔记**上提一级**，而不是一起删掉。
     *
     * 删文件夹是个整理动作，用户预期是「这个层级不要了」，不是「把里面的东西清空」。
     * 具体语义见 `MemoArchive.withoutFolder`（与测试指令共用）。
     */
    fun deleteFolder(folderId: String): MemoArchive = store.update { archive ->
        archive.withoutFolder(folderId)
    }

    fun moveFolder(folderId: String, newParentId: String?): MemoArchive = store.update { archive ->
        if (!archive.folders.canMoveFolder(folderId, newParentId)) return@update archive
        archive.copy(
            folders = archive.folders.map { folder ->
                if (folder.id == folderId) folder.copy(parentId = newParentId) else folder
            },
        )
    }

    fun setHistoryDepth(depth: Int): MemoArchive = store.update { archive ->
        archive.copy(historyDepth = depth.coerceIn(MIN_MEMO_HISTORY_DEPTH, MAX_MEMO_HISTORY_DEPTH))
    }
}

/** 便签实体 id：前缀 + 时间 + 随机数，避免同一毫秒内连点生成重复 id。 */
internal fun newMemoId(prefix: String): String =
    "memo_${prefix}_${System.currentTimeMillis()}_${abs(Random.nextInt(1_000_000))}"

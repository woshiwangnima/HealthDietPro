package com.woshiwangnima.healthdietpro.model.memo

import kotlinx.serialization.Serializable

internal const val MEMO_ARCHIVE_SCHEMA_VERSION = 1
internal const val DEFAULT_MEMO_HISTORY_DEPTH = 5
internal const val MIN_MEMO_HISTORY_DEPTH = 1
internal const val MAX_MEMO_HISTORY_DEPTH = 20

/**
 * 记便签的全部数据：文件夹、笔记、待办、设置。
 *
 * **一个领域一个文件**：文件夹与笔记是一张树、待办与笔记共用同一份设置（历史保留次数），
 * 拆成三个文件反而要在导入导出时保证三份互相自洽，所以统一放在 `notes.json`。
 */
@Serializable
internal data class MemoArchive(
    val schemaVersion: Int = MEMO_ARCHIVE_SCHEMA_VERSION,
    val folders: List<MemoFolder> = emptyList(),
    val notes: List<MemoNote> = emptyList(),
    val todos: List<MemoTodo> = emptyList(),
    /** 历史版本保留次数，按栈保留最近 N 次。 */
    val historyDepth: Int = DEFAULT_MEMO_HISTORY_DEPTH,
) {
    fun folderName(id: String?): String? = id?.let { fid -> folders.firstOrNull { it.id == fid }?.name }
}

/** 笔记文件夹。`parentId` 为空表示根级；层级不限，靠 [MemoArchive.folders] 递归解析。 */
@Serializable
internal data class MemoFolder(
    val id: String,
    val name: String,
    val parentId: String? = null,
    val createdAtMillis: Long,
)

@Serializable
internal enum class MemoBlockKind {
    TEXT,
    VOICE,
}

/**
 * 笔记内容块。
 *
 * 用「扁平块 + kind」而不是 sealed 层级：项目里的 `FoodDto` 也是这个写法，
 * `ignoreUnknownKeys` 保证以后加块类型（图片、表格）时旧版本不会解析失败。
 *
 * **语音只存文件名**：`voiceFile` 是 `<uid>/attachments/notes/voice/` 下的裸文件名，
 * 运行时按当前用户目录拼路径。这样导入到别的用户 / 别的设备不需要改写任何路径——
 * 而 medications 那套存绝对路径的方案必须配套 `rewrite`，多一处就可能漏一处。
 */
@Serializable
internal data class MemoBlock(
    val kind: MemoBlockKind = MemoBlockKind.TEXT,
    val text: String = "",
    val voiceFile: String = "",
    val durationMs: Long = 0L,
    /** 从手机音频文件导入时的原始文件名，仅用于展示；录音时为空。 */
    val sourceName: String = "",
    /** UI 用的稳定 id（key / 焦点定位），不参与业务语义。 */
    val id: String = "",
)

/** 一次保存前的快照。栈式保存，越靠后越新。 */
@Serializable
internal data class MemoNoteVersion(
    val savedAtMillis: Long,
    val title: String = "",
    val blocks: List<MemoBlock> = emptyList(),
)

@Serializable
internal data class MemoNote(
    val id: String,
    /** 为空表示未归档（不在任何文件夹下）。 */
    val folderId: String? = null,
    val title: String = "",
    val blocks: List<MemoBlock> = emptyList(),
    /** 历史版本，旧 → 新，长度上限见 [MemoArchive.historyDepth]。 */
    val versions: List<MemoNoteVersion> = emptyList(),
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    /**
     * 在当前列表置顶。只影响排序，**不属于内容**：
     * 置顶/取消置顶走 `MemoRepository.setNotePinned` 直接改存档，
     * 不经过 `mergedWith`，因此永远不会压历史版本栈。
     */
    val pinned: Boolean = false,
)

@Serializable
internal data class MemoTodoItem(
    val id: String,
    val text: String,
    val done: Boolean = false,
    /** 勾选时刻，用于「勾掉的项目按勾选先后沉底」；未勾选为 0。 */
    val doneAtMillis: Long = 0L,
)

/**
 * 一条待办：一条标题 + 若干待办项。
 *
 * 待办项顺序不靠单独的排序字段：未勾选的按录入顺序在前，已勾选的按勾选时间在后面，
 * 这样「勾选后立刻沉底」不需要每勾一次就重排整份存档。
 */
@Serializable
internal data class MemoTodo(
    val id: String,
    val title: String = "",
    val items: List<MemoTodoItem> = emptyList(),
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

/** 字数：不计空白字符（空格、换行、制表）。中文按字、英文按字母，口径统一。 */
internal fun countMemoWords(text: String): Int = text.count { !it.isWhitespace() }

internal fun List<MemoBlock>.memoWordCount(): Int =
    sumOf { block -> if (block.kind == MemoBlockKind.TEXT) countMemoWords(block.text) else 0 }

internal fun List<MemoBlock>.memoVoiceCount(): Int = count { it.kind == MemoBlockKind.VOICE }

internal val MemoNote.wordCount: Int get() = blocks.memoWordCount()

internal val MemoNote.voiceCount: Int get() = blocks.memoVoiceCount()

/** 列表摘要：第一段非空正文压平空白，最多 [limit] 字。 */
internal fun MemoNote.previewText(limit: Int = 80): String =
    blocks.asSequence()
        .filter { it.kind == MemoBlockKind.TEXT }
        .map { it.text.trim() }
        .firstOrNull { it.isNotEmpty() }
        ?.replace(Regex("\\s+"), " ")
        ?.take(limit)
        .orEmpty()

/** 笔记是否有实际内容；新建后没写任何字就退出的笔记不该留在列表里。 */
internal val MemoNote.hasContent: Boolean
    get() = title.isNotBlank() || blocks.any { block ->
        when (block.kind) {
            MemoBlockKind.TEXT -> block.text.isNotBlank()
            MemoBlockKind.VOICE -> block.voiceFile.isNotBlank()
        }
    }

/**
 * 退出编辑时合并保存：内容没变就原样返回（不产生空历史版本）。
 *
 * 有变化时把**保存前的内容**压进历史栈，再把栈裁到 [depth]。
 * 新建笔记第一次保存（标题和正文都还空）不压栈，否则每次打开空笔记都多一条空历史。
 */
internal fun MemoNote.mergedWith(
    title: String,
    blocks: List<MemoBlock>,
    depth: Int,
    now: Long,
): MemoNote {
    val nextTitle = title.trim()
    if (nextTitle == this.title && blocks == this.blocks) return this
    if (this.title.isBlank() && this.blocks.all { it.isBlank() }) {
        return copy(title = nextTitle, blocks = blocks, updatedAtMillis = now)
    }
    val snapshot = MemoNoteVersion(savedAtMillis = updatedAtMillis, title = this.title, blocks = this.blocks)
    return copy(
        title = nextTitle,
        blocks = blocks,
        versions = (versions + snapshot).takeLast(depth.coerceIn(MIN_MEMO_HISTORY_DEPTH, MAX_MEMO_HISTORY_DEPTH)),
        updatedAtMillis = now,
    )
}

/** 恢复历史版本：当前内容同样压栈，保证「恢复」本身也可以反悔。 */
internal fun MemoNote.restoredFrom(
    version: MemoNoteVersion,
    depth: Int,
    now: Long,
): MemoNote {
    val snapshot = MemoNoteVersion(savedAtMillis = updatedAtMillis, title = title, blocks = blocks)
    return copy(
        title = version.title,
        blocks = version.blocks,
        versions = (versions + snapshot).takeLast(depth.coerceIn(MIN_MEMO_HISTORY_DEPTH, MAX_MEMO_HISTORY_DEPTH)),
        updatedAtMillis = now,
    )
}

internal fun MemoBlock.isBlank(): Boolean = when (kind) {
    MemoBlockKind.TEXT -> text.isBlank()
    MemoBlockKind.VOICE -> voiceFile.isBlank()
}

/**
 * 笔记列表顺序：置顶在前，其余按最近更新倒序。
 *
 * 「针对当前列表置顶」的语义落在调用方：先按文件夹筛选，再整体过本函数。
 * 不叫 `sortedForDisplay`：那个名字已经给了待办列表，两个泛型扩展同名会在 JVM 层撞签名。
 */
internal fun List<MemoNote>.withPinnedFirst(): List<MemoNote> =
    sortedWith(compareByDescending(MemoNote::pinned).thenByDescending(MemoNote::updatedAtMillis))

/** 文件夹路径的可复制字符串：`工作/2026`。未归档或文件夹不存在时为空。 */
internal fun List<MemoFolder>.pathStringOf(folderId: String?): String =
    pathOf(folderId).joinToString("/") { it.name }

/**
 * 搜索匹配：命中标题、任一文本块或所属文件夹路径即算（不区分大小写）。
 *
 * 文件夹路径参与匹配，是为了配合「复制文件夹路径」：把复制来的路径粘进搜索框，
 * 就能直达那个文件夹下的全部笔记。
 */
internal fun MemoNote.matchesQuery(rawQuery: String, folderPath: String = ""): Boolean {
    val query = rawQuery.trim()
    if (query.isEmpty()) return true
    if (title.contains(query, ignoreCase = true)) return true
    blocks.forEach { block ->
        if (block.kind == MemoBlockKind.TEXT && block.text.contains(query, ignoreCase = true)) return true
    }
    return folderPath.contains(query, ignoreCase = true)
}

/** 文件夹子级，保持创建顺序（不按名称排：中文按码位排出来对用户没有意义）。 */
internal fun List<MemoFolder>.childrenOf(parentId: String?): List<MemoFolder> =
    filter { it.parentId == parentId }.sortedBy(MemoFolder::createdAtMillis)

/** 从根到该文件夹的路径；文件夹不存在时返回空列表。带去重防止坏数据里的环把调用方拖死。 */
internal fun List<MemoFolder>.pathOf(folderId: String?): List<MemoFolder> {
    if (folderId == null) return emptyList()
    val byId = associateBy(MemoFolder::id)
    val path = ArrayDeque<MemoFolder>()
    val visited = mutableSetOf<String>()
    var current = byId[folderId]
    while (current != null && visited.add(current.id)) {
        path.addFirst(current)
        current = current.parentId?.let(byId::get)
    }
    return path.toList()
}

/** 该文件夹的全部后代 id（不含自身）。 */
internal fun List<MemoFolder>.descendantIdsOf(folderId: String): Set<String> {
    val result = mutableSetOf<String>()
    val queue = ArrayDeque<String>()
    queue += folderId
    val visited = mutableSetOf(folderId)
    while (queue.isNotEmpty()) {
        val current = queue.removeFirst()
        forEach { folder ->
            if (folder.parentId == current && visited.add(folder.id)) {
                result += folder.id
                queue += folder.id
            }
        }
    }
    return result
}

/** 能否把 [folderId] 挂到 [newParentId] 下：不能挂自己、不能挂自己的后代、父级必须存在。 */
internal fun List<MemoFolder>.canMoveFolder(folderId: String, newParentId: String?): Boolean {
    if (folderId == newParentId) return false
    if (newParentId == null) return true
    if (none { it.id == newParentId }) return false
    return newParentId !in descendantIdsOf(folderId)
}

internal data class MemoFolderRow(val folder: MemoFolder, val depth: Int)

/** 深度优先展开成带缩进层级的一维列表，供设置页的文件夹树使用。 */
internal fun List<MemoFolder>.flattened(): List<MemoFolderRow> {
    val rows = mutableListOf<MemoFolderRow>()
    fun visit(parentId: String?, depth: Int) {
        childrenOf(parentId).forEach { folder ->
            rows += MemoFolderRow(folder, depth)
            visit(folder.id, depth + 1)
        }
    }
    visit(null, 0)
    return rows
}

/**
 * 删除文件夹：子文件夹与其中的笔记**上提一级**，而不是一起删掉。
 *
 * 删文件夹是个整理动作，用户预期是「这个层级不要了」，不是「把里面的东西清空」。
 * 仓库的 `deleteFolder` 与测试指令的「删除所有笔记文件夹」共用这一份语义。
 */
internal fun MemoArchive.withoutFolder(folderId: String): MemoArchive {
    val target = folders.firstOrNull { it.id == folderId } ?: return this
    val parentId = target.parentId
    return copy(
        folders = folders
            .filterNot { it.id == folderId }
            .map { folder -> if (folder.parentId == folderId) folder.copy(parentId = parentId) else folder },
        notes = notes.map { note ->
            if (note.folderId == folderId) note.copy(folderId = parentId) else note
        },
    )
}

internal fun MemoTodo.pendingItems(): List<MemoTodoItem> = items.filterNot(MemoTodoItem::done)

internal fun MemoTodo.doneItems(): List<MemoTodoItem> = items.filter(MemoTodoItem::done).sortedBy(MemoTodoItem::doneAtMillis)

/** 展示顺序：未勾选按录入顺序，已勾选按勾选先后沉底。 */
internal fun MemoTodo.displayItems(): List<MemoTodoItem> = pendingItems() + doneItems()

internal val MemoTodo.isCompleted: Boolean
    get() = items.isNotEmpty() && items.all(MemoTodoItem::done)

internal val MemoTodo.doneCount: Int get() = items.count(MemoTodoItem::done)

/**
 * 待办列表顺序：未完成在前、已完成沉底；组内都按最近更新倒序。
 *
 * 整条待办的「全部勾完」不需要单独标记：`isCompleted` 由待办项派生，
 * 勾掉最后一项时 `updatedAtMillis` 会刷新，它自然出现在已完成组的队首。
 */
internal fun List<MemoTodo>.sortedForDisplay(): List<MemoTodo> {
    val sorted = sortedByDescending(MemoTodo::updatedAtMillis)
    return sorted.filterNot(MemoTodo::isCompleted) + sorted.filter(MemoTodo::isCompleted)
}

internal fun MemoTodo.toggledItem(itemId: String, now: Long): MemoTodo {
    val target = items.firstOrNull { it.id == itemId } ?: return this
    val nextDone = !target.done
    return copy(
        items = items.map { item ->
            if (item.id != itemId) {
                item
            } else {
                item.copy(done = nextDone, doneAtMillis = if (nextDone) now else 0L)
            }
        },
        updatedAtMillis = now,
    )
}

/** 列表标题：优先用标题，没写标题就取第一项文本；都没有时返回空串，交给 UI 兜底文案。 */
internal fun MemoTodo.displayTitle(): String =
    title.trim().ifBlank { items.firstOrNull { it.text.isNotBlank() }?.text?.trim().orEmpty() }

internal fun validateMemoArchive(archive: MemoArchive) {
    require(archive.schemaVersion == MEMO_ARCHIVE_SCHEMA_VERSION) { "Unsupported memo archive schema" }
    val folderIds = archive.folders.map(MemoFolder::id)
    require(folderIds.all(String::isNotBlank) && folderIds.distinct().size == folderIds.size) {
        "Invalid memo folder ids"
    }
    require(archive.folders.all { it.name.isNotBlank() }) { "Invalid memo folder name" }
    val knownFolders = folderIds.toSet()
    require(archive.folders.all { it.parentId == null || it.parentId in knownFolders }) {
        "Memo folder points to a missing parent"
    }
    // 逐条向上走并记录途经节点：只要回头踩到自己（或已走过的节点）就是环。
    // 不能用 pathOf 判：它在遇到环时会静默截断，截断后的路径看起来仍然「正常」。
    val byId = archive.folders.associateBy(MemoFolder::id)
    archive.folders.forEach { start ->
        val visited = mutableSetOf(start.id)
        var current = start.parentId?.let(byId::get)
        while (current != null) {
            require(visited.add(current.id)) { "Memo folder hierarchy contains a cycle" }
            current = current.parentId?.let(byId::get)
        }
    }
    val noteIds = archive.notes.map(MemoNote::id)
    require(noteIds.all(String::isNotBlank) && noteIds.distinct().size == noteIds.size) { "Invalid memo note ids" }
    require(archive.notes.all { it.folderId == null || it.folderId in knownFolders }) {
        "Memo note points to a missing folder"
    }
    require(archive.notes.all { note -> note.blocks.all { it.kind != MemoBlockKind.VOICE || it.voiceFile.isNotBlank() } }) {
        "Memo voice block without a file"
    }
    val todoIds = archive.todos.map(MemoTodo::id)
    require(todoIds.all(String::isNotBlank) && todoIds.distinct().size == todoIds.size) { "Invalid memo todo ids" }
    require(archive.todos.all { todo ->
        val ids = todo.items.map(MemoTodoItem::id)
        ids.all(String::isNotBlank) && ids.distinct().size == ids.size
    }) { "Invalid memo todo item ids" }
    require(archive.historyDepth in MIN_MEMO_HISTORY_DEPTH..MAX_MEMO_HISTORY_DEPTH) {
        "Memo history depth out of range"
    }
}

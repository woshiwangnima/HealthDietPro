package com.woshiwangnima.healthdietpro.model.memo

import java.io.File
import kotlin.random.Random

/**
 * 【测试指令 · 记便签】的指令集。
 *
 * 定义在模型层而不是 UI 层：生成器与执行器都吃它，单测可以直接构造指令跑纯函数，
 * 不需要 Android 环境。UI 只负责把输入框里的值装进这些 data class。
 */
internal sealed interface MemoTestCommand {

    /** 创建 X 条随机笔记：无语音、未归档——最小可用的列表占位数据。 */
    data class CreatePlainNotes(val count: Int) : MemoTestCommand

    /** 创建随机笔记（自定义参数）：块数 / 字数 / 语音 / 归档 / 置顶 / 历史版本 / 时间跨度全部可调。 */
    data class CreateRichNotes(
        val count: Int,
        val minTextBlocks: Int,
        val maxTextBlocks: Int,
        val minCharsPerBlock: Int,
        val maxCharsPerBlock: Int,
        val voiceBlocksPerNote: Int,
        val assignToFolders: Boolean,
        val pinnedCount: Int,
        val historyVersionsPerNote: Int,
        val recentDays: Int,
    ) : MemoTestCommand

    /** 删除所有笔记；是否连语音附件文件一起清掉。 */
    data class DeleteAllNotes(val pruneVoiceFiles: Boolean) : MemoTestCommand

    /** 创建随机笔记文件夹：总数、最大嵌套层级、命名前缀。 */
    data class CreateFolders(val totalCount: Int, val maxDepth: Int, val namePrefix: String) : MemoTestCommand

    /** 删除所有笔记文件夹（子级逐级上提，笔记保留）。 */
    data object DeleteAllFolders : MemoTestCommand

    /** 创建随机待办：条数、每条待办项数范围、完成项占比、更新时间跨度。 */
    data class CreateTodos(
        val count: Int,
        val minItems: Int,
        val maxItems: Int,
        val donePercent: Int,
        val recentDays: Int,
    ) : MemoTestCommand

    /** 删除所有待办。 */
    data object DeleteAllTodos : MemoTestCommand

    /** 批量勾选待办：全部完成 / 全部重置为未完成。 */
    data class SetAllTodosCompletion(val completed: Boolean) : MemoTestCommand
}

/** 一次指令执行的结果计数，由调用方拼 Toast 文案。 */
internal data class MemoTestResult(
    val createdNotes: Int = 0,
    val createdVoices: Int = 0,
    val deletedNotes: Int = 0,
    val createdFolders: Int = 0,
    val deletedFolders: Int = 0,
    val createdTodos: Int = 0,
    val deletedTodos: Int = 0,
    val updatedTodos: Int = 0,
    val prunedVoiceFiles: Int = 0,
)

private const val DAY_MS = 24L * 60 * 60 * 1000
private const val MAX_TEST_NOTES = 200
private const val MAX_TEST_FOLDERS = 100
private const val MAX_TEST_FOLDER_DEPTH = 5
private const val MAX_TEST_TODOS = 200
private const val MAX_TEST_TODO_ITEMS = 20
private const val MAX_TEST_VOICE_BLOCKS_PER_NOTE = 3
private const val MAX_TEST_HISTORY_VERSIONS = 20

/** 随机正文用字池：中文词 + 少量英文与空白，保证字数统计与搜索都有得测。 */
private const val TEXT_POOL =
    "健康饮食睡眠血糖体重运动记录便签笔记待办完成清单晨起午间晚间提醒事项 abcdefg "

/** 随机笔记生成参数（UI 输入的收敛结果）。 */
internal data class RandomNoteSpec(
    val count: Int,
    val minTextBlocks: Int = 1,
    val maxTextBlocks: Int = 3,
    val minCharsPerBlock: Int = 20,
    val maxCharsPerBlock: Int = 120,
    val voiceBlocksPerNote: Int = 0,
    val assignToFolders: Boolean = false,
    val pinnedCount: Int = 0,
    val historyVersionsPerNote: Int = 0,
    val recentDays: Int = 30,
)

/** 随机待办生成参数。 */
internal data class RandomTodoSpec(
    val count: Int,
    val minItems: Int = 1,
    val maxItems: Int = 6,
    val donePercent: Int = 0,
    val recentDays: Int = 30,
)

/**
 * 随机笔记文件夹树：父级先建（`createdAtMillis` 递增，`childrenOf` 按它排序时树形稳定），
 * 每个新文件夹随机挂在「层级还没到上限」的既有文件夹下，不够挂就落根级。
 */
internal fun buildRandomFolders(
    totalCount: Int,
    maxDepth: Int,
    namePrefix: String,
    now: Long,
    random: Random,
): List<MemoFolder> {
    val count = totalCount.coerceIn(0, MAX_TEST_FOLDERS)
    val depth = maxDepth.coerceIn(1, MAX_TEST_FOLDER_DEPTH)
    if (count == 0) return emptyList()
    val base = now - count * 1_000L
    val folders = mutableListOf<MemoFolder>()
    val levels = mutableListOf<Int>()
    repeat(count) { index ->
        val parentIndex = folders.indices.filter { levels[it] < depth }.randomOrNull(random)
        val parentId = parentIndex?.let { folders[it].id }
        val level = (parentIndex?.let { levels[it] } ?: 0) + 1
        folders += MemoFolder(
            id = "test_folder_${now}_$index",
            name = "$namePrefix${index + 1}",
            parentId = parentId,
            createdAtMillis = base + index * 1_000L,
        )
        levels += level
    }
    return folders
}

/**
 * 随机笔记。语音块通过 [voiceFactory] 拿裸文件名（工厂造不出来就少一块，不阻断生成）；
 * 文本块与语音块整体随机排序，插在中间还是开头都可能出现，正好覆盖两种排版。
 */
internal fun buildRandomNotes(
    spec: RandomNoteSpec,
    folderIds: List<String>,
    voiceFactory: (() -> String?)?,
    now: Long,
    random: Random,
): List<MemoNote> {
    val count = spec.count.coerceIn(0, MAX_TEST_NOTES)
    if (count == 0) return emptyList()
    val minBlocks = spec.minTextBlocks.coerceAtLeast(1)
    val maxBlocks = spec.maxTextBlocks.coerceAtLeast(minBlocks).coerceAtMost(20)
    val minChars = spec.minCharsPerBlock.coerceIn(1, 2_000)
    val maxChars = spec.maxCharsPerBlock.coerceAtLeast(minChars).coerceAtMost(2_000)
    val voicePerNote = spec.voiceBlocksPerNote.coerceIn(0, MAX_TEST_VOICE_BLOCKS_PER_NOTE)
    val pinned = spec.pinnedCount.coerceIn(0, count)
    val historyDepth = spec.historyVersionsPerNote.coerceIn(0, MAX_TEST_HISTORY_VERSIONS)
    val recentDays = spec.recentDays.coerceIn(1, 365)

    return (0 until count).map { index ->
        val updatedAt = now - random.nextLong(recentDays * DAY_MS)
        val createdAt = updatedAt - random.nextLong(30 * DAY_MS) - 1
        val textBlocks = (0 until random.nextInt(minBlocks, maxBlocks + 1)).map { blockIndex ->
            MemoBlock(
                kind = MemoBlockKind.TEXT,
                text = randomText(minChars, maxChars, random),
                id = "test_note_${now}_${index}_t$blockIndex",
            )
        }
        val voiceBlocks = (0 until voicePerNote).mapNotNull { blockIndex ->
            voiceFactory?.invoke()?.let { fileName ->
                MemoBlock(
                    kind = MemoBlockKind.VOICE,
                    voiceFile = fileName,
                    durationMs = random.nextLong(3_000L, 60_000L),
                    id = "test_note_${now}_${index}_v$blockIndex",
                )
            }
        }
        val versions = (0 until historyDepth).map { versionIndex ->
            MemoNoteVersion(
                savedAtMillis = updatedAt - (historyDepth - versionIndex) * 3_600_000L,
                title = "随机笔记 ${index + 1} · 历史 ${versionIndex + 1}",
                blocks = listOf(
                    MemoBlock(
                        kind = MemoBlockKind.TEXT,
                        text = "历史版本内容 ${versionIndex + 1}：" + randomText(10, 40, random),
                        id = "test_note_${now}_${index}_h$versionIndex",
                    ),
                ),
            )
        }
        MemoNote(
            id = "test_note_${now}_$index",
            folderId = if (spec.assignToFolders && folderIds.isNotEmpty()) folderIds.random(random) else null,
            title = "随机笔记 ${index + 1}·${random.nextInt(100, 999)}",
            blocks = (textBlocks + voiceBlocks).shuffled(random),
            versions = versions,
            pinned = index < pinned,
            createdAtMillis = createdAt,
            updatedAtMillis = updatedAt,
        )
    }
}

internal fun buildRandomTodos(spec: RandomTodoSpec, now: Long, random: Random): List<MemoTodo> {
    val count = spec.count.coerceIn(0, MAX_TEST_TODOS)
    if (count == 0) return emptyList()
    val minItems = spec.minItems.coerceAtLeast(1)
    val maxItems = spec.maxItems.coerceAtLeast(minItems).coerceAtMost(MAX_TEST_TODO_ITEMS)
    val donePercent = spec.donePercent.coerceIn(0, 100)
    val recentDays = spec.recentDays.coerceIn(1, 365)

    return (0 until count).map { index ->
        val updatedAt = now - random.nextLong(recentDays * DAY_MS)
        val itemCount = random.nextInt(minItems, maxItems + 1)
        val doneCount = itemCount * donePercent / 100
        val items = (0 until itemCount).map { itemIndex ->
            MemoTodoItem(
                id = "test_todo_${now}_${index}_i$itemIndex",
                text = "随机待办 ${index + 1}-${itemIndex + 1}·${random.nextInt(10, 99)}",
                done = itemIndex < doneCount,
                doneAtMillis = if (itemIndex < doneCount) updatedAt - random.nextLong(3_600_000L) else 0L,
            )
        }
        MemoTodo(
            id = "test_todo_${now}_$index",
            // 编辑器已不再产生标题，测试数据同样留空，避免「旧字段有值」的假象。
            title = "",
            items = items,
            createdAtMillis = updatedAt - random.nextLong(30 * DAY_MS) - 1,
            updatedAtMillis = updatedAt,
        )
    }
}

private fun randomText(minChars: Int, maxChars: Int, random: Random): String {
    val target = random.nextInt(minChars, maxChars + 1)
    return buildString {
        while (length < target) {
            append(TEXT_POOL[random.nextInt(TEXT_POOL.length)])
            // 偶尔断行，让「第一段非空正文」的摘要逻辑也有两种形态可测。
            if (random.nextInt(12) == 0 && length < target) append('\n')
        }
    }.trim()
}

/**
 * 指令执行器：一次 `store.update` 落一条指令，避免逐条读写磁盘。
 * 语音文件只在 [voices] 与 [tempDir] 都给到时才生成（单测传 null 走纯文本路径）。
 */
internal class MemoTestCommandRunner(
    private val store: MemoArchiveStore,
    private val voices: MemoVoiceStore? = null,
    private val tempDir: File? = null,
) {
    fun run(command: MemoTestCommand): MemoTestResult = when (command) {
        is MemoTestCommand.CreatePlainNotes -> createNotes(
            RandomNoteSpec(count = command.count, voiceBlocksPerNote = 0, assignToFolders = false),
        )

        is MemoTestCommand.CreateRichNotes -> createNotes(
            RandomNoteSpec(
                count = command.count,
                minTextBlocks = command.minTextBlocks,
                maxTextBlocks = command.maxTextBlocks,
                minCharsPerBlock = command.minCharsPerBlock,
                maxCharsPerBlock = command.maxCharsPerBlock,
                voiceBlocksPerNote = command.voiceBlocksPerNote,
                assignToFolders = command.assignToFolders,
                pinnedCount = command.pinnedCount,
                historyVersionsPerNote = command.historyVersionsPerNote,
                recentDays = command.recentDays,
            ),
        )

        is MemoTestCommand.DeleteAllNotes -> deleteAllNotes(command.pruneVoiceFiles)

        is MemoTestCommand.CreateFolders -> createFolders(command)

        MemoTestCommand.DeleteAllFolders -> deleteAllFolders()

        is MemoTestCommand.CreateTodos -> createTodos(
            RandomTodoSpec(
                count = command.count,
                minItems = command.minItems,
                maxItems = command.maxItems,
                donePercent = command.donePercent,
                recentDays = command.recentDays,
            ),
        )

        MemoTestCommand.DeleteAllTodos -> deleteAllTodos()

        is MemoTestCommand.SetAllTodosCompletion -> setAllTodosCompletion(command.completed)
    }

    private fun createNotes(spec: RandomNoteSpec): MemoTestResult {
        val random = Random(System.nanoTime())
        val now = System.currentTimeMillis()
        var createdVoices = 0
        val voiceFactory: (() -> String?)? =
            if (spec.voiceBlocksPerNote > 0 && voices != null && tempDir != null) {
                {
                    createTestVoiceFile(random)?.also { createdVoices++ }
                }
            } else {
                null
            }
        val folderIds = if (spec.assignToFolders) store.load().folders.map(MemoFolder::id) else emptyList()
        val notes = buildRandomNotes(spec, folderIds, voiceFactory, now, random)
        store.update { archive ->
            archive.copy(notes = (archive.notes + notes).sortedByDescending(MemoNote::updatedAtMillis))
        }
        return MemoTestResult(createdNotes = notes.size, createdVoices = createdVoices)
    }

    /**
     * 造一个假语音文件并收进附件目录：内容是随机字节（不是真音频），
     * 列表与块展示、归档打包、孤儿清理都能测，播放本身不在测试范围。
     */
    private fun createTestVoiceFile(random: Random): String? {
        val voiceStore = voices ?: return null
        val dir = tempDir ?: return null
        val temp = File.createTempFile("test_voice_", ".m4a", dir)
        return try {
            temp.writeBytes(ByteArray(4_096) { random.nextInt().toByte() })
            when (val adopted = voiceStore.adoptRecording(temp)) {
                is VoiceAdoptResult.Saved -> adopted.fileName
                VoiceAdoptResult.TooLarge, VoiceAdoptResult.Failed -> null
            }
        } finally {
            temp.delete()
        }
    }

    private fun deleteAllNotes(pruneVoiceFiles: Boolean): MemoTestResult {
        var deleted = 0
        store.update { archive ->
            deleted = archive.notes.size
            archive.copy(notes = emptyList())
        }
        var pruned = 0
        if (pruneVoiceFiles) {
            voices?.directory()?.listFiles()?.forEach { file ->
                if (file.isFile && file.delete()) pruned++
            }
        }
        return MemoTestResult(deletedNotes = deleted, prunedVoiceFiles = pruned)
    }

    private fun createFolders(command: MemoTestCommand.CreateFolders): MemoTestResult {
        val prefix = command.namePrefix.trim().ifEmpty { "随机文件夹" }
        val folders = buildRandomFolders(
            totalCount = command.totalCount,
            maxDepth = command.maxDepth,
            namePrefix = prefix,
            now = System.currentTimeMillis(),
            random = Random(System.nanoTime()),
        )
        store.update { archive -> archive.copy(folders = archive.folders + folders) }
        return MemoTestResult(createdFolders = folders.size)
    }

    private fun deleteAllFolders(): MemoTestResult {
        var deleted = 0
        store.update { archive ->
            var current = archive
            // 删一个文件夹会把子级上提一级；对快照里的每个 id 逐一删，
            // 先删子后删父（或相反）都会收敛到空集，上提的笔记最后落在根级。
            archive.folders.map(MemoFolder::id).forEach { folderId ->
                if (current.folders.any { it.id == folderId }) {
                    current = current.withoutFolder(folderId)
                    deleted++
                }
            }
            current
        }
        return MemoTestResult(deletedFolders = deleted)
    }

    private fun createTodos(spec: RandomTodoSpec): MemoTestResult {
        val todos = buildRandomTodos(spec, System.currentTimeMillis(), Random(System.nanoTime()))
        store.update { archive -> archive.copy(todos = archive.todos + todos) }
        return MemoTestResult(createdTodos = todos.size)
    }

    private fun deleteAllTodos(): MemoTestResult {
        var deleted = 0
        store.update { archive ->
            deleted = archive.todos.size
            archive.copy(todos = emptyList())
        }
        return MemoTestResult(deletedTodos = deleted)
    }

    private fun setAllTodosCompletion(completed: Boolean): MemoTestResult {
        val now = System.currentTimeMillis()
        var updated = 0
        store.update { archive ->
            updated = archive.todos.size
            archive.copy(
                todos = archive.todos.map { todo ->
                    todo.copy(
                        // doneAtMillis 逐项递增：全部完成时「按勾选先后沉底」有稳定顺序可测。
                        items = todo.items.mapIndexed { index, item ->
                            if (completed) {
                                item.copy(done = true, doneAtMillis = now + index)
                            } else {
                                item.copy(done = false, doneAtMillis = 0L)
                            }
                        },
                        updatedAtMillis = now,
                    )
                },
            )
        }
        return MemoTestResult(updatedTodos = updated)
    }
}

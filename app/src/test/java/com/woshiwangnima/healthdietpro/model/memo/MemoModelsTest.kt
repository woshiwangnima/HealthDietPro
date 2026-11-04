package com.woshiwangnima.healthdietpro.model.memo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/** JUnit4 没有 assertFailsWith，这里给一个最小替代，避免为此引入额外断言库。 */
private inline fun <reified T : Throwable> assertThrows(block: () -> Unit) {
    try {
        block()
    } catch (error: Throwable) {
        if (error is T) return
        throw error
    }
    throw AssertionError("Expected ${T::class.java.simpleName} to be thrown")
}

class MemoModelsTest {

    private fun text(value: String, id: String = "t${value.hashCode()}") =
        MemoBlock(kind = MemoBlockKind.TEXT, text = value, id = id)

    private fun voice(file: String = "v1.m4a", id: String = file) =
        MemoBlock(kind = MemoBlockKind.VOICE, voiceFile = file, durationMs = 3_000, id = id)

    private fun note(
        title: String = "标题",
        blocks: List<MemoBlock> = listOf(text("正文")),
        versions: List<MemoNoteVersion> = emptyList(),
        updated: Long = 1_000L,
    ) = MemoNote(
        id = "n1",
        title = title,
        blocks = blocks,
        versions = versions,
        createdAtMillis = 0L,
        updatedAtMillis = updated,
    )

    private fun folder(id: String, parent: String? = null, created: Long = 1L) =
        MemoFolder(id = id, name = id, parentId = parent, createdAtMillis = created)

    @Test
    fun wordCountIgnoresWhitespaceAndCountsEachCharacter() {
        // 「你好 世界\n!」：空白不算，剩下 5 个字符。
        assertEquals(5, countMemoWords("你好 世界\n!"))
        assertEquals(0, countMemoWords("   \n\t"))
        assertEquals(3, countMemoWords("a b c"))
    }

    @Test
    fun noteWordAndVoiceCountsWalkAllBlocks() {
        val note = note(blocks = listOf(text("第一段"), voice(), text("第二段"), voice("v2.m4a")))
        assertEquals(6, note.wordCount)
        assertEquals(2, note.voiceCount)
    }

    @Test
    fun previewUsesFirstNonBlankTextBlock() {
        val note = note(blocks = listOf(text("   "), voice(), text("真正的正文\n换行")))
        assertEquals("真正的正文 换行", note.previewText())
    }

    @Test
    fun mergedWithPushesPreviousContentAndCapsStack() {
        var current = note(title = "v0", blocks = listOf(text("body0")))
        repeat(7) { index ->
            current = current.mergedWith(
                title = "v${index + 1}",
                blocks = listOf(text("body${index + 1}")),
                depth = 5,
                now = 2_000L + index,
            )
        }
        assertEquals(5, current.versions.size)
        // 栈里保留的是最近 5 次保存前的内容：v2…v6。
        assertEquals(listOf("v2", "v3", "v4", "v5", "v6"), current.versions.map(MemoNoteVersion::title))
        assertEquals("v7", current.title)
    }

    @Test
    fun mergedWithReturnsSameInstanceWhenNothingChanged() {
        val original = note()
        val merged = original.mergedWith(original.title, original.blocks, depth = 5, now = 9_999L)
        assertSame(original, merged)
    }

    @Test
    fun firstSaveOfEmptyNoteDoesNotCreateVersion() {
        val empty = MemoNote(
            id = "n2",
            title = "",
            blocks = emptyList(),
            createdAtMillis = 0L,
            updatedAtMillis = 0L,
        )
        val merged = empty.mergedWith("新标题", listOf(text("新正文")), depth = 5, now = 5L)
        assertTrue(merged.versions.isEmpty())
        assertEquals("新标题", merged.title)
    }

    @Test
    fun restoreCountsAsAChangeAndKeepsCurrentContent() {
        val version = MemoNoteVersion(savedAtMillis = 500L, title = "旧版", blocks = listOf(text("旧正文")))
        val current = note(title = "新版", blocks = listOf(text("新正文")), versions = listOf(version))
        val restored = current.restoredFrom(version, depth = 5, now = 3_000L)
        assertEquals("旧版", restored.title)
        assertEquals("新版", restored.versions.last().title)
    }

    @Test
    fun hasContentIgnoresEmptyBlocksButHonoursVoice() {
        assertTrue(!note(title = "", blocks = listOf(text("  "))).hasContent)
        assertTrue(note(title = "", blocks = listOf(voice())).hasContent)
    }

    @Test
    fun folderPathWalksFromRootToLeaf() {
        val folders = listOf(folder("a"), folder("b", parent = "a"), folder("c", parent = "b"))
        assertEquals(listOf("a", "b", "c"), folders.pathOf("c").map(MemoFolder::id))
        assertTrue(folders.pathOf(null).isEmpty())
    }

    @Test
    fun folderMoveRejectsSelfAndOwnDescendants() {
        val folders = listOf(folder("a"), folder("b", parent = "a"), folder("c", parent = "b"), folder("d"))
        assertTrue(!folders.canMoveFolder("a", "a"))
        assertTrue(!folders.canMoveFolder("a", "c"))
        assertTrue(folders.canMoveFolder("a", "d"))
        assertTrue(folders.canMoveFolder("a", null))
        assertTrue(!folders.canMoveFolder("a", "missing"))
    }

    @Test
    fun flattenedKeepsDepthFirstOrderWithDepths() {
        val folders = listOf(folder("a", created = 1L), folder("a1", parent = "a", created = 2L), folder("b", created = 3L))
        assertEquals(
            listOf("a" to 0, "a1" to 1, "b" to 0),
            folders.flattened().map { it.folder.id to it.depth },
        )
    }

    @Test
    fun todoDisplayItemsSinkCheckedItemsByCheckTime() {
        val todo = MemoTodo(
            id = "t",
            items = listOf(
                MemoTodoItem("i1", "one", done = true, doneAtMillis = 20L),
                MemoTodoItem("i2", "two"),
                MemoTodoItem("i3", "three", done = true, doneAtMillis = 10L),
                MemoTodoItem("i4", "four"),
            ),
            createdAtMillis = 0L,
            updatedAtMillis = 0L,
        )
        assertEquals(listOf("i2", "i4", "i3", "i1"), todo.displayItems().map(MemoTodoItem::id))
    }

    @Test
    fun toggledItemMarksDoneAndRefreshesTodoTimestamp() {
        val todo = MemoTodo(
            id = "t",
            items = listOf(MemoTodoItem("i1", "one")),
            createdAtMillis = 0L,
            updatedAtMillis = 1L,
        )
        val toggled = todo.toggledItem("i1", now = 42L)
        assertEquals(true, toggled.items.single().done)
        assertEquals(42L, toggled.items.single().doneAtMillis)
        assertEquals(42L, toggled.updatedAtMillis)
        assertTrue(toggled.isCompleted)
    }

    @Test
    fun completedTodosSortAfterPendingOnes() {
        val pendingLatest = MemoTodo("a", items = listOf(MemoTodoItem("x", "x")), createdAtMillis = 0, updatedAtMillis = 90)
        val pendingOlder = MemoTodo("b", items = listOf(MemoTodoItem("y", "y")), createdAtMillis = 0, updatedAtMillis = 10)
        val completed = MemoTodo(
            "c",
            items = listOf(MemoTodoItem("z", "z", done = true, doneAtMillis = 50)),
            createdAtMillis = 0,
            updatedAtMillis = 99,
        )
        assertEquals(listOf("a", "b", "c"), listOf(pendingLatest, pendingOlder, completed).sortedForDisplay().map(MemoTodo::id))
    }

    @Test
    fun todoDisplayTitleFallsBackToFirstItem() {
        assertEquals("买牛奶", MemoTodo("t", items = listOf(MemoTodoItem("1", "买牛奶")), createdAtMillis = 0, updatedAtMillis = 0).displayTitle())
    }

    @Test
    fun validationRejectsFolderCycle() {
        val cyclic = MemoArchive(
            folders = listOf(folder("a", parent = "b"), folder("b", parent = "a")),
            historyDepth = 5,
        )
        assertThrows<IllegalArgumentException> { validateMemoArchive(cyclic) }
    }

    @Test
    fun validationRejectsMissingParentAndDepthOutOfRange() {
        assertThrows<IllegalArgumentException> {
            validateMemoArchive(MemoArchive(folders = listOf(folder("a", parent = "ghost"))))
        }
        assertThrows<IllegalArgumentException> {
            validateMemoArchive(MemoArchive(historyDepth = MAX_MEMO_HISTORY_DEPTH + 1))
        }
        assertThrows<IllegalArgumentException> {
            validateMemoArchive(MemoArchive(notes = listOf(note(blocks = listOf(MemoBlock(kind = MemoBlockKind.VOICE))))))
        }
    }

    @Test
    fun validArchivePassesValidation() {
        validateMemoArchive(
            MemoArchive(
                folders = listOf(folder("a"), folder("b", parent = "a")),
                notes = listOf(note().copy(folderId = "b")),
                todos = listOf(
                    MemoTodo("t", items = listOf(MemoTodoItem("i1", "x")), createdAtMillis = 0, updatedAtMillis = 0),
                ),
                historyDepth = 3,
            ),
        )
    }

    @Test
    fun pinnedNotesSortFirstThenByRecency() {
        val pinnedOld = note().copy(id = "p1", pinned = true, updatedAtMillis = 10L)
        val normalNew = note().copy(id = "n1", updatedAtMillis = 90L)
        val pinnedNew = note().copy(id = "p2", pinned = true, updatedAtMillis = 50L)
        val normalOld = note().copy(id = "n2", updatedAtMillis = 20L)
        assertEquals(
            listOf("p2", "p1", "n1", "n2"),
            listOf(pinnedOld, normalNew, pinnedNew, normalOld).withPinnedFirst().map(MemoNote::id),
        )
    }

    @Test
    fun pathStringJoinsFolderNamesWithSlash() {
        val folders = listOf(folder("a"), folder("b", parent = "a"))
        assertEquals("a/b", folders.pathStringOf("b"))
        assertEquals("", folders.pathStringOf(null))
        assertEquals("", folders.pathStringOf("missing"))
    }

    @Test
    fun matchesQueryHitsTitleBodyAndFolderPathCaseInsensitively() {
        val target = note(title = "菜谱", blocks = listOf(text("番茄炒蛋的做法")))
        assertTrue(target.matchesQuery("菜谱"))
        assertTrue(target.matchesQuery("番茄"))
        assertTrue(target.matchesQuery("  番茄  "))
        assertTrue(target.matchesQuery("TOMATO", folderPath = "tomato/notes"))
        // 空查询 = 全匹配；「是否处于搜索态」由 UI 层用 query.isBlank() 判断。
        assertTrue(target.matchesQuery(""))
        assertTrue(!target.matchesQuery("牛奶"))
    }

    @Test
    fun matchesQueryFallsBackToFolderPath() {
        val target = note(title = "标题", blocks = listOf(text("正文")))
        assertTrue(target.matchesQuery("工作", folderPath = "工作/2026"))
        assertTrue(!target.matchesQuery("工作"))
    }
}

package com.woshiwangnima.healthdietpro.model.memo

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoTestCommandsTest {

    @Test
    fun foldersRespectCountDepthAndNaming() {
        val now = 1_000_000L
        val folders = buildRandomFolders(
            totalCount = 12,
            maxDepth = 3,
            namePrefix = "测试夹",
            now = now,
            random = Random(7),
        )
        assertEquals(12, folders.size)
        assertTrue(folders.all { it.name.startsWith("测试夹") })
        val byId = folders.associateBy(MemoFolder::id)
        folders.forEach { folder ->
            var level = 1
            var current = folder
            while (current.parentId != null) {
                current = byId[current.parentId] ?: error("parent missing")
                level++
            }
            assertTrue("level $level exceeds depth cap", level <= 3)
            // 父级必须先于子级创建（childrenOf 按创建顺序排，树形展开才稳定）。
            if (folder.parentId != null) {
                assertTrue(byId.getValue(folder.parentId!!).createdAtMillis < folder.createdAtMillis)
            }
        }
        assertTrue(folders.map(MemoFolder::id).distinct().size == folders.size)
    }

    @Test
    fun folderDepthCapForcesRootsWhenDepthIsOne() {
        val folders = buildRandomFolders(5, maxDepth = 1, namePrefix = "平", now = 5_000L, random = Random(1))
        assertTrue(folders.all { it.parentId == null })
    }

    @Test
    fun plainNotesStayTextOnlyUnfiledAndUnpinned() {
        val notes = buildRandomNotes(
            RandomNoteSpec(count = 8),
            folderIds = listOf("f1"),
            voiceFactory = { error("plain spec must not touch the voice factory") },
            now = 10_000L,
            random = Random(3),
        )
        assertEquals(8, notes.size)
        assertTrue(notes.all { it.folderId == null })
        assertTrue(notes.none { it.pinned })
        assertTrue(notes.all { it.versions.isEmpty() })
        assertTrue(notes.all { note -> note.blocks.all { it.kind == MemoBlockKind.TEXT } })
        assertTrue(notes.all { it.blocks.size in 1..3 })
    }

    @Test
    fun richNotesHonourVoicePinnedVersionsAndFolders() {
        var voiceCalls = 0
        val notes = buildRandomNotes(
            RandomNoteSpec(
                count = 10,
                minTextBlocks = 2,
                maxTextBlocks = 4,
                voiceBlocksPerNote = 2,
                assignToFolders = true,
                pinnedCount = 3,
                historyVersionsPerNote = 4,
                recentDays = 7,
            ),
            folderIds = listOf("fa", "fb"),
            voiceFactory = {
                voiceCalls++
                "voice_${voiceCalls}.m4a"
            },
            now = 100_000L,
            random = Random(11),
        )
        assertEquals(10, notes.size)
        assertEquals(20, voiceCalls)
        assertTrue(notes.all { it.folderId == null || it.folderId in setOf("fa", "fb") })
        assertEquals(3, notes.count(MemoNote::pinned))
        assertTrue(notes.all { it.versions.size == 4 })
        // 版本从旧到新，且都早于当前内容的更新时间。
        assertTrue(notes.all { note ->
            note.versions.zipWithNext().all { (older, newer) -> older.savedAtMillis < newer.savedAtMillis } &&
                note.versions.last().savedAtMillis < note.updatedAtMillis
        })
        assertTrue(notes.all { note -> note.blocks.count { it.kind == MemoBlockKind.VOICE } == 2 })
        assertTrue(notes.all { note -> note.blocks.count { it.kind == MemoBlockKind.TEXT } in 2..4 })
    }

    @Test
    fun voiceFactoryFailuresDegradeToTextOnlyNotes() {
        val notes = buildRandomNotes(
            RandomNoteSpec(count = 5, voiceBlocksPerNote = 2),
            folderIds = emptyList(),
            voiceFactory = { null },
            now = 1L,
            random = Random(5),
        )
        assertEquals(5, notes.size)
        assertTrue(notes.all { note -> note.blocks.all { it.kind == MemoBlockKind.TEXT } })
    }

    @Test
    fun todosRespectItemRangeAndDonePercent() {
        // now 取远大于时间跨度的值，保证随机回退后的时间戳仍然为正。
        val now = 10_000_000_000L
        val todos = buildRandomTodos(
            RandomTodoSpec(count = 30, minItems = 3, maxItems = 3, donePercent = 33, recentDays = 2),
            now = now,
            random = Random(9),
        )
        assertEquals(30, todos.size)
        assertTrue(todos.all { it.title.isEmpty() })
        todos.forEach { todo ->
            assertEquals(3, todo.items.size)
            // 占比按整数地板：3 项 × 33% = 0 项完成。
            assertEquals(0, todo.items.count(MemoTodoItem::done))
        }
        val fourItemTodos = buildRandomTodos(
            RandomTodoSpec(count = 1, minItems = 4, maxItems = 4, donePercent = 50),
            now = now,
            random = Random(9),
        )
        assertEquals(2, fourItemTodos.single().items.count(MemoTodoItem::done))
        assertTrue(fourItemTodos.single().items.all { item ->
            if (item.done) item.doneAtMillis > 0 else item.doneAtMillis == 0L
        })
    }

    @Test
    fun withoutFolderLiftsChildrenAndNotesOneLevel() {
        val archive = MemoArchive(
            folders = listOf(
                MemoFolder("root", "root", null, 1L),
                MemoFolder("mid", "mid", "root", 2L),
                MemoFolder("leaf", "leaf", "mid", 3L),
            ),
            notes = listOf(
                MemoNote("n1", folderId = "mid", createdAtMillis = 0, updatedAtMillis = 0),
                MemoNote("n2", folderId = "leaf", createdAtMillis = 0, updatedAtMillis = 0),
                MemoNote("n3", folderId = "root", createdAtMillis = 0, updatedAtMillis = 0),
            ),
        )
        val removed = archive.withoutFolder("mid")
        assertEquals(listOf("root", "leaf"), removed.folders.map(MemoFolder::id))
        assertEquals("root", removed.folders.first { it.id == "leaf" }.parentId)
        assertEquals("root", removed.notes.first { it.id == "n1" }.folderId)
        // 孙级文件夹（leaf）只是跟着上提到 root，本身还在：其中的笔记归属不变。
        assertEquals("leaf", removed.notes.first { it.id == "n2" }.folderId)
        assertEquals("root", removed.notes.first { it.id == "n3" }.folderId)
        // 幂等：删不存在的文件夹原样返回。
        assertEquals(removed, removed.withoutFolder("mid"))
    }
}

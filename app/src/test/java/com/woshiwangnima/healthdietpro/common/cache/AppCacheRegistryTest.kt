package com.woshiwangnima.healthdietpro.common.cache

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * 缓存注册表的契约：
 *
 * - id / 文案声明缺失、重复注册必须在接入时立刻失败，而不是等设置页打开才崩；
 * - 注册凭据关闭后只移除自己的实例，不能误摘同 id 的新缓存；
 * - 清理结果必须分别统计磁盘与内存的释放量，并报告「被占用 / 清理失败」的 id；
 * - 一个缓存读失败或抛异常不能拖垮其余缓存。
 */
class AppCacheRegistryTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private class FakeCache(
        override val spec: AppCacheSpec,
        private var byteCount: Long,
        private var itemCount: Int,
        private val failStats: Boolean = false,
        private val failClear: Boolean = false,
        private val clearPartially: Boolean = false,
    ) : AppCache {

        override fun stats(): AppCacheStats {
            if (failStats) error("stats failed")
            return AppCacheStats(byteCount, itemCount)
        }

        override fun clearStorage() {
            if (failClear) error("clear failed")
            if (clearPartially) {
                byteCount /= 2
                itemCount /= 2
            } else {
                byteCount = 0L
                itemCount = 0
            }
        }
    }

    private fun spec(
        id: String,
        labelRes: Int = 1,
        storage: AppCacheStorage = AppCacheStorage.MEMORY,
    ) = AppCacheSpec(id = id, labelRes = labelRes, storage = storage)

    @Test
    fun registerRejectsDuplicateId() {
        val registry = AppCacheRegistry()
        registry.register(FakeCache(spec("duplicate"), 10L, 1))

        assertThrows(IllegalStateException::class.java) {
            registry.register(FakeCache(spec("duplicate"), 20L, 2))
        }
    }

    @Test
    fun registerRejectsInvalidIdOrMissingLabel() {
        val registry = AppCacheRegistry()

        assertThrows(IllegalArgumentException::class.java) {
            registry.register(FakeCache(spec("Bad-Id"), 1L, 1))
        }
        assertThrows(IllegalArgumentException::class.java) {
            registry.register(FakeCache(spec("missing_label", labelRes = 0), 1L, 1))
        }
    }

    @Test
    fun snapshotKeepsRegistrationOrderAndStats() {
        val registry = AppCacheRegistry()
        registry.register(FakeCache(spec("first"), 10L, 1))
        registry.register(FakeCache(spec("second"), 20L, 2))

        val snapshot = registry.snapshot()

        assertEquals(listOf("first", "second"), snapshot.map { it.spec.id })
        assertEquals(10L, snapshot[0].stats.byteCount)
        assertEquals(2, snapshot[1].stats.itemCount)
    }

    @Test
    fun closingRegistrationOnlyRemovesItsOwnInstance() {
        val registry = AppCacheRegistry()
        val first = registry.register(FakeCache(spec("same"), 1L, 1))
        first.close()
        registry.register(FakeCache(spec("same"), 2L, 1))

        // 旧凭据延迟关闭时，不能把同 id 的新缓存摘掉。
        first.close()

        assertEquals(1, registry.snapshot().size)
        assertEquals(2L, registry.snapshot().single().stats.byteCount)
    }

    @Test
    fun clearAllAggregatesReleasedBytesAndItems() {
        val registry = AppCacheRegistry()
        registry.register(FakeCache(spec("disk"), 1_024L, 3))
        registry.register(FakeCache(spec("memory"), 2_048L, 2))

        val result = registry.clearAll()

        assertEquals(3_072L, result.releasedBytes)
        assertEquals(5, result.clearedItems)
        assertTrue(result.failedCacheIds.isEmpty())
        assertEquals(0L, registry.snapshot().sumOf { it.stats.byteCount })
    }

    @Test
    fun clearAllReportsPartialAndThrowingCaches() {
        val registry = AppCacheRegistry()
        registry.register(FakeCache(spec("ok"), 100L, 1))
        registry.register(FakeCache(spec("partial", storage = AppCacheStorage.DISK), 100L, 2, clearPartially = true))
        registry.register(FakeCache(spec("throws"), 50L, 1, failClear = true))

        val result = registry.clearAll()

        assertEquals(150L, result.releasedBytes)
        assertEquals(2, result.clearedItems)
        assertEquals(setOf("partial", "throws"), result.failedCacheIds.toSet())
    }

    @Test
    fun clearAllDoesNotTreatMemoryRepopulationAsFailure() {
        val registry = AppCacheRegistry()
        registry.register(FakeCache(spec("memory"), 100L, 2, clearPartially = true))

        val result = registry.clearAll()

        assertEquals(50L, result.releasedBytes)
        assertEquals(1, result.clearedItems)
        assertTrue(result.failedCacheIds.isEmpty())
    }

    @Test
    fun snapshotSurvivesCacheStatsFailure() {
        val registry = AppCacheRegistry()
        registry.register(FakeCache(spec("broken"), 0L, 0, failStats = true))
        registry.register(FakeCache(spec("healthy"), 10L, 1))

        val snapshot = registry.snapshot()

        assertTrue(snapshot[0].readFailed)
        assertEquals(0L, snapshot[0].stats.byteCount)
        assertFalse(snapshot[1].readFailed)
        assertEquals(10L, snapshot[1].stats.byteCount)
    }

    @Test
    fun directoryCacheCountsAndClearsOnlyItsOwnSubtree() {
        val root = temporaryFolder.newFolder("cache")
        File(root, "a.txt").writeText("1234")
        val sub = File(root, "sub").apply { mkdirs() }
        File(sub, "b.bin").writeBytes(ByteArray(6))

        val cache = DirectoryAppCache(spec("disk"), { root })
        val before = cache.stats()
        assertEquals(10L, before.byteCount)
        assertEquals(2, before.itemCount)

        val outcome = cache.clear()

        assertEquals(10L, outcome.releasedBytes)
        assertEquals(2, outcome.clearedItems)
        assertEquals(0, outcome.remainingItems)
        assertEquals(0L, cache.stats().byteCount)
        assertFalse(File(root, "a.txt").exists())
        assertFalse(sub.exists())
    }

    @Test
    fun missingDirectoryIsAnEmptyCache() {
        val cache = DirectoryAppCache(spec("missing"), { File(temporaryFolder.root, "nope") })

        assertEquals(0L, cache.stats().byteCount)
        assertEquals(0, cache.stats().itemCount)
        cache.clearStorage()
    }
}

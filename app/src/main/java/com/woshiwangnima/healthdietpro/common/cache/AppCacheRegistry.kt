package com.woshiwangnima.healthdietpro.common.cache

import android.app.Application
import com.woshiwangnima.healthdietpro.R
import java.io.File
import java.nio.file.Files

/**
 * 应用级缓存注册表：设置页的「清除缓存」**只认这里注册过的缓存**。
 *
 * 设计约束（新模块接入缓存时必须遵守，不靠口头约定）：
 *
 * 1. **注册即声明**：每个缓存实现 [AppCache]，并通过 [AppCacheSpec] 自带稳定 id、文案资源与
 *    存储层。设置页从 [snapshot] 自动渲染构成条 / 图例，不再维护 kind → 文案映射；
 *    漏写文案在运行时直接抛 [IllegalArgumentException]，不会等到打开设置页才崩。
 * 2. **id 唯一**：重复 id 直接抛 [IllegalStateException]。同一块缓存在不同入口被注册两次时，
 *    第一次接入就会暴露，而不是在清理结果里出现两条同名的「0 B」。
 * 3. **生命周期成对**：[register] 返回 [AppCacheRegistration]，持有者在 `onCleared()` /
 *    `onDestroy()` 里 `close()`。注册表不再长期持有已经废弃的 ViewModel / Activity 缓存。
 * 4. **清理范围封闭**：磁盘缓存一律通过 [DirectoryAppCache] 声明目录，删除只发生在这个目录的
 *    canonical 子树内，并跳过符号链接；用户存档、原始图片、附件不在任何已注册目录里，
 *    所以「清除缓存」不可能误删它们。自定义磁盘缓存目录必须与已注册目录互不重叠
 *    （不要往 `cacheDir` 里再注册子目录，否则会与 `app_files` 重复统计）。
 * 5. **故障隔离**：[snapshot] / [clearAll] 对每个缓存单独 `runCatching`，一个缓存读失败或删除
 *    失败只影响自己的条目，其余缓存照常显示 / 清理，并在结果里报告失败 id。
 */
internal class AppCacheRegistry {

    private val caches = LinkedHashMap<String, AppCache>()

    @Synchronized
    fun register(cache: AppCache): AppCacheRegistration {
        val spec = cache.spec
        require(CacheIdPattern.matches(spec.id)) {
            "Cache id '${spec.id}' must match ${CacheIdPattern.pattern}"
        }
        require(spec.labelRes != 0) {
            "Cache '${spec.id}' must declare a label resource"
        }
        check(!caches.containsKey(spec.id)) {
            "Cache '${spec.id}' is already registered"
        }
        caches[spec.id] = cache
        return AppCacheRegistration(this, cache)
    }

    /** 按注册顺序返回快照；读取失败的缓存保留在列表里并标记 [AppCacheSnapshot.readFailed]。 */
    fun snapshot(): List<AppCacheSnapshot> = registeredCaches().map { cache ->
        runCatching { cache.stats() }.fold(
            onSuccess = { AppCacheSnapshot(cache.spec, it) },
            onFailure = { AppCacheSnapshot(cache.spec, AppCacheStats(0, 0), readFailed = true) },
        )
    }

    /**
     * 清理全部已注册缓存。
     *
     * 返回的字节数包含磁盘与内存两部分；某个缓存抛异常，或磁盘缓存清理后仍有残留（文件被占用），
     * id 会进入 [AppCacheClearResult.failedCacheIds]，设置页据此提示「部分文件被占用」。
     *
     * 内存缓存清理后可能被正在显示的界面立刻重新填充（例如图片滚动到新条目），这属于正常使用，
     * 不按失败处理：否则用户每次清理都会看到一条假的「部分文件被占用」。
     */
    fun clearAll(): AppCacheClearResult {
        var releasedBytes = 0L
        var clearedItems = 0
        val failed = mutableListOf<String>()
        registeredCaches().forEach { cache ->
            val outcome = runCatching { cache.clear() }.getOrNull()
            if (outcome == null) {
                failed += cache.spec.id
            } else {
                releasedBytes += outcome.releasedBytes
                clearedItems += outcome.clearedItems
                val hasResidual = outcome.remainingBytes > 0L || outcome.remainingItems > 0
                if (cache.spec.storage == AppCacheStorage.DISK && hasResidual) {
                    failed += cache.spec.id
                }
            }
        }
        return AppCacheClearResult(
            releasedBytes = releasedBytes,
            clearedItems = clearedItems,
            failedCacheIds = failed,
        )
    }

    @Synchronized
    internal fun unregister(cache: AppCache) {
        // 只有「当前注册的就是这一个实例」时才移除：旧 registration 延迟 close 时，
        // 不能把同 id 的新缓存一起摘掉。
        if (caches[cache.spec.id] === cache) caches.remove(cache.spec.id)
    }

    @Synchronized
    private fun registeredCaches(): List<AppCache> = caches.values.toList()

    internal companion object {
        private val CacheIdPattern = Regex("^[a-z][a-z0-9_]*$")

        /**
         * Android 入口：把系统缓存目录注册成三个磁盘缓存，再交给各模块注册自己的内存缓存。
         *
         * 只注册目录、不在此处清理任何东西；真正的删除由 [DirectoryAppCache] 在自己的目录内完成。
         */
        fun from(application: Application): AppCacheRegistry {
            val registry = AppCacheRegistry()
            registry.register(
                DirectoryAppCache(
                    spec = AppCacheSpec(
                        id = "app_files",
                        labelRes = R.string.settings_cache_entry_app_files,
                        storage = AppCacheStorage.DISK,
                    ),
                    directory = { application.cacheDir },
                ),
            )
            registry.register(
                DirectoryAppCache(
                    spec = AppCacheSpec(
                        id = "code_files",
                        labelRes = R.string.settings_cache_entry_code_files,
                        storage = AppCacheStorage.DISK,
                    ),
                    directory = { application.codeCacheDir },
                ),
            )
            registry.register(
                DirectoryAppCache(
                    spec = AppCacheSpec(
                        id = "external_files",
                        labelRes = R.string.settings_cache_entry_external_files,
                        storage = AppCacheStorage.DISK,
                    ),
                    directory = { application.externalCacheDir },
                ),
            )
            return registry
        }
    }
}

/**
 * 注册凭据：关闭后从注册表移除对应缓存，且只移除「注册时那个实例」。
 *
 * 由缓存持有者保存；重复 [close] 无副作用。
 */
internal class AppCacheRegistration internal constructor(
    private val registry: AppCacheRegistry,
    private val cache: AppCache,
) : AutoCloseable {

    private var closed = false

    @Synchronized
    override fun close() {
        if (closed) return
        closed = true
        registry.unregister(cache)
    }
}

/**
 * 目录型磁盘缓存：目录内容即缓存，统计与删除都限制在这个目录的 canonical 子树内。
 *
 * 删除时跳过符号链接，避免 cache 目录里被放入指向 `filesDir` / 外部存储的链接后，
 * 「清除缓存」顺着链接删到用户数据。缓存目录通常只有几十个文件，递归统计在 IO 线程执行即可。
 */
internal class DirectoryAppCache(
    override val spec: AppCacheSpec,
    private val directory: () -> File?,
) : AppCache {

    override fun stats(): AppCacheStats {
        val root = directory() ?: return AppCacheStats(0L, 0)
        if (root.isSymbolicLink() || !root.isDirectory) return AppCacheStats(0L, 0)
        return root.safeChildren(root).fold(AppCacheStats(0L, 0)) { total, child ->
            total + child.statsSafely(root)
        }
    }

    override fun clearStorage() {
        val root = directory() ?: return
        if (root.isSymbolicLink() || !root.isDirectory) return
        // 这里用原始 listFiles（不用 safeChildren）：符号链接本身也要删掉，但只删链接、不跟进去。
        root.listFiles().orEmpty().forEach { it.deleteEntry(root) }
    }
}

private fun File.statsSafely(root: File): AppCacheStats {
    if (isSymbolicLink()) return AppCacheStats(0L, 0)
    if (isFile) return AppCacheStats(length(), 1)
    if (!isDirectory) return AppCacheStats(0L, 0)
    return safeChildren(root).fold(AppCacheStats(0L, 0)) { total, child ->
        total + child.statsSafely(root)
    }
}

private fun File.safeChildren(root: File): List<File> {
    val rootPath = runCatching { root.canonicalPath }.getOrNull() ?: return emptyList()
    return listFiles().orEmpty().filter { child ->
        val childPath = runCatching { child.canonicalPath }.getOrNull() ?: return@filter false
        childPath.startsWith(rootPath + File.separator)
    }
}

private fun File.deleteEntry(root: File) {
    // 符号链接只删链接自身：Java 的 delete() 不会跟随链接删除目标。
    if (isSymbolicLink()) {
        delete()
        return
    }
    val rootPath = runCatching { root.canonicalPath }.getOrNull() ?: return
    val path = runCatching { canonicalPath }.getOrNull() ?: return
    if (!path.startsWith(rootPath + File.separator)) return
    if (isDirectory) listFiles().orEmpty().forEach { it.deleteEntry(root) }
    delete()
}

private fun File.isSymbolicLink(): Boolean =
    // 不能比较 canonicalFile 与 absoluteFile：Android 的 /data/user/0 本身就是指向 /data/data 的链接，
    // 那样会把所有位于它下面的正常缓存目录都误判成符号链接。只判断路径最后一段。
    runCatching { Files.isSymbolicLink(toPath()) }.getOrDefault(true)

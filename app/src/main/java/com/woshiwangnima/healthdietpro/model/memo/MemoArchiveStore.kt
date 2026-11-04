package com.woshiwangnima.healthdietpro.model.memo

import android.content.Context
import com.woshiwangnima.healthdietpro.model.archive.decodeDomain
import com.woshiwangnima.healthdietpro.model.archive.decodeDomainSafely
import com.woshiwangnima.healthdietpro.model.archive.encodeDomain
import com.woshiwangnima.healthdietpro.model.archive.prepareArchiveWrite
import com.woshiwangnima.healthdietpro.model.archive.writeUserArchiveManifest
import com.woshiwangnima.healthdietpro.model.profile.ProfilePrefs
import java.io.File
import kotlinx.serialization.json.Json

/**
 * 便签归档存储：`user_archives/<userId>/notes.json`。
 *
 * 与物价、容器一致，走 `encodeDomain/decodeDomain` 的信封格式，导出导入自动带上。
 * 语音文件放在同目录的 `attachments/notes/voice/`，由档案的附件收集逻辑整目录带走。
 */
internal class MemoArchiveStore private constructor(
    private val context: Context,
    private val userId: String,
) {
    fun load(): MemoArchive = synchronized(lock) {
        normalize(read() ?: MemoArchive())
    }

    fun save(archive: MemoArchive) = synchronized(lock) {
        val normalized = normalize(archive)
        validateMemoArchive(normalized)
        val target = file()
        val temporary = prepareArchiveWrite(target)
        temporary.writeText(
            json.encodeDomain(context, DOMAIN_ID, normalized, MemoArchive.serializer()),
            Charsets.UTF_8,
        )
        check(temporary.renameTo(target)) { "Unable to replace memo archive" }
        writeUserArchiveManifest(context, userId)
        ProfilePrefs.noteUserActivity(context, userId)
    }

    fun update(transform: (MemoArchive) -> MemoArchive): MemoArchive = synchronized(lock) {
        transform(read() ?: MemoArchive()).also(::save)
    }

    internal fun validateJson(raw: String) {
        synchronized(lock) {
            validateMemoArchive(normalize(json.decodeDomain(raw, DOMAIN_ID, MemoArchive.serializer())))
        }
    }

    /** 当前用户目录，语音等附件也挂在这下面。 */
    fun userDirectory(): File = userDirectory(context, userId)

    private fun read(): MemoArchive? =
        json.decodeDomainSafely(file(), DOMAIN_ID, MemoArchive.serializer())

    private fun file() = File(userDirectory(), FILE_NAME)

    /** 历史保留次数来自存档，旧档案或手改过的值一律夹到合法区间，而不是让校验失败。 */
    private fun normalize(archive: MemoArchive): MemoArchive = archive.copy(
        historyDepth = archive.historyDepth.coerceIn(MIN_MEMO_HISTORY_DEPTH, MAX_MEMO_HISTORY_DEPTH),
    )

    companion object {
        private const val DOMAIN_ID = "notes"
        private const val FILE_NAME = "notes.json"
        private val lock = Any()
        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; explicitNulls = false }

        fun current(context: Context) =
            MemoArchiveStore(context.applicationContext, ProfilePrefs.getCurrentUserId(context))

        fun forUser(context: Context, userId: String) =
            MemoArchiveStore(context.applicationContext, userId)

        /** 与其它领域的用户目录命名保持一致（非法字符替换为下划线）。 */
        fun userDirectory(context: Context, userId: String): File = File(
            context.filesDir,
            "user_archives/${userId.replace(Regex("[^A-Za-z0-9_-]"), "_")}",
        )
    }
}

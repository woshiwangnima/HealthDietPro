package com.woshiwangnima.healthdietpro.model.memo

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import com.woshiwangnima.healthdietpro.model.profile.ProfilePrefs
import java.io.File
import kotlin.math.abs
import kotlin.random.Random

internal data class ImportedVoice(
    val fileName: String,
    val sourceName: String,
    val durationMs: Long,
)

/** 录音收编结果。超限要单独告诉用户，不能和「录音失败」共用一句话。 */
internal sealed interface VoiceAdoptResult {
    data class Saved(val fileName: String) : VoiceAdoptResult
    data object TooLarge : VoiceAdoptResult
    data object Failed : VoiceAdoptResult
}

/**
 * 便签语音文件的落盘与读取。
 *
 * **为什么从手机里选的音频要复制一份，而不是引用原路径**：
 * 1. 档案必须自包含——`ContentResolver` 给的 URI 只是临时授权，重启或换机就失效了；
 * 2. 导出档案时附件按 `attachments/` 整目录打包，引用外部路径的文件根本进不了档案；
 * 3. 用户删掉原文件后，笔记里的语音不该跟着变成一段无法播放的空白。
 * 所以这里统一「复制进用户目录」，代价只是一份音频的磁盘占用。
 *
 * 文件名一律用裸名（不带目录），原因见 [MemoBlock.voiceFile]。
 */
internal class MemoVoiceStore private constructor(
    private val context: Context,
    private val userId: String,
) {
    fun directory(): File = File(MemoArchiveStore.userDirectory(context, userId), "$ATTACHMENT_ROOT/").apply { mkdirs() }

    /** 解析裸文件名；带路径分隔符或 `..` 的名字一律拒绝，避免越出附件目录。 */
    fun file(fileName: String): File? {
        if (fileName.isBlank() || fileName != File(fileName).name || fileName.contains("..")) return null
        return File(directory(), fileName)
    }

    fun detailFile(fileName: String): File? = file(fileName)?.takeIf(File::isFile)

    fun newFileName(extension: String): String {
        val suffix = extension.lowercase().takeIf { it.matches(Regex("[a-z0-9]{1,5}")) } ?: "m4a"
        return "voice_${System.currentTimeMillis()}_${abs(Random.nextInt())}.$suffix"
    }

    /**
     * 录音完成后把临时文件收进附件目录。
     *
     * 录音机必须先写到一个真实路径（不能直接给目录名），所以先落缓存再整体搬进来；
     * 搬失败时保留缓存文件没有意义，直接丢弃并返回 null 由调用方提示。
     */
    fun adoptRecording(temp: File): VoiceAdoptResult {
        if (!temp.isFile || temp.length() <= 0) {
            temp.delete()
            return VoiceAdoptResult.Failed
        }
        if (temp.length() > MAX_VOICE_BYTES) {
            // 超限的录音不能留在附件目录：档案收集附件时按 15 MB 单文件上限校验，
            // 一条超限录音会让整份档案导出失败，而不是只丢这一条。
            temp.delete()
            return VoiceAdoptResult.TooLarge
        }
        val name = newFileName(temp.extension.ifBlank { "m4a" })
        val target = File(directory(), name)
        val moved = if (temp.renameTo(target)) {
            true
        } else {
            runCatching { temp.copyTo(target, overwrite = true) }.isSuccess.also { temp.delete() }
        }
        return if (moved) VoiceAdoptResult.Saved(name) else VoiceAdoptResult.Failed
    }

    /** 从系统文件选择器导入音频：复制进附件目录，原文件保持不动。 */
    fun importAudio(uri: Uri): Result<ImportedVoice> = runCatching {
        val resolver = context.contentResolver
        val sourceName = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
            ?.takeIf(String::isNotBlank)
            ?: "audio"
        val extension = resolver.getType(uri)?.let(::extensionForMime) ?: sourceName.substringAfterLast('.', "").lowercase()
        val name = newFileName(extension.ifBlank { "m4a" })
        val target = File(directory(), name)
        try {
            var copied = 0L
            resolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        copied += read
                        require(copied <= MAX_VOICE_BYTES) { "Audio file is too large" }
                        output.write(buffer, 0, read)
                    }
                }
            } ?: error("Unable to open the selected audio")
            require(copied > 0) { "Empty audio file" }
            ImportedVoice(fileName = name, sourceName = sourceName, durationMs = durationMs(target))
        } catch (error: Throwable) {
            // 复制到一半失败时不要留下半截文件——它会被附件收集带进档案。
            target.delete()
            throw error
        }
    }

    fun delete(fileName: String) {
        file(fileName)?.delete()
    }

    private fun durationMs(file: File): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } catch (_: Exception) {
            0L
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun extensionForMime(mime: String): String = when (mime.lowercase()) {
        "audio/mp4", "audio/m4a", "audio/x-m4a" -> "m4a"
        "audio/mpeg", "audio/mp3" -> "mp3"
        "audio/aac", "audio/x-aac" -> "aac"
        "audio/wav", "audio/x-wav", "audio/wave" -> "wav"
        "audio/ogg", "audio/opus" -> "ogg"
        "audio/amr", "audio/3gpp" -> "amr"
        "audio/flac", "audio/x-flac" -> "flac"
        else -> "m4a"
    }

    companion object {
        private const val ATTACHMENT_ROOT = "attachments/notes/voice"

        /**
         * 单条语音上限。
         *
         * 必须低于档案的 15 MB 单附件上限（`PlainUserArchiveRepository.MAX_ATTACHMENT_BYTES`）：
         * 附件收集是逐文件 require 的，一条超限音频会直接让整份导出失败。
         * 96 kbps 下 10 MB 约 14 分钟，够录一条长备忘。
         */
        const val MAX_VOICE_BYTES = 10L * 1024 * 1024

        fun current(context: Context) =
            MemoVoiceStore(context.applicationContext, ProfilePrefs.getCurrentUserId(context))

        fun forUser(context: Context, userId: String) =
            MemoVoiceStore(context.applicationContext, userId)
    }
}

package com.woshiwangnima.healthdietpro.ui.memo

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

/**
 * 语音录制。
 *
 * 只负责「起 / 停」并把结果落成一个缓存文件，搬运进用户附件目录由 [MemoVoiceStore] 做。
 * MediaRecorder 必须先有一个真实文件路径才能 prepare，所以先写 `cacheDir` 再搬。
 */
internal class MemoRecorder(private val context: Context) {

    private var engine: MediaRecorder? = null
    private var tempFile: File? = null

    val isRecording: Boolean get() = engine != null

    fun start(): Boolean {
        if (engine != null) return true
        val file = runCatching { File.createTempFile("memo_voice_", ".m4a", context.cacheDir) }
            .getOrNull() ?: return false
        val recorder = runCatching { createEngine() }.getOrNull() ?: run {
            file.delete()
            return false
        }
        val started = runCatching {
            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44_100)
                setAudioEncodingBitRate(96_000)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
        }
        return if (started.isSuccess) {
            engine = started.getOrThrow()
            tempFile = file
            true
        } else {
            runCatching { recorder.release() }
            file.delete()
            false
        }
    }

    /** 停止并返回缓存文件；录音过短导致 `stop()` 抛异常时按失败处理，不留下半截文件。 */
    fun stop(): File? {
        val recorder = engine ?: return null
        val file = tempFile
        engine = null
        tempFile = null
        val stopped = runCatching { recorder.stop() }.isSuccess
        runCatching { recorder.release() }
        if (!stopped || file == null || !file.isFile || file.length() == 0L) {
            file?.delete()
            return null
        }
        return file
    }

    fun cancel() {
        engine?.let { runCatching { it.stop() } }
        engine?.let { runCatching { it.release() } }
        engine = null
        tempFile?.delete()
        tempFile = null
    }

    @Suppress("DEPRECATION")
    private fun createEngine(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()
}

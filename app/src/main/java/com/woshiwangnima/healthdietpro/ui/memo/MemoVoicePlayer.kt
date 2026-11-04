package com.woshiwangnima.healthdietpro.ui.memo

import android.media.MediaPlayer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File

/**
 * 笔记内语音播放。
 *
 * 同一时刻只播一条：再点另一条会先停掉上一条，避免两条语音叠在一起。
 * `playingId` 是 Compose 状态，界面据此切换播放/暂停图标。
 */
internal class MemoVoicePlayer {

    private var player: MediaPlayer? = null

    var playingId: String? by mutableStateOf(null)
        private set

    fun toggle(blockId: String, file: File?) {
        if (playingId == blockId) {
            stop()
            return
        }
        stop()
        if (file == null || !file.isFile) return
        val engine = MediaPlayer()
        val prepared = runCatching {
            engine.setDataSource(file.absolutePath)
            engine.setOnCompletionListener { stop() }
            engine.prepare()
            engine.start()
        }
        if (prepared.isSuccess) {
            player = engine
            playingId = blockId
        } else {
            runCatching { engine.release() }
        }
    }

    fun stop() {
        player?.let { engine ->
            runCatching { engine.stop() }
            runCatching { engine.release() }
        }
        player = null
        playingId = null
    }
}

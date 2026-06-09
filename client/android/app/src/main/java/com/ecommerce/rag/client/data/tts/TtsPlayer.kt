package com.ecommerce.rag.client.data.tts

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Base64
import android.util.Log
import java.io.File

/**
 * 阶段 14：用 Android 原生 [MediaPlayer] 播放 TTS 音频。
 *
 * 支持两条路径：
 *  - [playFromUrl]：直接喂网络 URL，依赖 MediaPlayer 内部缓冲（推荐路径）；
 *  - [playFromBase64]：把 base64 解码到 cache/tts/ 临时文件再放，供后端只能返回 base64 的环境兜底。
 *
 * 不引入 ExoPlayer。MediaPlayer 状态机简单粗暴，每次播放都重新 reset，避免状态错乱。
 * Composable 退出时调用 [release] 释放底层资源。
 */
class TtsPlayer(
    private val appContext: Context
) {

    interface Callback {
        fun onStarted()
        fun onCompleted()
        fun onError(message: String)
    }

    private var mediaPlayer: MediaPlayer? = null
    /** 临时 base64 mp3 写盘文件，下次播放前清理。 */
    private var lastTempFile: File? = null

    /** 是否在播放（含准备中的瞬间）。stop / release 都会变 false。 */
    val isPlaying: Boolean
        get() = mediaPlayer?.let { runCatching { it.isPlaying }.getOrDefault(false) } ?: false

    fun playFromUrl(url: String, callback: Callback) {
        if (url.isBlank()) {
            callback.onError("音频地址为空")
            return
        }
        try {
            resetPlayer()
            val player = newPlayer(callback)
            player.setDataSource(url)
            player.prepareAsync()
            mediaPlayer = player
        } catch (e: Exception) {
            Log.w(TAG, "playFromUrl failed: $url", e)
            callback.onError(e.localizedMessage ?: "音频播放失败")
        }
    }

    fun playFromBase64(base64: String, callback: Callback) {
        if (base64.isBlank()) {
            callback.onError("音频数据为空")
            return
        }
        try {
            resetPlayer()
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            // 用 cache/tts/ 子目录，进程结束/重启都可以被系统清；
            // 同时复用同一份临时文件，避免堆积。
            val cacheDir = File(appContext.cacheDir, "tts").apply { mkdirs() }
            val file = File(cacheDir, "last_tts.mp3")
            file.writeBytes(bytes)
            lastTempFile = file

            val player = newPlayer(callback)
            player.setDataSource(file.absolutePath)
            player.prepareAsync()
            mediaPlayer = player
        } catch (e: Exception) {
            Log.w(TAG, "playFromBase64 failed", e)
            callback.onError(e.localizedMessage ?: "音频播放失败")
        }
    }

    /** 用户点静音 / 切走页面时调用，立刻停止当前朗读。 */
    fun stop() {
        resetPlayer()
    }

    /** Composable 退出 / ViewModel onCleared 时调用，释放底层资源。 */
    fun release() {
        resetPlayer()
        runCatching { lastTempFile?.delete() }
        lastTempFile = null
    }

    private fun newPlayer(callback: Callback): MediaPlayer {
        return MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            setOnPreparedListener {
                try {
                    it.start()
                    callback.onStarted()
                } catch (e: Exception) {
                    Log.w(TAG, "MediaPlayer start failed", e)
                    callback.onError(e.localizedMessage ?: "音频播放失败")
                }
            }
            setOnCompletionListener {
                callback.onCompleted()
                // 自然播放完不主动 release，等下次 reset 或 release()。
            }
            setOnErrorListener { _, what, extra ->
                Log.w(TAG, "MediaPlayer error what=$what extra=$extra")
                callback.onError("音频播放出错 ($what/$extra)")
                resetPlayer()
                true
            }
        }
    }

    private fun resetPlayer() {
        val current = mediaPlayer ?: return
        runCatching { if (current.isPlaying) current.stop() }
        runCatching { current.reset() }
        runCatching { current.release() }
        mediaPlayer = null
    }

    companion object {
        private const val TAG = "TtsPlayer"
    }
}

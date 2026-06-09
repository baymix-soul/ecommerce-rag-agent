package com.ecommerce.rag.client.data.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

/**
 * 阶段 14：封装 Android 原生 [SpeechRecognizer]，给 MiniChatPanel 的"按住说话"按钮用。
 *
 * 设计要点：
 *  - 不在 ViewModel 中持有 Activity Context；由 Composable 用 `LocalContext.current` 创建，
 *    Composable 退出时调用 [destroy] 释放。
 *  - 中文识别（zh-CN），开启 partial results。
 *  - 不上传原始音频到后端；最终文本走现有 `/api/chat/stream`。
 *  - 设备不支持 SpeechRecognizer 时 [isAvailable] 返回 false，UI 显示降级提示。
 *  - 所有回调都在主线程触发（SpeechRecognizer 默认行为）。
 *
 * 用法：
 *  ```
 *  val controller = remember { AndroidSpeechRecognizerController(context) }
 *  DisposableEffect(Unit) { onDispose { controller.destroy() } }
 *  controller.startListening(callback = object : Callback { ... })
 *  ```
 */
class AndroidSpeechRecognizerController(
    private val appContext: Context
) {

    interface Callback {
        fun onReadyForSpeech() {}
        fun onPartialResult(text: String) {}
        fun onFinalResult(text: String)
        fun onError(code: Int, message: String)
    }

    private var recognizer: SpeechRecognizer? = null
    private var currentCallback: Callback? = null

    /** 设备/系统是否支持 SpeechRecognizer。包可见性已在 Manifest <queries> 声明。 */
    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(appContext)

    fun startListening(callback: Callback) {
        if (!isAvailable()) {
            callback.onError(
                ERROR_NOT_AVAILABLE,
                "当前设备不支持语音识别，请改用键盘输入"
            )
            return
        }

        // 复用同一个 recognizer 实例，避免反复创建造成 Service 抖动；
        // 上次的 callback 在新会话开始前先解绑。
        if (recognizer == null) {
            recognizer = SpeechRecognizer.createSpeechRecognizer(appContext)
        }
        currentCallback = callback
        recognizer?.setRecognitionListener(buildListener())

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, LANGUAGE_ZH_CN)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, LANGUAGE_ZH_CN)
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, LANGUAGE_ZH_CN)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            // 把"用户停顿多久算说完"调得宽一些，避免说到一半被截断。
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
        }

        try {
            recognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.w(TAG, "startListening failed", e)
            callback.onError(ERROR_INTERNAL, e.localizedMessage ?: "语音识别启动失败")
        }
    }

    /**
     * 用户主动松手 → 通知 recognizer 结束录音并解析最终结果。
     * Recognizer 会先回调 onPartialResults / onResults，再回调 onEndOfSpeech。
     */
    fun stopListening() {
        try {
            recognizer?.stopListening()
        } catch (e: Exception) {
            Log.w(TAG, "stopListening failed", e)
        }
    }

    /** 用户取消（如手指上滑），直接放弃这一次识别。 */
    fun cancel() {
        try {
            recognizer?.cancel()
        } catch (e: Exception) {
            Log.w(TAG, "cancel failed", e)
        }
        currentCallback = null
    }

    /** Composable / Owner 销毁时调用，释放底层 Service。 */
    fun destroy() {
        try {
            recognizer?.destroy()
        } catch (e: Exception) {
            Log.w(TAG, "destroy failed", e)
        }
        recognizer = null
        currentCallback = null
    }

    private fun buildListener(): RecognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            currentCallback?.onReadyForSpeech()
        }

        override fun onBeginningOfSpeech() {}

        override fun onRmsChanged(rmsdB: Float) {}

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {}

        override fun onError(error: Int) {
            val msg = mapErrorCode(error)
            Log.w(TAG, "SpeechRecognizer onError code=$error msg=$msg")
            currentCallback?.onError(error, msg)
        }

        override fun onResults(results: Bundle?) {
            val text = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                ?.trim()
                .orEmpty()
            if (text.isEmpty()) {
                currentCallback?.onError(SpeechRecognizer.ERROR_NO_MATCH, "没听清，可以再说一次")
            } else {
                currentCallback?.onFinalResult(text)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val text = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                ?.trim()
                .orEmpty()
            if (text.isNotEmpty()) {
                currentCallback?.onPartialResult(text)
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun mapErrorCode(code: Int): String = when (code) {
        SpeechRecognizer.ERROR_AUDIO -> "录音异常，请检查麦克风"
        SpeechRecognizer.ERROR_CLIENT -> "语音识别客户端错误"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "需要麦克风权限才能语音输入"
        SpeechRecognizer.ERROR_NETWORK -> "网络异常，无法识别语音"
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "网络超时，请重试"
        SpeechRecognizer.ERROR_NO_MATCH -> "没听清，可以再说一次"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "语音识别忙碌中，请稍候"
        SpeechRecognizer.ERROR_SERVER -> "语音识别服务器出错"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "未检测到语音，请再试"
        else -> "语音识别失败 ($code)"
    }

    companion object {
        private const val TAG = "AsrController"
        private const val LANGUAGE_ZH_CN = "zh-CN"
        /** 自定义错误码，不与系统 SpeechRecognizer.ERROR_* 冲突。 */
        const val ERROR_NOT_AVAILABLE = -1
        const val ERROR_INTERNAL = -2
    }
}

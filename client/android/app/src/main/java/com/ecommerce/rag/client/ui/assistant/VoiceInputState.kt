package com.ecommerce.rag.client.ui.assistant

/**
 * 阶段 14：MiniChatPanel 语音输入状态机。
 *
 * 与 [AssistantUiState] 解耦：语音识别只在 Composable 层临时使用，
 * 不进 ViewModel——录音结束拿到 finalText 后直接走 [AssistantViewModel.sendMessage]。
 */
enum class VoiceInputStatus {
    /** 空闲：可以按下"按住说话"开始。 */
    Idle,

    /** 缺少 RECORD_AUDIO 权限，需要在用户点击时申请。 */
    PermissionRequired,

    /** 正在录音 / 正在听用户说话。 */
    Listening,

    /** 录音已结束，等待 SpeechRecognizer 返回最终结果。 */
    Recognizing,

    /** 已识别出 finalText，UI 将其交给 sendMessage 后回 Idle。 */
    Recognized,

    /** 失败：权限被拒、网络问题、no match、设备不支持等。 */
    Error
}

data class VoiceInputUiState(
    val status: VoiceInputStatus = VoiceInputStatus.Idle,
    /** SpeechRecognizer 推送的临时（partial）结果，仅做提示。 */
    val partialText: String = "",
    /** 识别结束后的最终文本，被 MiniChatPanel 消费一次后置空。 */
    val finalText: String = "",
    /** 失败时的中文提示，例如"请打开麦克风权限"。 */
    val error: String? = null
)

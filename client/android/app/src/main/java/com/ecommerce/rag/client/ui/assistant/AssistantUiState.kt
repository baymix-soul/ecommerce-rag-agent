package com.ecommerce.rag.client.ui.assistant

import com.ecommerce.rag.client.data.model.ChatMessage
import com.ecommerce.rag.client.data.model.PageContext

/**
 * 悬浮助手浮层的 UI 状态。
 *
 * 注意：此 State 仅描述"浮层本身"，不会替换现有 ChatUiState。
 * 与 ChatUiState 字段命名保持一致（isLoading），便于复用相同的 UI 组件。
 *
 * 阶段 7 新增（GIF 状态机相关）：
 *  - isStreaming        ：是否已经收到首个 Text token，正在持续接收文本流；
 *                         与 isLoading 区分开是为了让 GIF 区分 "Thinking" 和 "Talking"：
 *                           * isLoading=true,  isStreaming=false → Thinking（请求已发出，但没数据）
 *                           * isLoading=true,  isStreaming=true  → Talking（开始流式输出）
 *                           * isLoading=false                    → 流结束
 *  - temporaryBotMood   ：一次性短暂表情（如 Done 后 1.2s 的 Happy / Error 的 Confused），
 *                         在 ViewModel 里被 launch + delay 自动清空。
 *                         其他**瞬时**状态（拖拽 / hover / 菜单）仍留在 AssistantOverlay
 *                         的 remember 中推导，**不进** ViewModel。
 *
 * - mode                    ：当前形态 Collapsed / MiniChat / Expanded
 * - pageContext             ：宿主页面快照，发请求时一并带给后端（阶段 3 启用）
 * - messages               ：MiniChatPanel 内展示的对话流
 * - isLoading              ：是否正在 SSE 流式接收 / 等待回复
 * - error                  ：最近一次错误，UI 一次性展示
 * - isActionMenuVisible    ：长按 FloatingBot 后是否展示动作气泡菜单
 * - highlightedAction      ：当前手指悬浮在哪个动作气泡上
 * - cameraPlaceholderMessage：底部短提示（含"拍照功能下一步接入"/"长按我试试"等）
 */
data class AssistantUiState(
    val mode: AssistantMode = AssistantMode.Collapsed,
    val pageContext: PageContext = PageContext.Empty,
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val isStreaming: Boolean = false,
    val error: String? = null,
    val isActionMenuVisible: Boolean = false,
    val highlightedAction: AssistantAction? = null,
    val cameraPlaceholderMessage: String? = null,
    val temporaryBotMood: BotAnimationState? = null,
    /**
     * 阶段 9：SSE 流结束后递增，作为"是否要刷新购物车"的信号。
     * MainActivity 用 LaunchedEffect 监听其变化触发 CartViewModel.refreshCart()，
     * 让用户在机器人里说"加入购物车"后角标能更新。
     */
    val chatDoneCounter: Long = 0L,

    /**
     * 阶段 14（语音输入 / 朗读）。
     *
     * - isVoicePlaybackEnabled：用户是否打开了自动朗读开关。默认 true，
     *   静音按钮把它翻成 false 且打断当前播放。
     * - isSpeaking：TTS 正在播放（含准备阶段）。MiniChatPanel 据此切换"🔊/🔇"。
     * - ttsError：TTS 失败时一次性显示，不影响文字对话。
     * - pendingTtsText / ttsRequestId：done 后由 ViewModel 设置；
     *   Composable 用 `LaunchedEffect(ttsRequestId)` 监听并实际去发请求播音频，
     *   避免 ViewModel 直接持有 MediaPlayer / Context。
     */
    val isVoicePlaybackEnabled: Boolean = true,
    val isSpeaking: Boolean = false,
    val ttsError: String? = null,
    val pendingTtsText: String? = null,
    val ttsRequestId: Long = 0L
)

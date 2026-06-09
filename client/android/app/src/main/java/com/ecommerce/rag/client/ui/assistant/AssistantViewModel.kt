package com.ecommerce.rag.client.ui.assistant

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ecommerce.rag.client.config.AppConfig
import com.ecommerce.rag.client.data.local.AuthTokenStore
import com.ecommerce.rag.client.data.model.ChatMessage
import com.ecommerce.rag.client.data.model.PageContext
import com.ecommerce.rag.client.data.model.ProductCardUiModel
import com.ecommerce.rag.client.data.model.SseEvent
import com.ecommerce.rag.client.data.remote.SseClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 悬浮助手浮层的 ViewModel。
 *
 * 阶段 4 范围：
 *  - 保留 mode 控制（toggle/collapse/expandMiniChatPlaceholder）
 *  - 接入现有 SseClient，独立维护 messages / isLoading / error
 *  - 新增动作菜单状态管理（show/hide/highlight/perform）
 *  - 短按只显示"长按我试试"占位提示，不再直接打开 MiniChatPanel
 *  - 与 ChatViewModel 完全独立，不共享状态、不互相调用
 *  - 请求协议保持原样（{message, limit}），不传 PageContext，留到阶段 3
 */
class AssistantViewModel(application: Application) : AndroidViewModel(application) {

    private val sseClient = SseClient()
    private val authTokenStore = AuthTokenStore(application.applicationContext)

    private val _uiState = MutableStateFlow(AssistantUiState())
    val uiState: StateFlow<AssistantUiState> = _uiState.asStateFlow()

    // 当前 PageContext。UI 层通过 updatePageContext 持续同步，
    // 发送消息时直接读取最新值带给 SSE。
    @Volatile
    private var currentPageContext: PageContext = PageContext.Empty

    // 阶段 7：短暂表情（Happy / Confused）的清理任务，避免和下一次表情堆叠。
    private var moodResetJob: Job? = null

    // region mode

    /**
     * 阶段 4：短按只显示提示，不再切换 MiniChat。
     * 老的 toggleAssistant 仍保留，作为外部"强制切换"入口（例如保留给以后某些场景）。
     */
    fun toggleAssistant() {
        _uiState.update { state ->
            val nextMode = if (state.mode == AssistantMode.Collapsed) {
                AssistantMode.MiniChat
            } else {
                AssistantMode.Collapsed
            }
            state.copy(mode = nextMode)
        }
    }

    fun collapseAssistant() {
        _uiState.update {
            it.copy(
                mode = AssistantMode.Collapsed,
                isActionMenuVisible = false,
                highlightedAction = null
            )
        }
    }

    fun expandMiniChatPlaceholder() {
        _uiState.update { it.copy(mode = AssistantMode.MiniChat) }
    }

    // endregion

    // region action menu

    fun showActionMenu() {
        _uiState.update {
            it.copy(isActionMenuVisible = true, highlightedAction = null)
        }
    }

    fun hideActionMenu() {
        _uiState.update {
            it.copy(isActionMenuVisible = false, highlightedAction = null)
        }
    }

    fun setHighlightedAction(action: AssistantAction?) {
        _uiState.update { it.copy(highlightedAction = action) }
    }

    fun performAssistantAction(action: AssistantAction?) {
        when (action) {
            AssistantAction.Chat -> {
                _uiState.update {
                    it.copy(
                        mode = AssistantMode.MiniChat,
                        isActionMenuVisible = false,
                        highlightedAction = null
                    )
                }
            }
            AssistantAction.Camera -> {
                _uiState.update {
                    it.copy(
                        isActionMenuVisible = false,
                        highlightedAction = null,
                        cameraPlaceholderMessage = "拍照功能下一步接入"
                    )
                }
            }
            null -> {
                _uiState.update {
                    it.copy(isActionMenuVisible = false, highlightedAction = null)
                }
            }
        }
    }

    /**
     * FloatingBot 被普通短按时调用：显示一条临时提示，引导用户使用长按手势。
     */
    fun showShortPressHint() {
        _uiState.update { it.copy(cameraPlaceholderMessage = "长按我试试") }
    }

    fun clearCameraPlaceholderMessage() {
        _uiState.update { it.copy(cameraPlaceholderMessage = null) }
    }

    // endregion

    // region chat

    /**
     * 由 MainActivity 在 currentPageContext 变化时调用，
     * AssistantViewModel 内部以最新值发起后续请求。
     */
    fun updatePageContext(pageContext: PageContext) {
        currentPageContext = pageContext
    }

    fun sendMessage(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        if (_uiState.value.isLoading) return

        val userMessage = ChatMessage(isUser = true, text = trimmed)
        val aiPlaceholder = ChatMessage(isUser = false, isStreaming = true)

        // 新消息发出 → 清掉上一次的临时表情，防止旧的 Happy 还没消失就被叠加。
        moodResetJob?.cancel()
        _uiState.update { state ->
            state.copy(
                messages = state.messages + userMessage + aiPlaceholder,
                isLoading = true,
                isStreaming = false,
                error = null,
                temporaryBotMood = null
            )
        }

        val ctx = currentPageContext
        // 阶段 9：尽量带 Authorization，让"把这个加入购物车"等对话式加购可用。
        // 没登录就传 null，SseClient 不会加 header，行为同旧版。
        val token = authTokenStore.getAccessToken()
        viewModelScope.launch {
            sseClient.streamChat(
                message = trimmed,
                limit = AppConfig.DEFAULT_CANDIDATE_LIMIT,
                pageContext = ctx,
                accessToken = token,
                onEvent = ::handleSseEvent,
                onError = ::handleError
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    // endregion

    // region tts

    /** TTS 任务已经"派发"出去 → 把 pendingTtsText 清掉避免重复触发。 */
    fun consumeTtsText() {
        _uiState.update { it.copy(pendingTtsText = null) }
    }

    fun notifyTtsStarted() {
        _uiState.update { it.copy(isSpeaking = true, ttsError = null) }
    }

    fun notifyTtsStopped() {
        _uiState.update { it.copy(isSpeaking = false) }
    }

    fun notifyTtsError(message: String) {
        _uiState.update {
            it.copy(
                isSpeaking = false,
                ttsError = message,
                pendingTtsText = null
            )
        }
    }

    fun clearTtsError() {
        _uiState.update { it.copy(ttsError = null) }
    }

    /**
     * 点击静音 / 朗读开关：
     *  - 当前在播 → 立刻停（由 Composable 监听 isVoicePlaybackEnabled 翻成 false 后调用 player.stop）。
     *  - 当前未播 → 切换开关，影响下一轮 done。
     */
    fun toggleVoicePlayback() {
        _uiState.update { state ->
            val nextEnabled = !state.isVoicePlaybackEnabled
            state.copy(
                isVoicePlaybackEnabled = nextEnabled,
                // 关掉开关的同时把当前播放打断（实际 player.stop 在 Composable 副作用里执行）。
                isSpeaking = if (!nextEnabled) false else state.isSpeaking,
                pendingTtsText = if (!nextEnabled) null else state.pendingTtsText
            )
        }
    }

    // endregion

    // region sse

    private fun handleSseEvent(event: SseEvent) {
        when (event) {
            is SseEvent.Text -> appendStreamingText(event.content)
            is SseEvent.ProductCard -> appendStreamingProductCard(event)
            is SseEvent.Done -> finishStreaming()
            is SseEvent.Error -> finishWithError(event.message)
            is SseEvent.Unknown -> { /* ignore */ }
        }
    }

    private fun appendStreamingText(content: String) {
        _uiState.update { state ->
            val messages = state.messages.toMutableList()
            val idx = messages.indexOfLast { !it.isUser && it.isStreaming }
            if (idx >= 0) {
                messages[idx] = messages[idx].copy(text = messages[idx].text + content)
            }
            // 第一个 Text token 到达 → 翻到 Talking。
            state.copy(messages = messages, isStreaming = true)
        }
    }

    private fun appendStreamingProductCard(event: SseEvent.ProductCard) {
        _uiState.update { state ->
            val messages = state.messages.toMutableList()
            val idx = messages.indexOfLast { !it.isUser && it.isStreaming }
            if (idx >= 0) {
                val card = ProductCardUiModel(
                    productId = event.productId,
                    name = event.name,
                    price = event.price,
                    currency = event.currency,
                    imageUrl = resolveImageUrl(event.imageUrl),
                    reason = event.reason
                )
                messages[idx] = messages[idx].copy(
                    productCards = messages[idx].productCards + card
                )
            }
            // ProductCard 也算"有响应在持续到达"，对 GIF 来说和 Text 等价。
            state.copy(messages = messages, isStreaming = true)
        }
    }

    private fun finishStreaming() {
        _uiState.update { state ->
            val messages = state.messages.toMutableList()
            val idx = messages.indexOfLast { !it.isUser && it.isStreaming }
            val finishedAiText = if (idx >= 0) messages[idx].text else ""
            if (idx >= 0) {
                messages[idx] = messages[idx].copy(isStreaming = false)
            }
            // 阶段 14：done 后准备朗读文本。
            // - 商品卡片 JSON 等结构化数据不进 ChatMessage.text，所以这里只读 text；
            // - 截断 500 个字符防止 TTS 接口被长文本拖垮，长度阈值文档同步给后端；
            // - 仅当用户没主动关静音、且文本非空时投递任务，避免无意义请求；
            // - ttsRequestId 单调递增，Composable 用它做 LaunchedEffect 的 key。
            val ttsText = finishedAiText.trim().take(TTS_TEXT_LIMIT)
            val shouldQueueTts = state.isVoicePlaybackEnabled && ttsText.isNotEmpty()
            state.copy(
                messages = messages,
                isLoading = false,
                isStreaming = false,
                temporaryBotMood = BotAnimationState.Happy,
                // 阶段 9：每次 SSE done 都递增信号，MainActivity 监听后刷新购物车角标。
                // 即便后端没真改购物车，做一次 GET 也无害；倘若改了就能马上看到角标变化。
                chatDoneCounter = state.chatDoneCounter + 1,
                pendingTtsText = if (shouldQueueTts) ttsText else null,
                ttsRequestId = if (shouldQueueTts) state.ttsRequestId + 1 else state.ttsRequestId,
                ttsError = null
            )
        }
        scheduleMoodReset(BotAnimationState.Happy, delayMillis = 1200L)
    }

    private fun finishWithError(message: String) {
        _uiState.update { state ->
            val messages = state.messages.toMutableList()
            val idx = messages.indexOfLast { !it.isUser && it.isStreaming }
            if (idx >= 0) {
                val current = messages[idx].text
                val merged = if (current.isBlank()) "错误: $message" else "$current\n\n错误: $message"
                messages[idx] = messages[idx].copy(text = merged, isStreaming = false)
            }
            state.copy(
                messages = messages,
                isLoading = false,
                isStreaming = false,
                error = message,
                temporaryBotMood = BotAnimationState.Confused
            )
        }
        scheduleMoodReset(BotAnimationState.Confused, delayMillis = 1800L)
    }

    private fun handleError(throwable: Throwable) {
        val message = throwable.localizedMessage ?: "未知错误"
        _uiState.update { state ->
            val messages = state.messages.toMutableList()
            val idx = messages.indexOfLast { !it.isUser && it.isStreaming }
            if (idx >= 0) {
                messages[idx] = messages[idx].copy(
                    text = "网络错误: $message",
                    isStreaming = false
                )
            }
            state.copy(
                messages = messages,
                isLoading = false,
                isStreaming = false,
                error = message,
                temporaryBotMood = BotAnimationState.Confused
            )
        }
        scheduleMoodReset(BotAnimationState.Confused, delayMillis = 1800L)
    }

    /**
     * 延迟一段时间后把 temporaryBotMood 清掉，让机器人回到默认表情。
     * 只在 mood 没被后续逻辑改写时才清，避免覆盖新的 Happy / Confused。
     */
    private fun scheduleMoodReset(expected: BotAnimationState, delayMillis: Long) {
        moodResetJob?.cancel()
        moodResetJob = viewModelScope.launch {
            delay(delayMillis)
            _uiState.update { state ->
                if (state.temporaryBotMood == expected) state.copy(temporaryBotMood = null) else state
            }
        }
    }

    private companion object {
        /** 阶段 14：朗读文本最大长度，超出截断。文档同步给后端，避免 TTS 长任务。 */
        const val TTS_TEXT_LIMIT = 500
    }

    private fun resolveImageUrl(imageUrl: String): String {
        if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
            return imageUrl
        }
        if (imageUrl.startsWith("/")) {
            return AppConfig.BASE_URL + imageUrl
        }
        return AppConfig.BASE_URL + "/" + imageUrl
    }

    // endregion
}

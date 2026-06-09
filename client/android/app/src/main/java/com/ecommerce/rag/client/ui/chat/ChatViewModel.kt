package com.ecommerce.rag.client.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecommerce.rag.client.config.AppConfig
import com.ecommerce.rag.client.data.model.ChatMessage
import com.ecommerce.rag.client.data.model.ChatUiState
import com.ecommerce.rag.client.data.model.ProductCardUiModel
import com.ecommerce.rag.client.data.model.SseEvent
import com.ecommerce.rag.client.data.remote.SseClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {

    private val sseClient = SseClient()

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    fun sendMessage(message: String) {
        if (message.isBlank()) return
        if (_uiState.value.isLoading) return

        val userMessage = ChatMessage(isUser = true, text = message.trim())
        val aiMessage = ChatMessage(isUser = false, isStreaming = true)

        _uiState.update { state ->
            state.copy(
                messages = state.messages + userMessage + aiMessage,
                isLoading = true,
                error = null
            )
        }

        viewModelScope.launch {
            sseClient.streamChat(
                message = message.trim(),
                limit = AppConfig.DEFAULT_CANDIDATE_LIMIT,
                onEvent = { event -> handleSseEvent(event) },
                onError = { throwable -> handleError(throwable) }
            )
        }
    }

    private fun handleSseEvent(event: SseEvent) {
        when (event) {
            is SseEvent.Text -> {
                _uiState.update { state ->
                    val messages = state.messages.toMutableList()
                    val lastAiIdx = messages.indexOfLast { !it.isUser && it.isStreaming }
                    if (lastAiIdx >= 0) {
                        messages[lastAiIdx] = messages[lastAiIdx].copy(
                            text = messages[lastAiIdx].text + event.content
                        )
                    }
                    state.copy(messages = messages)
                }
            }
            is SseEvent.ProductCard -> {
                _uiState.update { state ->
                    val messages = state.messages.toMutableList()
                    val lastAiIdx = messages.indexOfLast { !it.isUser && it.isStreaming }
                    if (lastAiIdx >= 0) {
                        val card = ProductCardUiModel(
                            productId = event.productId,
                            name = event.name,
                            price = event.price,
                            currency = event.currency,
                            imageUrl = resolveImageUrl(event.imageUrl),
                            reason = event.reason
                        )
                        messages[lastAiIdx] = messages[lastAiIdx].copy(
                            productCards = messages[lastAiIdx].productCards + card
                        )
                    }
                    state.copy(messages = messages)
                }
            }
            is SseEvent.Done -> {
                _uiState.update { state ->
                    val messages = state.messages.toMutableList()
                    val lastAiIdx = messages.indexOfLast { !it.isUser && it.isStreaming }
                    if (lastAiIdx >= 0) {
                        messages[lastAiIdx] = messages[lastAiIdx].copy(isStreaming = false)
                    }
                    state.copy(messages = messages, isLoading = false)
                }
            }
            is SseEvent.Error -> {
                _uiState.update { state ->
                    val messages = state.messages.toMutableList()
                    val lastAiIdx = messages.indexOfLast { !it.isUser && it.isStreaming }
                    if (lastAiIdx >= 0) {
                        val currentText = messages[lastAiIdx].text
                        val errorText = if (currentText.isBlank()) {
                            "错误: ${event.message}"
                        } else {
                            "$currentText\n\n错误: ${event.message}"
                        }
                        messages[lastAiIdx] = messages[lastAiIdx].copy(
                            text = errorText,
                            isStreaming = false
                        )
                    }
                    state.copy(
                        messages = messages,
                        isLoading = false,
                        error = event.message
                    )
                }
            }
            is SseEvent.Unknown -> { }
        }
    }

    private fun handleError(throwable: Throwable) {
        _uiState.update { state ->
            val messages = state.messages.toMutableList()
            val lastAiIdx = messages.indexOfLast { !it.isUser && it.isStreaming }
            if (lastAiIdx >= 0) {
                messages[lastAiIdx] = messages[lastAiIdx].copy(
                    text = "网络错误: ${throwable.localizedMessage ?: "未知错误"}",
                    isStreaming = false
                )
            }
            state.copy(
                messages = messages,
                isLoading = false,
                error = throwable.localizedMessage ?: "未知错误"
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
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
}

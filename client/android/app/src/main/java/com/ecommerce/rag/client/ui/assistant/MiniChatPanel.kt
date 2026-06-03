package com.ecommerce.rag.client.ui.assistant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ecommerce.rag.client.data.model.ChatMessage
import com.ecommerce.rag.client.ui.chat.MessageBubble

/**
 * 悬浮购物助手的小型对话面板。
 *
 * 阶段 2 实现：
 *  - 标题栏 + 关闭按钮
 *  - 复用 MessageBubble 渲染对话流（含商品卡片）
 *  - 底部紧凑输入区，发送按钮在输入为空或正在加载时禁用
 *  - 高度受限（最高约 ~480dp），不撑满全屏
 *  - imePadding() 处理软键盘
 */
@Composable
fun MiniChatPanel(
    messages: List<ChatMessage>,
    isLoading: Boolean,
    error: String?,
    onSendMessage: (String) -> Unit,
    onClose: () -> Unit,
    onProductClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var inputText by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 360.dp, max = 480.dp)
            .imePadding()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            MiniChatHeader(onClose = onClose)

            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (messages.isEmpty()) {
                    EmptyHint()
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(items = messages, key = { it.id }) { message ->
                            MessageBubble(
                                message = message,
                                onProductClick = onProductClick
                            )
                        }
                    }
                }
            }

            if (error != null) {
                ErrorBanner(text = error)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            MiniChatInputRow(
                value = inputText,
                onValueChange = { inputText = it },
                isLoading = isLoading,
                onSend = {
                    val toSend = inputText
                    if (toSend.isNotBlank() && !isLoading) {
                        onSendMessage(toSend)
                        inputText = ""
                    }
                }
            )
        }
    }
}

@Composable
private fun MiniChatHeader(onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "🤖",
            style = MaterialTheme.typography.titleMedium
        )
        Box(modifier = Modifier.width(8.dp))
        Text(
            text = "购物助手",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "关闭助手"
            )
        }
    }
}

@Composable
private fun EmptyHint() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "问我点什么吧～\n例如：「想找一款 3000 元以内的运动耳机」",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorBanner(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun MiniChatInputRow(
    value: String,
    onValueChange: (String) -> Unit,
    isLoading: Boolean,
    onSend: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            placeholder = {
                Text(
                    text = if (isLoading) "等待回复中..." else "输入你的问题...",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            shape = RoundedCornerShape(20.dp),
            maxLines = 3,
            enabled = !isLoading
        )

        val canSend = value.isNotBlank() && !isLoading
        IconButton(
            onClick = onSend,
            enabled = canSend,
            modifier = Modifier.size(44.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Send,
                contentDescription = "发送",
                tint = if (canSend) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
        }
    }
}

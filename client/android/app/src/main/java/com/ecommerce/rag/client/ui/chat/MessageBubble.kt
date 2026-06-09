package com.ecommerce.rag.client.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ecommerce.rag.client.data.model.ChatMessage
import com.ecommerce.rag.client.ui.assistant.AgentProductCard
import com.ecommerce.rag.client.ui.theme.AiBubbleColor
import com.ecommerce.rag.client.ui.theme.UserBubbleColor

@Composable
fun MessageBubble(
    message: ChatMessage,
    onProductClick: (String) -> Unit = {},
    onAddToCartClick: ((String) -> Unit)? = null,
    onBuyNowClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val maxWidth = (configuration.screenWidthDp * 0.8).dp

    if (message.isUser) {
        UserMessageBubble(text = message.text, maxWidth = maxWidth, modifier = modifier)
    } else {
        AiMessageBubble(
            message = message,
            maxWidth = maxWidth,
            onProductClick = onProductClick,
            onAddToCartClick = onAddToCartClick,
            onBuyNowClick = onBuyNowClick,
            modifier = modifier
        )
    }
}

@Composable
private fun UserMessageBubble(
    text: String,
    maxWidth: Dp,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.End
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = maxWidth)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp))
                .background(UserBubbleColor)
                .padding(12.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun AiMessageBubble(
    message: ChatMessage,
    maxWidth: Dp,
    onProductClick: (String) -> Unit,
    onAddToCartClick: ((String) -> Unit)?,
    onBuyNowClick: ((String) -> Unit)?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = maxWidth)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp))
                .background(AiBubbleColor)
                .padding(12.dp)
        ) {
            if (message.text.isBlank() && message.isStreaming) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "正在思考...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (message.text.isNotBlank()) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        if (message.productCards.isNotEmpty()) {
            Column(
                modifier = Modifier.padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 同时拿到 add/buy 回调 → 用阶段 11 的柔和导购卡，带操作按钮；
                // 否则降级为旧的 ProductCardComposable（保留对其它调用方的兼容）。
                val useAgentCard = onAddToCartClick != null && onBuyNowClick != null
                message.productCards.forEach { card ->
                    if (useAgentCard) {
                        AgentProductCard(
                            product = card,
                            onProductClick = onProductClick,
                            onAddToCartClick = onAddToCartClick!!,
                            onBuyNowClick = onBuyNowClick!!
                        )
                    } else {
                        ProductCardComposable(
                            card = card,
                            onClick = { onProductClick(card.productId) }
                        )
                    }
                }
            }
        }

        if (message.isStreaming && message.text.isNotBlank()) {
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(start = 12.dp, top = 4.dp)
                    .size(14.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

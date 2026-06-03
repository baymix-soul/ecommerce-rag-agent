package com.ecommerce.rag.client.ui.assistant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 长按 FloatingBot 后浮出的动作菜单。
 *
 * 阶段 4 设计：
 *  - 垂直堆叠两个气泡（Chat 在上、Camera 在下），避免和屏幕左右边缘冲突；
 *  - 当前 hover 的气泡使用 primaryContainer 高亮，其余使用 surfaceVariant；
 *  - 菜单本身不消费手势，所有触摸由 FloatingBot 的 pointerInput 统一处理。
 */
@Composable
fun AssistantActionMenu(
    highlightedAction: AssistantAction?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.widthIn(min = 140.dp, max = 200.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ActionBubble(
            label = "和我聊聊",
            isHighlighted = highlightedAction == AssistantAction.Chat
        )
        ActionBubble(
            label = "拍照给我",
            isHighlighted = highlightedAction == AssistantAction.Camera
        )
    }
}

@Composable
private fun ActionBubble(
    label: String,
    isHighlighted: Boolean
) {
    val background = if (isHighlighted) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val foreground = if (isHighlighted) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val elevation = if (isHighlighted) 8.dp else 4.dp

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = background,
        contentColor = foreground,
        tonalElevation = elevation,
        shadowElevation = elevation
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isHighlighted) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )
    }
}

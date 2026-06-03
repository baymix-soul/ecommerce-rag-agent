package com.ecommerce.rag.client.ui.browse

import androidx.compose.ui.graphics.Color

/**
 * 阶段 6 视觉改造的局部颜色 token。
 *
 * 这些颜色仅限 ui/browse 内部以及 AssistantOverlay 计算底部避让时引用，
 * 不进入全局 Theme，避免影响详情页 / MiniChatPanel 已有可读性。
 */
internal object BrowseColors {
    val PageBg = Color(0xFFF6F6F6)
    val TextPrimary = Color(0xFF18181B)
    val TextSecondary = Color(0xFF71717A)
    val TextTertiary = Color(0xFFA1A1AA)
    val CardWhite = Color.White
    val AccentRed = Color(0xFFEF4444)
    val AccentRedSoft = Color(0xFFFFE9E9)
    val Dark = Color(0xFF09090B)
    val ChipBg = Color(0xFFF1F1F1)
    val Divider = Color(0xFFE5E5E5)
}

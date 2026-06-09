package com.ecommerce.rag.client.ui.profile

import androidx.compose.ui.graphics.Color

/**
 * 阶段 10：个人中心局部颜色 token。
 *
 * 不复用 ui/browse 的 `BrowseColors`，因为它是 internal 限定符；
 * 这里独立维护一份语义对齐的色板，避免跨包引用。
 */
internal object ProfileColors {
    val PageBg = Color(0xFFF6F6F6)
    val CardWhite = Color.White
    val TextPrimary = Color(0xFF18181B)
    val TextSecondary = Color(0xFF71717A)
    val TextTertiary = Color(0xFFA1A1AA)
    val AccentRed = Color(0xFFEF4444)
    val AccentRedSoft = Color(0xFFFFE9E9)
    val AccentOrange = Color(0xFFF97316)
    val Dark = Color(0xFF09090B)
    val ChipBg = Color(0xFFF1F1F1)
    val Divider = Color(0xFFE5E5E5)
    val BadgeGold = Color(0xFFFACC15)
}

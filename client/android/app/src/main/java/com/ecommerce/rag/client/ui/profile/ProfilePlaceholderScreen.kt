package com.ecommerce.rag.client.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * 阶段 10：个人中心的通用占位页。
 *
 * 适配以下入口（均由 [ProfileScreen] / [OrderFilter] 触发）：
 *  - 全部订单 / 待付款 / 待发货 / 待收货 / 待评价 / 退款售后
 *  - 收货地址 / 优惠券 / 我的收藏 / 浏览历史 / 设置 / 客服中心 / 关于项目
 *
 * 设计目标只有"占位"：不发任何后端请求，只展示标题 + 副标题 + 返回按钮，
 * 等后端接口落地后再替换为真实页面，无需调整 [MainActivity] 路由。
 */
@Composable
fun ProfilePlaceholderScreen(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ProfileColors.PageBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopBar(title = title, onBack = onBack)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    EmojiHeroBadge(emoji = pickEmojiFor(title))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = ProfileColors.TextPrimary,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ProfileColors.TextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "该功能为演示占位，后续可接入真实接口。",
                        style = MaterialTheme.typography.bodySmall,
                        color = ProfileColors.TextTertiary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    PrimaryButton(
                        text = "返回个人中心",
                        onClick = onBack
                    )
                }
            }
        }
    }
}

@Composable
private fun TopBar(title: String, onBack: () -> Unit) {
    Surface(
        color = ProfileColors.CardWhite,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.statusBarsPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "‹",
                        style = MaterialTheme.typography.headlineSmall,
                        color = ProfileColors.TextPrimary,
                        fontWeight = FontWeight.Light
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = ProfileColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun EmojiHeroBadge(emoji: String) {
    Box(
        modifier = Modifier
            .size(96.dp)
            .clip(CircleShape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        ProfileColors.AccentRedSoft,
                        Color(0xFFFFF1E5)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = emoji,
            style = MaterialTheme.typography.displaySmall
        )
    }
}

@Composable
private fun PrimaryButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(44.dp)
            .clip(RoundedCornerShape(50))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(ProfileColors.Dark, ProfileColors.AccentRed)
                )
            )
            .clickable { onClick() }
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * 根据标题猜测一个友好 emoji；找不到就回退到 📋。
 * 仅 UI 层用，不进数据层。
 */
private fun pickEmojiFor(title: String): String = when {
    title.contains("订单") -> "📦"
    title.contains("付款") -> "💳"
    title.contains("发货") -> "📦"
    title.contains("收货") && title.contains("地址") -> "📍"
    title.contains("收货") -> "🚚"
    title.contains("评价") -> "📝"
    title.contains("退款") || title.contains("售后") -> "💸"
    title.contains("收藏") -> "❤"
    title.contains("浏览") -> "🕘"
    title.contains("优惠券") -> "🎟"
    title.contains("设置") -> "⚙️"
    title.contains("客服") -> "🎧"
    title.contains("关于") -> "ℹ"
    else -> "📋"
}

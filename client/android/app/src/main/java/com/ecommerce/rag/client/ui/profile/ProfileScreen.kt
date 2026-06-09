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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ecommerce.rag.client.ui.browse.CommerceBottomBar

/**
 * 阶段 10：电商 App 个人中心。
 *
 * 设计参考淘宝 / 抖音电商个人中心：
 *  - 顶部用户卡：黑→红渐变背景 + 头像 + 用户名 + ID + 设置按钮 + 登录/退出入口
 *  - 资产/权益卡：优惠券 / 积分 / 红包 / 余额（全部占位）
 *  - 我的订单卡：标题 + "查看全部" + 5 个状态入口（待付款 / 待发货 / 待收货 / 待评价 / 退款）
 *  - 常用功能卡：收货地址 / 我的收藏 / 浏览历史 / 优惠券 / 客服中心 / 设置 / 关于项目（4 列宫格）
 *  - 推荐服务/占位卡：纯视觉，让页面饱满
 *
 * 业务真实接入只有：
 *  - 登录态 / 用户名 / 用户 ID（来自 AuthViewModel）
 *  - 购物车角标（来自 CartViewModel）
 *  - 登录 / 退出登录回调
 *  - 设置按钮 → SettingsScreen 占位
 * 其余订单 / 地址 / 优惠券 / 收藏 / 浏览历史均跳 [ProfilePlaceholderScreen]。
 */
@Composable
fun ProfileScreen(
    username: String?,
    userId: String?,
    isLoggedIn: Boolean,
    cartCount: Int,
    onLoginClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onCartClick: () -> Unit,
    onOrderClick: (OrderFilter) -> Unit,
    onAddressClick: () -> Unit,
    onCouponClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onCustomerServiceClick: () -> Unit,
    onAboutClick: () -> Unit,
    onBackToBrowse: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // 顶层用 Column 切两栏：上半 weight(1f) 可滚动内容，下半固定 CommerceBottomBar。
    // 这样底部 5 tab 在个人中心也始终可见，"我的" 高亮，点 "首页" 直接回 Browse。
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ProfileColors.PageBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            ProfileHeaderCard(
                username = username,
                userId = userId,
                isLoggedIn = isLoggedIn,
                onLoginClick = onLoginClick,
                onLogoutClick = onLogoutClick,
                onSettingsClick = onSettingsClick,
                onCartClick = onCartClick,
                cartCount = cartCount
            )

            Spacer(modifier = Modifier.height(12.dp))

            AssetsCard(
                onItemClick = { _ ->
                    // 资产/权益均无真实接口，统一走优惠券占位以减少回调数。
                    onCouponClick()
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            OrderCard(
                onSeeAll = { onOrderClick(OrderFilter.ALL) },
                onFilterClick = onOrderClick
            )

            Spacer(modifier = Modifier.height(12.dp))

            FeatureGridCard(
                onAddressClick = onAddressClick,
                onFavoriteClick = onFavoriteClick,
                onHistoryClick = onHistoryClick,
                onCouponClick = onCouponClick,
                onCustomerServiceClick = onCustomerServiceClick,
                onSettingsClick = onSettingsClick,
                onAboutClick = onAboutClick
            )

            Spacer(modifier = Modifier.height(12.dp))

            ServiceShowcaseCard()

            Spacer(modifier = Modifier.height(24.dp))
        }

        CommerceBottomBar(
            activeIndex = 4,
            onHomeClick = onBackToBrowse,
            onProfileClick = { /* 已在我的页，无需再跳 */ }
        )
    }
}

// ---------- 顶部用户卡 ----------

@Composable
private fun ProfileHeaderCard(
    username: String?,
    userId: String?,
    isLoggedIn: Boolean,
    onLoginClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onCartClick: () -> Unit,
    cartCount: Int
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        ProfileColors.Dark,
                        Color(0xFF1F0608),
                        ProfileColors.AccentRed
                    )
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "我的",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                CircleEmojiButton(
                    emoji = "⚙️",
                    onClick = onSettingsClick,
                    background = Color.White.copy(alpha = 0.18f),
                    contentColor = Color.White
                )
                Spacer(modifier = Modifier.size(8.dp))
                CartHeaderButton(count = cartCount, onClick = onCartClick)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AvatarCircle(
                    initial = (username?.takeIf { it.isNotBlank() }?.first()?.uppercaseChar()
                        ?: '?').toString(),
                    isLoggedIn = isLoggedIn
                )
                Spacer(modifier = Modifier.size(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isLoggedIn) {
                            username?.takeIf { it.isNotBlank() } ?: "demo"
                        } else {
                            "未登录"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isLoggedIn) {
                            "ID: ${userId?.takeIf { it.isNotBlank() } ?: "-"}"
                        } else {
                            "登录后查看订单与购物车"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (isLoggedIn) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            HeaderTag(text = "Demo 会员", emoji = "👑")
                            HeaderTag(text = "已登录", emoji = "✅")
                        }
                    } else {
                        HeaderTag(text = "立即登录解锁加购 / 收藏", emoji = "🔓")
                    }
                }

                Spacer(modifier = Modifier.size(8.dp))

                if (isLoggedIn) {
                    HeaderPillButton(text = "退出登录", onClick = onLogoutClick)
                } else {
                    HeaderPillButton(text = "立即登录", onClick = onLoginClick, primary = true)
                }
            }
        }
    }
}

@Composable
private fun AvatarCircle(initial: String, isLoggedIn: Boolean) {
    Box(
        modifier = Modifier
            .size(60.dp)
            .clip(CircleShape)
            .background(
                if (isLoggedIn) Color.White else Color.White.copy(alpha = 0.4f)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (isLoggedIn) initial else "?",
            style = MaterialTheme.typography.titleLarge,
            color = ProfileColors.AccentRed,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun HeaderTag(text: String, emoji: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = Color.White.copy(alpha = 0.18f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = emoji,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White
            )
            Spacer(modifier = Modifier.size(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun HeaderPillButton(
    text: String,
    onClick: () -> Unit,
    primary: Boolean = false
) {
    val bg = if (primary) Color.White else Color.White.copy(alpha = 0.18f)
    val fg = if (primary) ProfileColors.AccentRed else Color.White
    Surface(
        shape = RoundedCornerShape(50),
        color = bg,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = fg,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun CircleEmojiButton(
    emoji: String,
    onClick: () -> Unit,
    background: Color,
    contentColor: Color
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(background)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = emoji,
            style = MaterialTheme.typography.bodyMedium,
            color = contentColor
        )
    }
}

@Composable
private fun CartHeaderButton(count: Int, onClick: () -> Unit) {
    Box(modifier = Modifier.size(36.dp)) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.18f))
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🛒",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
        }
        if (count > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clip(CircleShape)
                    .background(ProfileColors.AccentRed)
                    .padding(horizontal = 4.dp, vertical = 1.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (count > 99) "99+" else count.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ---------- 资产/权益卡 ----------

private data class AssetItem(
    val key: String,
    val emoji: String,
    val title: String,
    val subtitle: String
)

private val ASSETS = listOf(
    AssetItem("COUPON", "🎟", "优惠券", "暂无可用"),
    AssetItem("POINTS", "✨", "积分", "0"),
    AssetItem("REDPACK", "🧧", "红包", "0"),
    AssetItem("BALANCE", "💰", "余额", "¥0.00"),
)

@Composable
private fun AssetsCard(onItemClick: (String) -> Unit) {
    SectionCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ASSETS.forEach { asset ->
                AssetCell(
                    item = asset,
                    onClick = { onItemClick(asset.key) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun AssetCell(
    item: AssetItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = item.emoji,
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = item.title,
            style = MaterialTheme.typography.labelMedium,
            color = ProfileColors.TextPrimary,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = item.subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = ProfileColors.TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ---------- 我的订单卡 ----------

private data class OrderEntry(
    val filter: OrderFilter,
    val emoji: String
)

private val ORDER_ENTRIES = listOf(
    OrderEntry(OrderFilter.PENDING_PAYMENT, "💳"),
    OrderEntry(OrderFilter.PENDING_SHIPMENT, "📦"),
    OrderEntry(OrderFilter.PENDING_RECEIPT, "🚚"),
    OrderEntry(OrderFilter.PENDING_REVIEW, "📝"),
    OrderEntry(OrderFilter.REFUND, "💸"),
)

@Composable
private fun OrderCard(
    onSeeAll: () -> Unit,
    onFilterClick: (OrderFilter) -> Unit
) {
    SectionCard {
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "我的订单",
                    style = MaterialTheme.typography.titleSmall,
                    color = ProfileColors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { onSeeAll() }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "查看全部",
                        style = MaterialTheme.typography.labelMedium,
                        color = ProfileColors.TextSecondary
                    )
                    Text(
                        text = " ›",
                        style = MaterialTheme.typography.labelMedium,
                        color = ProfileColors.TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ORDER_ENTRIES.forEach { entry ->
                    OrderCell(
                        emoji = entry.emoji,
                        title = entry.filter.title,
                        onClick = { onFilterClick(entry.filter) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderCell(
    emoji: String,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(ProfileColors.AccentRedSoft),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = emoji,
                style = MaterialTheme.typography.titleMedium
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = ProfileColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ---------- 常用功能卡 ----------

private data class FeatureItem(
    val key: String,
    val emoji: String,
    val title: String
)

@Composable
private fun FeatureGridCard(
    onAddressClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onCouponClick: () -> Unit,
    onCustomerServiceClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onAboutClick: () -> Unit
) {
    val items = listOf(
        FeatureItem("ADDRESS", "📍", "收货地址") to onAddressClick,
        FeatureItem("FAVORITE", "❤", "我的收藏") to onFavoriteClick,
        FeatureItem("HISTORY", "🕘", "浏览历史") to onHistoryClick,
        FeatureItem("COUPON", "🎟", "优惠券") to onCouponClick,
        FeatureItem("CS", "🎧", "客服中心") to onCustomerServiceClick,
        FeatureItem("SETTINGS", "⚙️", "设置") to onSettingsClick,
        FeatureItem("ABOUT", "ℹ", "关于项目") to onAboutClick,
    )

    SectionCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Text(
                text = "常用功能",
                style = MaterialTheme.typography.titleSmall,
                color = ProfileColors.TextPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            // 4 列宫格，最多 2 行；最后一行不足 4 个时占空 weight 维持栅格。
            items.chunked(4).forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    row.forEach { (item, click) ->
                        FeatureCell(
                            emoji = item.emoji,
                            title = item.title,
                            onClick = click,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // 用透明占位 weight 让最后一行也对齐 4 格。
                    repeat(4 - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureCell(
    emoji: String,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(ProfileColors.ChipBg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = emoji,
                style = MaterialTheme.typography.titleMedium
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = ProfileColors.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ---------- 推荐服务/占位卡 ----------

@Composable
private fun ServiceShowcaseCard() {
    SectionCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = ProfileColors.AccentRedSoft
                ) {
                    Text(
                        text = "🛡 服务保障",
                        style = MaterialTheme.typography.labelSmall,
                        color = ProfileColors.AccentRed,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Text(
                text = "正品保障 · 7 天无理由 · 极速退款",
                style = MaterialTheme.typography.bodyMedium,
                color = ProfileColors.TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "本页内容为演示，真实订单、地址、优惠券系统将在后端接入完成后启用。",
                style = MaterialTheme.typography.bodySmall,
                color = ProfileColors.TextSecondary
            )
        }
    }
}

// ---------- 通用卡片容器 ----------

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = ProfileColors.CardWhite,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

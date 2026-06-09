package com.ecommerce.rag.client.ui.assistant

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ecommerce.rag.client.data.model.ProductCardUiModel

/**
 * 阶段 11：机器人聊天小窗里专用的"柔和导购卡"。
 *
 * 设计要点：
 *  - 比 [com.ecommerce.rag.client.ui.chat.ProductCardComposable] 更轻、更"导购"风格：
 *    暖白底、大圆角、轻阴影、推荐胶囊、操作按钮。
 *  - 卡片主体（图片 + 文字信息）单独包在一个 clickable Row 里，点击进详情。
 *  - 操作按钮 Row 在主体外，独立的 clickable，不会冒泡触发"进详情"。
 *  - "加入购物车" = 浅红描边胶囊；"立刻下单" = 红色渐变实心胶囊。
 *  - 信息密度低于浏览页 BrowseProductCard，避免在小窗里挤爆。
 */
@Composable
fun AgentProductCard(
    product: ProductCardUiModel,
    onProductClick: (String) -> Unit,
    onAddToCartClick: (String) -> Unit,
    onBuyNowClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = AgentCardColors.CardBg,
        tonalElevation = 1.dp,
        shadowElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // ---- 卡片主体：点击进详情 ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onProductClick(product.productId) }
                    .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.Top
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(product.imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = product.name,
                    modifier = Modifier
                        .size(84.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(AgentCardColors.ImagePlaceholder),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 84.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = AgentCardColors.TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    val metaLine = listOfNotNull(
                        product.brand?.takeIf { it.isNotBlank() },
                        product.subCategory?.takeIf { it.isNotBlank() }
                    ).joinToString(" · ")
                    if (metaLine.isNotBlank()) {
                        Text(
                            text = metaLine,
                            style = MaterialTheme.typography.labelSmall,
                            color = AgentCardColors.TextTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "¥",
                            style = MaterialTheme.typography.labelSmall,
                            color = AgentCardColors.Price,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                        Text(
                            text = String.format("%.0f", product.price),
                            style = MaterialTheme.typography.titleMedium,
                            color = AgentCardColors.Price,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = product.currency,
                            style = MaterialTheme.typography.labelSmall,
                            color = AgentCardColors.TextTertiary,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                }
            }

            // ---- 推荐理由胶囊（在主体下方，按钮上方）----
            if (product.reason.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = AgentCardColors.ReasonBg,
                    modifier = Modifier
                        .padding(start = 12.dp, end = 12.dp, bottom = 8.dp)
                ) {
                    Text(
                        text = "推荐 · ${product.reason}",
                        style = MaterialTheme.typography.labelSmall,
                        color = AgentCardColors.ReasonText,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // ---- 操作按钮：加入购物车 / 立刻下单 ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, top = 2.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AddToCartPill(
                    onClick = { onAddToCartClick(product.productId) },
                    modifier = Modifier.weight(1f)
                )
                BuyNowPill(
                    onClick = { onBuyNowClick(product.productId) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun AddToCartPill(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(50))
            .background(Color.White)
            .border(1.dp, AgentCardColors.SoftBorder, RoundedCornerShape(50))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "加入购物车",
            style = MaterialTheme.typography.labelMedium,
            color = AgentCardColors.Price,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun BuyNowPill(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(50))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(AgentCardColors.BuyStart, AgentCardColors.BuyEnd)
                )
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "立刻下单",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * 仅本文件使用的柔色板。避免污染全局 Color.kt，也避免和 Browse/Cart 的红色完全统一，
 * 让聊天小窗里的卡片视觉更"柔和"。
 */
private object AgentCardColors {
    val CardBg = Color(0xFFFFFBFA)
    val ImagePlaceholder = Color(0xFFF1F1F1)
    val TextPrimary = Color(0xFF18181B)
    val TextTertiary = Color(0xFFA1A1AA)
    val Price = Color(0xFFEF4444)
    val ReasonBg = Color(0xFFFFF1F1)
    val ReasonText = Color(0xFFD9534F)
    val SoftBorder = Color(0xFFFFD8D8)
    val BuyStart = Color(0xFFFF6B6B)
    val BuyEnd = Color(0xFFEF4444)
}

package com.ecommerce.rag.client.ui.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ecommerce.rag.client.data.model.ProductCardUiModel
import kotlin.math.abs

/**
 * 浏览页专用商品卡片：大圆角、顶部大图、底部标题/价格/品牌。
 *
 * 设计稿对齐：
 *  - 白底，圆角 20dp，shadowElevation 2dp；
 *  - 顶部 170dp 大图，左上角浮层 subCategory 标签，
 *    右上角部分商品显示"视频"角标，右下角"♡"收藏；
 *  - 标题两行截断；价格红色 + 原价灰色删除线；
 *  - 已售 / 评分使用 index 派生的"假展示"数据，**不写回 PageContext**。
 *
 * UI 假数据规则（仅用于展示，对外协议不变）：
 *  - oldPrice  = price * 1.4
 *  - soldCount = 100 + ((index + productId.hashCode().abs()) % 9900) → "已售 8.6k+" 风格
 *  - rating    = avgRating ?: 4.6 + (index % 4) * 0.1
 *  - 标签池："今日爆款" / "精选好物" / "回购榜单" / "限时秒杀" / subCategory
 */
@Composable
fun BrowseProductCard(
    product: ProductCardUiModel,
    index: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = BrowseColors.CardWhite,
        shadowElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            ProductImage(product = product, index = index)
            ProductInfo(product = product, index = index)
        }
    }
}

@Composable
private fun ProductImage(
    product: ProductCardUiModel,
    index: Int
) {
    val tagLabel = pickTopTag(product, index)
    val showVideo = index % 4 == 2

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
            .background(BrowseColors.ChipBg)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(product.imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = product.name,
            modifier = Modifier.fillMaxWidth().height(170.dp),
            contentScale = ContentScale.Crop
        )

        // 左上角：subCategory / 派生标签
        if (tagLabel.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 0.dp, bottomEnd = 8.dp),
                color = BrowseColors.Dark.copy(alpha = 0.78f),
                modifier = Modifier
                    .align(Alignment.TopStart)
            ) {
                Text(
                    text = tagLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = BrowseColors.CardWhite,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        // 右上角：视频角标（部分商品）
        if (showVideo) {
            Surface(
                shape = RoundedCornerShape(bottomStart = 8.dp),
                color = BrowseColors.AccentRed,
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Text(
                    text = "▶ 视频",
                    style = MaterialTheme.typography.labelSmall,
                    color = BrowseColors.CardWhite,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        // 右下角：收藏按钮（仅 UI，无功能）
        Surface(
            shape = CircleShape,
            color = BrowseColors.CardWhite.copy(alpha = 0.92f),
            shadowElevation = 2.dp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
                .size(28.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "♡",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BrowseColors.AccentRed,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ProductInfo(
    product: ProductCardUiModel,
    index: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = product.name.ifBlank { "未命名商品" },
            style = MaterialTheme.typography.bodyMedium,
            color = BrowseColors.TextPrimary,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        val oldPrice = product.price * 1.4
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "¥",
                style = MaterialTheme.typography.labelMedium,
                color = BrowseColors.AccentRed,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = String.format("%.0f", product.price),
                style = MaterialTheme.typography.titleMedium,
                color = BrowseColors.AccentRed,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "¥${String.format("%.0f", oldPrice)}",
                style = MaterialTheme.typography.labelSmall,
                color = BrowseColors.TextTertiary,
                textDecoration = TextDecoration.LineThrough
            )
        }

        Text(
            text = "已售 ${formatSold(product, index)}",
            style = MaterialTheme.typography.labelSmall,
            color = BrowseColors.TextSecondary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = product.brand?.takeIf { it.isNotBlank() } ?: "官方旗舰",
                style = MaterialTheme.typography.labelSmall,
                color = BrowseColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "★ ${String.format("%.1f", deriveRating(product, index))}",
                style = MaterialTheme.typography.labelSmall,
                color = BrowseColors.AccentRed,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun pickTopTag(product: ProductCardUiModel, index: Int): String {
    product.subCategory?.takeIf { it.isNotBlank() }?.let { return it }
    val pool = listOf("今日爆款", "精选好物", "回购榜单", "限时秒杀")
    return pool[abs(index) % pool.size]
}

private fun formatSold(product: ProductCardUiModel, index: Int): String {
    val seed = abs((product.productId.hashCode() xor (index * 31)))
    val sold = 100 + seed % 9900
    return when {
        sold >= 10_000 -> "${sold / 1000 / 10.0}w+"
        sold >= 1000 -> "${sold / 100 / 10.0}k+"
        else -> "$sold+"
    }
}

private fun deriveRating(product: ProductCardUiModel, index: Int): Double {
    product.avgRating?.let {
        // products.json 里部分商品评分较低（如 2.x），做一个 UI 友好的最低值，
        // 但不改原始数据。
        return it.coerceIn(3.0, 5.0)
    }
    return 4.6 + (abs(index) % 4) * 0.1
}

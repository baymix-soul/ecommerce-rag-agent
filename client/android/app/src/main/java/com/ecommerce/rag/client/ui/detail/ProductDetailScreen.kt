package com.ecommerce.rag.client.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ecommerce.rag.client.data.model.ProductDetailUiModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 阶段 8 重写：淘宝/抖音风格商品详情页。
 *
 * 结构：
 *   Box
 *   ├── LazyColumn（顶部沉浸式首图 + 信息卡 + 优惠卡 + 规格卡 + 店铺卡 + 描述卡 + 底部留白）
 *   ├── 顶部浮动操作栏（返回 / 分享 / 收藏）
 *   └── 底部购买操作栏（客服 / 店铺 / 购物车 / 加入购物车 / 立即购买）
 *
 * 数据：
 *   - 完全使用 ProductDetailUiModel；
 *   - 后端失败时由 ViewModel 自动 fallback 到 LocalProductRepository；
 *   - 本页不直接读 LocalProductRepository。
 *
 * 行为限制：
 *   - 分享 / 收藏 / 购物车 / 加购 / 立即购买都是 UI 占位，点击只弹 toast；
 *   - 多图轮播本轮没接入，右下角"1/5"是静态占位；
 *   - FloatingBot 由 MainActivity 叠加在 Box 外层，本页不重复加 AssistantOverlay。
 */
@Composable
fun ProductDetailScreen(
    viewModel: ProductDetailViewModel,
    productId: String,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var toastMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(productId) {
        viewModel.loadProductDetail(productId)
    }

    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(1600)
            toastMessage = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DetailColors.PageBg)
    ) {
        when {
            uiState.isLoading && uiState.product == null -> {
                DetailLoading()
            }
            uiState.product != null -> {
                DetailBody(
                    product = uiState.product!!,
                    onShowToast = { msg ->
                        scope.launch { toastMessage = msg }
                    }
                )
            }
            else -> {
                DetailError(
                    message = uiState.error ?: "商品详情加载失败",
                    onBack = onBack
                )
            }
        }

        // 顶部浮动操作栏（透明背景，叠在 HeroImage 之上）
        DetailFloatingTopBar(
            onBack = onBack,
            onShare = { toastMessage = "已分享（占位）" },
            onFavorite = { toastMessage = "已加入收藏（占位）" }
        )

        // 底部购买操作栏（FloatingBot 默认底部留 160dp 安全区，会落在购买栏之上）
        if (uiState.product != null) {
            DetailBottomBar(
                modifier = Modifier.align(Alignment.BottomCenter),
                onCustomerService = { toastMessage = "客服中（占位）" },
                onShop = { toastMessage = "店铺主页（占位）" },
                onCart = { toastMessage = "购物车（占位）" },
                onAddToCart = { toastMessage = "已加入购物车（占位）" },
                onBuyNow = { toastMessage = "立即购买（占位）" }
            )
        }

        // toast
        toastMessage?.let { msg ->
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xCC222222),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 120.dp)
            ) {
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }
    }
}

// ============== body ==============

@Composable
private fun DetailBody(
    product: ProductDetailUiModel,
    onShowToast: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 160.dp), // 留给底部购买栏 + 视觉缓冲
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { HeroImage(product = product) }
        item { CoreInfoCard(product = product) }
        item { PromotionCard() }
        item { SpecPickerCard(product = product) }
        item { ShopCard(product = product, onEnterShop = { onShowToast("进店逛逛（占位）") }) }
        item { DescriptionCard(product = product) }
    }
}

// ============== hero ==============

@Composable
private fun HeroImage(product: ProductDetailUiModel) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp)
            .background(Color.White)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(product.imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = product.name,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // 右下角：图片页码占位 1/5
        Surface(
            shape = RoundedCornerShape(50),
            color = Color(0x99000000),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
        ) {
            Text(
                text = "1 / 5",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun DetailFloatingTopBar(
    onBack: () -> Unit,
    onShare: () -> Unit,
    onFavorite: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        FloatingCircleAction(label = "‹", onClick = onBack)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FloatingCircleAction(label = "↗", onClick = onShare)
            FloatingCircleAction(label = "♡", onClick = onFavorite)
        }
    }
}

@Composable
private fun FloatingCircleAction(label: String, onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = Color(0x55000000),
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .clickable { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ============== cards ==============

@Composable
private fun CoreInfoCard(product: ProductDetailUiModel) {
    DetailCard(horizontalPadding = 16.dp) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "¥",
                style = MaterialTheme.typography.titleMedium,
                color = DetailColors.Price,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = formatPrice(product.price),
                style = MaterialTheme.typography.headlineMedium,
                color = DetailColors.Price,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            val oldPrice = product.price * 1.4
            Text(
                text = "¥${formatPrice(oldPrice)}",
                style = MaterialTheme.typography.bodySmall,
                color = DetailColors.TextTertiary,
                textDecoration = TextDecoration.LineThrough,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = product.currency,
                style = MaterialTheme.typography.labelSmall,
                color = DetailColors.TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = DetailColors.Price,
                modifier = Modifier.padding(end = 6.dp)
            ) {
                Text(
                    text = "到手价",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            Text(
                text = "下单立减 · 限时直降",
                style = MaterialTheme.typography.labelSmall,
                color = DetailColors.Price
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = product.name,
            style = MaterialTheme.typography.titleLarge,
            color = DetailColors.TextPrimary,
            fontWeight = FontWeight.Bold,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(6.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = product.brand.takeIf { it.isNotBlank() } ?: "官方旗舰",
                style = MaterialTheme.typography.bodySmall,
                color = DetailColors.TextSecondary
            )
            Text(
                text = "·",
                style = MaterialTheme.typography.bodySmall,
                color = DetailColors.TextTertiary
            )
            Text(
                text = "★ ${formatRating(product.avgRating)}",
                style = MaterialTheme.typography.bodySmall,
                color = DetailColors.Price,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        val tags = buildList {
            product.category.takeIf { it.isNotBlank() }?.let(::add)
            product.subCategory.takeIf { it.isNotBlank() }?.let(::add)
            add("官方正品")
            add("极速退款")
            add("7 天无理由")
        }
        TagFlowRow(tags = tags)
    }
}

@Composable
private fun PromotionCard() {
    Box(modifier = Modifier.padding(horizontal = 10.dp)) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = DetailColors.PromoBg,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "🎁 限时优惠",
                        style = MaterialTheme.typography.titleSmall,
                        color = DetailColors.Price,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "今晚 8 点开抢",
                        style = MaterialTheme.typography.labelSmall,
                        color = DetailColors.Price
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PromoChip(text = "满 300 减 40")
                    PromoChip(text = "限时券 ¥20")
                    PromoChip(text = "新人专享")
                }
                Text(
                    text = "保障服务：假一赔三 · 顺丰包邮 · 30 天质保",
                    style = MaterialTheme.typography.bodySmall,
                    color = DetailColors.TextSecondary
                )
            }
        }
    }
}

@Composable
private fun SpecPickerCard(product: ProductDetailUiModel) {
    DetailCard(horizontalPadding = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "已选",
                style = MaterialTheme.typography.bodyMedium,
                color = DetailColors.TextSecondary
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = previewSelection(product),
                style = MaterialTheme.typography.bodyMedium,
                color = DetailColors.TextPrimary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "›",
                style = MaterialTheme.typography.titleMedium,
                color = DetailColors.TextTertiary
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        val chipValues = buildList {
            product.specs.entries.take(3).forEach { add(it.value) }
            if (isEmpty()) add("默认款")
        }
        TagFlowRow(tags = chipValues, accent = true)

        Spacer(modifier = Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "配送",
                style = MaterialTheme.typography.bodySmall,
                color = DetailColors.TextSecondary
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "顺丰包邮 · 预计 48 小时内发货",
                style = MaterialTheme.typography.bodySmall,
                color = DetailColors.TextPrimary
            )
        }
    }
}

@Composable
private fun ShopCard(
    product: ProductDetailUiModel,
    onEnterShop: () -> Unit
) {
    DetailCard(horizontalPadding = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = DetailColors.PromoBg,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = (product.brand.firstOrNull()?.toString() ?: "店").take(1),
                        style = MaterialTheme.typography.titleMedium,
                        color = DetailColors.Price,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.brand.takeIf { it.isNotBlank() } ?: "官方旗舰店",
                    style = MaterialTheme.typography.titleSmall,
                    color = DetailColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "综合评分 ★ ${formatRating(product.avgRating)} · 粉丝 12.6w",
                    style = MaterialTheme.typography.bodySmall,
                    color = DetailColors.TextSecondary
                )
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = DetailColors.Price,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable { onEnterShop() }
            ) {
                Text(
                    text = "进店逛逛",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun DescriptionCard(product: ProductDetailUiModel) {
    DetailCard(horizontalPadding = 16.dp) {
        Text(
            text = "商品详情",
            style = MaterialTheme.typography.titleMedium,
            color = DetailColors.TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = product.description.ifBlank { "暂无更多详细介绍。" },
            style = MaterialTheme.typography.bodyMedium,
            color = DetailColors.TextPrimary
        )

        if (product.specs.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "规格参数",
                style = MaterialTheme.typography.titleSmall,
                color = DetailColors.TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                product.specs.forEach { (k, v) ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = k,
                            style = MaterialTheme.typography.bodySmall,
                            color = DetailColors.TextSecondary,
                            modifier = Modifier.width(96.dp)
                        )
                        Text(
                            text = v,
                            style = MaterialTheme.typography.bodySmall,
                            color = DetailColors.TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

// ============== bottom buy bar ==============

@Composable
private fun DetailBottomBar(
    modifier: Modifier = Modifier,
    onCustomerService: () -> Unit,
    onShop: () -> Unit,
    onCart: () -> Unit,
    onAddToCart: () -> Unit,
    onBuyNow: () -> Unit
) {
    Surface(
        color = Color.White,
        shadowElevation = 12.dp,
        modifier = modifier
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BottomMiniAction(icon = "💬", label = "客服", onClick = onCustomerService)
            BottomMiniAction(icon = "🏬", label = "店铺", onClick = onShop)
            BottomMiniAction(icon = "🛒", label = "购物车", onClick = onCart)

            Spacer(modifier = Modifier.width(4.dp))

            BottomCta(
                text = "加入购物车",
                gradient = Brush.horizontalGradient(
                    listOf(Color(0xFFFFB14F), Color(0xFFFF7A1A))
                ),
                onClick = onAddToCart,
                modifier = Modifier.weight(1f)
            )
            BottomCta(
                text = "立即购买",
                gradient = Brush.horizontalGradient(
                    listOf(Color(0xFFFF5757), Color(0xFFEF4444))
                ),
                onClick = onBuyNow,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BottomMiniAction(icon: String, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Text(text = icon, style = MaterialTheme.typography.titleMedium)
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = DetailColors.TextSecondary
        )
    }
}

@Composable
private fun BottomCta(
    text: String,
    gradient: Brush,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(50))
            .background(gradient)
            .clickable { onClick() },
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

// ============== shared building blocks ==============

@Composable
private fun DetailCard(
    horizontalPadding: androidx.compose.ui.unit.Dp,
    content: @Composable () -> Unit
) {
    Box(modifier = Modifier.padding(horizontal = 10.dp)) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = horizontalPadding, vertical = 14.dp)
            ) {
                content()
            }
        }
    }
}

/**
 * 极简 flow row：仅依赖标准 Compose，避免引入 accompanist。
 * 每行手动累计宽度比较脆，所以这里直接用一个横向布局 + 限制 6 个 tag，简化处理。
 */
@Composable
private fun TagFlowRow(tags: List<String>, accent: Boolean = false) {
    val take = tags.take(6)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        take.forEach { t ->
            TagChip(text = t, accent = accent)
        }
    }
}

@Composable
private fun TagChip(text: String, accent: Boolean) {
    val bg = if (accent) DetailColors.PromoBg else DetailColors.ChipBg
    val fg = if (accent) DetailColors.Price else DetailColors.TextSecondary
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bg,
        modifier = Modifier.border(
            width = if (accent) 1.dp else 0.dp,
            color = if (accent) DetailColors.Price.copy(alpha = 0.4f) else Color.Transparent,
            shape = RoundedCornerShape(6.dp)
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun PromoChip(text: String) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color.White,
        modifier = Modifier.border(
            width = 1.dp,
            color = DetailColors.Price.copy(alpha = 0.6f),
            shape = RoundedCornerShape(6.dp)
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = DetailColors.Price,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

// ============== status ==============

@Composable
private fun DetailLoading() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(color = DetailColors.Price)
            Text(
                text = "加载商品详情…",
                style = MaterialTheme.typography.bodySmall,
                color = DetailColors.TextSecondary
            )
        }
    }
}

@Composable
private fun DetailError(message: String, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = DetailColors.Price
            )
            Surface(
                shape = RoundedCornerShape(50),
                color = DetailColors.Price,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable { onBack() }
            ) {
                Text(
                    text = "返回",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
            }
        }
    }
}

// ============== helpers ==============

private fun formatPrice(price: Double): String = String.format("%.0f", price)

private fun formatRating(rating: Double): String {
    val clamped = rating.coerceIn(0.0, 5.0)
    return String.format("%.1f", clamped)
}

private fun previewSelection(product: ProductDetailUiModel): String {
    val pieces = buildList {
        product.specs.entries.take(2).forEach { add("${it.key}:${it.value}") }
        if (isEmpty()) add("默认款 · 标准配置")
    }
    return pieces.joinToString(" / ")
}

// ============== local color tokens ==============

private object DetailColors {
    val PageBg = Color(0xFFF6F6F6)
    val TextPrimary = Color(0xFF18181B)
    val TextSecondary = Color(0xFF71717A)
    val TextTertiary = Color(0xFFA1A1AA)
    val Price = Color(0xFFEF4444)
    val PromoBg = Color(0xFFFFE9E9)
    val ChipBg = Color(0xFFF1F1F1)
}

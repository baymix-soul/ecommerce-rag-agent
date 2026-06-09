package com.ecommerce.rag.client.ui.cart

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ecommerce.rag.client.data.remote.CartItem
import kotlinx.coroutines.delay

/**
 * 阶段 9 重写：电商风格购物车页。
 *
 * - 顶部栏（返回 / "购物车" + 件数）
 * - 未登录时展示登录提示卡，点击跳 [onRequireLogin]
 * - LazyColumn 商品卡片 + 数量步进 + 删除
 * - 底部结算栏（合计 + 件数 + 去结算，结算仅 toast）
 * - 空购物车有"去首页逛逛"按钮
 * - 清空购物车前要二次确认（AlertDialog）
 */
@Composable
fun CartScreen(
    viewModel: CartViewModel = viewModel(),
    onBack: () -> Unit,
    onProductClick: (String) -> Unit = {},
    onGoBrowse: () -> Unit = onBack,
    onRequireLogin: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var confirmClear by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadCart()
    }

    val latestRequireLogin by rememberUpdatedState(onRequireLogin)

    LaunchedEffect(uiState.message) {
        val msg = uiState.message ?: return@LaunchedEffect
        toast = msg
        delay(1600)
        toast = null
        viewModel.clearMessage()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CartColors.PageBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            CartTopBar(
                totalQuantity = uiState.cartView?.totalQuantity ?: 0,
                showClearButton = (uiState.cartView?.items?.isNotEmpty() == true),
                onBack = onBack,
                onClearAll = { confirmClear = true }
            )

            // 登录提示卡常驻在顶部，未登录任何时间点都能看见
            if (uiState.needsLogin) {
                LoginPromptCard(
                    onLogin = {
                        viewModel.clearNeedsLogin()
                        latestRequireLogin()
                    }
                )
            }

            when {
                uiState.isLoading && uiState.cartView == null -> CartLoading()
                uiState.cartView == null && uiState.error != null && !uiState.needsLogin -> {
                    CartEmpty(
                        message = uiState.error ?: "购物车加载失败",
                        ctaText = "回到首页",
                        onCta = onGoBrowse
                    )
                }
                uiState.cartView == null || uiState.cartView!!.items.isEmpty() -> {
                    CartEmpty(
                        message = "购物车还是空的",
                        ctaText = "去首页逛逛",
                        onCta = onGoBrowse
                    )
                }
                else -> {
                    val cartView = uiState.cartView!!

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(
                            start = 12.dp, end = 12.dp, top = 10.dp, bottom = 16.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(
                            items = cartView.items,
                            key = { item -> item.productId }
                        ) { item ->
                            CartItemRow(
                                item = item,
                                onClick = { onProductClick(item.productId) },
                                onIncrease = { viewModel.increase(item.productId) },
                                onDecrease = {
                                    if (item.quantity > 1) {
                                        viewModel.decrease(item.productId)
                                    }
                                },
                                onRemove = { viewModel.removeItem(item.productId) }
                            )
                        }
                    }

                    CartBottomBar(
                        totalAmount = cartView.totalAmount,
                        totalQuantity = cartView.totalQuantity,
                        currency = cartView.currency,
                        onCheckout = { toast = "结算功能下一步接入" },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        toast?.let { msg ->
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xCC222222),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 140.dp)
            ) {
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }

        if (confirmClear) {
            AlertDialog(
                onDismissRequest = { confirmClear = false },
                title = {
                    Text(text = "清空购物车")
                },
                text = {
                    Text(text = "确认要清空购物车中全部商品吗？此操作无法撤销。")
                },
                confirmButton = {
                    TextButton(onClick = {
                        confirmClear = false
                        viewModel.clearCart()
                    }) {
                        Text(text = "清空", color = CartColors.Price)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { confirmClear = false }) {
                        Text(text = "取消", color = CartColors.TextSecondary)
                    }
                }
            )
        }
    }
}

// ============== top bar ==============

@Composable
private fun CartTopBar(
    totalQuantity: Int,
    showClearButton: Boolean,
    onBack: () -> Unit,
    onClearAll: () -> Unit
) {
    Surface(
        color = Color.White,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color.Transparent,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable { onBack() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "‹",
                        style = MaterialTheme.typography.headlineSmall,
                        color = CartColors.TextPrimary,
                        fontWeight = FontWeight.Light
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = "购物车",
                style = MaterialTheme.typography.titleMedium,
                color = CartColors.TextPrimary,
                fontWeight = FontWeight.Bold
            )

            if (totalQuantity > 0) {
                Text(
                    text = "（${totalQuantity}件）",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CartColors.TextSecondary
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            if (showClearButton) {
                Text(
                    text = "清空",
                    style = MaterialTheme.typography.labelLarge,
                    color = CartColors.TextSecondary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onClearAll() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

// ============== login prompt ==============

@Composable
private fun LoginPromptCard(onLogin: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CartColors.PromoBg,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "登录后查看购物车",
                    style = MaterialTheme.typography.titleSmall,
                    color = CartColors.Price,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "登录可同步加购记录与对话式买单",
                    style = MaterialTheme.typography.bodySmall,
                    color = CartColors.TextSecondary
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Surface(
                shape = RoundedCornerShape(50),
                color = CartColors.Price,
                modifier = Modifier.clickable { onLogin() }
            ) {
                Text(
                    text = "去登录",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

// ============== item ==============

@Composable
private fun CartItemRow(
    item: CartItem,
    onClick: () -> Unit,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(item.imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = item.name,
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CartColors.ChipBg),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = CartColors.TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "删除",
                        style = MaterialTheme.typography.labelSmall,
                        color = CartColors.TextTertiary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onRemove() }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "¥",
                        style = MaterialTheme.typography.labelSmall,
                        color = CartColors.Price,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = String.format("%.2f", item.price),
                        style = MaterialTheme.typography.titleMedium,
                        color = CartColors.Price,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    QuantityControl(
                        quantity = item.quantity,
                        canDecrease = item.quantity > 1,
                        onIncrease = onIncrease,
                        onDecrease = onDecrease
                    )

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "小计",
                            style = MaterialTheme.typography.labelSmall,
                            color = CartColors.TextTertiary
                        )
                        Text(
                            text = "¥${String.format("%.2f", item.subtotal)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CartColors.TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuantityControl(
    quantity: Int,
    canDecrease: Boolean,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StepButton(
            label = "−",
            enabled = canDecrease,
            onClick = onDecrease
        )

        Text(
            text = "$quantity",
            style = MaterialTheme.typography.bodyMedium,
            color = CartColors.TextPrimary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        StepButton(
            label = "+",
            enabled = true,
            onClick = onIncrease
        )
    }
}

@Composable
private fun StepButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val bg = if (enabled) CartColors.ChipBg else CartColors.ChipBg.copy(alpha = 0.5f)
    val fg = if (enabled) CartColors.TextPrimary else CartColors.TextTertiary
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bg,
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(6.dp))
            .clickable(enabled = enabled) { onClick() }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = fg,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ============== bottom checkout bar ==============

@Composable
private fun CartBottomBar(
    totalAmount: Double,
    totalQuantity: Int,
    currency: String,
    onCheckout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.White,
        shadowElevation = 12.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "合计",
                        style = MaterialTheme.typography.labelSmall,
                        color = CartColors.TextSecondary,
                        modifier = Modifier.padding(end = 6.dp, bottom = 2.dp)
                    )
                    Text(
                        text = "¥",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CartColors.Price,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                    Text(
                        text = String.format("%.2f", totalAmount),
                        style = MaterialTheme.typography.titleLarge,
                        color = CartColors.Price,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "共 ${totalQuantity} 件 · $currency",
                    style = MaterialTheme.typography.labelSmall,
                    color = CartColors.TextTertiary
                )
            }

            Box(
                modifier = Modifier
                    .height(44.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        brush = Brush.horizontalGradient(
                            listOf(Color(0xFFFF5757), CartColors.Price)
                        )
                    )
                    .clickable { onCheckout() }
                    .padding(horizontal = 28.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "去结算",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ============== status placeholders ==============

@Composable
private fun CartLoading() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = CartColors.Price,
            modifier = Modifier.size(36.dp)
        )
    }
}

@Composable
private fun CartEmpty(
    message: String,
    ctaText: String,
    onCta: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "🛒",
                style = MaterialTheme.typography.displaySmall
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = CartColors.TextSecondary
            )
            Surface(
                shape = RoundedCornerShape(50),
                color = CartColors.Price,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable { onCta() }
            ) {
                Text(
                    text = ctaText,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
            }
        }
    }
}

private object CartColors {
    val PageBg = Color(0xFFF6F6F6)
    val TextPrimary = Color(0xFF18181B)
    val TextSecondary = Color(0xFF71717A)
    val TextTertiary = Color(0xFFA1A1AA)
    val Price = Color(0xFFEF4444)
    val ChipBg = Color(0xFFF1F1F1)
    val PromoBg = Color(0xFFFFE9E9)
}

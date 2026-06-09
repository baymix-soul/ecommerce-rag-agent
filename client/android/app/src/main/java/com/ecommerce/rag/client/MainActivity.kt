package com.ecommerce.rag.client

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ecommerce.rag.client.data.model.PageContext
import com.ecommerce.rag.client.data.model.PageType
import com.ecommerce.rag.client.ui.assistant.AssistantOverlay
import com.ecommerce.rag.client.ui.assistant.AssistantViewModel
import com.ecommerce.rag.client.ui.auth.AuthViewModel
import com.ecommerce.rag.client.ui.auth.LoginScreen
import com.ecommerce.rag.client.ui.browse.ProductBrowseScreen
import com.ecommerce.rag.client.ui.cart.CartScreen
import com.ecommerce.rag.client.ui.cart.CartViewModel
import com.ecommerce.rag.client.ui.detail.ProductDetailScreen
import com.ecommerce.rag.client.ui.detail.ProductDetailViewModel
import com.ecommerce.rag.client.ui.profile.OrderFilter
import com.ecommerce.rag.client.ui.profile.ProfilePlaceholderScreen
import com.ecommerce.rag.client.ui.profile.ProfileScreen
import com.ecommerce.rag.client.ui.theme.EcommerceRagClientTheme

/**
 * 阶段 9：
 *  - 启动后自动尝试 demo 账号登录；
 *  - 顶层页面状态扩展为 Browse / Detail / Cart / Login，不引入 Navigation-Compose；
 *  - 监听 AssistantViewModel.chatDoneCounter，让 Agent 的"加入购物车"成功后角标会刷新；
 *  - 监听 AuthViewModel.lastLoginAt，登录成功后刷新购物车；
 *  - LoginScreen 上不叠加 AssistantOverlay，避免干扰登录手势；
 *  - 立即购买 = 加购 + 跳购物车；未登录时由 CartViewModel.needsLogin 引导到 LoginScreen。
 *
 * 阶段 10：
 *  - 顶层页面再叠加 Profile / ProfilePlaceholder；
 *  - 底部"我的" → Profile；Profile 内"我的订单 / 收货地址 / 优惠券 / 收藏 / 浏览历史 /
 *    设置 / 客服 / 关于" → ProfilePlaceholder(title, subtitle)；
 *  - 退出登录调用 [AuthViewModel.logout] 并清空购物车本地状态，停留在 Profile；
 *  - 登录页若由 Profile 触发未登录入口，登录成功后默认回 Profile；
 *  - PageContext 同步：Profile → PROFILE，ProfilePlaceholder → PROFILE，Cart → CART；
 *  - AssistantOverlay：Profile 可显示，ProfilePlaceholder / Login 不显示。
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            EcommerceRagClientTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var screen by remember { mutableStateOf<AppScreen>(AppScreen.Browse) }
                    var currentPageContext by remember { mutableStateOf(PageContext.Empty) }
                    // 登录成功后的"返回意图"。例如：detail 加购→未登录→跳登录；登录后回 detail。
                    var pendingAfterLogin by remember { mutableStateOf<PendingAction?>(null) }
                    // 登录成功后默认回到的页面。例如 Profile 点"立即登录"，登完应回到 Profile。
                    var returnAfterLogin by remember { mutableStateOf<AppScreen?>(null) }

                    val assistantViewModel: AssistantViewModel = viewModel()
                    val assistantState by assistantViewModel.uiState.collectAsStateWithLifecycle()

                    val cartViewModel: CartViewModel = viewModel()
                    val cartState by cartViewModel.uiState.collectAsStateWithLifecycle()

                    val authViewModel: AuthViewModel = viewModel()
                    val authState by authViewModel.uiState.collectAsStateWithLifecycle()

                    // ---- 启动：尝试自动登录 ----
                    LaunchedEffect(Unit) {
                        authViewModel.autoLoginDemoIfNeeded()
                    }

                    // ---- 登录态变 true 时刷一次购物车（覆盖"启动时已登录"和"刚刚登录成功"两条路径）----
                    LaunchedEffect(authState.isLoggedIn) {
                        if (authState.isLoggedIn) {
                            cartViewModel.refreshCart()
                        }
                    }

                    // ---- 手动登录成功后兑现 pending 动作 + 返回意图 ----
                    LaunchedEffect(authState.lastLoginAt) {
                        if (authState.lastLoginAt > 0L && authState.isLoggedIn) {
                            val pending = pendingAfterLogin
                            if (pending != null) {
                                pendingAfterLogin = null
                                when (pending) {
                                    is PendingAction.AddToCart -> cartViewModel.addToCart(pending.productId)
                                    is PendingAction.OpenCart -> screen = AppScreen.Cart
                                }
                            } else {
                                val ret = returnAfterLogin
                                if (ret != null) {
                                    returnAfterLogin = null
                                    screen = ret
                                }
                            }
                        }
                    }

                    // ---- Agent SSE done → 刷新购物车角标 ----
                    LaunchedEffect(assistantState.chatDoneCounter) {
                        if (assistantState.chatDoneCounter > 0L && authState.isLoggedIn) {
                            cartViewModel.refreshCart()
                        }
                    }

                    // ---- 购物车流程发现未登录 → 跳 LoginScreen ----
                    LaunchedEffect(cartState.needsLogin) {
                        if (cartState.needsLogin) {
                            cartViewModel.clearNeedsLogin()
                            // 如果当前在 Profile，登录回 Profile；否则保持原默认（回 Browse 由 onLoggedIn 决定）
                            if (screen is AppScreen.Profile) returnAfterLogin = AppScreen.Profile
                            screen = AppScreen.Login
                        }
                    }

                    // ---- PageContext 同步：跟随顶层 screen 切换 page_type ----
                    LaunchedEffect(screen) {
                        currentPageContext = when (val s = screen) {
                            is AppScreen.Detail -> PageContext(
                                pageType = PageType.PRODUCT_DETAIL,
                                currentProductId = s.productId,
                                recentlyViewedProductIds = (
                                    listOf(s.productId) + currentPageContext.recentlyViewedProductIds
                                ).distinct().take(RECENTLY_VIEWED_LIMIT)
                            )
                            AppScreen.Cart -> currentPageContext.copy(pageType = PageType.CART)
                            AppScreen.Login -> currentPageContext.copy(pageType = PageType.LOGIN)
                            AppScreen.Profile,
                            is AppScreen.ProfilePlaceholder -> currentPageContext.copy(pageType = PageType.PROFILE)
                            AppScreen.Browse -> currentPageContext.copy(pageType = PageType.PRODUCT_LIST)
                        }
                    }

                    LaunchedEffect(currentPageContext) {
                        assistantViewModel.updatePageContext(currentPageContext)
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        when (val s = screen) {
                            AppScreen.Login -> {
                                LoginScreen(
                                    viewModel = authViewModel,
                                    onLoggedIn = {
                                        // 登录成功后由 lastLoginAt 监听去消化 pending / return；
                                        // 这里只兜底从登录页跳走。
                                        if (pendingAfterLogin == null) {
                                            val ret = returnAfterLogin
                                            returnAfterLogin = null
                                            screen = ret ?: AppScreen.Browse
                                        }
                                    },
                                    onBack = {
                                        // 用户主动放弃登录 → 回到原先页面，不强行登录
                                        pendingAfterLogin = null
                                        val ret = returnAfterLogin
                                        returnAfterLogin = null
                                        screen = ret ?: AppScreen.Browse
                                    }
                                )
                            }
                            AppScreen.Cart -> {
                                CartScreen(
                                    viewModel = cartViewModel,
                                    onBack = { screen = AppScreen.Browse },
                                    onProductClick = { productId ->
                                        screen = AppScreen.Detail(productId)
                                    },
                                    onGoBrowse = { screen = AppScreen.Browse },
                                    onRequireLogin = {
                                        pendingAfterLogin = PendingAction.OpenCart
                                        screen = AppScreen.Login
                                    }
                                )
                            }
                            is AppScreen.Detail -> {
                                val detailViewModel: ProductDetailViewModel = viewModel()
                                val pid = s.productId
                                ProductDetailScreen(
                                    viewModel = detailViewModel,
                                    productId = pid,
                                    onBack = { screen = AppScreen.Browse },
                                    onAddToCart = { productId ->
                                        if (!authState.isLoggedIn) {
                                            pendingAfterLogin = PendingAction.AddToCart(productId)
                                            screen = AppScreen.Login
                                        } else {
                                            cartViewModel.addToCart(productId)
                                        }
                                    },
                                    onCartClick = {
                                        if (!authState.isLoggedIn) {
                                            pendingAfterLogin = PendingAction.OpenCart
                                            screen = AppScreen.Login
                                        } else {
                                            screen = AppScreen.Cart
                                        }
                                    },
                                    onBuyNow = { productId ->
                                        if (!authState.isLoggedIn) {
                                            pendingAfterLogin =
                                                PendingAction.AddToCart(productId)
                                            screen = AppScreen.Login
                                        } else {
                                            cartViewModel.addToCart(productId)
                                            screen = AppScreen.Cart
                                        }
                                    }
                                )
                            }
                            AppScreen.Profile -> {
                                ProfileScreen(
                                    username = authState.username.takeIf { it.isNotBlank() },
                                    userId = authState.userId,
                                    isLoggedIn = authState.isLoggedIn,
                                    cartCount = cartState.cartView?.totalQuantity ?: 0,
                                    onLoginClick = {
                                        returnAfterLogin = AppScreen.Profile
                                        screen = AppScreen.Login
                                    },
                                    onLogoutClick = {
                                        authViewModel.logout()
                                        cartViewModel.clearLocalState()
                                        // 留在 Profile，显示未登录态
                                    },
                                    onCartClick = {
                                        if (!authState.isLoggedIn) {
                                            returnAfterLogin = AppScreen.Profile
                                            pendingAfterLogin = PendingAction.OpenCart
                                            screen = AppScreen.Login
                                        } else {
                                            screen = AppScreen.Cart
                                        }
                                    },
                                    onOrderClick = { filter ->
                                        screen = AppScreen.ProfilePlaceholder(
                                            title = filter.title,
                                            subtitle = "暂未接入订单接口，敬请期待"
                                        )
                                    },
                                    onAddressClick = {
                                        screen = AppScreen.ProfilePlaceholder(
                                            title = "收货地址",
                                            subtitle = "暂未接入地址接口，敬请期待"
                                        )
                                    },
                                    onCouponClick = {
                                        screen = AppScreen.ProfilePlaceholder(
                                            title = "优惠券",
                                            subtitle = "暂无可用优惠券"
                                        )
                                    },
                                    onFavoriteClick = {
                                        screen = AppScreen.ProfilePlaceholder(
                                            title = "我的收藏",
                                            subtitle = "暂未接入收藏接口"
                                        )
                                    },
                                    onHistoryClick = {
                                        screen = AppScreen.ProfilePlaceholder(
                                            title = "浏览历史",
                                            subtitle = "暂未接入浏览历史接口"
                                        )
                                    },
                                    onSettingsClick = {
                                        screen = AppScreen.ProfilePlaceholder(
                                            title = "设置",
                                            subtitle = "通用设置项尚未启用"
                                        )
                                    },
                                    onCustomerServiceClick = {
                                        screen = AppScreen.ProfilePlaceholder(
                                            title = "客服中心",
                                            subtitle = "演示版本暂无在线客服，可在助手对话框中咨询"
                                        )
                                    },
                                    onAboutClick = {
                                        screen = AppScreen.ProfilePlaceholder(
                                            title = "关于项目",
                                            subtitle = "Ecommerce Bot 演示项目 · Compose + RAG Agent"
                                        )
                                    },
                                    onBackToBrowse = { screen = AppScreen.Browse }
                                )
                            }
                            is AppScreen.ProfilePlaceholder -> {
                                ProfilePlaceholderScreen(
                                    title = s.title,
                                    subtitle = s.subtitle,
                                    onBack = { screen = AppScreen.Profile }
                                )
                            }
                            AppScreen.Browse -> {
                                ProductBrowseScreen(
                                    onProductClick = { productId ->
                                        screen = AppScreen.Detail(productId)
                                    },
                                    onPageContextChange = { ctx ->
                                        currentPageContext = ctx.copy(
                                            recentlyViewedProductIds =
                                                currentPageContext.recentlyViewedProductIds
                                        )
                                    },
                                    cartItemCount = cartState.cartView?.totalQuantity ?: 0,
                                    onCartClick = {
                                        if (!authState.isLoggedIn) {
                                            pendingAfterLogin = PendingAction.OpenCart
                                            screen = AppScreen.Login
                                        } else {
                                            screen = AppScreen.Cart
                                        }
                                    },
                                    onProfileClick = { screen = AppScreen.Profile }
                                )
                            }
                        }

                        // AssistantOverlay：登录页与占位页不叠加。
                        //  - Login：避免干扰登录手势；
                        //  - ProfilePlaceholder：占位页内容简单，避免机器人遮挡返回按钮和正文。
                        val overlayVisible = screen !is AppScreen.Login &&
                            screen !is AppScreen.ProfilePlaceholder
                        if (overlayVisible) {
                            AssistantOverlay(
                                uiState = assistantState,
                                onShortPressHint = assistantViewModel::showShortPressHint,
                                onLongPressStart = assistantViewModel::showActionMenu,
                                onHighlightAction = assistantViewModel::setHighlightedAction,
                                onPerformAction = assistantViewModel::performAssistantAction,
                                onSendMessage = assistantViewModel::sendMessage,
                                onCollapse = assistantViewModel::collapseAssistant,
                                onClearCameraMessage = assistantViewModel::clearCameraPlaceholderMessage,
                                onProductClick = { productId ->
                                    // 机器人卡片点击 → 切到 Detail
                                    screen = AppScreen.Detail(productId)
                                },
                                onAddToCartClick = { productId ->
                                    // 阶段 11：Agent 卡片"加入购物车"：未登录走 pending；
                                    // 已登录直接 addToCart，CartViewModel 内部刷新 cartView，
                                    // 顶部的购物车角标会自动跟随 cartState.cartView.totalQuantity 更新。
                                    if (!authState.isLoggedIn) {
                                        pendingAfterLogin = PendingAction.AddToCart(productId)
                                        screen = AppScreen.Login
                                    } else {
                                        cartViewModel.addToCart(productId)
                                    }
                                },
                                onBuyNowClick = { productId ->
                                    // 阶段 11：Agent 卡片"立刻下单"：先加购再跳购物车页；
                                    // 未登录则按 detail 页同样的 pending 模式，登录回来后会自动 addToCart，
                                    // 但目前 pendingAfterLogin 不会再跳 Cart，避免和"用户主动放弃登录"产生冲突。
                                    if (!authState.isLoggedIn) {
                                        pendingAfterLogin = PendingAction.AddToCart(productId)
                                        screen = AppScreen.Login
                                    } else {
                                        cartViewModel.addToCart(productId)
                                        screen = AppScreen.Cart
                                    }
                                },
                                // 阶段 14：TTS 朗读控制全部转交给 AssistantViewModel；
                                // AssistantOverlay 内部用 LaunchedEffect 监听 ttsRequestId
                                // 去拿音频 + 播放，状态回写通过这几个回调。
                                onToggleVoicePlayback = assistantViewModel::toggleVoicePlayback,
                                onConsumePendingTtsText = assistantViewModel::consumeTtsText,
                                onTtsStarted = assistantViewModel::notifyTtsStarted,
                                onTtsStopped = assistantViewModel::notifyTtsStopped,
                                onTtsError = assistantViewModel::notifyTtsError
                            )
                        }

                        // ---- 阶段 11：Agent 加购后的轻提示 ----
                        // CartScreen 自己已经会消费 cartState.message；这里只在非 Cart 页兜底，
                        // 显示一条短暂 Snackbar 后调用 clearMessage()。
                        val cartMessage = cartState.message
                        if (cartMessage != null && screen !is AppScreen.Cart) {
                            LaunchedEffect(cartMessage) {
                                delay(1600)
                                cartViewModel.clearMessage()
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(bottom = 120.dp),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color(0xCC222222),
                                    contentColor = Color.White
                                ) {
                                    Text(
                                        text = cartMessage,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 阶段 10：顶层页面状态。
     *
     * 不引入 Navigation-Compose；用 sealed class + `screen by remember { mutableStateOf(...) }`
     * 来表达 5 种顶层页 + Profile 子占位。
     */
    private sealed class AppScreen {
        // 用普通 `object` 而非 `data object`，与阶段 9 既有 PendingAction.OpenCart 写法保持一致，
        // 避免在边缘 Kotlin 配置上踩 data object 兼容坑。
        object Browse : AppScreen()
        data class Detail(val productId: String) : AppScreen()
        object Cart : AppScreen()
        object Login : AppScreen()
        object Profile : AppScreen()
        data class ProfilePlaceholder(
            val title: String,
            val subtitle: String
        ) : AppScreen()
    }

    /**
     * 登录成功后要兑现的动作。例如：detail 页加购被未登录拦截，
     * 登录成功后回来自动完成加购。
     */
    private sealed class PendingAction {
        data class AddToCart(val productId: String) : PendingAction()
        object OpenCart : PendingAction()
    }

    private companion object {
        const val RECENTLY_VIEWED_LIMIT = 10
    }
}

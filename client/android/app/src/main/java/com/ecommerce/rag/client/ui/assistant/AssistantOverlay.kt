package com.ecommerce.rag.client.ui.assistant

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.ecommerce.rag.client.data.local.AuthTokenStore
import com.ecommerce.rag.client.data.tts.TtsApiClient
import com.ecommerce.rag.client.data.tts.TtsPlayer
import com.ecommerce.rag.client.data.tts.TtsRequest
import kotlinx.coroutines.delay

/**
 * 浮层根容器：
 *  - 自身透明、不拦截背景点击；
 *  - 用 BoxWithConstraints 拿到屏幕尺寸，本地维护 FloatingBot 的像素 offset；
 *  - 拖拽即时更新 offset 并钳制到安全边界；松手贴到左/右屏幕边缘；
 *  - 长按时打开 AssistantActionMenu（在机器人上方），手指移动时根据相对位置高亮 Chat / Camera；
 *  - MiniChatPanel 始终从屏幕底部弹出，独立于 FloatingBot 位置；
 *  - cameraPlaceholderMessage 在底部以 Snackbar 样式短暂展示后自动消失；
 *  - 阶段 7：综合 isDragging / hover / menu / SSE 状态推导 [BotAnimationState]，
 *    驱动 [FloatingBot] 的 GIF 切换。
 *
 * 阶段 4 范围内有意保留的简化：
 *  - 命中检测使用相对手指 y 偏移阈值，不做精确 LayoutCoordinates 命中；
 *  - 没有处理屏幕旋转 / 分屏尺寸变化（变化后 offset 仍会被边界 coerceIn 钳制）；
 *  - 没有保存 bot 位置到持久层。
 *
 * 阶段 7 选择不实现 Sleep 计时：空闲计时容易引起 GIF 闪烁与意外重组，
 * 在 GIF 系统先跑稳之前不引入，保留枚举即可，[derivedBotState] 暂不返回 Sleep。
 */
@Composable
fun AssistantOverlay(
    uiState: AssistantUiState,
    onShortPressHint: () -> Unit,
    onLongPressStart: () -> Unit,
    onHighlightAction: (AssistantAction?) -> Unit,
    onPerformAction: (AssistantAction?) -> Unit,
    onSendMessage: (String) -> Unit,
    onCollapse: () -> Unit,
    onClearCameraMessage: () -> Unit,
    onProductClick: (String) -> Unit = {},
    onAddToCartClick: (String) -> Unit = {},
    onBuyNowClick: (String) -> Unit = {},
    onToggleVoicePlayback: () -> Unit = {},
    onConsumePendingTtsText: () -> Unit = {},
    onTtsStarted: () -> Unit = {},
    onTtsStopped: () -> Unit = {},
    onTtsError: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val context = LocalContext.current

    // ---- 阶段 14：TTS 客户端 + 播放器 ----
    // - TtsApiClient 无状态，直接 remember 单实例。
    // - TtsPlayer 内部持 MediaPlayer，离开时必须 release，避免泄漏。
    // - AuthTokenStore 仅在调用时读 token，不持久订阅，参照 AssistantViewModel 同款做法。
    val tokenStore = remember(context.applicationContext) {
        AuthTokenStore(context.applicationContext)
    }
    val ttsApiClient = remember { TtsApiClient() }
    val ttsPlayer = remember(context.applicationContext) {
        TtsPlayer(context.applicationContext)
    }
    DisposableEffect(ttsPlayer) {
        onDispose { ttsPlayer.release() }
    }

    // 用户翻关静音 → 立刻打断当前播放（ViewModel 也会同步 isSpeaking=false）。
    LaunchedEffect(uiState.isVoicePlaybackEnabled) {
        if (!uiState.isVoicePlaybackEnabled) {
            ttsPlayer.stop()
            onTtsStopped()
        }
    }

    // SSE done 后 ViewModel 递增 ttsRequestId 投递文本 → 这里实际去拿音频。
    LaunchedEffect(uiState.ttsRequestId) {
        val rid = uiState.ttsRequestId
        if (rid <= 0L) return@LaunchedEffect
        val text = uiState.pendingTtsText ?: return@LaunchedEffect
        if (!uiState.isVoicePlaybackEnabled) {
            onConsumePendingTtsText()
            return@LaunchedEffect
        }
        // 提前消费 pending，避免重组导致重复请求。
        onConsumePendingTtsText()
        val token = tokenStore.getAccessToken()
        val result = ttsApiClient.speak(TtsRequest(text = text), accessToken = token)
        result
            .onSuccess { resp ->
                val callback = object : TtsPlayer.Callback {
                    override fun onStarted() { onTtsStarted() }
                    override fun onCompleted() { onTtsStopped() }
                    override fun onError(message: String) { onTtsError(message) }
                }
                when {
                    !resp.audioUrl.isNullOrBlank() ->
                        ttsPlayer.playFromUrl(resp.audioUrl, callback)
                    !resp.audioBase64.isNullOrBlank() ->
                        ttsPlayer.playFromBase64(resp.audioBase64, callback)
                    else -> onTtsError("TTS 响应缺少音频数据")
                }
            }
            .onFailure { err ->
                onTtsError(err.localizedMessage ?: "语音合成失败")
            }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val containerWidthPx = with(density) { maxWidth.toPx() }
        val containerHeightPx = with(density) { maxHeight.toPx() }
        // 阶段 7：bot 由 56dp 圆形按钮升级为 96dp GIF 角色；
        // 这里的 botSizePx 同时用于：默认位置、拖拽钳制、长按 hover 阈值参考。
        val botSize = 96.dp
        val botSizePx = with(density) { botSize.toPx() }

        val safeMarginPx = with(density) { 16.dp.toPx() }
        val topSafePx = with(density) { 24.dp.toPx() }
        // 阶段 6：浏览页底部加了 CommerceBottomBar（≈64dp）+ 系统底部 inset，
        // 把 bot 默认 y 抬高，松手后纵向钳制时也避开导航栏。
        val bottomSafePx = with(density) { 160.dp.toPx() }
        val initialOffsetX = containerWidthPx - botSizePx - safeMarginPx
        val initialOffsetY = containerHeightPx - botSizePx - bottomSafePx

        // 机器人当前 offset（左上角，像素）。仅在本 Composable 内维护，
        // 不进 ViewModel，避免每次拖拽都 update StateFlow。
        var botOffset by remember {
            mutableStateOf(
                Offset(
                    initialOffsetX.coerceAtLeast(0f),
                    initialOffsetY.coerceAtLeast(topSafePx)
                )
            )
        }

        // 阶段 7：是否正在拖拽。从首次 onDragDelta 进入 true，onDragEnd 退回 false。
        // 仅用于 GIF 状态推导，不进 ViewModel。
        var isDragging by remember { mutableStateOf(false) }

        val currentBotOffset by rememberUpdatedState(botOffset)
        val currentHighlight by rememberUpdatedState(uiState.highlightedAction)

        // 长按命中阈值（机器人本地坐标 y，单位 px）：
        //  - relY = localPos.y - botSize/2，relY 为负表示手指在机器人中心之上。
        //  - 阶段 7 botSize 由 56→96dp，菜单仍按"在 bot 上方 12dp 处"绘制 ≈ 120dp 高，
        //    所以把阈值从 -80/-10 调到 -120/-30，让 Chat 落在菜单上半部、
        //    Camera 落在菜单下半部 + bot 顶端，整体手感和原版一致。
        val chatThresholdPx = with(density) { (-120).dp.toPx() }
        val cameraThresholdPx = with(density) { (-30).dp.toPx() }

        // ---- MiniChatPanel：从屏幕底部弹出（不再依附 FloatingBot 位置） ----
        // 阶段 6：浏览页底部多了 CommerceBottomBar，bottom padding 88dp 让面板坐在
        //        底部导航栏上方，不盖住"首页 / 逛逛 / 直播 / 消息 / 我的"。
        // 阶段 12：edge-to-edge 下用 WindowInsets.ime 抬高 panel，不再叠 imePadding。
        // 阶段 13（键盘模式 / 兜底定位）：
        //  - 在 enableEdgeToEdge() + adjustResize 组合下，少数 ROM 上
        //    `WindowInsets.ime.getBottom()` 不会随键盘弹起触发 Compose 重组，
        //    panel 还停在 88dp 的位置，看上去就是"靠在屏幕中上部、底下大块空隙"。
        //  - 把判定改成 `imeVisible || inputFocused` 双路：
        //      * imeVisible：标准路径，常规 Android 设备一来键盘就立刻生效；
        //      * inputFocused：兜底路径，输入框拿到焦点立即触发，不依赖 inset 重组。
        //  - 实际抬高仍优先用 `windowInsetsPadding(WindowInsets.ime.only(Bottom))`
        //    跟随键盘真实高度；如果 inset 为 0，至少 panel 已经移到 8dp 底位置，
        //    用户视觉上不会再看到"中上部漂浮"的奇怪状态。
        var inputFocused by remember { mutableStateOf(false) }
        // panel 收起时强制清掉焦点态，避免下次再开仍残留"键盘模式"。
        LaunchedEffect(uiState.mode) {
            if (uiState.mode == AssistantMode.Collapsed) {
                inputFocused = false
            }
        }

        val imeBottomPx = WindowInsets.ime.getBottom(density)
        val imeVisible = imeBottomPx > 0
        val keyboardMode = imeVisible || inputFocused
        Log.d(
            "MiniChatKeyboard",
            "imeVisible=$imeVisible inputFocused=$inputFocused keyboardMode=$keyboardMode imeBottomPx=$imeBottomPx"
        )

        // 键盘模式下，panel 顶部到屏幕底的距离 = ime_inset + 8dp（紧贴键盘）；
        // 非键盘模式下，panel 顶部到屏幕底 = 88dp（让出底部导航栏）。
        val panelExtraBottomPadding = if (keyboardMode) 8.dp else 88.dp

        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = uiState.mode != AssistantMode.Collapsed,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 4 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 4 }),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    // 阶段 13：WindowInsets.ime 由 panel 自身吃掉，AssistantOverlay 整体
                    // 不再加 imePadding。只把 IME 的底部 inset 抬到 panel 上，避免
                    // 上层布局也被推上去。
                    .windowInsetsPadding(WindowInsets.ime.only(WindowInsetsSides.Bottom))
                    .padding(
                        start = 12.dp,
                        end = 12.dp,
                        bottom = panelExtraBottomPadding
                    )
            ) {
                MiniChatPanel(
                    messages = uiState.messages,
                    isLoading = uiState.isLoading,
                    error = uiState.error,
                    onSendMessage = onSendMessage,
                    onClose = onCollapse,
                    onProductClick = { productId ->
                        // 点击机器人推荐卡 → 先收起面板，再让 MainActivity 跳详情，
                        // 避免详情页首屏被聊天面板压住。
                        onCollapse()
                        onProductClick(productId)
                    },
                    onAddToCartClick = { productId ->
                        // 阶段 11：加购不收起面板，让用户继续和机器人对话。
                        // 角标刷新交给 MainActivity（CartViewModel.addToCart 会更新 cartView）。
                        onAddToCartClick(productId)
                    },
                    onBuyNowClick = { productId ->
                        // 阶段 11：立刻下单 = 加购 + 跳购物车，提前收起面板，
                        // 避免购物车页被聊天小窗盖住。
                        onCollapse()
                        onBuyNowClick(productId)
                    },
                    keyboardMode = keyboardMode,
                    onInputFocusChanged = { focused -> inputFocused = focused },
                    isVoicePlaybackEnabled = uiState.isVoicePlaybackEnabled,
                    isSpeaking = uiState.isSpeaking,
                    onToggleVoicePlayback = onToggleVoicePlayback,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // ---- 动作菜单：跟随 FloatingBot，居中悬在它上方 ----
        // 估算菜单尺寸用于水平居中和向上偏移（实际渲染由 AssistantActionMenu 决定）。
        val menuEstimatedWidthPx = with(density) { 160.dp.toPx() }
        val menuEstimatedHeightPx = with(density) { 120.dp.toPx() }
        val menuGapPx = with(density) { 12.dp.toPx() }

        val rawMenuX = botOffset.x + botSizePx / 2f - menuEstimatedWidthPx / 2f
        val menuX = rawMenuX.coerceIn(
            safeMarginPx,
            (containerWidthPx - menuEstimatedWidthPx - safeMarginPx).coerceAtLeast(safeMarginPx)
        )
        val rawMenuY = botOffset.y - menuGapPx - menuEstimatedHeightPx
        val menuY = rawMenuY.coerceAtLeast(topSafePx)

        AnimatedVisibility(
            visible = uiState.isActionMenuVisible,
            enter = fadeIn() + scaleIn(initialScale = 0.85f),
            exit = fadeOut() + scaleOut(targetScale = 0.85f),
            modifier = Modifier.offset {
                IntOffset(menuX.toInt(), menuY.toInt())
            }
        ) {
            AssistantActionMenu(
                highlightedAction = uiState.highlightedAction
            )
        }

        // ---- 推导 GIF 状态 ----
        // 优先级（高 → 低）：
        //   1. isDragging                        → Dragging（强交互，永远盖过其它）
        //   2. highlightedAction == Chat / Cam   → HoverChat / HoverCamera
        //   3. isActionMenuVisible               → Menu（长按已经出菜单但还没选）
        //   4. uiState.isStreaming               → Talking（已经在吐 token）
        //   5. uiState.isLoading                 → Thinking（请求已发但还没收到 token）
        //   6. uiState.temporaryBotMood          → Happy / Confused（Done / Error 一次性表情）
        //   7. uiState.error != null             → Confused（兜底）
        //   8. else                              → Idle
        // 注意：temporaryBotMood 比 idle 高，但比强交互低；这样拖拽时不会被旧的 Happy 覆盖。
        val tempMood = uiState.temporaryBotMood
        val derivedBotState: BotAnimationState = when {
            isDragging -> BotAnimationState.Dragging
            uiState.highlightedAction == AssistantAction.Chat -> BotAnimationState.HoverChat
            uiState.highlightedAction == AssistantAction.Camera -> BotAnimationState.HoverCamera
            uiState.isActionMenuVisible -> BotAnimationState.Menu
            uiState.isStreaming -> BotAnimationState.Talking
            uiState.isLoading -> BotAnimationState.Thinking
            tempMood != null -> tempMood
            uiState.error != null -> BotAnimationState.Confused
            else -> BotAnimationState.Idle
        }

        // ---- FloatingBot：受拖拽 / 长按手势驱动 ----
        // 阶段 12：键盘弹起 + MiniChatPanel 已展开时，FloatingBot 会落在面板/输入框上方，
        // 极易遮挡输入和发送按钮。这种状态下临时不画机器人，键盘收回后自然恢复。
        // botOffset 在 remember 中，隐藏期间位置不会丢。
        // 阶段 13：判定改用 keyboardMode（imeVisible || inputFocused），
        // 在 IME inset 不触发重组的 ROM 上也能正确隐藏机器人。
        val miniPanelOpen = uiState.mode != AssistantMode.Collapsed
        val hideBotForIme = miniPanelOpen && keyboardMode
        if (!hideBotForIme) {
            FloatingBot(
                animationState = derivedBotState,
                onClickHint = onShortPressHint,
                onLongPressStart = onLongPressStart,
                onLongPressMove = { localPos ->
                    // localPos：手指在 FloatingBot 局部坐标（px），原点在机器人左上角
                    val relY = localPos.y - botSizePx / 2f
                    val action: AssistantAction? = when {
                        relY < chatThresholdPx -> AssistantAction.Chat
                        relY < cameraThresholdPx -> AssistantAction.Camera
                        else -> null
                    }
                    if (action != currentHighlight) {
                        onHighlightAction(action)
                    }
                },
                onLongPressEnd = {
                    onPerformAction(currentHighlight)
                },
                onDragDelta = { delta ->
                    if (!isDragging) isDragging = true
                    val newX = (currentBotOffset.x + delta.x).coerceIn(
                        0f,
                        (containerWidthPx - botSizePx).coerceAtLeast(0f)
                    )
                    // 下界用 bottomSafePx 钳住，避免拖到底部导航栏下方"看不见"。
                    val maxY = (containerHeightPx - botSizePx - bottomSafePx).coerceAtLeast(topSafePx)
                    val newY = (currentBotOffset.y + delta.y).coerceIn(topSafePx, maxY)
                    botOffset = Offset(newX, newY)
                },
                onDragEnd = {
                    // 贴边：以机器人中心 x 是否过半判断。
                    val centerX = currentBotOffset.x + botSizePx / 2f
                    val targetX = if (centerX < containerWidthPx / 2f) {
                        0f + safeMarginPx
                    } else {
                        containerWidthPx - botSizePx - safeMarginPx
                    }
                    botOffset = Offset(
                        targetX.coerceAtLeast(0f),
                        currentBotOffset.y
                    )
                    isDragging = false
                },
                size = botSize,
                modifier = Modifier.offset {
                    IntOffset(botOffset.x.toInt(), botOffset.y.toInt())
                }
            )
        }

        // ---- 底部短提示（Snackbar 样式），自动消失 ----
        val cameraMessage = uiState.cameraPlaceholderMessage
        if (cameraMessage != null) {
            LaunchedEffect(cameraMessage) {
                delay(2200)
                onClearCameraMessage()
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 32.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp
                ) {
                    Text(
                        text = cameraMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }
        }

    }
}

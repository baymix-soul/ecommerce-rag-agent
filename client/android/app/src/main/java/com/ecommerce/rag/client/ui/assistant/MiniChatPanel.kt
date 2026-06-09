package com.ecommerce.rag.client.ui.assistant

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.ecommerce.rag.client.data.model.ChatMessage
import com.ecommerce.rag.client.data.speech.AndroidSpeechRecognizerController
import com.ecommerce.rag.client.ui.chat.MessageBubble

/**
 * 悬浮购物助手的小型对话面板。
 *
 * 阶段 2 实现：
 *  - 标题栏 + 关闭按钮
 *  - 复用 MessageBubble 渲染对话流（含商品卡片）
 *  - 底部紧凑输入区，发送按钮在输入为空或正在加载时禁用
 *  - 高度受限（最高约 ~480dp），不撑满全屏
 *
 * 阶段 12（键盘体验修复）：
 *  - 不再用 `imePadding()`：MainActivity 是 edge-to-edge，IME 由 AssistantOverlay 用
 *    `WindowInsets.ime.getBottom()` 统一抬高 panel 底部，避免内部再加一层 padding
 *    把面板压窄；
 *  - heightIn 分两档：键盘隐藏时 420~560dp（更接近设计稿），键盘弹起时 360~460dp
 *    （保证输入区可见、不被键盘挡，也不会塌成一条细缝）；
 *  - 输入框改为单行：软键盘右下角 ImeAction = Send；硬件键盘 Enter 通过
 *    `onPreviewKeyEvent` 一并触发 [submitMessage]，三条路径共享同一发送逻辑；
 *  - 空白输入 / 正在加载时不发送；发送后清空输入框，键盘保留以便连续提问。
 *
 * 阶段 13（键盘模式 / 兜底定位）：
 *  - `imeVisible` 改名为 `keyboardMode`，由 AssistantOverlay 用
 *    `WindowInsets.ime.getBottom() > 0 || inputFocused` 双路推导；
 *  - 新增 `onInputFocusChanged`：把输入框焦点状态回传给 AssistantOverlay，
 *    当部分厂商 ROM 在 adjustResize 下 IME inset 不触发 Compose 重组时，
 *    AssistantOverlay 仍可凭"输入框已聚焦"立即切到键盘模式（贴键盘 8dp 定位）。
 */
@Composable
fun MiniChatPanel(
    messages: List<ChatMessage>,
    isLoading: Boolean,
    error: String?,
    onSendMessage: (String) -> Unit,
    onClose: () -> Unit,
    onProductClick: (String) -> Unit = {},
    onAddToCartClick: (String) -> Unit = {},
    onBuyNowClick: (String) -> Unit = {},
    keyboardMode: Boolean = false,
    onInputFocusChanged: (Boolean) -> Unit = {},
    isVoicePlaybackEnabled: Boolean = true,
    isSpeaking: Boolean = false,
    onToggleVoicePlayback: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var inputText by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // 发送按钮、软键盘 Send、硬件 Enter 共用同一个发送函数。
    // 用 rememberUpdatedState 包一层最新值，避免 onPreviewKeyEvent / KeyboardActions
    // 捕获到过期的 inputText。
    val latestInput by rememberUpdatedState(inputText)
    val latestIsLoading by rememberUpdatedState(isLoading)
    val submitMessage: () -> Unit = {
        val toSend = latestInput.trim()
        if (toSend.isNotEmpty() && !latestIsLoading) {
            onSendMessage(toSend)
            inputText = ""
        }
    }

    // ---- 阶段 14：语音输入（ASR）----
    val context = LocalContext.current
    // 用 applicationContext 创建 controller，避免泄漏 Activity；
    // Composable 退出时 destroy。
    val asrController = remember(context.applicationContext) {
        AndroidSpeechRecognizerController(context.applicationContext)
    }
    DisposableEffect(asrController) {
        onDispose { asrController.destroy() }
    }

    var voiceState by remember { mutableStateOf(VoiceInputUiState()) }
    // 是否已经有 RECORD_AUDIO 权限。首次 Composition 时按系统状态读一次，
    // 拿到权限回调后实时更新。
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        voiceState = if (granted) {
            VoiceInputUiState(status = VoiceInputStatus.Idle)
        } else {
            VoiceInputUiState(
                status = VoiceInputStatus.PermissionRequired,
                error = "需要麦克风权限才能语音输入"
            )
        }
    }

    val asrCallback = remember(submitMessage) {
        object : AndroidSpeechRecognizerController.Callback {
            override fun onReadyForSpeech() {
                voiceState = VoiceInputUiState(status = VoiceInputStatus.Listening)
            }

            override fun onPartialResult(text: String) {
                voiceState = voiceState.copy(
                    status = VoiceInputStatus.Listening,
                    partialText = text
                )
            }

            override fun onFinalResult(text: String) {
                voiceState = VoiceInputUiState(
                    status = VoiceInputStatus.Recognized,
                    finalText = text
                )
                // 直接把识别结果作为一条文本消息发出去，不再二次确认。
                val trimmed = text.trim()
                if (trimmed.isNotEmpty() && !latestIsLoading) {
                    onSendMessage(trimmed)
                }
                // 一次性消费后回到 Idle。
                voiceState = VoiceInputUiState(status = VoiceInputStatus.Idle)
            }

            override fun onError(code: Int, message: String) {
                voiceState = VoiceInputUiState(
                    status = VoiceInputStatus.Error,
                    error = message
                )
            }
        }
    }

    // 键盘弹起时给输入框留出更多空间、消息区压一压；隐藏时回到默认高度。
    val panelMinHeight = if (keyboardMode) 360.dp else 420.dp
    val panelMaxHeight = if (keyboardMode) 460.dp else 560.dp

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = panelMinHeight, max = panelMaxHeight)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            MiniChatHeader(
                onClose = onClose,
                isVoicePlaybackEnabled = isVoicePlaybackEnabled,
                isSpeaking = isSpeaking,
                onToggleVoicePlayback = onToggleVoicePlayback
            )

            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (messages.isEmpty()) {
                    EmptyHint()
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(items = messages, key = { it.id }) { message ->
                            MessageBubble(
                                message = message,
                                onProductClick = onProductClick,
                                onAddToCartClick = onAddToCartClick,
                                onBuyNowClick = onBuyNowClick
                            )
                        }
                    }
                }
            }

            if (error != null) {
                ErrorBanner(text = error)
            }

            // 阶段 14：语音临时提示（按住说话 / 正在听 / 识别中 / 错误 / partial 文本）。
            VoiceStatusStrip(state = voiceState)

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            MiniChatInputRow(
                value = inputText,
                onValueChange = { inputText = it },
                isLoading = isLoading,
                onSend = submitMessage,
                onFocusChanged = onInputFocusChanged,
                voiceStatus = voiceState.status,
                onVoicePressStart = {
                    when {
                        !hasMicPermission -> {
                            voiceState = VoiceInputUiState(
                                status = VoiceInputStatus.PermissionRequired,
                                error = "请允许麦克风权限后再试"
                            )
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                        !asrController.isAvailable() -> {
                            voiceState = VoiceInputUiState(
                                status = VoiceInputStatus.Error,
                                error = "当前设备不支持语音识别"
                            )
                        }
                        else -> {
                            voiceState = VoiceInputUiState(status = VoiceInputStatus.Listening)
                            asrController.startListening(asrCallback)
                        }
                    }
                },
                onVoicePressEnd = { released ->
                    if (released) {
                        // 正常松手 → 让 recognizer 收尾出 final 结果。
                        if (voiceState.status == VoiceInputStatus.Listening) {
                            voiceState = voiceState.copy(status = VoiceInputStatus.Recognizing)
                            asrController.stopListening()
                        }
                    } else {
                        // 被取消（手指上滑 / pointer 丢失） → 整轮丢弃。
                        asrController.cancel()
                        voiceState = VoiceInputUiState(status = VoiceInputStatus.Idle)
                    }
                }
            )
        }
    }
}

@Composable
private fun MiniChatHeader(
    onClose: () -> Unit,
    isVoicePlaybackEnabled: Boolean,
    isSpeaking: Boolean,
    onToggleVoicePlayback: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "🤖",
            style = MaterialTheme.typography.titleMedium
        )
        Box(modifier = Modifier.width(8.dp))
        Text(
            text = "购物助手",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )

        // 阶段 14：朗读 / 静音切换。
        // 播放中显示 🔇（停止），未播且开启时显示 🔊，未播且关闭时显示 🔈。
        // 点击统一走 onToggleVoicePlayback：ViewModel 会同时清掉 isSpeaking / pendingTtsText。
        val speakerLabel = when {
            isSpeaking -> "🔇"
            isVoicePlaybackEnabled -> "🔊"
            else -> "🔈"
        }
        IconButton(onClick = onToggleVoicePlayback) {
            Text(
                text = speakerLabel,
                style = MaterialTheme.typography.titleMedium
            )
        }

        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "关闭助手"
            )
        }
    }
}

@Composable
private fun EmptyHint() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "问我点什么吧～\n例如：「想找一款 3000 元以内的运动耳机」",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorBanner(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun MiniChatInputRow(
    value: String,
    onValueChange: (String) -> Unit,
    isLoading: Boolean,
    onSend: () -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    voiceStatus: VoiceInputStatus,
    onVoicePressStart: () -> Unit,
    onVoicePressEnd: (released: Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // 阶段 14：按住说话按钮。
        // 不抢 TextField 的焦点；交互完全靠 pointerInput 探测按下 / 松开。
        VoiceButton(
            voiceStatus = voiceStatus,
            isInputBusy = isLoading,
            onPressStart = onVoicePressStart,
            onPressEnd = onVoicePressEnd
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                // 阶段 13：把焦点状态回传给 AssistantOverlay，
                // 作为 WindowInsets.ime 在部分 ROM 下不触发重组时的兜底信号。
                .onFocusChanged { state -> onFocusChanged(state.isFocused) }
                // 阶段 12：硬件键盘 / 外接键盘按 Enter 时直接走发送，
                // 避免软键盘 Send 触发不到的设备体验断档。
                // KeyUp 而非 KeyDown：防止按住 Enter 时疯狂触发。
                .onPreviewKeyEvent { event ->
                    if (event.key == Key.Enter &&
                        event.type == KeyEventType.KeyUp &&
                        !isLoading
                    ) {
                        onSend()
                        true
                    } else {
                        false
                    }
                },
            placeholder = {
                Text(
                    text = if (isLoading) "等待回复中..." else "输入你的问题...",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            shape = RoundedCornerShape(20.dp),
            singleLine = true,
            enabled = !isLoading,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() })
        )

        val canSend = value.isNotBlank() && !isLoading
        IconButton(
            onClick = onSend,
            enabled = canSend,
            modifier = Modifier.size(44.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Send,
                contentDescription = "发送",
                tint = if (canSend) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
        }
    }
}

@Composable
private fun VoiceButton(
    voiceStatus: VoiceInputStatus,
    isInputBusy: Boolean,
    onPressStart: () -> Unit,
    onPressEnd: (released: Boolean) -> Unit
) {
    // 视觉：Idle/灰底；Listening/红底；Recognizing/橙底；其它/灰底。
    val (bg, fg, label) = when {
        isInputBusy -> Triple(
            Color(0xFFF1F1F1), Color(0xFFA1A1AA), "🎙"
        )
        voiceStatus == VoiceInputStatus.Listening -> Triple(
            Color(0xFFFFE5E5), Color(0xFFEF4444), "🎙"
        )
        voiceStatus == VoiceInputStatus.Recognizing -> Triple(
            Color(0xFFFFF0DA), Color(0xFFEA8A1F), "…"
        )
        else -> Triple(
            Color(0xFFF1F1F1), Color(0xFF18181B), "🎙"
        )
    }
    // 最新值闭包，避免 pointerInput 长时间挂起后 capture 到过期的 lambda。
    val latestPressStart by rememberUpdatedState(onPressStart)
    val latestPressEnd by rememberUpdatedState(onPressEnd)

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, fg.copy(alpha = 0.25f), CircleShape)
            .pointerInput(isInputBusy) {
                if (isInputBusy) return@pointerInput
                detectTapGestures(
                    onPress = {
                        // 按下即开始识别。短按（<500ms）走的是同一条路径，
                        // 最终会被 recognizer 报 NO_MATCH，UI 会显示"没听清"提示，
                        // 用户自然学到要按住说话。
                        latestPressStart()
                        val released = tryAwaitRelease()
                        latestPressEnd(released)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = fg,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * 阶段 14：语音状态条。位于输入栏上方，根据 [VoiceInputStatus] 显示提示。
 *
 * Idle 时不渲染，避免占用 panel 高度。
 */
@Composable
private fun VoiceStatusStrip(state: VoiceInputUiState) {
    if (state.status == VoiceInputStatus.Idle && state.partialText.isBlank()) return

    val (bg, fg, headline) = when (state.status) {
        VoiceInputStatus.Listening -> Triple(
            Color(0xFFFFF1F1), Color(0xFFEF4444), "正在听，请说话…"
        )
        VoiceInputStatus.Recognizing -> Triple(
            Color(0xFFFFF7E6), Color(0xFFEA8A1F), "识别中…"
        )
        VoiceInputStatus.Recognized -> Triple(
            Color(0xFFEFFCEE), Color(0xFF22A06B), "已识别，发送中…"
        )
        VoiceInputStatus.PermissionRequired -> Triple(
            Color(0xFFFFF1F1), Color(0xFFEF4444),
            state.error ?: "需要麦克风权限才能语音输入"
        )
        VoiceInputStatus.Error -> Triple(
            Color(0xFFFFF1F1), Color(0xFFEF4444),
            state.error ?: "识别失败，请重试"
        )
        VoiceInputStatus.Idle -> Triple(
            Color(0xFFF6F6F6), Color(0xFF71717A), "长按按住说话"
        )
    }

    Surface(
        color = bg,
        contentColor = fg,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = headline,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium
            )
            if (state.partialText.isNotBlank()) {
                Text(
                    text = state.partialText,
                    style = MaterialTheme.typography.bodySmall,
                    color = fg.copy(alpha = 0.85f),
                    maxLines = 2
                )
            }
        }
    }
}

package com.ecommerce.rag.client.ui.assistant

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 屏幕上的悬浮机器人。
 *
 * 阶段 7 升级：
 *  - 不再使用纯色圆形 Surface + Text("🤖")，
 *    改为透明 [Box] 包裹 [AnimatedBot]，让 GIF 角色的透明背景能保留下来；
 *  - 由 [animationState] 决定播放哪一张 GIF；
 *  - 默认尺寸从 56dp 提到 96dp（更接近"桌面宠物"），
 *    [AssistantOverlay] 同步把 botSizePx、贴边、菜单偏移都按 96dp 重新算。
 *
 * 手势分流（与阶段 4 一致）：
 *  1. 短按（onClickHint）       ：手指按下后未明显移动也未超时即抬起；
 *  2. 长按 + 滑动选动作         ：按下后保持 ~500ms 不抬起 → onLongPressStart；
 *                                此后每次手指移动调用 onLongPressMove(localPos)，
 *                                手指抬起时调用 onLongPressEnd()；
 *  3. 普通拖拽                  ：按下后短时间内手指移动超过 touchSlop →
 *                                onDragDelta(delta) 持续触发，抬起时 onDragEnd()。
 *
 * 注意：
 *  - localPos 是手指在本 Composable 局部坐标系中的位置（单位 px），
 *    由外部 Overlay 结合自身尺寸做命中判断；
 *  - 不使用 clickable，避免和 pointerInput 抢事件。
 */
@Composable
fun FloatingBot(
    animationState: BotAnimationState,
    onClickHint: () -> Unit,
    onLongPressStart: () -> Unit,
    onLongPressMove: (Offset) -> Unit,
    onLongPressEnd: () -> Unit,
    onDragDelta: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp
) {
    val currentOnClickHint by rememberUpdatedState(onClickHint)
    val currentOnLongPressStart by rememberUpdatedState(onLongPressStart)
    val currentOnLongPressMove by rememberUpdatedState(onLongPressMove)
    val currentOnLongPressEnd by rememberUpdatedState(onLongPressEnd)
    val currentOnDragDelta by rememberUpdatedState(onDragDelta)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)

    Box(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = "购物助手" }
            .pointerInput(Unit) {
                val longPressTimeoutMillis = viewConfiguration.longPressTimeoutMillis
                val slop = viewConfiguration.touchSlop

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)

                    // Phase 1：在 longPressTimeout 内，等待 up（短按）或 drag 超过 slop（拖拽）。
                    // 若超时仍未触发，则视为长按。
                    // 注意：在 AwaitPointerEventScope 中，withTimeout 超时会抛
                    // PointerEventTimeoutCancellationException（Compose 自己抛的，不是
                    // kotlinx 的 TimeoutCancellationException），所以必须显式 try/catch。
                    var phase1: Phase1Result = Phase1Result.LongPress
                    try {
                        withTimeout(longPressTimeoutMillis) {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id }
                                if (change == null || !change.pressed) {
                                    phase1 = Phase1Result.Tap
                                    return@withTimeout
                                }
                                val distance = (change.position - down.position).getDistance()
                                if (distance > slop) {
                                    phase1 = Phase1Result.Drag
                                    return@withTimeout
                                }
                            }
                        }
                    } catch (_: PointerEventTimeoutCancellationException) {
                        // 超时 → phase1 保持默认 LongPress
                    }

                    when (phase1) {
                        Phase1Result.Tap -> currentOnClickHint()
                        Phase1Result.Drag -> {
                            // 进入拖拽：持续把 delta 抛给外部，直到抬起。
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id }
                                if (change == null || !change.pressed) {
                                    currentOnDragEnd()
                                    break
                                }
                                val delta = change.position - change.previousPosition
                                if (delta != Offset.Zero) {
                                    currentOnDragDelta(delta)
                                }
                                change.consume()
                            }
                        }
                        Phase1Result.LongPress -> {
                            currentOnLongPressStart()
                            // 触发长按时先把当前手指位置抛一次，让 hover 立刻有响应。
                            currentOnLongPressMove(down.position)
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id }
                                if (change == null || !change.pressed) {
                                    currentOnLongPressEnd()
                                    break
                                }
                                currentOnLongPressMove(change.position)
                                change.consume()
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        AnimatedBot(
            animationState = animationState,
            size = size
        )
    }
}

private enum class Phase1Result { Tap, Drag, LongPress }

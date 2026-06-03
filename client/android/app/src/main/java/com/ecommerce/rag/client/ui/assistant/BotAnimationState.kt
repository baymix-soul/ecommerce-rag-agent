package com.ecommerce.rag.client.ui.assistant

/**
 * 悬浮机器人 GIF 状态机。
 *
 * 每个状态对应 assets/bot/ 下的一张 GIF。状态由 [AssistantOverlay] 在 Composable 内推导，
 * **不进 ViewModel**——拖拽 / hover 这类瞬时状态留在 UI 层，
 * 业务相关的 isLoading / isStreaming / error 才走 [AssistantUiState]。
 *
 * 阶段 7 注意：
 *  - Sleep 状态本轮**保留枚举不触发**，避免空闲计时引入闪烁；
 *  - 若 GIF 资源缺失，[AnimatedBot] 会回落到 emoji，不会抛错。
 */
enum class BotAnimationState {
    Idle,
    Dragging,
    Menu,
    HoverChat,
    HoverCamera,
    Thinking,
    Talking,
    Happy,
    Confused,
    Sleep
}

/**
 * 返回对应的 asset URI；Coil 配合 AssetUriFetcher 可以直接加载。
 *
 * 路径与 `android/app/src/main/assets/bot/&lt;name&gt;.gif` 一一对应。
 */
fun BotAnimationState.assetUri(): String = when (this) {
    BotAnimationState.Idle -> "file:///android_asset/bot/idle.gif"
    BotAnimationState.Dragging -> "file:///android_asset/bot/dragging.gif"
    BotAnimationState.Menu -> "file:///android_asset/bot/menu.gif"
    BotAnimationState.HoverChat -> "file:///android_asset/bot/hover_chat.gif"
    BotAnimationState.HoverCamera -> "file:///android_asset/bot/hover_camera.gif"
    BotAnimationState.Thinking -> "file:///android_asset/bot/thinking.gif"
    BotAnimationState.Talking -> "file:///android_asset/bot/talking.gif"
    BotAnimationState.Happy -> "file:///android_asset/bot/happy.gif"
    BotAnimationState.Confused -> "file:///android_asset/bot/confused.gif"
    BotAnimationState.Sleep -> "file:///android_asset/bot/sleep.gif"
}

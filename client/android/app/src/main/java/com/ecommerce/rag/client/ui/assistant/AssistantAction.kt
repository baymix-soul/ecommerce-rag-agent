package com.ecommerce.rag.client.ui.assistant

/**
 * 长按 FloatingBot 后显示的动作菜单中的可选项。
 *
 * 阶段 4 仅支持两个动作：
 *  - Chat   ：打开 MiniChatPanel
 *  - Camera ：占位，提示"拍照功能下一步接入"
 */
enum class AssistantAction {
    Chat,
    Camera
}

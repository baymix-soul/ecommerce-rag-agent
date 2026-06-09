package com.ecommerce.rag.client.ui.assistant

/**
 * 悬浮助手的 UI 形态。
 *
 * Collapsed  ：默认状态，屏幕边缘趴着的小机器人
 * MiniChat   ：点击/拖拽后弹出的小型对话框
 * Expanded   ：未来阶段：展开 ResultPreviewPanel（商品对比、筛选结果等）
 */
enum class AssistantMode {
    Collapsed,
    MiniChat,
    Expanded
}

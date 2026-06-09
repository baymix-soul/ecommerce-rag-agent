package com.ecommerce.rag.client.ui.auth

/**
 * 阶段 9：认证状态。
 *
 * - 启动时由 AuthViewModel.autoLoginDemoIfNeeded() 主动尝试一次 demo 自动登录；
 * - 失败不会让 App 崩溃，UI 可以提示走手动登录；
 * - 字段 `lastLoginAt` 用作"登录成功"广播的轻量信号：
 *   MainActivity 监听其变化触发购物车刷新，避免引入额外回调链。
 */
data class AuthUiState(
    val isLoggedIn: Boolean = false,
    val isLoading: Boolean = false,
    val username: String = "",
    val userId: String? = null,
    val error: String? = null,
    val autoLoginAttempted: Boolean = false,
    val lastLoginAt: Long = 0L
)

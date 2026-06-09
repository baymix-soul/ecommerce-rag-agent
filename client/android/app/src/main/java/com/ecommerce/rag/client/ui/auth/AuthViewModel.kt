package com.ecommerce.rag.client.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ecommerce.rag.client.data.local.AuthTokenStore
import com.ecommerce.rag.client.data.remote.LoginApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 阶段 9：登录态包装层。
 *
 * 本类不引入新的数据层（SharedPreferences 仍由 [AuthTokenStore] 提供），
 * 只是把"是否登录 / 自动登录 / 手动登录"等 UI 行为收敛到一个 ViewModel，
 * 方便 MainActivity 和 LoginScreen 统一消费。
 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val authTokenStore = AuthTokenStore(application.applicationContext)
    private val loginApiClient = LoginApiClient()

    private val _uiState = MutableStateFlow(loadInitialState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private fun loadInitialState(): AuthUiState {
        val token = authTokenStore.getAccessToken()
        val username = authTokenStore.getUsername().orEmpty()
        val userId = authTokenStore.getUserId()
        return AuthUiState(
            isLoggedIn = !token.isNullOrBlank(),
            username = username,
            userId = userId
        )
    }

    /**
     * 启动时调用。若本地无 token，则尝试 demo/demo123 自动登录。
     * 失败不会抛出，只是把 `error` 暴露给 UI，让用户走手动登录。
     */
    fun autoLoginDemoIfNeeded() {
        if (_uiState.value.autoLoginAttempted) return
        if (authTokenStore.isLoggedIn()) {
            _uiState.update {
                it.copy(autoLoginAttempted = true, isLoggedIn = true)
            }
            return
        }
        login(
            username = DEFAULT_DEMO_USERNAME,
            password = DEFAULT_DEMO_PASSWORD,
            isAutoLogin = true
        )
    }

    fun login(
        username: String,
        password: String,
        isAutoLogin: Boolean = false
    ) {
        val u = username.trim()
        val p = password
        if (u.isBlank() || p.isBlank()) {
            _uiState.update { it.copy(error = "请填写用户名和密码") }
            return
        }
        if (_uiState.value.isLoading) return

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                val result = loginApiClient.login(u, p)
                authTokenStore.saveTokens(
                    accessToken = result.accessToken,
                    userId = result.userId,
                    username = result.username
                )
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        username = result.username,
                        userId = result.userId,
                        error = null,
                        autoLoginAttempted = true,
                        lastLoginAt = System.currentTimeMillis()
                    )
                }
            } catch (e: Exception) {
                val msg = e.localizedMessage ?: "登录失败"
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isLoggedIn = authTokenStore.isLoggedIn(),
                        error = if (isAutoLogin) {
                            // 自动登录失败不要弹大红字，只是落到 error 字段，
                            // 让 UI 可以静默提示或忽略。
                            "自动登录失败：$msg"
                        } else {
                            msg
                        },
                        autoLoginAttempted = true
                    )
                }
            }
        }
    }

    fun logout() {
        authTokenStore.clearTokens()
        _uiState.update {
            AuthUiState(
                isLoggedIn = false,
                autoLoginAttempted = true
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun getAccessToken(): String? = authTokenStore.getAccessToken()

    companion object {
        const val DEFAULT_DEMO_USERNAME = "demo"
        const val DEFAULT_DEMO_PASSWORD = "demo123"
    }
}

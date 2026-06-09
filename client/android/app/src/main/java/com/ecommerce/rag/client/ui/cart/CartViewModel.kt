package com.ecommerce.rag.client.ui.cart

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ecommerce.rag.client.data.local.AuthTokenStore
import com.ecommerce.rag.client.data.remote.CartApiClient
import com.ecommerce.rag.client.data.remote.CartView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 阶段 9 扩展：
 *  - 新增 needsLogin：未登录或 401 时置 true，UI 跳 LoginScreen；
 *  - 新增 message：操作成功后的短提示（如"已加入购物车"），由 UI 通过 clearMessage() 消费；
 *  - 加购/改数/删/清空都先校验 token，避免裸调后端 401；
 *  - addToCart 不再阻塞 loadCart 的并发（保留 isLoading 互斥防抖）。
 *
 * MainActivity 既有用法 `cartState.cartView?.totalQuantity ?: 0` 仍然有效，
 * cartView 字段未改名。
 */
data class CartUiState(
    val cartView: CartView? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val needsLogin: Boolean = false
)

class CartViewModel(application: Application) : AndroidViewModel(application) {

    private val authTokenStore = AuthTokenStore(application.applicationContext)
    private val cartApiClient = CartApiClient(authTokenStore)

    private val _uiState = MutableStateFlow(CartUiState())
    val uiState: StateFlow<CartUiState> = _uiState.asStateFlow()

    fun loadCart() {
        if (_uiState.value.isLoading) return
        if (!ensureLoggedIn()) return

        _uiState.update { it.copy(isLoading = true, error = null, needsLogin = false) }

        viewModelScope.launch {
            try {
                val cartView = withContext(Dispatchers.IO) {
                    cartApiClient.getCart()
                }
                _uiState.update {
                    it.copy(isLoading = false, error = null, cartView = cartView)
                }
            } catch (e: Exception) {
                applyError(e, fallbackMsg = "加载购物车失败")
            }
        }
    }

    /** 阶段 9 别名：和需求文档对齐；行为同 [loadCart]。 */
    fun refreshCart() = loadCart()

    fun addToCart(productId: String, quantity: Int = 1) {
        if (productId.isBlank()) return
        if (_uiState.value.isLoading) return
        if (!ensureLoggedIn()) return

        _uiState.update { it.copy(isLoading = true, error = null, needsLogin = false) }

        viewModelScope.launch {
            try {
                val cartView = withContext(Dispatchers.IO) {
                    cartApiClient.addToCart(productId, quantity)
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = null,
                        cartView = cartView,
                        message = "已加入购物车"
                    )
                }
            } catch (e: Exception) {
                applyError(e, fallbackMsg = "加入购物车失败")
            }
        }
    }

    fun updateQuantity(productId: String, quantity: Int) {
        if (productId.isBlank()) return
        if (quantity < 1) return
        if (_uiState.value.isLoading) return
        if (!ensureLoggedIn()) return

        _uiState.update { it.copy(isLoading = true, error = null, needsLogin = false) }

        viewModelScope.launch {
            try {
                val cartView = withContext(Dispatchers.IO) {
                    cartApiClient.updateQuantity(productId, quantity)
                }
                _uiState.update {
                    it.copy(isLoading = false, error = null, cartView = cartView)
                }
            } catch (e: Exception) {
                applyError(e, fallbackMsg = "更新数量失败")
            }
        }
    }

    fun increase(productId: String) {
        val current = _uiState.value.cartView?.items?.firstOrNull { it.productId == productId }
        val next = (current?.quantity ?: 0) + 1
        updateQuantity(productId, next)
    }

    fun decrease(productId: String) {
        val current = _uiState.value.cartView?.items?.firstOrNull { it.productId == productId }
            ?: return
        if (current.quantity <= 1) {
            // 降到 0 时不直接删，由调用方按钮显式触发 removeItem。
            return
        }
        updateQuantity(productId, current.quantity - 1)
    }

    fun removeItem(productId: String) {
        if (productId.isBlank()) return
        if (_uiState.value.isLoading) return
        if (!ensureLoggedIn()) return

        _uiState.update { it.copy(isLoading = true, error = null, needsLogin = false) }

        viewModelScope.launch {
            try {
                val cartView = withContext(Dispatchers.IO) {
                    cartApiClient.removeItem(productId)
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = null,
                        cartView = cartView,
                        message = "已移除商品"
                    )
                }
            } catch (e: Exception) {
                applyError(e, fallbackMsg = "移除商品失败")
            }
        }
    }

    fun clearCart() {
        if (_uiState.value.isLoading) return
        if (!ensureLoggedIn()) return

        _uiState.update { it.copy(isLoading = true, error = null, needsLogin = false) }

        viewModelScope.launch {
            try {
                val cartView = withContext(Dispatchers.IO) {
                    cartApiClient.clearCart()
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = null,
                        cartView = cartView,
                        message = "购物车已清空"
                    )
                }
            } catch (e: Exception) {
                applyError(e, fallbackMsg = "清空购物车失败")
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun clearNeedsLogin() {
        _uiState.update { it.copy(needsLogin = false) }
    }

    /**
     * 阶段 10：退出登录时本地重置购物车状态（不调用后端）。
     *
     * 用 [clearCart] 会发 DELETE /api/cart，本地登出场景里那个 token 可能已被清理，
     * 必然失败；这里只把 UI 上的件数 / 总价 / 提示统统归零，避免角标残留。
     */
    fun clearLocalState() {
        _uiState.value = CartUiState()
    }

    // ---- internal ----

    /**
     * 未登录直接走"请先登录"分支，避免裸调后端拿 401。
     * 同时把 needsLogin 置 true，方便 UI 跳到 LoginScreen。
     */
    private fun ensureLoggedIn(): Boolean {
        val token = authTokenStore.getAccessToken()
        if (token.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    needsLogin = true,
                    error = "请先登录后再操作购物车"
                )
            }
            return false
        }
        return true
    }

    private fun applyError(e: Exception, fallbackMsg: String) {
        val raw = e.localizedMessage ?: fallbackMsg
        val authBroken = raw.contains("401") || raw.contains("403") ||
            raw.contains("未登录", ignoreCase = true) ||
            raw.contains("unauthorized", ignoreCase = true)
        _uiState.update {
            it.copy(
                isLoading = false,
                error = if (authBroken) "登录状态已失效，请重新登录" else raw,
                needsLogin = it.needsLogin || authBroken
            )
        }
    }
}

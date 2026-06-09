package com.ecommerce.rag.client.ui.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ecommerce.rag.client.config.AppConfig
import com.ecommerce.rag.client.data.local.LocalProductRepository
import com.ecommerce.rag.client.data.model.ProductDetailUiModel
import com.ecommerce.rag.client.data.remote.ProductApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProductDetailUiState(
    val isLoading: Boolean = false,
    val product: ProductDetailUiModel? = null,
    val error: String? = null
)

/**
 * 阶段 8：
 *  - 仍优先调用后端 ProductApiClient。
 *  - 如果后端失败 / 抛错 / 返回空，自动回退到 assets/products.json
 *    （由 LocalProductRepository 提供）。
 *  - 后端 + 本地都拿不到时才暴露 error 给 UI。
 */
class ProductDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val apiClient = ProductApiClient()
    private val localRepository = LocalProductRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(ProductDetailUiState())
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()

    fun loadProductDetail(productId: String) {
        if (_uiState.value.isLoading) return
        if (productId.isBlank()) return

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            val remote = runCatching { apiClient.fetchProductDetail(productId) }
                .getOrNull()
                ?.getOrNull()

            if (remote != null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = null,
                        product = remote.copy(imageUrl = resolveImageUrl(remote.imageUrl))
                    )
                }
                return@launch
            }

            // 后端没拿到 → 本地 fallback
            val local = runCatching { localRepository.getProductDetailById(productId) }
                .getOrNull()

            if (local != null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = null,
                        product = local
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        product = null,
                        error = "商品详情加载失败"
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun resolveImageUrl(imageUrl: String): String {
        if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
            return imageUrl
        }
        if (imageUrl.startsWith("file:///android_asset/")) {
            return imageUrl
        }
        if (imageUrl.startsWith("/")) {
            return AppConfig.BASE_URL + imageUrl
        }
        return AppConfig.BASE_URL + "/" + imageUrl
    }
}

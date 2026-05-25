package com.ecommerce.rag.client.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ecommerce.rag.client.config.AppConfig
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

class ProductDetailViewModel : ViewModel() {

    private val apiClient = ProductApiClient()

    private val _uiState = MutableStateFlow(ProductDetailUiState())
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()

    fun loadProductDetail(productId: String) {
        if (_uiState.value.isLoading) return
        if (productId.isBlank()) return

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            apiClient.fetchProductDetail(productId)
                .onSuccess { detail ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            product = detail.copy(
                                imageUrl = resolveImageUrl(detail.imageUrl)
                            )
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = throwable.localizedMessage ?: "加载失败"
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
        if (imageUrl.startsWith("/")) {
            return AppConfig.BASE_URL + imageUrl
        }
        return AppConfig.BASE_URL + "/" + imageUrl
    }
}

package com.ecommerce.rag.client.ui.browse

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ecommerce.rag.client.data.local.LocalProduct
import com.ecommerce.rag.client.data.local.LocalProductRepository
import com.ecommerce.rag.client.data.model.ProductCardUiModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 电商浏览页 ViewModel。
 *
 * 阶段 5 设计：
 *  - 用 AndroidViewModel 拿 Application Context 读 assets；MainActivity 调用 `viewModel()` 即可。
 *  - 全量 LocalProduct 私有，过滤后的 ProductCardUiModel 暴露给 UI；
 *  - 简易搜索：匹配 name / brand / subCategory（不区分大小写、不分词）；
 *  - 分类筛选：根据 selectedCategory 过滤一级类目；
 *  - 不接入后端、不依赖 ProductDetailViewModel；
 *  - 出错或解析失败时 products = 空，UI 显示空状态/错误信息。
 */
class ProductBrowseViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = LocalProductRepository(application.applicationContext)

    private val _uiState = MutableStateFlow(ProductBrowseUiState(isLoading = true))
    val uiState: StateFlow<ProductBrowseUiState> = _uiState.asStateFlow()

    private var allProducts: List<LocalProduct> = emptyList()

    init {
        refresh()
    }

    fun refresh() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val products = repository.getProducts()
                allProducts = products
                val categories = products
                    .mapNotNull { it.category?.takeIf { c -> c.isNotBlank() } }
                    .distinct()
                if (products.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "本地商品数据为空，请检查 assets/products.json",
                            products = emptyList(),
                            categories = emptyList()
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = null,
                            categories = categories
                        )
                    }
                    applyFilters()
                }
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = t.localizedMessage ?: "加载本地商品失败"
                    )
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilters()
    }

    fun onCategorySelected(category: String?) {
        _uiState.update { it.copy(selectedCategory = category) }
        applyFilters()
    }

    private fun applyFilters() {
        val current = _uiState.value
        val q = current.searchQuery.trim().lowercase()
        val cat = current.selectedCategory

        val filtered = allProducts.asSequence()
            .filter { p -> cat == null || p.category == cat }
            .filter { p ->
                if (q.isEmpty()) true
                else {
                    val name = p.name?.lowercase().orEmpty()
                    val brand = p.brand?.lowercase().orEmpty()
                    val sub = p.subCategory?.lowercase().orEmpty()
                    q in name || q in brand || q in sub
                }
            }
            .map { it.toCardUiModel() }
            .toList()

        _uiState.update { it.copy(products = filtered) }
    }

    private fun LocalProduct.toCardUiModel(): ProductCardUiModel = ProductCardUiModel(
        productId = productId,
        name = name.orEmpty(),
        price = price ?: 0.0,
        currency = currency ?: "CNY",
        imageUrl = LocalProductRepository.resolveAssetImageUri(imageUrl),
        reason = "",
        brand = brand,
        subCategory = subCategory,
        avgRating = avgRating
    )
}

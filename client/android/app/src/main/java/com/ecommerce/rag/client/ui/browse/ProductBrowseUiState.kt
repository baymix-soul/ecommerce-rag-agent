package com.ecommerce.rag.client.ui.browse

import com.ecommerce.rag.client.data.model.ProductCardUiModel

/**
 * 电商浏览页 UI 状态。
 *
 * 阶段 5 设计：
 *  - 复用 ProductCardUiModel 作为渲染模型（reason 字段为空串，
 *    ProductCardComposable 内部已经做了空判断不会渲染）。
 *  - 全量商品保留在 ViewModel 内部，products 字段是 query + category 过滤后的可见列表。
 *  - categories 字段独立暴露给搜索栏 chips。
 */
data class ProductBrowseUiState(
    val products: List<ProductCardUiModel> = emptyList(),
    val categories: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val selectedCategory: String? = null
)

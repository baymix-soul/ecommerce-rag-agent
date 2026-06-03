package com.ecommerce.rag.client

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ecommerce.rag.client.data.model.PageContext
import com.ecommerce.rag.client.data.model.PageType
import com.ecommerce.rag.client.ui.assistant.AssistantOverlay
import com.ecommerce.rag.client.ui.assistant.AssistantViewModel
import com.ecommerce.rag.client.ui.browse.ProductBrowseScreen
import com.ecommerce.rag.client.ui.detail.ProductDetailScreen
import com.ecommerce.rag.client.ui.detail.ProductDetailViewModel
import com.ecommerce.rag.client.ui.theme.EcommerceRagClientTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EcommerceRagClientTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var selectedProductId by remember { mutableStateOf<String?>(null) }
                    var currentPageContext by remember { mutableStateOf(PageContext.Empty) }

                    val assistantViewModel: AssistantViewModel = viewModel()
                    val assistantState by assistantViewModel.uiState.collectAsStateWithLifecycle()

                    // 把当前 PageContext 推给 AssistantViewModel，
                    // sendMessage 时会被拍快照一并发给 SSE。
                    LaunchedEffect(currentPageContext) {
                        assistantViewModel.updatePageContext(currentPageContext)
                    }

                    // 进入详情页时，主导更新 PageContext 为 PRODUCT_DETAIL；
                    // 返回浏览页 (selectedProductId == null) 时不重置，
                    // 让 ProductBrowseScreen 的 LaunchedEffect 重新发自己的 PRODUCT_LIST。
                    LaunchedEffect(selectedProductId) {
                        val id = selectedProductId
                        if (id != null) {
                            currentPageContext = PageContext(
                                pageType = PageType.PRODUCT_DETAIL,
                                currentProductId = id,
                                recentlyViewedProductIds = (
                                    listOf(id) + currentPageContext.recentlyViewedProductIds
                                ).distinct().take(RECENTLY_VIEWED_LIMIT)
                            )
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        if (selectedProductId != null) {
                            val detailViewModel: ProductDetailViewModel = viewModel()
                            ProductDetailScreen(
                                viewModel = detailViewModel,
                                productId = selectedProductId!!,
                                onBack = { selectedProductId = null }
                            )
                        } else {
                            ProductBrowseScreen(
                                onProductClick = { productId ->
                                    selectedProductId = productId
                                },
                                onPageContextChange = { ctx ->
                                    currentPageContext = ctx.copy(
                                        recentlyViewedProductIds =
                                            currentPageContext.recentlyViewedProductIds
                                    )
                                }
                            )
                        }

                        AssistantOverlay(
                            uiState = assistantState,
                            onShortPressHint = assistantViewModel::showShortPressHint,
                            onLongPressStart = assistantViewModel::showActionMenu,
                            onHighlightAction = assistantViewModel::setHighlightedAction,
                            onPerformAction = assistantViewModel::performAssistantAction,
                            onSendMessage = assistantViewModel::sendMessage,
                            onCollapse = assistantViewModel::collapseAssistant,
                            onClearCameraMessage = assistantViewModel::clearCameraPlaceholderMessage,
                            onProductClick = { productId ->
                                selectedProductId = productId
                            }
                        )
                    }
                }
            }
        }
    }

    private companion object {
        const val RECENTLY_VIEWED_LIMIT = 10
    }
}

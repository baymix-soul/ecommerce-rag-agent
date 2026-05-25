package com.ecommerce.rag.client

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ecommerce.rag.client.ui.chat.ChatScreen
import com.ecommerce.rag.client.ui.chat.ChatViewModel
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

                    if (selectedProductId != null) {
                        val detailViewModel: ProductDetailViewModel = viewModel()
                        ProductDetailScreen(
                            viewModel = detailViewModel,
                            productId = selectedProductId!!,
                            onBack = { selectedProductId = null }
                        )
                    } else {
                        val chatViewModel: ChatViewModel = viewModel()
                        ChatScreen(
                            viewModel = chatViewModel,
                            onProductClick = { productId ->
                                selectedProductId = productId
                            }
                        )
                    }
                }
            }
        }
    }
}

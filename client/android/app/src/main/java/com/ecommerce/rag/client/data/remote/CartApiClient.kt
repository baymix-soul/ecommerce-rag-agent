package com.ecommerce.rag.client.data.remote

import android.util.Log
import com.ecommerce.rag.client.config.AppConfig
import com.ecommerce.rag.client.data.local.AuthTokenStore
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

data class CartItem(
    val productId: String,
    val name: String,
    val price: Double,
    val currency: String,
    val imageUrl: String,
    val quantity: Int,
    val subtotal: Double
)

data class CartView(
    val userId: String,
    val items: List<CartItem>,
    val totalQuantity: Int,
    val totalAmount: Double,
    val currency: String
)

class CartApiClient(
    private val authTokenStore: AuthTokenStore,
    private val baseUrl: String = AppConfig.BASE_URL
) {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun getCart(): CartView {
        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("$baseUrl/api/cart")
                .get()
                .addAuthHeader()
                .build()

            executeCartRequest(request)
        }
    }

    suspend fun addToCart(productId: String, quantity: Int = 1): CartView {
        return withContext(Dispatchers.IO) {
            val requestBody = JsonObject().apply {
                addProperty("product_id", productId)
                addProperty("quantity", quantity)
            }.toString()

            val request = Request.Builder()
                .url("$baseUrl/api/cart/items")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .addAuthHeader()
                .build()

            executeCartRequest(request)
        }
    }

    suspend fun updateQuantity(productId: String, quantity: Int): CartView {
        return withContext(Dispatchers.IO) {
            val requestBody = JsonObject().apply {
                addProperty("quantity", quantity)
            }.toString()

            val request = Request.Builder()
                .url("$baseUrl/api/cart/items/$productId")
                .patch(requestBody.toRequestBody("application/json".toMediaType()))
                .addAuthHeader()
                .build()

            executeCartRequest(request)
        }
    }

    suspend fun removeItem(productId: String): CartView {
        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("$baseUrl/api/cart/items/$productId")
                .delete()
                .addAuthHeader()
                .build()

            executeCartRequest(request)
        }
    }

    suspend fun clearCart(): CartView {
        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("$baseUrl/api/cart")
                .delete()
                .addAuthHeader()
                .build()

            executeCartRequest(request)
        }
    }

    private fun Request.Builder.addAuthHeader(): Request.Builder {
        val token = authTokenStore.getAccessToken()
        if (!token.isNullOrBlank()) {
            addHeader("Authorization", "Bearer $token")
        }
        return this
    }

    private suspend fun executeCartRequest(request: Request): CartView {
        try {
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Unknown error"
                throw RuntimeException("购物车请求失败 (${response.code}): $errorBody")
            }

            val body = response.body?.string()
                ?: throw RuntimeException("购物车响应为空")

            return parseCartView(body)
        } catch (e: Exception) {
            Log.e(TAG, "Cart request failed", e)
            throw e
        }
    }

    private fun parseCartView(json: String): CartView {
        val root = JsonParser.parseString(json).asJsonObject

        val itemsArray = root.getAsJsonArray("items")
        val items: List<CartItem> = if (itemsArray != null && itemsArray.size() > 0) {
            itemsArray.map { elem ->
                val obj = elem.asJsonObject
                CartItem(
                    productId = obj.get("product_id")?.asString ?: "",
                    name = obj.get("name")?.asString ?: "",
                    price = obj.get("price")?.asDouble ?: 0.0,
                    currency = obj.get("currency")?.asString ?: "CNY",
                    imageUrl = obj.get("image_url")?.asString ?: "",
                    quantity = obj.get("quantity")?.asInt ?: 0,
                    subtotal = obj.get("subtotal")?.asDouble ?: 0.0
                )
            }
        } else {
            emptyList()
        }

        return CartView(
            userId = root.get("user_id")?.asString ?: "",
            items = items,
            totalQuantity = root.get("total_quantity")?.asInt ?: 0,
            totalAmount = root.get("total_amount")?.asDouble ?: 0.0,
            currency = root.get("currency")?.asString ?: "CNY"
        )
    }

    companion object {
        private const val TAG = "CartApiClient"
    }
}

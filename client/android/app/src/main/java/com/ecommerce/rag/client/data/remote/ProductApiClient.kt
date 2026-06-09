package com.ecommerce.rag.client.data.remote

import android.util.Log
import com.ecommerce.rag.client.config.AppConfig
import com.ecommerce.rag.client.data.model.ProductDetailUiModel
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class ProductApiClient(
    private val baseUrl: String = AppConfig.BASE_URL
) {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(AppConfig.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(AppConfig.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    suspend fun fetchProductDetail(productId: String): Result<ProductDetailUiModel> {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$baseUrl/api/products/$productId")
                    .get()
                    .build()

                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    if (response.code == 404) {
                        return@withContext Result.failure(
                            RuntimeException("商品不存在: $productId")
                        )
                    }
                    return@withContext Result.failure(
                        RuntimeException("请求失败 (${response.code})")
                    )
                }

                val body = response.body?.string()
                if (body.isNullOrBlank()) {
                    return@withContext Result.failure(
                        RuntimeException("响应为空")
                    )
                }

                val jsonObj = JsonParser.parseString(body).asJsonObject
                val specsObj = jsonObj.getAsJsonObject("specs")
                val specs: Map<String, String> = if (specsObj != null) {
                    val type = object : TypeToken<Map<String, String>>() {}.type
                    gson.fromJson(specsObj, type)
                } else {
                    emptyMap()
                }

                val detail = ProductDetailUiModel(
                    productId = jsonObj.get("product_id")?.asString ?: productId,
                    name = jsonObj.get("name")?.asString ?: "",
                    brand = jsonObj.get("brand")?.asString ?: "",
                    category = jsonObj.get("category")?.asString ?: "",
                    subCategory = jsonObj.get("sub_category")?.asString ?: "",
                    price = jsonObj.get("price")?.asDouble ?: 0.0,
                    currency = jsonObj.get("currency")?.asString ?: "CNY",
                    imageUrl = jsonObj.get("image_url")?.asString ?: "",
                    description = jsonObj.get("description")?.asString ?: "",
                    specs = specs,
                    avgRating = jsonObj.get("avg_rating")?.asDouble ?: 0.0
                )

                Result.success(detail)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch product detail", e)
                Result.failure(e)
            }
        }
    }

    companion object {
        private const val TAG = "ProductApiClient"
    }
}

package com.ecommerce.rag.client.data.local

import android.content.Context
import android.util.Log
import com.ecommerce.rag.client.data.model.ProductDetailUiModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * 从 Android assets 读取 products.json 的本地仓库。
 *
 * 特性：
 *  - 解析结果一次性缓存（assets 不会变），后续 get 直接返回缓存；
 *  - 解析失败或文件缺失时返回空列表，不抛异常，避免 App 崩溃；
 *  - 不引入 Room / DataStore / 网络层；
 *  - 不与后端 ProductApiClient 冲突，详情页仍走后端获取。
 */
class LocalProductRepository(
    private val context: Context
) {
    private val gson = Gson()
    private val mutex = Mutex()

    @Volatile
    private var cached: List<LocalProduct>? = null

    suspend fun getProducts(): List<LocalProduct> {
        cached?.let { return it }
        return mutex.withLock {
            cached?.let { return@withLock it }
            val loaded = loadFromAssets()
            cached = loaded
            loaded
        }
    }

    suspend fun getProductById(id: String): LocalProduct? {
        if (id.isBlank()) return null
        return getProducts().firstOrNull { it.productId == id }
    }

    /**
     * 阶段 8：详情页本地 fallback 入口。
     * 找不到时返回 null；缺字段时用安全缺省值兜底，保证 UI 能稳渲染。
     */
    suspend fun getProductDetailById(id: String): ProductDetailUiModel? {
        val raw = getProductById(id) ?: return null
        return toDetailUiModel(raw)
    }

    private fun toDetailUiModel(p: LocalProduct): ProductDetailUiModel = ProductDetailUiModel(
        productId = p.productId,
        name = p.name?.takeIf { it.isNotBlank() } ?: "未命名商品",
        brand = p.brand?.takeIf { it.isNotBlank() } ?: "官方旗舰",
        category = p.category.orEmpty(),
        subCategory = p.subCategory.orEmpty(),
        price = p.price ?: 0.0,
        currency = p.currency?.takeIf { it.isNotBlank() } ?: "CNY",
        imageUrl = resolveAssetImageUri(p.imageUrl),
        description = p.description.orEmpty(),
        specs = p.specs ?: emptyMap(),
        avgRating = (p.avgRating ?: 0.0).coerceIn(0.0, 5.0)
    )

    private suspend fun loadFromAssets(): List<LocalProduct> = withContext(Dispatchers.IO) {
        try {
            context.assets.open(ASSET_PATH).use { input ->
                val text = input.bufferedReader(Charsets.UTF_8).readText()
                val type = object : TypeToken<List<LocalProduct>>() {}.type
                val parsed: List<LocalProduct>? = gson.fromJson(text, type)
                val result = parsed
                    ?.filter { it.productId.isNotBlank() }
                    ?: emptyList()
                Log.d(TAG, "Loaded ${result.size} products from assets/$ASSET_PATH")
                result
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to load $ASSET_PATH from assets", t)
            emptyList()
        }
    }

    companion object {
        private const val TAG = "LocalProductRepository"
        private const val ASSET_PATH = "products.json"

        /**
         * 把 products.json 里的 image_url（如 "/images/p_beauty_001.jpg"）
         * 转成可被 Coil 加载的本地 assets URI。
         *
         * 阶段 5 约定：本地浏览页的图片走 assets，不走 backend；
         *            网络返回的商品卡片仍由各 ViewModel 自己处理。
         */
        fun resolveAssetImageUri(imageUrl: String?): String {
            if (imageUrl.isNullOrBlank()) return ""
            val trimmed = imageUrl.trim()
            if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                return trimmed
            }
            if (trimmed.startsWith("file:///android_asset/")) {
                return trimmed
            }
            val rel = trimmed.trimStart('/')
            return "file:///android_asset/$rel"
        }

    }
}

package com.ecommerce.rag.client.data.local

import com.google.gson.annotations.SerializedName

/**
 * 从 assets/products.json 解析出的本地商品。
 *
 * products.json 字段（snake_case，部分商品可能字段缺失，因此除 productId 外都 nullable）：
 *  - product_id       String   ：唯一 id（如 p_beauty_001）
 *  - name             String   ：商品名
 *  - brand            String   ：品牌
 *  - category         String   ：一级类目（美妆护肤 / 数码电子 / 服饰运动 / 食品饮料）
 *  - sub_category     String   ：二级类目
 *  - price            Double   ：售价
 *  - price_range      String   ：参考价区间
 *  - image_url        String   ："/images/xxx.jpg"，相对 backend 的路径；前端会被转成
 *                               assets 本地 URI 或者 backend HTTP URL（见 Repository）
 *  - description      String   ：长文本介绍
 *  - specs            Map<String,String>
 *  - avg_rating       Double
 *  - currency         String   ："CNY"
 */
data class LocalProduct(
    @SerializedName("product_id") val productId: String,
    @SerializedName("name") val name: String? = null,
    @SerializedName("brand") val brand: String? = null,
    @SerializedName("category") val category: String? = null,
    @SerializedName("sub_category") val subCategory: String? = null,
    @SerializedName("price") val price: Double? = null,
    @SerializedName("price_range") val priceRange: String? = null,
    @SerializedName("image_url") val imageUrl: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("specs") val specs: Map<String, String>? = null,
    @SerializedName("avg_rating") val avgRating: Double? = null,
    @SerializedName("currency") val currency: String? = null
)

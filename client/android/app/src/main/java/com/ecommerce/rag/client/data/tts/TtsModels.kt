package com.ecommerce.rag.client.data.tts

import com.google.gson.annotations.SerializedName

/**
 * 阶段 14：前端调用 `POST /api/tts/speak` 的请求/响应模型。
 *
 * 字段命名与现有 5 个接口保持一致：请求体 / 响应体一律 snake_case，
 * Kotlin 字段名通过 [SerializedName] 桥接。
 *
 * 后端未来推荐用 `edge-tts` 生成 mp3，详细约定见 `frontend-api-spec.md` §12 TTS。
 */
data class TtsRequest(
    @SerializedName("text")
    val text: String,
    @SerializedName("voice")
    val voice: String = DEFAULT_VOICE,
    @SerializedName("format")
    val format: String = DEFAULT_FORMAT
) {
    companion object {
        const val DEFAULT_VOICE = "zh-CN-XiaoxiaoNeural"
        const val DEFAULT_FORMAT = "mp3"
    }
}

/**
 * 后端可选两种响应：
 *  - 推荐 A：返回 `audio_url`（指向后端静态资源或临时签名 URL）。
 *  - 可选 B：返回 `audio_base64`（直接内嵌二进制，前端写入 cache 文件后再播）。
 *
 * 前端两种都解析，但优先用 audioUrl；若两个字段都空，[TtsApiClient] 当作失败。
 */
data class TtsResponse(
    @SerializedName("audio_url")
    val audioUrl: String? = null,
    @SerializedName("audio_base64")
    val audioBase64: String? = null,
    @SerializedName("content_type")
    val contentType: String? = null
)

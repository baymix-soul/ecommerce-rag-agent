package com.ecommerce.rag.client.data.tts

import android.util.Log
import com.ecommerce.rag.client.config.AppConfig
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * 阶段 14：调用 `POST /api/tts/speak` 把文本转语音。
 *
 * 与 [com.ecommerce.rag.client.data.remote.SseClient] 一样手写 JSON，
 * 避免和未来后端字段命名差异耦合。失败不抛业务异常，统一走 [Result]。
 *
 * 后端未就绪时：
 *  - HTTP 404 / 502 → [Result.failure]，UI 显示一次小提示。
 *  - 解析失败 / 字段全空 → [Result.failure]，行为同上。
 *  - **不会**影响文字 SSE 输出。
 */
class TtsApiClient(
    private val baseUrl: String = AppConfig.BASE_URL
) {
    private val gson: Gson = Gson()

    private val client: OkHttpClient = OkHttpClient.Builder()
        // TTS 体量小，超时设短一些；后端没接也能快速失败回到聊天主流程。
        .connectTimeout(10L, TimeUnit.SECONDS)
        .readTimeout(30L, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request()
            Log.d(TAG, "--> ${request.method} ${request.url}")
            val response = chain.proceed(request)
            Log.d(TAG, "<-- HTTP ${response.code} ${request.url}")
            response
        }
        .build()

    suspend fun speak(
        request: TtsRequest,
        accessToken: String? = null
    ): Result<TtsResponse> = withContext(Dispatchers.IO) {
        try {
            val bodyJson = JsonObject().apply {
                addProperty("text", request.text)
                addProperty("voice", request.voice)
                addProperty("format", request.format)
            }.toString()

            val builder = Request.Builder()
                .url("$baseUrl${AppConfig.TTS_SPEAK_PATH}")
                .post(bodyJson.toRequestBody(JSON_MEDIA_TYPE))
            if (!accessToken.isNullOrBlank()) {
                builder.addHeader("Authorization", "Bearer $accessToken")
            }

            client.newCall(builder.build()).execute().use { response ->
                val rawBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        RuntimeException("TTS HTTP ${response.code}: ${rawBody.take(200)}")
                    )
                }
                if (rawBody.isBlank()) {
                    return@withContext Result.failure(RuntimeException("TTS 响应为空"))
                }
                val parsed = try {
                    gson.fromJson(rawBody, TtsResponse::class.java)
                } catch (e: Exception) {
                    Log.w(TAG, "TTS response parse failed", e)
                    return@withContext Result.failure(RuntimeException("TTS 响应解析失败"))
                }
                if (parsed.audioUrl.isNullOrBlank() && parsed.audioBase64.isNullOrBlank()) {
                    return@withContext Result.failure(RuntimeException("TTS 响应不包含 audio_url / audio_base64"))
                }
                Result.success(parsed)
            }
        } catch (e: Exception) {
            Log.w(TAG, "TTS request failed", e)
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "TtsApiClient"
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }
}

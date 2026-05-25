package com.ecommerce.rag.client.data.remote

import android.util.Log
import com.ecommerce.rag.client.config.AppConfig
import com.ecommerce.rag.client.data.model.SseEvent
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class SseClient(
    private val baseUrl: String = AppConfig.BASE_URL
) {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(AppConfig.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(AppConfig.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request()
            Log.d(TAG, "--> ${request.method} ${request.url}")
            val response = chain.proceed(request)
            Log.d(TAG, "<-- HTTP ${response.code} ${request.url}")
            response
        }
        .build()

    private val gson = Gson()

    suspend fun streamChat(
        message: String,
        limit: Int = AppConfig.DEFAULT_CANDIDATE_LIMIT,
        onEvent: (SseEvent) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            try {
                val requestJson = gson.toJson(
                    mapOf("message" to message, "limit" to limit)
                )
                val request = Request.Builder()
                    .url("$baseUrl${AppConfig.CHAT_STREAM_PATH}")
                    .post(requestJson.toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "Unknown error"
                    onError(RuntimeException("HTTP ${response.code}: $errorBody"))
                    return@withContext
                }

                val body = response.body
                if (body == null) {
                    onError(RuntimeException("Empty response body"))
                    return@withContext
                }

                val reader = BufferedReader(InputStreamReader(body.byteStream()))
                var currentEventType = ""

                try {
                    var line: String?
                    var lineCount = 0
                    while (reader.readLine().also { line = it } != null) {
                        lineCount++
                        val currentLine = line!!
                        Log.v(TAG, "SSE line #$lineCount: $currentLine")
                        when {
                            currentLine.startsWith("event:") -> {
                                currentEventType = currentLine.substring(6).trim()
                                Log.d(TAG, "SSE event type: '$currentEventType'")
                            }
                            currentLine.startsWith("data:") -> {
                                val data = currentLine.substring(5).trim()
                                Log.d(TAG, "SSE data for event='$currentEventType': $data")
                                val event = parseEvent(currentEventType, data)
                                Log.d(TAG, "SSE parsed event: ${event.javaClass.simpleName}")
                                onEvent(event)
                                currentEventType = ""
                            }
                            currentLine.isBlank() -> {
                                currentEventType = ""
                            }
                        }
                    }
                    Log.d(TAG, "SSE stream ended, total lines: $lineCount")
                } catch (e: Exception) {
                    Log.w(TAG, "SSE stream read interrupted or error", e)
                } finally {
                    reader.close()
                    response.close()
                }
            } catch (e: Exception) {
                Log.e(TAG, "SSE connection failed", e)
                onError(e)
            }
        }
    }

    private fun parseEvent(eventType: String, data: String): SseEvent {
        return try {
            when (eventType) {
                "text" -> {
                    val jsonObj = JsonParser.parseString(data).asJsonObject
                    val content = jsonObj.get("content")?.asString ?: ""
                    SseEvent.Text(content)
                }
                "product_card" -> {
                    val jsonObj = JsonParser.parseString(data).asJsonObject
                    SseEvent.ProductCard(
                        productId = jsonObj.get("product_id")?.asString ?: "",
                        name = jsonObj.get("name")?.asString ?: "",
                        price = jsonObj.get("price")?.asDouble ?: 0.0,
                        currency = jsonObj.get("currency")?.asString ?: "CNY",
                        imageUrl = jsonObj.get("image_url")?.asString ?: "",
                        reason = jsonObj.get("reason")?.asString ?: ""
                    )
                }
                "done" -> SseEvent.Done
                "error" -> {
                    val jsonObj = JsonParser.parseString(data).asJsonObject
                    SseEvent.Error(
                        code = jsonObj.get("code")?.asString ?: "UNKNOWN",
                        message = jsonObj.get("message")?.asString ?: "Unknown error"
                    )
                }
                else -> SseEvent.Unknown
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse SSE event: type=$eventType, data=$data", e)
            SseEvent.Unknown
        }
    }

    companion object {
        private const val TAG = "SseClient"
    }
}

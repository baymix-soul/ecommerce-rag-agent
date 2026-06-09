package com.ecommerce.rag.client.data.remote

import android.util.Log
import com.ecommerce.rag.client.config.AppConfig
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

data class LoginResult(
    val accessToken: String,
    val tokenType: String,
    val expiresIn: Long,
    val userId: String,
    val username: String
)

class LoginApiClient(
    private val baseUrl: String = AppConfig.BASE_URL
) {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun login(username: String, password: String): LoginResult {
        return withContext(Dispatchers.IO) {
            try {
                val requestBody = JsonObject().apply {
                    addProperty("username", username)
                    addProperty("password", password)
                }.toString()

                val request = Request.Builder()
                    .url("$baseUrl/api/auth/login")
                    .post(requestBody.toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "Unknown error"
                    throw RuntimeException("登录失败 (${response.code}): $errorBody")
                }

                val body = response.body?.string()
                    ?: throw RuntimeException("登录响应为空")

                val json = JsonParser.parseString(body).asJsonObject

                LoginResult(
                    accessToken = json.get("access_token")?.asString
                        ?: throw RuntimeException("响应缺少 access_token"),
                    tokenType = json.get("token_type")?.asString ?: "Bearer",
                    expiresIn = json.get("expires_in")?.asLong ?: 3600L,
                    userId = json.get("user_id")?.asString ?: "",
                    username = json.get("username")?.asString ?: username
                )
            } catch (e: Exception) {
                Log.e(TAG, "Login failed", e)
                throw e
            }
        }
    }

    companion object {
        private const val TAG = "LoginApiClient"
    }
}

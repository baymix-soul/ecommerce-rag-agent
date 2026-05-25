package com.ecommerce.rag.client.config

object AppConfig {
    const val BASE_URL = "http://10.0.2.2:8080"
    const val CHAT_STREAM_PATH = "/api/chat/stream"
    const val DEFAULT_CANDIDATE_LIMIT = 5
    const val CONNECT_TIMEOUT_SECONDS = 30L
    const val READ_TIMEOUT_SECONDS = 120L
}

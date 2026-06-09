package com.ecommerce.rag.client.config

object AppConfig {
    // 开发调试（Android Emulator 访问本机后端）
    // const val BASE_URL = "http://10.0.2.2:8080"

    // 生产环境（HTTPS 公网 API）
    // 上线前将下方占位符替换为真实 API 域名，并取消注释：
    // const val BASE_URL = "https://api.your-domain.com"

    // 当前使用（上线前需切换为上面 HTTPS 地址）
    const val BASE_URL = "http://10.0.2.2:8080"

    const val CHAT_STREAM_PATH = "/api/chat/stream"
    const val AUTH_LOGIN_PATH = "/api/auth/login"
    const val CART_PATH = "/api/cart"
    const val CART_ITEMS_PATH = "/api/cart/items"
    // 阶段 14：TTS 语音合成。后端可选实现（不强依赖），失败时前端只显示一次错误。
    const val TTS_SPEAK_PATH = "/api/tts/speak"
    const val DEFAULT_CANDIDATE_LIMIT = 5
    const val CONNECT_TIMEOUT_SECONDS = 30L
    const val READ_TIMEOUT_SECONDS = 120L

    // Android 9+ 默认禁止 cleartext HTTP。本地开发时已在 AndroidManifest.xml
    // 设置 usesCleartextTraffic=true。生产环境必须使用 HTTPS，并在上线前移除
    // usesCleartextTraffic 或设为 false。
}

package com.ecommerce.rag.client.ui.assistant

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.SubcomposeAsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest

/**
 * 把 [BotAnimationState] 渲染成 GIF 角色。
 *
 * 实现要点：
 *  - 用 [SubcomposeAsyncImage] 加载 `file:///android_asset/bot/&lt;name&gt;.gif`；
 *    SubcomposeAsyncImage 可以直接拿到 loading / error 子槽位，方便做 fallback。
 *  - GIF decoder 跟 Android 版本绑定：
 *      * API >= 28：使用 [ImageDecoderDecoder]（系统原生 ImageDecoder，更省内存，支持 WebP/HEIF 等）；
 *      * API <  28：使用 [GifDecoder]（Coil 的纯 Java 解码器，作为 fallback）。
 *    minSdk = 26，所以两条分支都会用到，必须都注册。
 *  - 每次创建一个 **局部 ImageLoader** 并按需注册 GIF decoder factory；
 *    不依赖 ChatApplication 全局配置，避免污染主进程 Coil 实例。
 *  - **不**包圆形纯色背景 —— GIF 自带透明角色，强行加底色会盖掉透明区域。
 *    只在加载失败时退回到 emoji 占位（带圆形 Surface 给视觉一致性）。
 *
 * @param size 显示尺寸（dp）。默认 96dp，比之前的 56dp 圆形按钮大一圈，更接近"桌面宠物"。
 */
@Composable
fun AnimatedBot(
    animationState: BotAnimationState,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp
) {
    val context = LocalContext.current

    // ImageLoader 跟 context 绑定即可；Composable 多次重组只会 build 一次同样配置的 ImageRequest，
    // Coil 内部按 URI 做缓存，性能足够。
    val imageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }

    val uri = animationState.assetUri()

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context)
                .data(uri)
                .crossfade(true)
                .build(),
            imageLoader = imageLoader,
            contentDescription = "购物助手",
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(size),
            error = { EmojiFallback(size = size) },
            loading = {
                // 短暂加载期间维持空白即可，避免一闪一闪的占位 UI。
                Box(modifier = Modifier.size(size))
            }
        )
    }
}

/**
 * GIF 加载失败时显示的 emoji 占位。
 * 这里**才**用纯色圆形背景，是为了和 idle.gif 缺失时仍能看出"机器人在这里"。
 */
@Composable
private fun EmojiFallback(size: Dp) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        tonalElevation = 4.dp,
        shadowElevation = 4.dp,
        modifier = Modifier.size(size)
    ) {
        Box(
            modifier = Modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🤖",
                style = MaterialTheme.typography.headlineSmall
            )
        }
    }
}

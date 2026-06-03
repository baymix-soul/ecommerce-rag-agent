package com.ecommerce.rag.client.ui.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 阶段 6 视觉占位用底部导航。
 *
 * - 5 个 tab：首页 / 逛逛 / 直播 / 消息 / 我的，纯展示，不做真实跳转；
 * - 首页默认 active：黑底白字的小圆角 icon；
 * - 其他 tab：灰色 emoji + 灰色文字；
 * - navigationBarsPadding() 让出系统手势区；
 * - 总高度 ≈ 64dp + 系统底 inset，AssistantOverlay 据此调整 bot 初始 y。
 */
@Composable
fun CommerceBottomBar(
    activeIndex: Int = 0,
    onTabClick: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        color = BrowseColors.CardWhite,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(
                color = BrowseColors.Divider,
                thickness = 0.5.dp
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(PaddingValues(horizontal = 4.dp, vertical = 6.dp))
                    .height(56.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                TABS.forEachIndexed { idx, tab ->
                    BottomTab(
                        emoji = tab.emoji,
                        label = tab.label,
                        active = idx == activeIndex,
                        onClick = { onTabClick(idx) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

private data class TabSpec(val label: String, val emoji: String)

private val TABS = listOf(
    TabSpec("首页", "🏠"),
    TabSpec("逛逛", "🧭"),
    TabSpec("直播", "▶"),
    TabSpec("消息", "💬"),
    TabSpec("我的", "👤"),
)

@Composable
private fun BottomTab(
    emoji: String,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (active) BrowseColors.Dark else BrowseColors.CardWhite),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = emoji,
                style = MaterialTheme.typography.titleMedium
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (active) BrowseColors.Dark else BrowseColors.TextSecondary,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

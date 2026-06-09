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
 * 底部导航。
 *
 * 阶段 6：仅 UI 占位，5 个 tab 真正能跳转的只有"首页"高亮。
 * 阶段 10：暴露 [onHomeClick] / [onProfileClick] 给 MainActivity，
 *  - "首页" 点击：在非首页（例如个人中心）调用时返回浏览页；
 *  - "我的" 点击：进入 [ProfileScreen]；
 *  - "逛逛 / 直播 / 消息" 仍是 UI 占位，可通过通用 [onTabClick] 接管。
 *
 * activeIndex 含义沿用之前：0 首页 / 1 逛逛 / 2 直播 / 3 消息 / 4 我的。
 */
@Composable
fun CommerceBottomBar(
    activeIndex: Int = 0,
    onTabClick: (Int) -> Unit = {},
    onHomeClick: () -> Unit = { onTabClick(0) },
    onProfileClick: () -> Unit = { onTabClick(4) },
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
                        onClick = {
                            when (idx) {
                                0 -> onHomeClick()
                                4 -> onProfileClick()
                                else -> onTabClick(idx)
                            }
                        },
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

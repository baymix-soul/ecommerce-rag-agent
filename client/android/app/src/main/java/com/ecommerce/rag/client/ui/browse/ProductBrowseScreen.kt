package com.ecommerce.rag.client.ui.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ecommerce.rag.client.data.model.PageContext
import com.ecommerce.rag.client.data.model.PageType

/**
 * 阶段 6 视觉重写后的电商浏览主页。
 *
 * 结构（自上而下）：
 *  - Box（背景 #F6F6F6）
 *    └── Column
 *        ├── TopHeader：状态栏 inset + 搜索胶囊 + 通知 + 购物车 + 横向 tabs
 *        ├── LazyVerticalGrid(2 列, weight 1f)
 *        │     ├── 全宽行 Banner（黑→红渐变）
 *        │     ├── 全宽行 CategoryGrid（4 列宫格 8 项）
 *        │     ├── 全宽行 GuessYouLikeHeader（"猜你喜欢" + 副标题）
 *        │     ├── 全宽行 LoadingState / ErrorState / EmptyState（条件分支）
 *        │     └── items(products) → BrowseProductCard
 *        └── CommerceBottomBar
 *
 * 保留：
 *  - 真实搜索（viewModel.onSearchQueryChange）
 *  - 真实分类筛选（viewModel.onCategorySelected）
 *  - PageContext 上报（PRODUCT_LIST + visibleProductIds + searchQuery + selectedFilters）
 *
 * 仅 UI 占位：
 *  - 顶部 5 个 tabs（推荐/同城/关注/直播/新品）只切高亮、不过滤
 *  - 8 项分类的"百亿补贴/直播"映射为 selectedCategory=null
 *  - "家居" 映射为真实 raw="家居"，但 products.json 无此类目，呈现空状态
 *  - Banner 上 "进入会场 / 看直播" 按钮无后续行为
 *  - 顶栏通知 🔔 / 购物车 🛒 仅 UI
 *  - 收藏 ♡ / 视频角标 仅 UI
 */
@Composable
fun ProductBrowseScreen(
    onProductClick: (String) -> Unit,
    onPageContextChange: (PageContext) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProductBrowseViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var activeTopTab by remember { mutableStateOf(TOP_TABS.first()) }
    var activeCategoryLabel by remember { mutableStateOf(BROWSE_CATEGORIES.first().label) }

    val visibleIds = uiState.products.take(VISIBLE_ID_REPORT_LIMIT).map { it.productId }
    val filters: Map<String, String> = uiState.selectedCategory
        ?.let { mapOf("category" to it) }
        ?: emptyMap()

    LaunchedEffect(visibleIds, uiState.searchQuery, uiState.selectedCategory) {
        onPageContextChange(
            PageContext(
                pageType = PageType.PRODUCT_LIST,
                currentProductId = null,
                visibleProductIds = visibleIds,
                searchQuery = uiState.searchQuery.takeIf { it.isNotBlank() },
                selectedFilters = filters
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BrowseColors.PageBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopHeader(
                searchValue = uiState.searchQuery,
                onSearchChange = viewModel::onSearchQueryChange,
                onClearSearch = { viewModel.onSearchQueryChange("") },
                activeTopTab = activeTopTab,
                onTopTabSelect = { activeTopTab = it }
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(
                    start = 12.dp, end = 12.dp, top = 12.dp, bottom = 128.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    PromoBanner()
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    CategoryGrid(
                        activeLabel = activeCategoryLabel,
                        onClick = { cat ->
                            activeCategoryLabel = cat.label
                            viewModel.onCategorySelected(cat.rawCategory)
                        }
                    )
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    GuessYouLikeHeader()
                }

                when {
                    uiState.isLoading -> item(span = { GridItemSpan(maxLineSpan) }) {
                        LoadingState()
                    }
                    uiState.error != null && uiState.products.isEmpty() ->
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            ErrorState(uiState.error ?: "加载失败")
                        }
                    uiState.products.isEmpty() ->
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            EmptyState(
                                query = uiState.searchQuery,
                                category = uiState.selectedCategory
                            )
                        }
                    else -> itemsIndexed(
                        items = uiState.products,
                        key = { _, card -> card.productId }
                    ) { idx, card ->
                        BrowseProductCard(
                            product = card,
                            index = idx,
                            onClick = { onProductClick(card.productId) }
                        )
                    }
                }
            }

            CommerceBottomBar()
        }
    }
}

// ---------- 顶部搜索栏 + Tabs ----------

@Composable
private fun TopHeader(
    searchValue: String,
    onSearchChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    activeTopTab: String,
    onTopTabSelect: (String) -> Unit
) {
    Surface(
        color = BrowseColors.CardWhite,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SearchCapsule(
                    value = searchValue,
                    onChange = onSearchChange,
                    onClear = onClearSearch,
                    modifier = Modifier.weight(1f)
                )
                CircleEmojiButton(
                    emoji = "🔔",
                    background = BrowseColors.ChipBg,
                    badge = true
                )
                CircleEmojiButton(
                    emoji = "🛒",
                    background = BrowseColors.Dark,
                    badge = false,
                    invert = true
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            TopTabs(activeTab = activeTopTab, onSelect = onTopTabSelect)
        }
    }
}

@Composable
private fun SearchCapsule(
    value: String,
    onChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = BrowseColors.ChipBg,
        modifier = modifier.height(40.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "🔍", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(
                        text = "搜索商品 / 品牌 / 子类目",
                        style = MaterialTheme.typography.bodySmall,
                        color = BrowseColors.TextTertiary
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = BrowseColors.TextPrimary,
                        fontSize = MaterialTheme.typography.bodyMedium.fontSize
                    ),
                    cursorBrush = SolidColor(BrowseColors.AccentRed),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (value.isNotEmpty()) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(BrowseColors.TextTertiary.copy(alpha = 0.45f))
                        .clickable { onClear() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "×",
                        style = MaterialTheme.typography.bodySmall,
                        color = BrowseColors.CardWhite,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "☰",
                style = MaterialTheme.typography.bodyMedium,
                color = BrowseColors.TextSecondary
            )
        }
    }
}

@Composable
private fun CircleEmojiButton(
    emoji: String,
    background: Color,
    badge: Boolean,
    invert: Boolean = false
) {
    Box(
        modifier = Modifier.size(40.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = CircleShape,
            color = background,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = emoji,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (invert) BrowseColors.CardWhite else BrowseColors.TextPrimary
                )
            }
        }
        if (badge) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(2.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(BrowseColors.AccentRed)
            )
        }
    }
}

@Composable
private fun TopTabs(
    activeTab: String,
    onSelect: (String) -> Unit
) {
    val scroll = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scroll),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TOP_TABS.forEach { tab ->
            val active = tab == activeTab
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onSelect(tab) }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = tab,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (active) BrowseColors.TextPrimary else BrowseColors.TextSecondary,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .height(3.dp)
                        .width(if (active) 20.dp else 0.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(BrowseColors.AccentRed)
                )
            }
        }
    }
}

// ---------- Banner ----------

@Composable
private fun PromoBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        BrowseColors.Dark,
                        Color(0xFF1F0608),
                        BrowseColors.AccentRed
                    )
                )
            )
            .padding(20.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = BrowseColors.CardWhite.copy(alpha = 0.18f)
                ) {
                    Text(
                        text = "✨ 今晚 8 点爆款开抢",
                        style = MaterialTheme.typography.labelSmall,
                        color = BrowseColors.CardWhite,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            Text(
                text = "边刷边买，发现你的下一件好物",
                style = MaterialTheme.typography.titleLarge,
                color = BrowseColors.CardWhite,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "个性化推荐、直播讲解、真实评价和优惠券集中展示。",
                style = MaterialTheme.typography.bodySmall,
                color = BrowseColors.CardWhite.copy(alpha = 0.85f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BannerPrimaryButton(text = "进入会场")
                BannerSecondaryButton(text = "看直播")
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "最高省 50%",
                    style = MaterialTheme.typography.labelSmall,
                    color = BrowseColors.CardWhite.copy(alpha = 0.85f)
                )
            }
        }
    }
}

@Composable
private fun BannerPrimaryButton(text: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = BrowseColors.CardWhite
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = BrowseColors.Dark,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun BannerSecondaryButton(text: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = BrowseColors.CardWhite.copy(alpha = 0.18f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = BrowseColors.CardWhite,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

// ---------- 分类宫格 ----------

@Composable
private fun CategoryGrid(
    activeLabel: String,
    onClick: (BrowseCategory) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = BrowseColors.CardWhite,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 4 列，2 行，手动 chunked(4) 排
            BROWSE_CATEGORIES.chunked(4).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    row.forEach { cat ->
                        CategoryCell(
                            category = cat,
                            active = cat.label == activeLabel,
                            onClick = { onClick(cat) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryCell(
    category: BrowseCategory,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (active) BrowseColors.AccentRedSoft else BrowseColors.ChipBg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = category.emoji,
                style = MaterialTheme.typography.titleMedium
            )
        }
        Text(
            text = category.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (active) BrowseColors.AccentRed else BrowseColors.TextPrimary,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

// ---------- 商品标题 ----------

@Composable
private fun GuessYouLikeHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 4.dp)
    ) {
        Text(
            text = "猜你喜欢",
            style = MaterialTheme.typography.titleMedium,
            color = BrowseColors.TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "根据浏览行为动态推荐",
            style = MaterialTheme.typography.labelSmall,
            color = BrowseColors.TextSecondary
        )
    }
}

// ---------- 占位三态 ----------

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CircularProgressIndicator(color = BrowseColors.AccentRed)
            Text(
                text = "正在加载本地商品…",
                style = MaterialTheme.typography.bodySmall,
                color = BrowseColors.TextSecondary
            )
        }
    }
}

@Composable
private fun ErrorState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp, horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = BrowseColors.AccentRed
        )
    }
}

@Composable
private fun EmptyState(query: String, category: String?) {
    val hint = when {
        query.isNotBlank() -> "没有匹配 \"$query\" 的商品"
        category != null -> "「$category」分类下暂无商品"
        else -> "暂无商品"
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp, horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = hint,
            style = MaterialTheme.typography.bodyMedium,
            color = BrowseColors.TextSecondary
        )
    }
}

// ---------- 数据 / 常量 ----------

/**
 * 设计稿分类项 → products.json 真实 category 的映射。
 * rawCategory == null 表示"全集/UI 占位"，不向后端发送 category 过滤。
 */
internal data class BrowseCategory(
    val label: String,
    val emoji: String,
    val rawCategory: String?
)

private val BROWSE_CATEGORIES = listOf(
    BrowseCategory("推荐", "🔥", null),
    BrowseCategory("百亿补贴", "💸", null),
    BrowseCategory("直播", "🎥", null),
    BrowseCategory("服饰", "👕", "服饰运动"),
    BrowseCategory("美妆", "💄", "美妆护肤"),
    BrowseCategory("数码", "📱", "数码电子"),
    // products.json 实际无"家居"类目，点击后会进入空状态分支
    BrowseCategory("家居", "🛋", "家居"),
    BrowseCategory("食品", "🍪", "食品饮料"),
)

private val TOP_TABS = listOf("推荐", "同城", "关注", "直播", "新品")

private const val VISIBLE_ID_REPORT_LIMIT = 20

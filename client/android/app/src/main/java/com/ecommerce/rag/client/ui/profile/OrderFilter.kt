package com.ecommerce.rag.client.ui.profile

/**
 * 阶段 10：个人中心订单分类的轻量枚举。
 *
 * 本轮 ProfileScreen 上"我的订单"卡片里的 5 个 tab + 顶部"查看全部"都映射到这个 enum。
 * 真实订单接口尚未实现，MainActivity 收到回调后只是跳一个 ProfilePlaceholderScreen，
 * 标题取自 [title] 字段，跟设计稿一致。
 *
 * 当前不要扩展到"已完成 / 已取消"等更细分状态：等后端 `/api/orders` 接口落地后再补。
 */
enum class OrderFilter(val title: String) {
    ALL("全部订单"),
    PENDING_PAYMENT("待付款"),
    PENDING_SHIPMENT("待发货"),
    PENDING_RECEIPT("待收货"),
    PENDING_REVIEW("待评价"),
    REFUND("退款/售后")
}

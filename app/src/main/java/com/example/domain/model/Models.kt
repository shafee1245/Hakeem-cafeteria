package com.example.domain.model

data class UserSession(
    val userId: String,
    val fullName: String,
    val registerNumber: String,
    val department: String,
    val year: String,
    val email: String,
    val phone: String,
    val role: String // "STUDENT" or "ADMIN"
)

data class BookingStatusResult(
    val isOpen: Boolean,
    val openTime: String,
    val closeTime: String,
    val remainingSeconds: Long,
    val closureReason: String
)

data class CartDisplayItem(
    val product: com.example.data.local.ProductEntity,
    val quantity: Int,
    val lineTotal: Double
)

data class OrderWithItems(
    val order: com.example.data.local.OrderEntity,
    val items: List<com.example.data.local.OrderItemEntity>
)

data class SalesAnalyticsSummary(
    val grossSales: Double,
    val netSales: Double,
    val totalOrders: Int,
    val deliveredOrders: Int,
    val cancelledOrders: Int,
    val rejectedOrders: Int,
    val totalRefunds: Double,
    val averageOrderValue: Double,
    val bestSellingProduct: String,
    val topCategory: String
)

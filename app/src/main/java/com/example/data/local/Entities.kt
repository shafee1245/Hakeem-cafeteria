package com.example.data.local

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class UserEntity(
    val id: String,
    @SerialName("full_name") val fullName: String,
    @SerialName("register_number") val registerNumber: String? = null,
    val department: String? = null,
    val year: String? = null,
    val email: String,
    val phone: String? = null,
    val role: String = "STUDENT",
    @SerialName("created_at") val createdAt: Long? = null
)

@Serializable
data class ProductEntity(
    val id: String = "",
    val name: String,
    val description: String? = null,
    val category: String? = null,
    val price: Double,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("is_veg") val isVeg: Boolean = true,
    val availability: String = "AVAILABLE",
    @SerialName("is_featured") val isFeatured: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class CategoryEntity(
    val id: String = "",
    val name: String,
    @SerialName("icon_name") val iconName: String? = null,
    @SerialName("sort_order") val sortOrder: Int = 0
)

@Serializable
data class CartItemEntity(
    @SerialName("user_id") val userId: String,
    @SerialName("product_id") val productId: String,
    val quantity: Int
)

@Serializable
data class OrderEntity(
    val id: String = "",
    @SerialName("user_id") val userId: String,
    @SerialName("user_full_name") val userFullName: String? = null,
    @SerialName("user_register_number") val userRegisterNumber: String? = null,
    @SerialName("token_number") val tokenNumber: String,
    val subtotal: Double,
    val total: Double,
    @SerialName("payment_method") val paymentMethod: String = "ONLINE",
    @SerialName("payment_status") val paymentStatus: String = "PAID",
    @SerialName("order_status") val orderStatus: String = "PLACED",
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class OrderItemEntity(
    val id: String = "",
    @SerialName("order_id") val orderId: String,
    @SerialName("product_id") val productId: String? = null,
    @SerialName("product_name") val productName: String? = null,
    @SerialName("product_price") val productPrice: Double? = null,
    val quantity: Int
)

@Serializable
data class OrderWithItemsRelation(
    val order: OrderEntity,
    val items: List<OrderItemEntity>
)

@Serializable
data class AdvertisementEntity(
    val id: String = "",
    @SerialName("company_name") val companyName: String? = null,
    val title: String,
    val description: String? = null,
    @SerialName("banner_url") val bannerUrl: String? = null,
    @SerialName("cta_text") val ctaText: String? = null,
    @SerialName("redirect_url") val redirectUrl: String? = null,
    @SerialName("start_date") val startDate: Long? = null,
    @SerialName("end_date") val endDate: Long? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    val impressions: Int = 0,
    val clicks: Int = 0
)

@Serializable
data class BookingSettingsEntity(
    val id: Int = 1,
    @SerialName("open_time") val openTime: String = "09:00",
    @SerialName("close_time") val closeTime: String = "11:40",
    val timezone: String = "Asia/Kolkata",
    @SerialName("active_days") val activeDays: String = "Mon,Tue,Wed,Thu,Fri,Sat",
    @SerialName("is_booking_enabled") val isBookingEnabled: Boolean = true,
    @SerialName("is_emergency_closed") val isEmergencyClosed: Boolean = false,
    @SerialName("emergency_closure_reason") val emergencyClosureReason: String? = null
)

@Serializable
data class NotificationEntity(
    val id: String = "",
    @SerialName("user_id") val userId: String,
    val title: String,
    val message: String,
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class AuditLogEntity(
    val id: String = "",
    @SerialName("admin_id") val adminId: String,
    @SerialName("admin_email") val adminEmail: String,
    val action: String,
    val details: String,
    @SerialName("timestamp") val timestamp: String? = null
)

fun String?.parseToEpochMillis(): Long? {
    if (this == null) return null
    return try {
        this.toLong()
    } catch (e: NumberFormatException) {
        try {
            java.time.Instant.parse(this).toEpochMilli()
        } catch (e: Exception) {
            null
        }
    }
}

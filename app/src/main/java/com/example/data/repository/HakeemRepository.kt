package com.example.data.repository

import com.example.data.local.*
import com.example.domain.model.*
import com.example.data.remote.SupabaseService
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.delay
import kotlinx.serialization.json.*
import java.util.UUID
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

class HakeemRepository {
    private val client = SupabaseService.client

    // ========================
    // USERS
    // ========================
    suspend fun registerStudent(
        userId: String,
        fullName: String,
        registerNumber: String,
        department: String,
        year: String,
        email: String,
        phone: String
    ): Result<UserEntity> {
        return try {
            val user = UserEntity(
                id = userId, // Provided by Supabase Auth
                fullName = fullName,
                registerNumber = registerNumber,
                department = department,
                year = year,
                email = email,
                phone = phone
            )
            // Insert or update profile in public.users (avoids trigger duplicate key error)
            client.postgrest["users"].upsert(user)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createOrUpdateUser(
        id: String,
        email: String,
        fullName: String
    ): Result<UserEntity> {
        return try {
            var user = getUserById(id)
            if (user == null) {
                // If it's the specific admin email, make them admin
                val role = if (email == "hakeemcafeteria786@gmail.com") "ADMIN" else "STUDENT"
                user = UserEntity(
                    id = id,
                    email = email,
                    fullName = fullName,
                    registerNumber = null,
                    department = null,
                    year = null,
                    phone = null,
                    role = role
                )
                client.postgrest["users"].upsert(user)
            } else if (email == "hakeemcafeteria786@gmail.com" && user.role != "ADMIN") {
                // Ensure admin role is set if they somehow had student
                user = user.copy(role = "ADMIN")
                client.postgrest["users"].update(
                    { UserEntity::role setTo "ADMIN" }
                ) {
                    filter { UserEntity::id eq id }
                }
            }
            Result.success(user)
        } catch (e: Exception) {
            android.util.Log.e("HakeemRepository", "Error in createOrUpdateUser", e)
            Result.failure(e)
        }
    }

    suspend fun getEmailByRegisterNumber(regNumber: String): String? {
        return try {
            val response = client.postgrest.rpc(
                "get_email_by_reg", 
                buildJsonObject { put("p_reg", regNumber) }
            ).data
            if (response != "null" && response.isNotBlank()) {
                 response.trim('"') // Remove JSON quotes
            } else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getUserById(userId: String): UserEntity? {
        return try {
            client.postgrest["users"].select {
                filter { UserEntity::id eq userId }
            }.decodeSingleOrNull<UserEntity>()
        } catch (e: Exception) {
            android.util.Log.e("HakeemRepository", "Error in getUserById", e)
            null
        }
    }

    suspend fun updateUserProfile(
        userId: String,
        fullName: String,
        department: String,
        phone: String,
        registerNumber: String,
        year: String
    ): Result<Unit> {
        return try {
            client.postgrest["users"].update(
                {
                    UserEntity::fullName setTo fullName
                    UserEntity::department setTo department
                    UserEntity::phone setTo phone
                    UserEntity::registerNumber setTo registerNumber
                    UserEntity::year setTo year
                }
            ) {
                filter { UserEntity::id eq userId }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========================
    // BOOKING SETTINGS
    // ========================
    suspend fun computeBookingStatus(settings: BookingSettingsEntity): BookingStatusResult {
        var isOpen = settings.isBookingEnabled && !settings.isEmergencyClosed
        var remainingSeconds = 0L

        try {
            val zoneId = java.time.ZoneId.of(settings.timezone.ifBlank { "Asia/Kolkata" })
            val now = java.time.ZonedDateTime.now(zoneId)
            
            // Normalize active days
            val normalizedDays = settings.activeDays
                .split(",")
                .map { it.trim().lowercase() }
                .filter { it.isNotBlank() }
                .toSet()

            val currentDay = now.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.US).lowercase()
            
            if (!normalizedDays.contains(currentDay)) {
                isOpen = false
            }

            fun parseTime(t: String): java.time.LocalTime {
                val clean = t.trim().uppercase()
                return try {
                    java.time.LocalTime.parse(clean)
                } catch (e: Exception) {
                    val fmt = java.time.format.DateTimeFormatterBuilder()
                        .parseCaseInsensitive()
                        .appendPattern("h:mm a")
                        .toFormatter(java.util.Locale.US)
                    java.time.LocalTime.parse(clean, fmt)
                }
            }

            val openTime = parseTime(settings.openTime)
            val closeTime = parseTime(settings.closeTime)
            val currentTime = now.toLocalTime()

            if (currentTime.isBefore(openTime) || !currentTime.isBefore(closeTime)) {
                isOpen = false
            } else if (isOpen) {
                // If still open, calculate remaining seconds
                remainingSeconds = java.time.Duration.between(currentTime, closeTime).seconds
            }
        } catch (e: Exception) {
            android.util.Log.e("HakeemRepository", "Error calculating booking status: ${e.message}", e)
            isOpen = false
        }

        return BookingStatusResult(
            isOpen = isOpen,
            openTime = settings.openTime,
            closeTime = settings.closeTime,
            remainingSeconds = remainingSeconds,
            closureReason = if (settings.isEmergencyClosed) (settings.emergencyClosureReason ?: "") else ""
        )
    }

    fun getBookingSettingsFlow(): Flow<BookingSettingsEntity?> = flow {
        while(true) {
            val s = try {
                client.postgrest["settings"].select().decodeSingleOrNull<BookingSettingsEntity>()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch(e: Exception) { 
                null
            }
            emit(s)
            delay(5000)
        }
    }

    suspend fun updateBookingSettings(settings: BookingSettingsEntity, adminId: String, adminEmail: String) {
        try {
            client.postgrest["settings"]
                .update(settings) {
                    filter { eq("id", 1) }
                }
        } catch (e: Exception) {
            android.util.Log.e("HakeemRepository", "Failed to update settings", e)
            throw e // Rethrow to let ViewModel know it failed
        }
    }

    // ========================
    // PRODUCTS & CATEGORIES
    // ========================
    fun getActiveProductsFlow(): Flow<List<ProductEntity>> = flow {
        while(true) {
            try {
                val p = client.postgrest["products"].select {
                    filter { ProductEntity::availability neq "INACTIVE" }
                    limit(100)
                }.decodeList<ProductEntity>()
                emit(p)
            } catch(e: Exception) { 
                android.util.Log.e("HakeemRepository", "Error fetching active products", e)
                emit(emptyList()) 
            }
            delay(5000)
        }
    }

    fun getAllProductsFlow(): Flow<List<ProductEntity>> = flow {
        while(true) {
            try {
                emit(client.postgrest["products"].select { limit(100) }.decodeList<ProductEntity>())
            } catch(e: Exception) {
                android.util.Log.e("HakeemRepository", "EXACT_FLOW_ERROR: getAllProductsFlow", e)
                emit(emptyList())
            }
            delay(5000)
        }
    }

    suspend fun getProductById(id: String): ProductEntity? {
        return try {
            client.postgrest["products"].select { filter { ProductEntity::id eq id } }.decodeSingleOrNull<ProductEntity>()
        } catch (e: Exception) { null }
    }

    suspend fun saveProduct(product: ProductEntity, isNew: Boolean, adminId: String, adminEmail: String) {
        try {
            if (isNew) {
                client.postgrest["products"].insert(product)
            } else {
                client.postgrest["products"].update(
                    {
                        ProductEntity::name setTo product.name
                        ProductEntity::description setTo product.description
                        ProductEntity::category setTo product.category
                        ProductEntity::price setTo product.price
                        ProductEntity::imageUrl setTo product.imageUrl
                        ProductEntity::isVeg setTo product.isVeg
                        ProductEntity::availability setTo product.availability
                        ProductEntity::isFeatured setTo product.isFeatured
                    }
                ) {
                    filter { ProductEntity::id eq product.id }
                }
            }
        } catch(e: Exception) {
            android.util.Log.e("HakeemRepository", "Error saving product", e)
            throw e
        }
    }

    suspend fun deleteProduct(productId: String, adminId: String, adminEmail: String) {
        try {
            client.postgrest["products"].delete {
                filter { ProductEntity::id eq productId }
            }
        } catch (e: Exception) {
            android.util.Log.e("HakeemRepository", "Error deleting product", e)
            throw e
        }
    }

    suspend fun uploadImage(bytes: ByteArray, productId: String): Result<String> {
        return try {
            val bucket = client.storage["images"]
            val timestamp = System.currentTimeMillis()
            val path = "products/$productId/${timestamp}.webp"
            bucket.upload(path, bytes) {
                upsert = true
            }
            Result.success(bucket.publicUrl(path))
        } catch (e: Exception) {
            android.util.Log.e("HakeemRepository", "Upload failed", e)
            Result.failure(e)
        }
    }

    suspend fun uploadImageRaw(bytes: ByteArray, fileName: String): Result<String> {
        return try {
            val bucket = client.storage["images"]
            val path = "uploads/$fileName"
            bucket.upload(path, bytes) {
                upsert = true
            }
            Result.success(bucket.publicUrl(path))
        } catch (e: Exception) {
            android.util.Log.e("HakeemRepository", "Raw upload failed", e)
            Result.failure(e)
        }
    }

    suspend fun deleteImage(imageUrl: String) {
        try {
            // imageUrl example: https://<project>.supabase.co/storage/v1/object/public/images/products/UUID/123.webp
            val pathStartIndex = imageUrl.indexOf("/images/") + 8
            if (pathStartIndex > 7) {
                val path = imageUrl.substring(pathStartIndex)
                val bucket = client.storage["images"]
                bucket.delete(path)
            }
        } catch (e: Exception) {
            android.util.Log.e("HakeemRepository", "Failed to delete image: $imageUrl", e)
        }
    }

    fun getActiveCategoriesFlow(): Flow<List<CategoryEntity>> = flow { emit(emptyList()) }
    fun getAllCategoriesFlow(): Flow<List<CategoryEntity>> = flow { emit(emptyList()) }
    suspend fun saveCategory(category: CategoryEntity) {}
    suspend fun deleteCategory(id: String) {}

    // ========================
    // CART
    // ========================
    private val memoryCarts = mutableMapOf<String, MutableList<CartItemEntity>>()

    fun getCartForUserFlow(userId: String): Flow<List<CartItemEntity>> = flow {
        while(true) {
            emit(memoryCarts[userId]?.toList() ?: emptyList())
            delay(1000)
        }
    }

    suspend fun addToCart(userId: String, productId: String, quantityToAdd: Int): Result<Unit> {
        val cart = memoryCarts.getOrPut(userId) { mutableListOf() }
        val existing = cart.find { it.productId == productId }
        if (existing != null) {
            val index = cart.indexOf(existing)
            cart[index] = existing.copy(quantity = existing.quantity + quantityToAdd)
        } else {
            cart.add(CartItemEntity(userId, productId, quantityToAdd))
        }
        return Result.success(Unit)
    }

    suspend fun updateCartQuantity(userId: String, productId: String, newQuantity: Int) {
        val cart = memoryCarts[userId] ?: return
        val existing = cart.find { it.productId == productId }
        if (existing != null) {
            if (newQuantity <= 0) {
                cart.remove(existing)
            } else {
                val index = cart.indexOf(existing)
                cart[index] = existing.copy(quantity = newQuantity)
            }
        }
    }

    suspend fun removeFromCart(userId: String, productId: String) {
        memoryCarts[userId]?.removeIf { it.productId == productId }
    }

    suspend fun clearCart(userId: String) {
        memoryCarts[userId]?.clear()
    }

    // ========================
    // ORDERS
    // ========================
    @kotlinx.serialization.Serializable
    private data class CartRpcItem(val product_id: String, val quantity: Int)

    suspend fun checkoutOrder(
        userId: String,
        paymentMethodChoice: String
    ): Result<OrderEntity> {
        return try {
            val cartItems = memoryCarts[userId] ?: emptyList()
            if (cartItems.isEmpty()) throw Exception("Cart is empty")

            val idempotencyKey = UUID.randomUUID().toString()
            val rpcItems = cartItems.map { CartRpcItem(it.productId, it.quantity) }

            val response = client.postgrest.rpc(
                function = "place_order",
                parameters = buildJsonObject {
                    put("p_cart_items", Json.encodeToJsonElement(
                        kotlinx.serialization.builtins.ListSerializer(CartRpcItem.serializer()), 
                        rpcItems
                    ))
                    put("p_idempotency_key", idempotencyKey)
                    put("p_payment_method", paymentMethodChoice)
                }
            ).decodeAs<JsonObject>()

            if (response["status"]?.jsonPrimitive?.content != "success") {
                throw Exception("Checkout failed: ${response["message"]?.jsonPrimitive?.content}")
            }
            
            val orderId = response["order_id"]?.jsonPrimitive?.content ?: ""
            
            // Return a minimal order entity just to satisfy UI success state
            Result.success(OrderEntity(
                id = orderId,
                userId = userId,
                userFullName = "",
                userRegisterNumber = null,
                tokenNumber = "",
                subtotal = response["total"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0,
                total = response["total"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0,
                paymentMethod = paymentMethodChoice,
                paymentStatus = "PAID",
                orderStatus = "PLACED"
            ))
        } catch (e: Exception) {
            android.util.Log.e("HakeemRepository", "Checkout error", e)
            Result.failure(e)
        }
    }

    suspend fun initiateRazorpayPayment(orderId: String): Result<JsonObject> {
        return try {
            val url = "${com.example.BuildConfig.NEXT_PUBLIC_SUPABASE_URL}/functions/v1/razorpay-create-order"
            val httpClient = okhttp3.OkHttpClient()
            val mediaType = "application/json".toMediaType()
            val requestBody = "{\"order_id\":\"$orderId\"}".toRequestBody(mediaType)
            var token = client.auth.currentAccessTokenOrNull()
            if (token == null) {
                try {
                    client.auth.refreshCurrentSession()
                    token = client.auth.currentAccessTokenOrNull()
                } catch (e: Exception) {
                    android.util.Log.e("HakeemRepository", "Session refresh failed", e)
                }
            }
            if (token == null) {
                throw Exception("User session expired or not authenticated. Please log in again.")
            }

            val request = okhttp3.Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("apikey", com.example.BuildConfig.NEXT_PUBLIC_SUPABASE_ANON_KEY)
                .post(requestBody)
                .build()

            val response = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { httpClient.newCall(request).execute() }
            val responseBody = response.body?.string() ?: "{}"
            if (!response.isSuccessful) {
                throw Exception("HTTP ${response.code}: $responseBody")
            }
            val jsonResponse = kotlinx.serialization.json.Json.parseToJsonElement(responseBody).jsonObject

            if (jsonResponse["error"] != null) {
                val errorMsg = jsonResponse["error"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it.content else "Failed to generate Razorpay order" } ?: "Failed to generate Razorpay order"
                throw Exception(errorMsg)
            }
            Result.success(jsonResponse)
        } catch (e: Exception) {
            android.util.Log.e("HakeemRepository", "Razorpay initiate error", e)
            Result.failure(e)
        }
    }

    suspend fun verifyRazorpayPayment(
        orderId: String,
        paymentId: String,
        razorpayOrderId: String,
        signature: String
    ): Result<Unit> {
        return try {
            val url = "${com.example.BuildConfig.NEXT_PUBLIC_SUPABASE_URL}/functions/v1/razorpay-verify-payment"
            val bodyJson = kotlinx.serialization.json.buildJsonObject {
                put("order_id", orderId)
                put("razorpay_payment_id", paymentId)
                put("razorpay_order_id", razorpayOrderId)
                put("razorpay_signature", signature)
            }
            val httpClient = okhttp3.OkHttpClient()
            val mediaType = "application/json".toMediaType()
            val requestBody = bodyJson.toString().toRequestBody(mediaType)
            var token = client.auth.currentAccessTokenOrNull()
            if (token == null) {
                try {
                    client.auth.refreshCurrentSession()
                    token = client.auth.currentAccessTokenOrNull()
                } catch (e: Exception) {
                    android.util.Log.e("HakeemRepository", "Session refresh failed", e)
                }
            }
            if (token == null) {
                throw Exception("User session expired or not authenticated. Please log in again.")
            }

            val request = okhttp3.Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("apikey", com.example.BuildConfig.NEXT_PUBLIC_SUPABASE_ANON_KEY)
                .post(requestBody)
                .build()

            val response = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { httpClient.newCall(request).execute() }
            val responseBody = response.body?.string() ?: "{}"
            if (!response.isSuccessful) {
                throw Exception("HTTP ${response.code}: $responseBody")
            }
            val jsonResponse = kotlinx.serialization.json.Json.parseToJsonElement(responseBody).jsonObject
            
            if (jsonResponse["error"] != null) {
                val errorMsg = jsonResponse["error"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it.content else "Verification failed" } ?: "Verification failed"
                throw Exception(errorMsg)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("HakeemRepository", "Razorpay verify error", e)
            Result.failure(e)
        }
    }

    fun getOrdersWithItemsForUserFlow(userId: String): Flow<List<OrderWithItems>> = flow {
        while(true) {
            try {
                val orders = client.postgrest["orders"].select {
                    filter { eq("user_id", userId) }
                    limit(100)
                }.decodeList<OrderEntity>()
                val items = client.postgrest["order_items"].select().decodeList<OrderItemEntity>()
                
                val result = orders.map { o ->
                    OrderWithItems(
                        order = o,
                        items = items.filter { it.orderId == o.id }
                    )
                }
                emit(result)
            } catch(e: Exception) {
                android.util.Log.e("HakeemRepository", "EXACT_FLOW_ERROR: getOrdersWithItemsForUserFlow", e)
                emit(emptyList())
            }
            delay(5000)
        }
    }

    fun getAllOrdersFlow(): Flow<List<OrderEntity>> = flow {
        while(true) {
            try {
                emit(client.postgrest["orders"].select().decodeList<OrderEntity>())
            } catch(e: Exception) {
                android.util.Log.e("HakeemRepository", "EXACT_FLOW_ERROR: getAllOrdersFlow", e)
                emit(emptyList())
            }
            delay(5000)
        }
    }

    suspend fun getOrderById(orderId: String): OrderEntity? {
        return try {
            client.postgrest["orders"].select { filter { OrderEntity::id eq orderId } }.decodeSingleOrNull<OrderEntity>()
        } catch(e: Exception) { null }
    }

    fun getAllOrdersWithItemsFlow(): Flow<List<OrderWithItems>> = flow {
        while(true) {
            try {
                val orders = client.postgrest["orders"].select { limit(100) }.decodeList<OrderEntity>()
                val items = client.postgrest["order_items"].select().decodeList<OrderItemEntity>()
                
                val result = orders.map { o ->
                    OrderWithItems(
                        order = o,
                        items = items.filter { it.orderId == o.id }
                    )
                }
                emit(result)
            } catch(e: Exception) {
                android.util.Log.e("HakeemRepository", "EXACT_FLOW_ERROR: getAllOrdersWithItemsFlow", e)
                emit(emptyList())
            }
            delay(5000)
        }
    }

    fun getOrderItemsFlow(orderId: String): Flow<List<OrderItemEntity>> = flow {
        while(true) {
            try {
                emit(client.postgrest["order_items"].select { filter { OrderItemEntity::orderId eq orderId } }.decodeList<OrderItemEntity>())
            } catch(e: Exception) { emit(emptyList()) }
            delay(5000)
        }
    }

    suspend fun updateOrderStatus(orderId: String, newStatus: String, adminId: String, adminEmail: String): Result<Unit> {
        return try {
            client.postgrest["orders"].update(
                mapOf("order_status" to newStatus)
            ) { filter { eq("id", orderId) } }
            Result.success(Unit)
        } catch(e: Exception) { 
            android.util.Log.e("HakeemRepository", "Failed to update order status", e)
            Result.failure(e) 
        }
    }

    // ========================
    // ADVERTISEMENTS
    // ========================
    fun getActiveAdsFlow(): Flow<List<AdvertisementEntity>> = flow {
        while(true) {
            try {
                emit(client.postgrest["campaigns"].select {
                    filter { AdvertisementEntity::isActive eq true }
                    limit(50)
                }.decodeList<AdvertisementEntity>())
            } catch(e: Exception) { emit(emptyList()) }
            delay(5000)
        }
    }

    fun getAllAdsFlow(): Flow<List<AdvertisementEntity>> = flow {
        while(true) {
            try {
                emit(client.postgrest["campaigns"].select { limit(100) }.decodeList<AdvertisementEntity>())
            } catch(e: Exception) { emit(emptyList()) }
            delay(5000)
        }
    }

    suspend fun saveAdvertisement(ad: AdvertisementEntity) {
        try {
            val a = if (ad.id.isEmpty()) ad.copy(id = UUID.randomUUID().toString()) else ad
            client.postgrest["campaigns"].upsert(a)
        } catch(e: Exception) {}
    }

    suspend fun deleteAdvertisement(id: String) {
        try {
            client.postgrest["campaigns"].delete { filter { AdvertisementEntity::id eq id } }
        } catch(e: Exception) {}
    }

    suspend fun recordAdImpression(id: String) {}
    suspend fun recordAdClick(id: String) {}

    // ========================
    // ANALYTICS
    // ========================
    suspend fun calculateSalesAnalytics(): SalesAnalyticsSummary {
        return try {
            val zoneId = java.time.ZoneId.of("Asia/Kolkata")
            val now = java.time.ZonedDateTime.now(zoneId)
            val startOfDay = now.toLocalDate().atStartOfDay(zoneId).toInstant().toString()
            val endOfDay = now.toLocalDate().plusDays(1).atStartOfDay(zoneId).toInstant().toString()
            
            val orders = client.postgrest["orders"].select {
                filter {
                    gte("created_at", startOfDay)
                    lt("created_at", endOfDay)
                }
            }.decodeList<OrderEntity>()
            
            val validOrders = orders.filter { it.paymentStatus == "PAID" || it.paymentMethod == "CASH" }
            val gross = validOrders.sumOf { it.total }
            
            SalesAnalyticsSummary(
                grossSales = gross,
                netSales = gross,
                totalOrders = validOrders.size,
                deliveredOrders = validOrders.count { it.orderStatus == "DELIVERED" },
                cancelledOrders = orders.count { it.orderStatus == "CANCELLED" },
                rejectedOrders = orders.count { it.orderStatus == "REJECTED" },
                totalRefunds = 0.0,
                averageOrderValue = if (validOrders.isEmpty()) 0.0 else gross / validOrders.size,
                bestSellingProduct = "N/A",
                topCategory = "N/A"
            )
        } catch(e: Exception) {
            android.util.Log.e("HakeemRepository", "EXACT_FLOW_ERROR: calculateSalesAnalytics", e)
            SalesAnalyticsSummary(0.0, 0.0, 0, 0, 0, 0, 0.0, 0.0, "", "")
        }
    }

    // ========================
    // MISSING STUBS
    // ========================
    suspend fun updateProductStock(productId: String, stock: Int, adminId: String, adminEmail: String) {}
    
    suspend fun cancelOrderStudent(orderId: String, userId: String): Result<Unit> = Result.success(Unit)
    
    suspend fun rejectOrderAdmin(orderId: String, reason: String, adminId: String, adminEmail: String): Result<Unit> = Result.success(Unit)
    
    fun getAllAuditLogsFlow(): Flow<List<AuditLogEntity>> = flow { emit(emptyList()) }
    
    fun getNotificationsForUserFlow(userId: String): Flow<List<NotificationEntity>> = flow {
        while(true) {
            val list = try {
                client.postgrest["notifications"]
                    .select { 
                        filter { eq("user_id", userId) }
                        order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                    }
                    .decodeList<NotificationEntity>()
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                emptyList()
            }
            emit(list)
            delay(5000) // Poll every 5 seconds
        }
    }
    
    suspend fun markNotificationRead(id: String) {
        try {
            client.postgrest["notifications"]
                .update(mapOf("is_read" to true)) {
                    filter { eq("id", id) }
                }
        } catch (e: Exception) {
            android.util.Log.e("HakeemRepository", "Failed to mark notification read", e)
        }
    }
    
    suspend fun saveFcmToken(uid: String, token: String) {
        try {
            val session = client.auth.currentSessionOrNull()
            val hasSession = session != null
            val authUid = session?.user?.id
            
            android.util.Log.d("FCM_DEBUG", "Attempting to save FCM token.")
            android.util.Log.d("FCM_DEBUG", "Provided uid (target user_id): $uid")
            android.util.Log.d("FCM_DEBUG", "Has active Supabase session: $hasSession")
            android.util.Log.d("FCM_DEBUG", "Authenticated Supabase authUid: $authUid")

            @kotlinx.serialization.Serializable
            data class FcmToken(val user_id: String, val token: String)
            
            val tokenData = FcmToken(uid, token)
            client.postgrest["fcm_tokens"].upsert(tokenData) {
                // Upsert based on token to prevent duplicates
            }
            android.util.Log.d("FCM_DEBUG", "Successfully saved FCM token to database.")
        } catch (e: Exception) {
            android.util.Log.e("FCM_DEBUG", "Error saving FCM token: ${e.message}", e)
        }
    }
}

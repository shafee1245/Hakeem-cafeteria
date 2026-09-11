package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.*
import com.example.data.repository.HakeemRepository
import com.example.domain.model.SalesAnalyticsSummary
import com.example.domain.model.OrderWithItems
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AdminViewModel(
    private val repository: HakeemRepository,
    val adminUser: UserEntity
) : ViewModel() {

    // Optimistic UI state
    private val _optimisticStatuses = MutableStateFlow<Map<String, String>>(emptyMap())
    private val _inProgressOrders = MutableStateFlow<Set<String>>(emptySet())

    val allOrders: StateFlow<List<OrderWithItems>> = repository.getAllOrdersWithItemsFlow()
        .combine(_optimisticStatuses) { remoteOrders, overrides ->
            remoteOrders.map { order ->
                val override = overrides[order.order.id]
                // Only apply override if remote order hasn't caught up or passed it
                if (override != null && order.order.orderStatus != override) {
                    order.copy(order = order.order.copy(orderStatus = override))
                } else {
                    order
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Products & Categories
    val allProducts: StateFlow<List<ProductEntity>> = repository.getAllProductsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories: StateFlow<List<CategoryEntity>> = repository.getAllCategoriesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Advertisements
    val allAds: StateFlow<List<AdvertisementEntity>> = repository.getAllAdsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Booking Settings
    val bookingSettings: StateFlow<BookingSettingsEntity?> = repository.getBookingSettingsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Audit Logs
    val auditLogs: StateFlow<List<AuditLogEntity>> = repository.getAllAuditLogsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Analytics Summary
    private val _analyticsSummary = MutableStateFlow<SalesAnalyticsSummary?>(null)
    val analyticsSummary: StateFlow<SalesAnalyticsSummary?> = _analyticsSummary.asStateFlow()

    // Toast / User Feedback
    private val _adminMessage = MutableStateFlow<String?>(null)
    val adminMessage: StateFlow<String?> = _adminMessage.asStateFlow()

    init {
        refreshAnalytics()
    }

    fun refreshAnalytics() {
        viewModelScope.launch {
            _analyticsSummary.value = repository.calculateSalesAnalytics()
        }
    }

    private fun optimisticallyUpdateOrderStatus(
        orderId: String,
        newStatus: String,
        serverUpdate: suspend () -> Result<Unit>
    ) {
        if (_inProgressOrders.value.contains(orderId)) return

        viewModelScope.launch {
            _inProgressOrders.update { it + orderId }
            val previousStatus = _optimisticStatuses.value[orderId]
            
            // Apply optimistic update immediately
            _optimisticStatuses.update { it + (orderId to newStatus) }
            
            println("OPTIMISTIC UPDATE: Order $orderId -> $newStatus (UI Updated)")

            // Perform background network update
            serverUpdate()
                .onSuccess {
                    println("OPTIMISTIC SUCCESS: Order $orderId -> $newStatus (DB Confirmed)")
                    _adminMessage.value = "Order #$orderId status updated to $newStatus"
                    refreshAnalytics()
                    _inProgressOrders.update { it - orderId }
                }
                .onFailure { err ->
                    println("OPTIMISTIC FAIL: Order $orderId -> $newStatus (Rolling back)")
                    _adminMessage.value = err.message ?: "Failed to update order status"
                    // Rollback
                    if (previousStatus != null) {
                        _optimisticStatuses.update { it + (orderId to previousStatus) }
                    } else {
                        _optimisticStatuses.update { it - orderId }
                    }
                    _inProgressOrders.update { it - orderId }
                }
        }
    }

    fun updateOrderStatus(orderId: String, newStatus: String) {
        optimisticallyUpdateOrderStatus(orderId, newStatus) {
            repository.updateOrderStatus(orderId, newStatus, adminUser.id, adminUser.email)
        }
    }

    fun rejectOrder(orderId: String, reason: String) {
        viewModelScope.launch {
            repository.rejectOrderAdmin(orderId, reason, adminUser.id, adminUser.email)
                .onSuccess {
                    _adminMessage.value = "Order #$orderId rejected and refund credited"
                    refreshAnalytics()
                }
                .onFailure { err ->
                    _adminMessage.value = err.message ?: "Failed to reject order"
                }
        }
    }

    fun saveProductWithImage(product: ProductEntity, imageBytes: ByteArray?, oldImageUrl: String?, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val isNewProduct = product.id.isEmpty()
            var finalProduct = product
            try {
                var newImageUrl: String? = null
                
                // Assign an ID if it's a new product so the image path is valid
                if (finalProduct.id.isEmpty()) {
                    finalProduct = finalProduct.copy(id = java.util.UUID.randomUUID().toString())
                }

                // If we have new image bytes to upload
                if (imageBytes != null) {
                    val uploadResult = repository.uploadImage(imageBytes, finalProduct.id)
                    uploadResult.onSuccess { url ->
                        newImageUrl = url
                        finalProduct = finalProduct.copy(imageUrl = newImageUrl)
                    }.onFailure { e ->
                        _adminMessage.value = "Image upload failed: ${e.message ?: "Unknown error"}"
                        onComplete(false)
                        return@launch
                    }
                }

                repository.saveProduct(finalProduct, isNewProduct, adminUser.id, adminUser.email)
                
                // Cleanup old image if it was replaced and exists
                if (imageBytes != null && oldImageUrl != null && oldImageUrl != newImageUrl) {
                    repository.deleteImage(oldImageUrl)
                }

                _adminMessage.value = "Product '${finalProduct.name}' saved successfully"
                onComplete(true)
            } catch (e: Exception) {
                _adminMessage.value = "Error: ${e.message}"
                android.util.Log.e("AdminViewModel", "Product save error", e)
                onComplete(false)
            }
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            try {
                repository.deleteProduct(productId, adminUser.id, adminUser.email)
                _adminMessage.value = "Product removed"
            } catch (e: Exception) {
                if (e.message == "SOFT_DELETED_DUE_TO_FK") {
                    _adminMessage.value = com.example.utils.ErrorMapper.getUserFriendlyMessage(e)
                } else {
                    _adminMessage.value = com.example.utils.ErrorMapper.getUserFriendlyMessage(e)
                }
            }
        }
    }

    fun updateProductStock(productId: String, stock: Int) {
        viewModelScope.launch {
            repository.updateProductStock(productId, stock, adminUser.id, adminUser.email)
            _adminMessage.value = "Stock updated to $stock"
        }
    }

    fun saveProduct(product: ProductEntity) {
        viewModelScope.launch {
            val isNewProduct = product.id.isEmpty()
            try {
                repository.saveProduct(product, isNewProduct, adminUser.id, adminUser.email)
                _adminMessage.value = "Product '${product.name}' saved successfully"
            } catch (e: Exception) {
                _adminMessage.value = "Failed to save product: ${e.message}"
            }
        }
    }

    suspend fun uploadImage(bytes: ByteArray, fileName: String): Result<String> {
        // Used by Advertisements or other screens
        return repository.uploadImageRaw(bytes, fileName)
    }

    fun saveCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.saveCategory(category)
            _adminMessage.value = "Category '${category.name}' saved"
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            repository.deleteCategory(categoryId)
            _adminMessage.value = "Category deleted"
        }
    }

    fun saveAdvertisement(ad: AdvertisementEntity) {
        viewModelScope.launch {
            repository.saveAdvertisement(ad)
            _adminMessage.value = "Advertisement banner saved"
        }
    }

    fun deleteAdvertisement(adId: String) {
        viewModelScope.launch {
            repository.deleteAdvertisement(adId)
            _adminMessage.value = "Advertisement deleted"
        }
    }

    fun updateBookingSettings(settings: BookingSettingsEntity) {
        viewModelScope.launch {
            try {
                repository.updateBookingSettings(settings, adminUser.id, adminUser.email)
                _adminMessage.value = "Booking window settings updated"
            } catch (e: Exception) {
                _adminMessage.value = "Failed to update settings: ${e.message}"
            }
        }
    }

    fun clearMessage() {
        _adminMessage.value = null
    }
}

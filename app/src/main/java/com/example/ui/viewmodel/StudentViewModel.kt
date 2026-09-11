package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.*
import com.example.data.repository.HakeemRepository
import com.example.domain.model.BookingStatusResult
import com.example.domain.model.CartDisplayItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface CheckoutState {
    data object Idle : CheckoutState
    data object Processing : CheckoutState
    data class RazorpayPending(val rzpOrderId: String, val amount: Int, val key: String, val order: OrderEntity) : CheckoutState
    data class Success(val order: OrderEntity) : CheckoutState
    data class Error(val message: String) : CheckoutState
}

class StudentViewModel(
    private val repository: HakeemRepository,
    private val userId: String
) : ViewModel() {

    // Booking Status
    private val _bookingStatus = MutableStateFlow(
        BookingStatusResult(false, "09:00", "11:40", 0, "Checking booking status...")
    )
    val bookingStatus: StateFlow<BookingStatusResult> = _bookingStatus.asStateFlow()

    // Products & Categories
    val activeProducts: StateFlow<List<ProductEntity>> = repository.getActiveProductsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeCategories: StateFlow<List<CategoryEntity>> = repository.getActiveCategoriesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeAds: StateFlow<List<AdvertisementEntity>> = repository.getActiveAdsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Search & Filters
    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow("ALL")
    val vegOnlyFilter = MutableStateFlow<Boolean?>(null)

    // Filtered Products
    val filteredProducts: StateFlow<List<ProductEntity>> = combine(
        activeProducts,
        searchQuery,
        selectedCategory,
        vegOnlyFilter
    ) { products, query, category, isVeg ->
        products.filter { prod ->
            val matchesQuery = query.isBlank() || 
                prod.name.contains(query, ignoreCase = true) || 
                (prod.description?.contains(query, ignoreCase = true) == true) ||
                (prod.category?.contains(query, ignoreCase = true) == true)
            val matchesCategory = category == "ALL" || prod.category.equals(category, ignoreCase = true)
            val matchesVeg = isVeg == null || prod.isVeg == isVeg
            matchesQuery && matchesCategory && matchesVeg
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Cart Items
    private val rawCartItems: StateFlow<List<CartItemEntity>> = repository.getCartForUserFlow(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cartDisplayItems: StateFlow<List<CartDisplayItem>> = combine(
        rawCartItems,
        activeProducts
    ) { items, products ->
        val map = products.associateBy { it.id }
        items.mapNotNull { cartItem ->
            map[cartItem.productId]?.let { prod ->
                CartDisplayItem(
                    product = prod,
                    quantity = cartItem.quantity,
                    lineTotal = prod.price * cartItem.quantity
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cartTotal: StateFlow<Double> = cartDisplayItems.map { list ->
        list.sumOf { it.lineTotal }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val cartItemCount: StateFlow<Int> = cartDisplayItems.map { list ->
        list.sumOf { it.quantity }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Orders
    val userOrders: StateFlow<List<com.example.domain.model.OrderWithItems>> = repository.getOrdersWithItemsForUserFlow(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userNotifications: StateFlow<List<NotificationEntity>> = repository.getNotificationsForUserFlow(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Checkout Flow State
    private val _checkoutState = MutableStateFlow<CheckoutState>(CheckoutState.Idle)
    val checkoutState: StateFlow<CheckoutState> = _checkoutState.asStateFlow()

    val useWalletToggle = MutableStateFlow(false)
    val selectedPaymentMethod = MutableStateFlow("ONLINE")

    // Toast / Action Messaging
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    init {
        // Start continuous countdown timer loop without spamming network
        viewModelScope.launch {
            var currentSettings = com.example.data.local.BookingSettingsEntity()
            
            // Job 1: Keep settings updated from DB
            launch {
                repository.getBookingSettingsFlow().collect { settings ->
                    if (settings != null) {
                        currentSettings = settings
                    }
                }
            }
            
            // Job 2: Calculate countdown locally every second
            while (true) {
                _bookingStatus.value = repository.computeBookingStatus(currentSettings)
                delay(1000)
            }
        }
    }

    fun addToCart(productId: String, quantity: Int = 1) {
        viewModelScope.launch {
            repository.addToCart(userId, productId, quantity)
                .onFailure { err ->
                    _userMessage.value = com.example.utils.ErrorMapper.getUserFriendlyMessage(err)
                }
        }
    }

    fun updateCartQuantity(productId: String, newQty: Int) {
        viewModelScope.launch {
            repository.updateCartQuantity(userId, productId, newQty)
        }
    }

    fun removeFromCart(productId: String) {
        viewModelScope.launch {
            repository.removeFromCart(userId, productId)
        }
    }

    fun clearCart() {
        viewModelScope.launch {
            repository.clearCart(userId)
        }
    }

    fun executeCheckout() {
        viewModelScope.launch {
            _checkoutState.value = CheckoutState.Processing
            repository.checkoutOrder(
                userId = userId,
                paymentMethodChoice = selectedPaymentMethod.value
            )
            .onSuccess { newOrder ->
                if (selectedPaymentMethod.value == "CASH") {
                    repository.clearCart(userId)
                    _checkoutState.value = CheckoutState.Success(newOrder)
                    return@launch
                }
                
                // For ONLINE, initiate Razorpay
                repository.initiateRazorpayPayment(newOrder.id)
                    .onSuccess { json ->
                        val rzpId = json["razorpay_order_id"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it.content else "" } ?: ""
                        val key = json["key"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it.content else "" } ?: ""
                        val amount = json["amount"]?.let { if (it is kotlinx.serialization.json.JsonPrimitive) it.content.toIntOrNull() else 0 } ?: 0
                        
                        if (rzpId.isEmpty()) {
                            _checkoutState.value = CheckoutState.Error("Order could not be placed. Please try again.")
                            return@onSuccess
                        }

                        // Register global callback for when Razorpay finishes
                        com.example.MainActivity.razorpayCallback = { success, data, errorMsg ->
                            if (success && data != null) {
                                viewModelScope.launch {
                                    _checkoutState.value = CheckoutState.Processing
                                    repository.verifyRazorpayPayment(
                                        orderId = newOrder.id,
                                        paymentId = data.paymentId,
                                        razorpayOrderId = data.orderId,
                                        signature = data.signature
                                    ).onSuccess {
                                        repository.clearCart(userId)
                                        _checkoutState.value = CheckoutState.Success(newOrder.copy(paymentStatus = "PAID"))
                                    }.onFailure { err ->
                                        _checkoutState.value = CheckoutState.Error(com.example.utils.ErrorMapper.getUserFriendlyMessage(err))
                                    }
                                }
                            } else {
                                _checkoutState.value = CheckoutState.Error(errorMsg ?: "Payment cancelled")
                            }
                        }

                        _checkoutState.value = CheckoutState.RazorpayPending(
                            rzpOrderId = rzpId,
                            amount = amount,
                            key = key,
                            order = newOrder
                        )
                    }
                    .onFailure { err ->
                        _checkoutState.value = CheckoutState.Error(com.example.utils.ErrorMapper.getUserFriendlyMessage(err))
                    }
            }
            .onFailure { err ->
                _checkoutState.value = CheckoutState.Error(com.example.utils.ErrorMapper.getUserFriendlyMessage(err))
            }
        }
    }

    fun cancelOrder(orderId: String) {
        viewModelScope.launch {
            repository.cancelOrderStudent(orderId, userId)
                .onSuccess {
                    _userMessage.value = "Order cancelled successfully."
                }
                .onFailure { err ->
                    _userMessage.value = com.example.utils.ErrorMapper.getUserFriendlyMessage(err)
                }
        }
    }

    fun recordAdClick(adId: String) {
        viewModelScope.launch {
            repository.recordAdClick(adId)
        }
    }

    fun recordAdImpression(adId: String) {
        viewModelScope.launch {
            repository.recordAdImpression(adId)
        }
    }

    fun clearCheckoutState() {
        _checkoutState.value = CheckoutState.Idle
    }

    fun markNotificationRead(id: String) {
        viewModelScope.launch {
            repository.markNotificationRead(id)
        }
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }
}

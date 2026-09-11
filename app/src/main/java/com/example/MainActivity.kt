package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeOut
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.data.local.OrderEntity
import com.example.data.local.UserEntity
import com.example.data.repository.HakeemRepository
import com.example.ui.components.BottomNavBar
import com.example.ui.components.StudentTab
import com.example.ui.screens.admin.*
import com.example.ui.screens.auth.LoginScreen
import com.example.ui.screens.auth.RegisterScreen
import com.example.ui.screens.student.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.HakeemTheme
import com.example.ui.viewmodel.AdminViewModel
import com.example.ui.viewmodel.AuthUiState
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.StudentViewModel
import android.os.Build
import android.Manifest
import androidx.activity.result.contract.ActivityResultContracts

import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener

class MainActivity : ComponentActivity(), PaymentResultWithDataListener {

    companion object {
        var razorpayCallback: ((Boolean, PaymentData?, String?) -> Unit)? = null
    }

    override fun onPaymentSuccess(razorpayPaymentId: String?, paymentData: PaymentData?) {
        razorpayCallback?.invoke(true, paymentData, null)
    }

    override fun onPaymentError(code: Int, response: String?, paymentData: PaymentData?) {
        razorpayCallback?.invoke(false, paymentData, response)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        com.razorpay.Checkout.preload(applicationContext)

        val repository = HakeemRepository()

        val authViewModel = AuthViewModel(repository)

        // Request POST_NOTIFICATIONS for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val requestPermissionLauncher = registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (!isGranted) {
                    Toast.makeText(this, "Notifications disabled", Toast.LENGTH_SHORT).show()
                }
            }
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            var isDarkMode by rememberSaveable { mutableStateOf(false) }

            HakeemTheme(darkTheme = isDarkMode) {
                val context = LocalContext.current
                val authState by authViewModel.uiState.collectAsState()
                val currentUser by authViewModel.currentUser.collectAsState()

                var authScreenRoute by remember { mutableStateOf("login") } // "login", "register"

                if (currentUser == null) {
                    when (authScreenRoute) {
                        "register" -> RegisterScreen(
                            viewModel = authViewModel,
                            onNavigateToLogin = { authScreenRoute = "login" }
                        )
                        else -> LoginScreen(
                            viewModel = authViewModel,
                            onNavigateToRegister = { authScreenRoute = "register" },
                            isDarkMode = isDarkMode,
                            onToggleDarkMode = { isDarkMode = !isDarkMode }
                        )
                    }
                } else {
                    val user = currentUser!!
                    if (user.role == "ADMIN") {
                        AdminAppContainer(
                            adminUser = user,
                            repository = repository,
                            onLogout = { authViewModel.logout() },
                            isDarkMode = isDarkMode,
                            onToggleDarkMode = { isDarkMode = !isDarkMode }
                        )
                    } else {
                        StudentAppContainer(
                            user = currentUser!!,
                            repository = repository,
                            onLogout = { authViewModel.logout() },
                            onEditProfile = { name, dept, phone, regNo, year ->
                                authViewModel.updateProfile(name, dept, phone, regNo, year)
                            },
                            isDarkMode = isDarkMode,
                            onToggleDarkMode = { isDarkMode = !isDarkMode }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentAppContainer(
    user: UserEntity,
    repository: HakeemRepository,
    onLogout: () -> Unit,
    onEditProfile: (String, String, String, String, String) -> Unit,
    isDarkMode: Boolean = false,
    onToggleDarkMode: () -> Unit = {}
) {
    val context = LocalContext.current
    val studentViewModel = remember(user.id) { StudentViewModel(repository, user.id) }

    var currentScreen by remember { mutableStateOf("main") } // "main", "checkout", "confirmation", "notifications"
    var selectedTab by remember { mutableStateOf(StudentTab.HOME) }
    var menuCategoryFilter by remember { mutableStateOf<String?>(null) }
    var confirmedOrder by remember { mutableStateOf<OrderEntity?>(null) }

    var showCartSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val cartItemCount by studentViewModel.cartItemCount.collectAsState()
    val cartTotal by studentViewModel.cartTotal.collectAsState()
    val userMessage by studentViewModel.userMessage.collectAsState()

    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            studentViewModel.clearUserMessage()
        }
    }

    Scaffold(
        bottomBar = {
            if (currentScreen == "main") {
                BottomNavBar(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    cartItemCount = cartItemCount
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            when (currentScreen) {
                "checkout" -> CheckoutScreen(
                    viewModel = studentViewModel,
                    onNavigateToConfirmation = { order ->
                        confirmedOrder = order
                        currentScreen = "confirmation"
                    },
                    onNavigateBack = { 
                        showCartSheet = true
                        currentScreen = "main" 
                    },
                    modifier = Modifier.padding(innerPadding)
                )

                "confirmation" -> confirmedOrder?.let { order ->
                    OrderConfirmationScreen(
                        order = order,
                        onNavigateToOrders = {
                            selectedTab = StudentTab.ORDERS
                            currentScreen = "main"
                        },
                        onNavigateToHome = {
                            selectedTab = StudentTab.HOME
                            currentScreen = "main"
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }

                "notifications" -> NotificationsScreen(
                    viewModel = studentViewModel,
                    onNavigateBack = { currentScreen = "main" },
                    modifier = Modifier.padding(innerPadding)
                )

                else -> {
                    when (selectedTab) {
                        StudentTab.HOME -> StudentHomeScreen(
                            user = user,
                            viewModel = studentViewModel,
                            onNavigateToMenu = { category ->
                                menuCategoryFilter = category
                                selectedTab = StudentTab.MENU
                            },
                            onNavigateToCart = { showCartSheet = true },
                            onNavigateToNotifications = { currentScreen = "notifications" },
                            onNavigateToOrders = { selectedTab = StudentTab.ORDERS },
                            onLogout = onLogout,
                            modifier = Modifier.padding(innerPadding)
                        )

                        StudentTab.MENU -> MenuScreen(
                            viewModel = studentViewModel,
                            initialCategory = menuCategoryFilter,
                            onNavigateToCart = { showCartSheet = true },
                            modifier = Modifier.padding(innerPadding)
                        )

                        StudentTab.ORDERS -> StudentOrdersScreen(
                            viewModel = studentViewModel,
                            modifier = Modifier.padding(innerPadding)
                        )

                        StudentTab.PROFILE -> StudentProfileScreen(
                            user = user,
                            viewModel = studentViewModel,
                            onLogout = onLogout,
                            onEditProfile = onEditProfile,
                            isDarkMode = isDarkMode,
                            onToggleDarkMode = onToggleDarkMode,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }

            // Floating Cart Bar
            AnimatedVisibility(
                visible = cartItemCount > 0 && currentScreen == "main" && !showCartSheet,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                Card(
                    onClick = { showCartSheet = true },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = EmeraldPrimary),
                    elevation = CardDefaults.cardElevation(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$cartItemCount Item(s) | ₹${cartTotal.toInt()}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = Color.White)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("View Cart", style = MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = Color.White))
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White)
                        }
                    }
                }
            }
        }
    }

    // Bottom Sheet for Cart
    if (showCartSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCartSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.background,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Box(modifier = Modifier.fillMaxHeight(0.9f)) {
                CartScreen(
                    viewModel = studentViewModel,
                    onNavigateToCheckout = {
                        showCartSheet = false
                        currentScreen = "checkout"
                    },
                    onNavigateToMenu = {
                        showCartSheet = false
                        selectedTab = StudentTab.MENU
                        currentScreen = "main"
                    }
                )
            }
        }
    }
}

@Composable
fun AdminAppContainer(
    adminUser: UserEntity,
    repository: HakeemRepository,
    onLogout: () -> Unit,
    isDarkMode: Boolean = false,
    onToggleDarkMode: () -> Unit = {}
) {
    val context = LocalContext.current
    val adminViewModel = remember(adminUser.id) { AdminViewModel(repository, adminUser) }
    var currentAdminScreen by remember { mutableStateOf("dashboard") }

    val adminMessage by adminViewModel.adminMessage.collectAsState()

    LaunchedEffect(adminMessage) {
        adminMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            adminViewModel.clearMessage()
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = EmeraldPrimary
            ) {
                NavigationBarItem(
                    selected = currentAdminScreen == "dashboard",
                    onClick = { currentAdminScreen = "dashboard" },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Overview") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = EmeraldPrimary,
                        selectedTextColor = EmeraldPrimary,
                        indicatorColor = EmeraldPrimary.copy(alpha = 0.15f)
                    )
                )
                NavigationBarItem(
                    selected = currentAdminScreen == "orders",
                    onClick = { currentAdminScreen = "orders" },
                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "Orders") },
                    label = { Text("Orders") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = EmeraldPrimary,
                        selectedTextColor = EmeraldPrimary,
                        indicatorColor = EmeraldPrimary.copy(alpha = 0.15f)
                    )
                )
                NavigationBarItem(
                    selected = currentAdminScreen == "products",
                    onClick = { currentAdminScreen = "products" },
                    icon = { Icon(Icons.Default.RestaurantMenu, contentDescription = "Products") },
                    label = { Text("Products") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = EmeraldPrimary,
                        selectedTextColor = EmeraldPrimary,
                        indicatorColor = EmeraldPrimary.copy(alpha = 0.15f)
                    )
                )
                NavigationBarItem(
                    selected = currentAdminScreen == "analytics",
                    onClick = { currentAdminScreen = "analytics" },
                    icon = { Icon(Icons.Default.Analytics, contentDescription = "Analytics") },
                    label = { Text("Analytics") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = EmeraldPrimary,
                        selectedTextColor = EmeraldPrimary,
                        indicatorColor = EmeraldPrimary.copy(alpha = 0.15f)
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentAdminScreen) {
                "orders" -> AdminOrdersScreen(
                    viewModel = adminViewModel,
                    onNavigateBack = { currentAdminScreen = "dashboard" }
                )

                "products" -> AdminProductsScreen(
                    viewModel = adminViewModel,
                    onNavigateBack = { currentAdminScreen = "dashboard" }
                )

                "ads" -> AdminAdvertisementsScreen(
                    viewModel = adminViewModel,
                    onNavigateBack = { currentAdminScreen = "dashboard" }
                )

                "analytics" -> AdminSalesAnalyticsScreen(
                    viewModel = adminViewModel,
                    onNavigateBack = { currentAdminScreen = "dashboard" }
                )

                "booking_settings" -> AdminBookingSettingsScreen(
                    viewModel = adminViewModel,
                    onNavigateBack = { currentAdminScreen = "dashboard" }
                )

                "audit_logs" -> AdminAuditLogsScreen(
                    viewModel = adminViewModel,
                    onNavigateBack = { currentAdminScreen = "dashboard" }
                )

                else -> AdminDashboardScreen(
                    adminUser = adminUser,
                    viewModel = adminViewModel,
                    onNavigateToOrders = { currentAdminScreen = "orders" },
                    onNavigateToProducts = { currentAdminScreen = "products" },
                    onNavigateToAds = { currentAdminScreen = "ads" },
                    onNavigateToAnalytics = { currentAdminScreen = "analytics" },
                    onNavigateToBookingSettings = { currentAdminScreen = "booking_settings" },
                    onNavigateToAuditLogs = { currentAdminScreen = "audit_logs" },
                    onLogout = onLogout,
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = onToggleDarkMode
                )
            }
        }
    }
}

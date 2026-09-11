package com.example.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.UserEntity
import com.example.domain.model.OrderWithItems
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.WarmOrange
import com.example.ui.viewmodel.AdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    adminUser: UserEntity,
    viewModel: AdminViewModel,
    onNavigateToOrders: () -> Unit,
    onNavigateToProducts: () -> Unit,
    onNavigateToAds: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToBookingSettings: () -> Unit,
    onNavigateToAuditLogs: () -> Unit,
    onLogout: () -> Unit,
    isDarkMode: Boolean = false,
    onToggleDarkMode: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val orders by viewModel.allOrders.collectAsState()
    val products by viewModel.allProducts.collectAsState()
    val bookingSettings by viewModel.bookingSettings.collectAsState()
    val analytics by viewModel.analyticsSummary.collectAsState()

    val activeOrders = remember(orders) {
        orders.filter { it.order.paymentMethod == "CASH" || it.order.paymentStatus == "PAID" }
    }
    val pendingCount = remember(activeOrders) { activeOrders.count { it.order.orderStatus == "PLACED" } }
    val processingCount = remember(activeOrders) { activeOrders.count { it.order.orderStatus in listOf("ACCEPTED", "PROCESSING") } }
    val readyCount = remember(activeOrders) { activeOrders.count { it.order.orderStatus == "READY" } }
    val deliveredCount = remember(activeOrders) { activeOrders.count { it.order.orderStatus == "DELIVERED" } }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Cafeteria Admin", fontWeight = FontWeight.Bold)
                        // Live Sync Indicator
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0x33FFFFFF)
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(modifier = Modifier.size(8.dp).background(Color(0xFF10B981), shape = RoundedCornerShape(12.dp)))
                                Text("Live Sync", style = MaterialTheme.typography.labelSmall, color = Color.White)
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EmeraldPrimary,
                    titleContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = onToggleDarkMode) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Dark/Light Mode",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Logout",
                            tint = Color.White
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(if (isDarkMode) Color(0xFF121212) else MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // 1. Operational Hero & Fast-Action Widgets
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkMode) Color(0xFF1E1E1E) else EmeraldPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Status Pill
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (bookingSettings?.isEmergencyClosed == true) MaterialTheme.colorScheme.error else Color(0x33FFFFFF)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (bookingSettings?.isEmergencyClosed == true) Icons.Default.Warning else Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (bookingSettings?.isEmergencyClosed == true) "EMERGENCY SHUTOFF ACTIVE" else "OPEN: ${bookingSettings?.openTime ?: "09:00"} - ${bookingSettings?.closeTime ?: "11:40"}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                                    color = Color.White
                                )
                            }
                        }

                        // Daily Revenue Hero
                        Column {
                            Text("Today's Gross Sales", style = MaterialTheme.typography.titleSmall, color = Color(0xCCFFFFFF))
                            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "₹${analytics?.grossSales?.toInt() ?: 0}",
                                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold, color = Color.White)
                                )
                                Text(
                                    text = "+${analytics?.totalOrders ?: 0} txns",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color(0xAAFFFFFF),
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                        }

                        // Fast Action Bar
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FastActionButton(
                                icon = Icons.Default.PauseCircleFilled, 
                                label = if(bookingSettings?.isEmergencyClosed == true) "Resume" else "Pause Orders", 
                                color = if(bookingSettings?.isEmergencyClosed == true) EmeraldPrimary else MaterialTheme.colorScheme.error,
                                onClick = onNavigateToBookingSettings,
                                modifier = Modifier.weight(1f)
                            )
                            FastActionButton(
                                icon = Icons.Default.Campaign, 
                                label = "Broadcast", 
                                color = WarmOrange,
                                onClick = onNavigateToAds,
                                modifier = Modifier.weight(1f)
                            )
                            FastActionButton(
                                icon = Icons.Default.Inventory, 
                                label = "Quick Stock", 
                                color = GoldAccent,
                                onClick = onNavigateToProducts,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // 2. Live Counter Metric Cards
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Kitchen Status", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        SemanticMetricCard(
                            label = "NEW ORDERS",
                            value = pendingCount.toString(),
                            accentColor = Color(0xFF3B82F6), // Blue
                            icon = Icons.Default.Receipt,
                            isDarkMode = isDarkMode,
                            onClick = onNavigateToOrders,
                            modifier = Modifier.weight(1f)
                        )
                        SemanticMetricCard(
                            label = "PROCESSING",
                            value = processingCount.toString(),
                            accentColor = Color(0xFFF59E0B), // Amber
                            icon = Icons.Default.SoupKitchen,
                            isDarkMode = isDarkMode,
                            onClick = onNavigateToOrders,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        SemanticMetricCard(
                            label = "READY PICKUP",
                            value = readyCount.toString(),
                            accentColor = Color(0xFF10B981), // Emerald
                            icon = Icons.Default.CheckCircle,
                            isDarkMode = isDarkMode,
                            onClick = onNavigateToOrders,
                            modifier = Modifier.weight(1f)
                        )
                        SemanticMetricCard(
                            label = "DELIVERED",
                            value = deliveredCount.toString(),
                            accentColor = Color(0xFF64748B), // Slate
                            icon = Icons.Default.DoneAll,
                            isDarkMode = isDarkMode,
                            onClick = onNavigateToOrders,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }


            // Bottom Spacing for Nav Bar
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun FastActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
        modifier = modifier
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(imageVector = icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(24.dp))
            Text(label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
        }
    }
}

@Composable
private fun SemanticMetricCard(
    label: String,
    value: String,
    accentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isDarkMode: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        modifier = modifier.clickable { onClick() }
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Left Accent Border
            Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(accentColor))
            
            Column(
                modifier = Modifier.padding(16.dp).weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp), color = accentColor)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = accentColor.copy(alpha = 0.1f)
                    ) {
                        Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.padding(6.dp).size(16.dp))
                    }
                }
                Text(value, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black, color = if(isDarkMode) Color.White else Color.Black))
            }
        }
    }
}

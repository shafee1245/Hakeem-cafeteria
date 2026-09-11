package com.example.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.OrderEntity
import com.example.data.local.parseToEpochMillis
import com.example.domain.model.OrderWithItems
import com.example.ui.components.StatusBadge
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.StatusReady
import com.example.ui.theme.WarmOrange
import com.example.ui.viewmodel.AdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminOrdersScreen(
    viewModel: AdminViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val orders by viewModel.allOrders.collectAsState()
    var selectedTab by remember { mutableStateOf(0) } // 0: NEW, 1: ACCEPTED, 2: DELIVERED, 3: REJECTED/CANCELLED
    var searchQuery by remember { mutableStateOf("") }
    var isKdsGridMode by remember { mutableStateOf(false) } // Stub for KDS view toggle

    val filteredOrders = remember(orders, selectedTab, searchQuery) {
        val targetStatus = when (selectedTab) {
            0 -> listOf("PLACED")
            1 -> listOf("ACCEPTED")
            else -> listOf("DELIVERED")
        }

        orders.filter { orderWithItems ->
            val order = orderWithItems.order
            
            // Critical fix: Only show CASH orders or PAID ONLINE orders.
            // Do not show PENDING ONLINE orders to the admin.
            val isPaidOrCash = order.paymentMethod == "CASH" || order.paymentStatus == "PAID"
            
            val matchesStatus = order.orderStatus in targetStatus && isPaidOrCash
            val matchesQuery = searchQuery.isBlank() ||
                    order.tokenNumber.contains(searchQuery, ignoreCase = true) ||
                    order.id.contains(searchQuery, ignoreCase = true) ||
                    (order.userRegisterNumber?.contains(searchQuery, ignoreCase = true) == true) ||
                    (order.userFullName?.contains(searchQuery, ignoreCase = true) == true)

            matchesStatus && matchesQuery
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("KDS & Order Flow", fontWeight = FontWeight.Bold)
                        // Pulse Badge Stub for New Orders
                        val newCount = orders.count { it.order.orderStatus == "PLACED" }
                        if (newCount > 0) {
                            Surface(color = MaterialTheme.colorScheme.error, shape = RoundedCornerShape(12.dp)) {
                                Text("$newCount NEW", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { isKdsGridMode = !isKdsGridMode }) {
                        Icon(Icons.Default.ViewModule, contentDescription = "Toggle Grid Mode", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EmeraldPrimary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search Token (e.g. A041) or Reg No...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Status Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = EmeraldPrimary
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("New (${orders.count { it.order.orderStatus == "PLACED" }})") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Accepted") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Delivered") })
            }

            if (filteredOrders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ConfirmationNumber,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No orders found in this queue",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredOrders, key = { it.order.id }) { orderWithItems ->
                        AdminOrderCard(
                            orderWithItems = orderWithItems,
                            onUpdateStatus = { nextStatus -> viewModel.updateOrderStatus(orderWithItems.order.id, nextStatus) }
                        )
                    }
                }
            }
        }
    }


}

@Composable
private fun AdminOrderCard(
    orderWithItems: OrderWithItems,
    onUpdateStatus: (String) -> Unit
) {
    val order = orderWithItems.order
    val items = orderWithItems.items
    
    val elapsedMillis = System.currentTimeMillis() - (order.createdAt.parseToEpochMillis() ?: System.currentTimeMillis())
    val elapsedMinutes = (elapsedMillis / 60000).coerceAtLeast(0)
    
    val timerColor = when {
        elapsedMinutes > 15 && order.orderStatus in listOf("PLACED", "ACCEPTED") -> MaterialTheme.colorScheme.error
        elapsedMinutes > 10 && order.orderStatus in listOf("PLACED", "ACCEPTED") -> WarmOrange
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = EmeraldPrimary,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "TOKEN ${order.tokenNumber}",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, color = Color.White),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (order.orderStatus !in listOf("READY", "DELIVERED", "REJECTED", "CANCELLED")) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.Timer, contentDescription = null, tint = timerColor, modifier = Modifier.size(16.dp))
                            Text("${elapsedMinutes}m", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = timerColor)
                        }
                    }
                    StatusBadge(status = order.orderStatus)
                }
            }

            Column {
                Text(
                    text = "${order.userFullName} (${order.userRegisterNumber})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Order ID: ${order.id} • Payment: ${order.paymentMethod}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Inline Items Breakdown
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Order Items", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (items.isEmpty()) {
                         Text("- No items found", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        items.forEach { item ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${item.quantity}x ${item.productName}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                Text("₹${((item.productPrice ?: 0.0) * item.quantity).toInt()}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total: ₹${order.total.toInt()}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold, color = EmeraldPrimary)
                )

                // Quick Actions based on state
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    when (order.orderStatus) {
                        "PLACED" -> {
                            Button(onClick = { onUpdateStatus("ACCEPTED") }, colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)) {
                                Text("Accept")
                            }
                        }
                        "ACCEPTED" -> {
                            Button(onClick = { onUpdateStatus("DELIVERED") }, colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)) {
                                Text("Complete (Handed Over)")
                            }
                        }
                    }
                }
            }
        }
    }
}

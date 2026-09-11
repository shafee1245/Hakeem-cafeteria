package com.example.ui.screens.admin

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.OrderEntity
import com.example.data.local.parseToEpochMillis
import com.example.domain.model.OrderWithItems
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.WarmOrange
import com.example.ui.viewmodel.AdminViewModel
import java.text.SimpleDateFormat
import java.util.*

enum class TimeframePeriod(val label: String) {
    DAILY("Daily Analysis"),
    WEEKLY("Weekly Analysis"),
    MONTHLY("Monthly Analysis"),
    ALL_TIME("All Time")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSalesAnalyticsScreen(
    viewModel: AdminViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allOrdersWithItems by viewModel.allOrders.collectAsState()
    val allOrders = remember(allOrdersWithItems) { allOrdersWithItems.map { it.order } }
    var selectedTimeframe by remember { mutableStateOf(TimeframePeriod.DAILY) }

    // Filter orders based on selected timeframe
    val now = remember { System.currentTimeMillis() }
    val filteredOrders = remember(allOrders, selectedTimeframe) {
        val calendar = Calendar.getInstance()
        when (selectedTimeframe) {
            TimeframePeriod.DAILY -> {
                calendar.apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val startOfDay = calendar.timeInMillis
                allOrders.filter { (it.createdAt.parseToEpochMillis() ?: 0L) >= startOfDay }
            }
            TimeframePeriod.WEEKLY -> {
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                val startOfWeek = calendar.timeInMillis
                allOrders.filter { (it.createdAt.parseToEpochMillis() ?: 0L) >= startOfWeek }
            }
            TimeframePeriod.MONTHLY -> {
                calendar.add(Calendar.DAY_OF_YEAR, -30)
                val startOfMonth = calendar.timeInMillis
                allOrders.filter { (it.createdAt.parseToEpochMillis() ?: 0L) >= startOfMonth }
            }
            TimeframePeriod.ALL_TIME -> allOrders
        }
    }

    // Calculated Statistics
    val grossSales = remember(filteredOrders) {
        filteredOrders.filter { it.orderStatus != "CANCELLED" && it.orderStatus != "REJECTED" }.sumOf { it.total }
    }
    val refunds = remember(filteredOrders) {
        filteredOrders.filter { it.orderStatus == "CANCELLED" || it.orderStatus == "REJECTED" }.sumOf { it.total }
    }
    val netSales = grossSales - refunds
    val totalOrdersCount = filteredOrders.size
    val deliveredCount = filteredOrders.count { it.orderStatus == "DELIVERED" }
    val activeCount = filteredOrders.count { it.orderStatus in listOf("PLACED", "ACCEPTED", "PROCESSING", "READY") }
    val cancelledCount = filteredOrders.count { it.orderStatus in listOf("CANCELLED", "REJECTED") }
    val averageOrderValue = if (totalOrdersCount > 0) grossSales / totalOrdersCount else 0.0


    // Peak booking hours calculation
    val hourlyDistribution = remember(filteredOrders) {
        val hourMap = IntArray(24) { 0 }
        val cal = Calendar.getInstance()
        filteredOrders.forEach { order ->
            cal.timeInMillis = order.createdAt.parseToEpochMillis() ?: 0L
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            hourMap[hour]++
        }
        hourMap
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cafeteria Analytics & Reports", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EmeraldPrimary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Timeframe Selector Tabs (Daily, Weekly, Monthly, All Time)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TimeframePeriod.entries.forEach { period ->
                            val isSelected = selectedTimeframe == period
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) EmeraldPrimary else Color.Transparent)
                                    .clickable { selectedTimeframe = period }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = period.label.replace(" Analysis", ""),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Primary Revenue Header Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = EmeraldPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${selectedTimeframe.label.uppercase()} GROSS REVENUE",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = GoldAccent
                            )
                            Surface(
                                color = Color.White.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text(
                                    text = "$totalOrdersCount Orders",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Text(
                            text = "₹${grossSales.toInt()}",
                            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Black),
                            color = Color.White
                        )

                        HorizontalDivider(color = Color.White.copy(alpha = 0.3f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Net Sales (After Refunds)", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                                Text("₹${netSales.toInt()}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
                            }

                            Column {
                                Text("Avg. Order Value", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                                Text("₹${averageOrderValue.toInt()}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color.White))
                            }
                        }
                    }
                }
            }

            // Key Metrics Grid
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        MetricCard(
                            title = "TOTAL ORDERS",
                            value = totalOrdersCount.toString(),
                            subtitle = "In selected period",
                            icon = Icons.Default.ReceiptLong,
                            accentColor = EmeraldPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = "DELIVERED",
                            value = deliveredCount.toString(),
                            subtitle = if (totalOrdersCount > 0) "${(deliveredCount * 100 / totalOrdersCount)}% completion" else "0%",
                            icon = Icons.Default.CheckCircle,
                            accentColor = Color(0xFF16A34A),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        MetricCard(
                            title = "IN KITCHEN / READY",
                            value = activeCount.toString(),
                            subtitle = "Live active orders",
                            icon = Icons.Default.SoupKitchen,
                            accentColor = WarmOrange,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = "REFUNDED / CANCELLED",
                            value = cancelledCount.toString(),
                            subtitle = "₹${refunds.toInt()} refunded",
                            icon = Icons.Default.Cancel,
                            accentColor = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Sales Trend Bar Chart Visualizer
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = EmeraldPrimary)
                                Text("Sales Trend Distribution", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            }
                            Text(
                                text = selectedTimeframe.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        HorizontalDivider()

                        // Visual Bar Chart representation based on timeframe
                        TimeframeBarChart(
                            orders = filteredOrders,
                            timeframe = selectedTimeframe
                        )
                    }
                }
            }

            // Peak Booking / Ordering Hours Breakdown
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, tint = EmeraldPrimary)
                            Text("Peak Order Time Slots", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        }

                        HorizontalDivider()

                        val morningCount = hourlyDistribution.slice(8..11).sum()
                        val lunchCount = hourlyDistribution.slice(12..14).sum()
                        val afternoonCount = hourlyDistribution.slice(15..17).sum()
                        val eveningCount = hourlyDistribution.slice(18..21).sum()
                        val maxSlot = maxOf(morningCount, lunchCount, afternoonCount, eveningCount, 1)

                        PeakSlotRow(label = "Morning Slot (08:00 AM - 11:59 AM)", count = morningCount, maxCount = maxSlot)
                        PeakSlotRow(label = "Lunch Peak (12:00 PM - 02:59 PM)", count = lunchCount, maxCount = maxSlot, isPeak = lunchCount == maxSlot && maxSlot > 0)
                        PeakSlotRow(label = "Afternoon Snacks (03:00 PM - 05:59 PM)", count = afternoonCount, maxCount = maxSlot)
                        PeakSlotRow(label = "Evening Dinner (06:00 PM - 09:00 PM)", count = eveningCount, maxCount = maxSlot)
                    }
                }
            }


        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
            }

            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, color = accentColor)
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PeakSlotRow(
    label: String,
    count: Int,
    maxCount: Int,
    isPeak: Boolean = false
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isPeak) FontWeight.Bold else FontWeight.Medium,
                        color = if (isPeak) EmeraldPrimary else MaterialTheme.colorScheme.onSurface
                    )
                )
                if (isPeak) {
                    Surface(
                        color = WarmOrange,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "PEAK",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Text(
                text = "$count orders",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
            )
        }

        val progress = if (maxCount > 0) count.toFloat() / maxCount else 0f
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = if (isPeak) WarmOrange else EmeraldPrimary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
private fun TimeframeBarChart(
    orders: List<OrderEntity>,
    timeframe: TimeframePeriod
) {
    val labels = when (timeframe) {
        TimeframePeriod.DAILY -> listOf("8-10 AM", "10-12 PM", "12-2 PM", "2-4 PM", "4-6 PM", "6-8 PM")
        TimeframePeriod.WEEKLY -> listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        TimeframePeriod.MONTHLY -> listOf("Wk 1", "Wk 2", "Wk 3", "Wk 4")
        TimeframePeriod.ALL_TIME -> listOf("Q1", "Q2", "Q3", "Q4")
    }

    // Group sales into buckets
    val bucketSales = remember(orders, timeframe) {
        DoubleArray(labels.size) { 0.0 }
    }

    val cal = Calendar.getInstance()
    orders.filter { it.orderStatus != "CANCELLED" && it.orderStatus != "REJECTED" }.forEach { order ->
        cal.timeInMillis = order.createdAt.parseToEpochMillis() ?: 0L
        when (timeframe) {
            TimeframePeriod.DAILY -> {
                val hour = cal.get(Calendar.HOUR_OF_DAY)
                val idx = when (hour) {
                    in 8..9 -> 0
                    in 10..11 -> 1
                    in 12..13 -> 2
                    in 14..15 -> 3
                    in 16..17 -> 4
                    in 18..20 -> 5
                    else -> 2 // default to lunch
                }
                bucketSales[idx] += order.total
            }
            TimeframePeriod.WEEKLY -> {
                val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1 = Sun, 2 = Mon...
                val idx = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - 2
                if (idx in bucketSales.indices) {
                    bucketSales[idx] += order.total
                }
            }
            TimeframePeriod.MONTHLY -> {
                val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
                val idx = ((dayOfMonth - 1) / 7).coerceIn(0, 3)
                if (idx in bucketSales.indices) {
                    bucketSales[idx] += order.total
                }
            }
            TimeframePeriod.ALL_TIME -> {
                val month = cal.get(Calendar.MONTH)
                val idx = (month / 3).coerceIn(0, 3)
                if (idx in bucketSales.indices) {
                    bucketSales[idx] += order.total
                }
            }
        }
    }

    val maxSales = bucketSales.maxOrNull()?.coerceAtLeast(100.0) ?: 100.0

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(top = 16.dp, bottom = 8.dp)
    ) {
        // Horizontal Gridlines
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), thickness = 2.dp) // Zero Baseline
        }

        // Bars
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 20.dp), // Space for labels
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            labels.forEachIndexed { index, label ->
                val sales = bucketSales[index]
                val heightFraction = (sales / maxSales).toFloat().coerceIn(0.02f, 1f)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    if (sales > 0) {
                        Text(
                            text = "₹${sales.toInt()}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    Box(
                        modifier = Modifier
                            .width(26.dp)
                            .fillMaxHeight(heightFraction)
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(if (sales > 0) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    )
                }
            }
        }

        // Labels at the bottom
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            labels.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(36.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}


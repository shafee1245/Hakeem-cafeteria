package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.OrderEntity
import com.example.data.local.OrderItemEntity
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.StatusReady

@Composable
fun TokenCard(
    order: OrderEntity,
    items: List<OrderItemEntity> = emptyList(),
    modifier: Modifier = Modifier
) {
    val isReady = order.orderStatus == "READY"
    val isDelivered = order.orderStatus == "DELIVERED"
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isReady) StatusReady else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Token Number Box
            Surface(
                color = if (isReady) Color.White else EmeraldPrimary,
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 4.dp
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "DIGITAL PICKUP TOKEN",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (isReady) EmeraldPrimary else Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                        text = order.tokenNumber,
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = if (isReady) EmeraldPrimary else GoldAccent
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Product Details (Replaces Order ID)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                items.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${item.quantity}x ${item.productName}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isReady) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "₹${(item.productPrice ?: 0.0) * item.quantity}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isReady) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                
                if (items.isEmpty()) {
                    Text(
                        text = "Order ID: ${order.id}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isReady) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Ready Banner Alert
            if (isReady) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restaurant,
                            contentDescription = null,
                            tint = StatusReady
                        )
                        Text(
                            text = "YOUR FOOD IS READY! Present Token ${order.tokenNumber} at the counter.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = StatusReady
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Status Badge Step Timeline
            OrderStatusTimeline(currentStatus = order.orderStatus, isCardReady = isReady)
        }
    }
}

@Composable
fun OrderStatusTimeline(currentStatus: String, isCardReady: Boolean = false) {
    val steps = listOf("PLACED", "ACCEPTED", "DELIVERED")
    val currentIndex = steps.indexOf(currentStatus).let { if (it == -1) 0 else it }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, step ->
            val isPassed = index <= currentIndex
            val isCurrent = index == currentIndex

            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val scale by if (isCurrent && step == "DELIVERED") {
                infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.3f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "scale"
                )
            } else {
                remember { mutableStateOf(1f) }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .scale(scale)
                        .clip(CircleShape)
                        .background(
                            when {
                                isCurrent -> GoldAccent
                                isPassed -> if (isCardReady) Color.White else EmeraldPrimary
                                else -> if (isCardReady) Color.White.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isPassed) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (isCurrent) Color.White else if (isCardReady) StatusReady else Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = when(step) {
                        "PLACED" -> "Placed"
                        "ACCEPTED" -> "Accepted"
                        "DELIVERED" -> "Collected"
                        else -> step
                    },
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                    ),
                    color = if (isCardReady) Color.White else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

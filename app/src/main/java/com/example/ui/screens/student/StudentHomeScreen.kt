package com.example.ui.screens.student

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.example.data.local.UserEntity
import com.example.ui.components.AdvertisementCarousel
import com.example.ui.components.BookingCountdownCard
import com.example.ui.components.ProductItemCard
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.WarmOrange
import androidx.compose.ui.platform.LocalUriHandler
import com.example.ui.viewmodel.StudentViewModel

@Composable
fun StudentHomeScreen(
    user: UserEntity,
    viewModel: StudentViewModel,
    onNavigateToMenu: (String?) -> Unit,
    onNavigateToCart: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToOrders: () -> Unit,
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val bookingStatus by viewModel.bookingStatus.collectAsState()
    val isBookingOpen = bookingStatus.isOpen
    val activeAds by viewModel.activeAds.collectAsState()
    val activeProducts by viewModel.activeProducts.collectAsState()
    val activeCategories by viewModel.activeCategories.collectAsState()
    val cartDisplayItems by viewModel.cartDisplayItems.collectAsState()
    val cartItemCount by viewModel.cartItemCount.collectAsState()

    val featuredProducts = remember(activeProducts) {
        activeProducts.filter { it.isFeatured }
    }
    val popularProducts = remember(activeProducts) {
        activeProducts.take(6)
    }

    val cartQtyMap = remember(cartDisplayItems) {
        cartDisplayItems.associate { it.product.id to it.quantity }
    }
    
    val uriHandler = LocalUriHandler.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // 1. Header Bar
        item {
            Surface(
                color = EmeraldPrimary.copy(alpha = 0.95f),
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "HAKEEM CAFETERIA",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    letterSpacing = 1.5.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = WarmOrange
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Good day, ${user.fullName.split(" ").firstOrNull() ?: "Student"} 👋",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(
                                onClick = onNavigateToNotifications,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Notifications",
                                    tint = Color.White
                                )
                            }

                            IconButton(
                                onClick = onNavigateToCart,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(WarmOrange)
                            ) {
                                BadgedBox(
                                    badge = {
                                        if (cartItemCount > 0) {
                                            Badge(containerColor = Color.White) {
                                                Text(text = cartItemCount.toString(), color = WarmOrange, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ShoppingCart,
                                        contentDescription = "Cart",
                                        tint = Color.White
                                    )
                                }
                            }

                            // Logout button removed as per requirements
                        }
                    }
                }
            }
        }

        // 2. Booking Countdown Status Banner
        item {
            BookingCountdownCard(
                status = bookingStatus,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
            )
        }

        // 3. Advertisements Carousel Banner
        item {
            AdvertisementCarousel(
                ads = activeAds,
                onAdClick = { ad ->
                    viewModel.recordAdClick(ad.id)
                    if (!ad.redirectUrl.isNullOrBlank()) {
                        try {
                            val url = ad.redirectUrl.trim()
                            val validUrl = if (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) {
                                "https://$url"
                            } else {
                                url
                            }
                            uriHandler.openUri(validUrl)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    } else {
                        onNavigateToMenu(null)
                    }
                }
            )
        }

        // 4. Quick Category Chips Row
        item {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                Text(
                    text = "Food Categories",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = true,
                            onClick = { onNavigateToMenu(null) },
                            label = { Text("All Menu") },
                            leadingIcon = { Icon(Icons.Default.RestaurantMenu, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }
                    items(activeCategories) { cat ->
                        FilterChip(
                            selected = false,
                            onClick = { onNavigateToMenu(cat.name) },
                            label = { Text(cat.name) }
                        )
                    }
                }
            }
        }

        // 5. Today's Specials Header
        if (featuredProducts.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = WarmOrange, modifier = Modifier.size(20.dp))
                        Text(
                            text = "Today's Specials",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    Text(
                        text = "View All",
                        style = MaterialTheme.typography.labelMedium.copy(color = EmeraldPrimary, fontWeight = FontWeight.Bold),
                        modifier = Modifier.clickable { onNavigateToMenu(null) }
                    )
                }
            }

            items(featuredProducts, key = { "feat_" + it.id }) { product ->
                ProductItemCard(
                    product = product,
                    cartQuantity = cartQtyMap[product.id] ?: 0,
                    isBookingOpen = isBookingOpen,
                    onAddToCart = { viewModel.addToCart(product.id, 1) },
                    onUpdateQuantity = { newQty -> viewModel.updateCartQuantity(product.id, newQty) },
                    onProductClick = { onNavigateToMenu(null) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }

        // 6. Popular Menu Items
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Popular Pre-Order Items",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        items(popularProducts, key = { "pop_" + it.id }) { product ->
            ProductItemCard(
                product = product,
                cartQuantity = cartQtyMap[product.id] ?: 0,
                isBookingOpen = isBookingOpen,
                onAddToCart = { viewModel.addToCart(product.id, 1) },
                onUpdateQuantity = { newQty -> viewModel.updateCartQuantity(product.id, newQty) },
                onProductClick = { onNavigateToMenu(null) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
    }
}

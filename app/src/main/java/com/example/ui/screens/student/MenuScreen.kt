package com.example.ui.screens.student

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.ProductItemCard
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.NonVegRed
import com.example.ui.theme.VegGreen
import com.example.ui.viewmodel.StudentViewModel

@Composable
fun MenuScreen(
    viewModel: StudentViewModel,
    initialCategory: String? = null,
    onNavigateToCart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categories by viewModel.activeCategories.collectAsState()
    val filteredProducts by viewModel.filteredProducts.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val vegOnlyFilter by viewModel.vegOnlyFilter.collectAsState()
    val cartDisplayItems by viewModel.cartDisplayItems.collectAsState()
    val bookingStatus by viewModel.bookingStatus.collectAsState()
    val isBookingOpen = bookingStatus.isOpen

    val cartQtyMap = remember(cartDisplayItems) {
        cartDisplayItems.associate { it.product.id to it.quantity }
    }

    LaunchedEffect(initialCategory) {
        if (!initialCategory.isNullOrBlank()) {
            viewModel.selectedCategory.value = initialCategory
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Search & Filter Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Cafeteria Menu",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = EmeraldPrimary
                )

                // Persistent Search Bar for filtering food items by name or category
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.searchQuery.value = it },
                    placeholder = { Text("Search food item or category (e.g., Biryani, Drinks)...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Icon") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Merged Filter & Category Row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        FilterChip(
                            selected = vegOnlyFilter == null,
                            onClick = { viewModel.vegOnlyFilter.value = null },
                            label = { Text("All Types") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = vegOnlyFilter == true,
                            onClick = { viewModel.vegOnlyFilter.value = true },
                            label = { Text("Pure Veg") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = VegGreen.copy(alpha = 0.2f),
                                selectedLabelColor = VegGreen
                            )
                        )
                    }
                    item {
                        FilterChip(
                            selected = vegOnlyFilter == false,
                            onClick = { viewModel.vegOnlyFilter.value = false },
                            label = { Text("Non-Veg") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NonVegRed.copy(alpha = 0.2f),
                                selectedLabelColor = NonVegRed
                            )
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.width(4.dp))
                        Divider(
                            modifier = Modifier
                                .height(24.dp)
                                .width(1.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    item {
                        FilterChip(
                            selected = selectedCategory == "ALL",
                            onClick = { viewModel.selectedCategory.value = "ALL" },
                            label = { Text("All Categories") }
                        )
                    }
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCategory.equals(cat.name, ignoreCase = true),
                            onClick = { viewModel.selectedCategory.value = cat.name },
                            label = { Text(cat.name) }
                        )
                    }
                }
            }
        }

        // Food Items List
        if (filteredProducts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) "No dishes found for '$searchQuery' — try browsing other categories." else "No dishes found for selected filters.",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredProducts, key = { it.id }) { product ->
                    ProductItemCard(
                        product = product,
                        cartQuantity = cartQtyMap[product.id] ?: 0,
                        isBookingOpen = isBookingOpen,
                        onAddToCart = { viewModel.addToCart(product.id, 1) },
                        onUpdateQuantity = { newQty -> viewModel.updateCartQuantity(product.id, newQty) },
                        onProductClick = { /* Can show detail modal if needed */ }
                    )
                }
            }
        }
    }
}

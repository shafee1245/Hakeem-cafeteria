package com.example.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.local.ProductEntity
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.StatusReady
import com.example.ui.viewmodel.AdminViewModel
import java.util.UUID
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.utils.ImageOptimizer
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminProductsScreen(
    viewModel: AdminViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val products by viewModel.allProducts.collectAsState()
    val categories by viewModel.allCategories.collectAsState()

    var showProductDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var productToDelete by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Food Catalog & Stock", fontWeight = FontWeight.Bold) },
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
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    editingProduct = null
                    showProductDialog = true
                },
                containerColor = EmeraldPrimary,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = "Add Item") },
                text = { Text("New Product", fontWeight = FontWeight.Bold) }
            )
        }
    ) { innerPadding ->
        if (products.isEmpty()) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.RestaurantMenu,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No products found",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Tap the + button to add your first menu item",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(products, key = { it.id }) { product ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Image Thumbnail
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(product.imageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = product.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.LightGray)
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(product.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            Text("₹${product.price.toInt()} • ${product.category}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            
                            // Fast Availability Toggle
                            var optimisticAvailability by remember(product.availability) { mutableStateOf(product.availability) }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Switch(
                                    checked = optimisticAvailability == "AVAILABLE",
                                    onCheckedChange = { isAvailable ->
                                        if (optimisticAvailability != "INACTIVE") {
                                            val newState = if (isAvailable) "AVAILABLE" else "OUT_OF_STOCK"
                                            optimisticAvailability = newState // Optimistic UI update
                                            viewModel.saveProduct(product.copy(availability = newState))
                                        }
                                    },
                                    enabled = optimisticAvailability != "INACTIVE",
                                    modifier = Modifier.scale(0.8f) // Make switch slightly smaller
                                )
                                Text(
                                    text = when (optimisticAvailability) {
                                        "AVAILABLE" -> "Available"
                                        "OUT_OF_STOCK" -> "Sold Out"
                                        "INACTIVE" -> "Archived"
                                        else -> optimisticAvailability
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = when (optimisticAvailability) {
                                        "AVAILABLE" -> StatusReady
                                        "INACTIVE" -> Color.Gray
                                        else -> MaterialTheme.colorScheme.error
                                    }
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(onClick = {
                                editingProduct = product
                                showProductDialog = true
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = EmeraldPrimary)
                            }
                            IconButton(onClick = { productToDelete = product.id }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
        }
    }

    // Delete Confirmation Dialog
    if (productToDelete != null) {
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to delete this product? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProduct(productToDelete!!)
                        productToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { productToDelete = null }) { Text("Cancel") }
            }
        )
    }

    if (showProductDialog) {
        ProductFormDialog(
            existing = editingProduct,
            categories = categories.map { it.name },
            onDismiss = { showProductDialog = false },
            onSave = { newProd, imageBytes ->
                viewModel.saveProductWithImage(newProd, imageBytes, editingProduct?.imageUrl) { success ->
                    if (success) {
                        showProductDialog = false
                    }
                }
            }
        )
    }
}

@Composable
private fun ProductFormDialog(
    existing: ProductEntity?,
    categories: List<String>,
    onDismiss: () -> Unit,
    onSave: (ProductEntity, ByteArray?) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isUploading by remember { mutableStateOf(false) }
    
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var description by remember { mutableStateOf(existing?.description ?: "") }
    var category by remember { mutableStateOf(existing?.category ?: (categories.firstOrNull() ?: "Meals")) }
    var priceStr by remember { mutableStateOf(existing?.price?.toString()?.removeSuffix(".0") ?: "100") }
    var isVeg by remember { mutableStateOf(existing?.isVeg ?: true) }
    var imageUrl by remember { mutableStateOf(existing?.imageUrl ?: "https://images.unsplash.com/photo-1589301760014-d929f3979dbc?w=500") }
    
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add New Food Item" else "Edit Food Item", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Image Dropzone
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .clickable { imagePickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isUploading) {
                            CircularProgressIndicator(color = EmeraldPrimary)
                        } else {
                            if (selectedImageUri != null) {
                                AsyncImage(
                                    model = selectedImageUri,
                                    contentDescription = "Selected Image",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else if (imageUrl.startsWith("http") && !imageUrl.contains("unsplash.com")) {
                                AsyncImage(
                                    model = imageUrl,
                                    contentDescription = "Uploaded Image",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.CloudUpload, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(32.dp))
                                    Text("Tap to upload image", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Food Name") }, modifier = Modifier.fillMaxWidth())
                }
                
                item {
                    OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                }

                item {
                    OutlinedTextField(
                        value = priceStr, 
                        onValueChange = { priceStr = it.filter { char -> char.isDigit() || char == '.' } }, 
                        label = { Text("Price (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isVeg, onCheckedChange = { isVeg = it }, colors = CheckboxDefaults.colors(checkedColor = EmeraldPrimary))
                        Text("Pure Vegetarian Item")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isUploading) return@Button
                    
                    coroutineScope.launch {
                        isUploading = true
                        var imageBytes: ByteArray? = null
                        if (selectedImageUri != null) {
                            imageBytes = withContext(Dispatchers.IO) {
                                ImageOptimizer.optimizeImageUri(context, selectedImageUri!!)
                            }
                            if (imageBytes == null) {
                                Toast.makeText(context, "Invalid image format or size.", Toast.LENGTH_SHORT).show()
                                isUploading = false
                                return@launch
                            }
                        }

                        val availability = existing?.availability ?: "AVAILABLE"

                        val prod = ProductEntity(
                            id = existing?.id ?: "",
                            name = name.ifBlank { "New Item" },
                            description = description,
                            category = category,
                            price = priceStr.toDoubleOrNull() ?: 100.0,
                            isVeg = isVeg,
                            availability = availability,
                            imageUrl = imageUrl,
                            isFeatured = existing?.isFeatured ?: false
                        )
                        
                        onSave(prod, imageBytes)
                        // Note: isUploading will remain true while dialog closes or error shows via viewModel.
                        // Ideally we would reset it on failure, but the dialog might just stay open and we can rely on ViewModel state if needed.
                        // Let's reset it after a short delay just in case.
                        kotlinx.coroutines.delay(2000)
                        isUploading = false
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("Save Item")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    )
}

@Composable
private fun NumericStepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    step: Int = 1,
    minValue: Int = 0
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconButton(
            onClick = { if (value - step >= minValue) onValueChange(value - step) },
            modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
        ) {
            Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(16.dp))
        }
        
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.widthIn(min = 40.dp),
            textAlign = TextAlign.Center
        )

        IconButton(
            onClick = { onValueChange(value + step) },
            modifier = Modifier.size(32.dp).background(EmeraldPrimary.copy(alpha = 0.1f), CircleShape)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Increase", tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
        }
    }
}

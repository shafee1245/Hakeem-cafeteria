package com.example.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import com.example.data.local.AdvertisementEntity
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.viewmodel.AdminViewModel
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAdvertisementsScreen(
    viewModel: AdminViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ads by viewModel.allAds.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sponsored Campaigns", fontWeight = FontWeight.Bold) },
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
                onClick = { showAddDialog = true },
                containerColor = EmeraldPrimary,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = "New Campaign") },
                text = { Text("Create Campaign", fontWeight = FontWeight.Bold) }
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
            items(ads, key = { it.id }) { ad ->
                val ctr = if (ad.impressions > 0) (ad.clicks.toDouble() / ad.impressions * 100) else 0.0
                val ctrFormatted = String.format("%.1f%%", ctr)

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        // Banner Preview
                        Box(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                            AsyncImage(
                                model = ad.bannerUrl,
                                contentDescription = ad.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().background(Color.LightGray)
                            )
                            
                            // Active Badge
                            Surface(
                                color = if (ad.isActive) EmeraldPrimary else MaterialTheme.colorScheme.error,
                                shape = RoundedCornerShape(bottomStart = 12.dp),
                                modifier = Modifier.align(Alignment.TopEnd)
                            ) {
                                Text(
                                    text = if (ad.isActive) "ACTIVE" else "INACTIVE",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(ad.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                    Text("By ${ad.companyName ?: ""}", style = MaterialTheme.typography.labelMedium, color = EmeraldPrimary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(ad.description ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                IconButton(onClick = { viewModel.deleteAdvertisement(ad.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }

                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(ad.impressions.toString(), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                        Text("Views", style = MaterialTheme.typography.labelSmall)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(ad.clicks.toString(), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                        Text("Clicks", style = MaterialTheme.typography.labelSmall)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(ctrFormatted, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = EmeraldPrimary))
                                        Text("CTR", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }

                            // Activate / Deactivate Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Campaign Status", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                                Switch(
                                    checked = ad.isActive,
                                    onCheckedChange = { isActive ->
                                        viewModel.saveAdvertisement(ad.copy(isActive = isActive))
                                    },
                                    colors = SwitchDefaults.colors(checkedTrackColor = EmeraldPrimary)
                                )
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    if (showAddDialog) {
        AddAdDialog(
            onDismiss = { showAddDialog = false },
            onSave = { newAd ->
                viewModel.saveAdvertisement(newAd)
                showAddDialog = false
            },
            onUploadImage = { bytes, name -> viewModel.uploadImage(bytes, name) }
        )
    }
}

@Composable
private fun AddAdDialog(
    onDismiss: () -> Unit,
    onSave: (AdvertisementEntity) -> Unit,
    onUploadImage: suspend (ByteArray, String) -> Result<String>
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isUploading by remember { mutableStateOf(false) }

    var companyName by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var bannerUrl by remember { mutableStateOf("https://images.unsplash.com/photo-1513104890138-7c749659a591?w=600") }
    var ctaText by remember { mutableStateOf("Pre-Order Special Offer") }
    var redirectUrl by remember { mutableStateOf("") }
    var durationDays by remember { mutableStateOf("7") }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch {
                isUploading = true
                try {
                    val bytes = context.contentResolver.openInputStream(it)?.readBytes()
                    if (bytes != null) {
                        val fileName = "ad_${System.currentTimeMillis()}.jpg"
                        val result = onUploadImage(bytes, fileName)
                        result.onSuccess { url ->
                            bannerUrl = url
                        }.onFailure { e ->
                            android.widget.Toast.makeText(context, "Image upload failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    isUploading = false
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Campaign Setup", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .clickable { imagePickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isUploading) {
                            CircularProgressIndicator(color = EmeraldPrimary)
                        } else if (bannerUrl != "https://images.unsplash.com/photo-1513104890138-7c749659a591?w=600") {
                            AsyncImage(
                                model = bannerUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Image, contentDescription = null, tint = EmeraldPrimary)
                                Text("Upload Banner Graphic (16:9)", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                item { OutlinedTextField(value = companyName, onValueChange = { companyName = it }, label = { Text("Brand / Sponsor Name") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Campaign Headline") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Offer Details / Subtitle") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = ctaText, onValueChange = { ctaText = it }, label = { Text("Action Button Text") }, modifier = Modifier.fillMaxWidth()) }
                item { OutlinedTextField(value = redirectUrl, onValueChange = { redirectUrl = it }, label = { Text("Redirect URL (Optional)") }, modifier = Modifier.fillMaxWidth()) }
                
                // Simplified UI stub for duration
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Duration (Days)", style = MaterialTheme.typography.titleSmall)
                        OutlinedTextField(
                            value = durationDays,
                            onValueChange = { durationDays = it },
                            modifier = Modifier.width(100.dp),
                            singleLine = true
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val days = durationDays.toIntOrNull() ?: 7
                    val ad = AdvertisementEntity(
                        id = UUID.randomUUID().toString(),
                        companyName = companyName.ifBlank { "Sponsor Name" },
                        title = title.ifBlank { "Special Promotion" },
                        description = description,
                        bannerUrl = bannerUrl,
                        ctaText = ctaText,
                        redirectUrl = redirectUrl,
                        startDate = System.currentTimeMillis(),
                        endDate = System.currentTimeMillis() + (days * 86400000L),
                        isActive = true
                    )
                    onSave(ad)
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) { Text("Launch Campaign") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    )
}

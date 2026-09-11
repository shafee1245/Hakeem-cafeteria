package com.example.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.scale
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.example.data.local.ProductEntity
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.NonVegRed
import com.example.ui.theme.VegGreen

@Composable
fun ProductItemCard(
    product: ProductEntity,
    cartQuantity: Int,
    isBookingOpen: Boolean = true,
    onAddToCart: () -> Unit,
    onUpdateQuantity: (Int) -> Unit,
    onProductClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isAvailable = product.availability != "OUT_OF_STOCK" && product.availability != "INACTIVE"


    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onProductClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Product Image with Veg/Non-Veg Badge
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(product.imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = product.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().aspectRatio(1f)
                )

                // Veg/NonVeg Dot Badge
                Box(
                    modifier = Modifier
                        .padding(6.dp)
                        .size(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .border(
                            width = 1.5.dp,
                            color = if (product.isVeg) VegGreen else NonVegRed,
                            shape = RoundedCornerShape(4.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .border(width = 0.dp, color = Color.Transparent, shape = CircleShape)
                    ) {
                        Surface(color = if (product.isVeg) VegGreen else NonVegRed) {}
                    }
                }
            }

            // Info Column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = product.description ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = "₹${product.price.toInt()}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = EmeraldPrimary
                        )
                    )

                    if (!isAvailable) {
                        Surface(
                            color = Color.LightGray,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "Sold Out",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = Color.DarkGray,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Action Button / Quantity Selector
            if (isAvailable) {
                if (!isBookingOpen) {
                    Button(
                        onClick = {},
                        enabled = false,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(disabledContainerColor = Color.LightGray),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                            text = "CLOSED",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.DarkGray
                        )
                    }
                } else if (cartQuantity > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, EmeraldPrimary, RoundedCornerShape(8.dp))
                    ) {
                        val interactionSourceDec = remember { MutableInteractionSource() }
                        val isPressedDec by interactionSourceDec.collectIsPressedAsState()
                        val scaleDec by animateFloatAsState(if (isPressedDec) 0.9f else 1f)
                        
                        IconButton(
                            onClick = { onUpdateQuantity(cartQuantity - 1) },
                            interactionSource = interactionSourceDec,
                            modifier = Modifier.size(44.dp).scale(scaleDec)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Remove,
                                contentDescription = "Decrease",
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Text(
                            text = cartQuantity.toString(),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = EmeraldPrimary,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )

                        val interactionSourceInc = remember { MutableInteractionSource() }
                        val isPressedInc by interactionSourceInc.collectIsPressedAsState()
                        val scaleInc by animateFloatAsState(if (isPressedInc) 0.9f else 1f)

                        IconButton(
                            onClick = { onUpdateQuantity(cartQuantity + 1) },
                            interactionSource = interactionSourceInc,
                            modifier = Modifier.size(44.dp).scale(scaleInc)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Increase",
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                } else {
                    val interactionSourceAdd = remember { MutableInteractionSource() }
                    val isPressedAdd by interactionSourceAdd.collectIsPressedAsState()
                    val scaleAdd by animateFloatAsState(if (isPressedAdd) 0.9f else 1f)

                    Button(
                        onClick = onAddToCart,
                        interactionSource = interactionSourceAdd,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier.height(36.dp).scale(scaleAdd)
                    ) {
                        Text(
                            text = "ADD",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

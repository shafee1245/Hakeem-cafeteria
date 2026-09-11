package com.example.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (status) {
        "PLACED" -> StatusPlaced.copy(alpha = 0.15f) to StatusPlaced
        "ACCEPTED" -> StatusAccepted.copy(alpha = 0.15f) to StatusAccepted
        "PROCESSING" -> StatusProcessing.copy(alpha = 0.15f) to StatusProcessing
        "READY" -> StatusReady.copy(alpha = 0.15f) to StatusReady
        "DELIVERED" -> StatusDelivered.copy(alpha = 0.15f) to StatusDelivered
        "REJECTED", "CANCELLED" -> StatusRejected.copy(alpha = 0.15f) to StatusRejected
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Text(
            text = status,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            ),
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

package com.example.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material3.*
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.BookingSettingsEntity
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.viewmodel.AdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminBookingSettingsScreen(
    viewModel: AdminViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentSettings by viewModel.bookingSettings.collectAsState()

    var openTime by remember(currentSettings) { mutableStateOf(currentSettings?.openTime ?: "09:00") }
    var closeTime by remember(currentSettings) { mutableStateOf(currentSettings?.closeTime ?: "11:40") }
    
    // Convert activeDays CSV to a set of strings
    var activeDaysSet by remember(currentSettings) { 
        mutableStateOf(currentSettings?.activeDays?.split(",")?.map { it.trim() }?.toSet() ?: setOf("MON", "TUE", "WED", "THU", "FRI", "SAT")) 
    }
    
    var isEmergencyClosed by remember(currentSettings) { mutableStateOf(currentSettings?.isEmergencyClosed ?: false) }
    var closureReason by remember(currentSettings) { mutableStateOf(currentSettings?.emergencyClosureReason ?: "Cafeteria closed for maintenance") }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pre-Booking Rules", fontWeight = FontWeight.Bold) },
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
                    val newSettings = currentSettings?.copy(
                        openTime = openTime,
                        closeTime = closeTime,
                        activeDays = activeDaysSet.joinToString(","),
                        isEmergencyClosed = isEmergencyClosed,
                        emergencyClosureReason = closureReason
                    ) ?: BookingSettingsEntity(
                        id = 1,
                        openTime = openTime,
                        closeTime = closeTime,
                        activeDays = activeDaysSet.joinToString(","),
                        isEmergencyClosed = isEmergencyClosed,
                        emergencyClosureReason = closureReason
                    )
                    viewModel.updateBookingSettings(newSettings)
                    onNavigateBack()
                },
                containerColor = EmeraldPrimary,
                contentColor = Color.White
            ) {
                Text("Save Settings", fontWeight = FontWeight.Bold)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            
            // 1. Emergency Shutoff Banner Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isEmergencyClosed) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = if (isEmergencyClosed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Emergency Shutoff", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        }
                        Switch(
                            checked = isEmergencyClosed,
                            onCheckedChange = { isEmergencyClosed = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = MaterialTheme.colorScheme.error
                            )
                        )
                    }

                    if (isEmergencyClosed) {
                        Text(
                            text = "All new pre-bookings are currently suspended. Existing orders must still be fulfilled or rejected manually.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )

                        OutlinedTextField(
                            value = closureReason,
                            onValueChange = { closureReason = it },
                            label = { Text("Closure Notice (Shown to students)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.error,
                                focusedLabelColor = MaterialTheme.colorScheme.error
                            )
                        )
                    } else {
                        Text(
                            text = "Cafeteria is accepting orders based on standard operating hours.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 2. Standard Operating Hours Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.AccessTime, contentDescription = null, tint = EmeraldPrimary)
                        Text("Daily Booking Window", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                    
                    Text(
                        text = "Set the daily time window during which students can place pre-orders.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Stubbed Time Picker Triggers
                        TimeSelectorUI(
                            label = "Window Opens",
                            time = openTime,
                            onTimeChange = { openTime = it }, // Real app: Open native TimePickerDialog
                            modifier = Modifier.weight(1f)
                        )

                        TimeSelectorUI(
                            label = "Window Closes",
                            time = closeTime,
                            onTimeChange = { closeTime = it }, // Real app: Open native TimePickerDialog
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 3. Operating Days Selector
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.EventAvailable, contentDescription = null, tint = EmeraldPrimary)
                        Text("Active Operating Days", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }

                    Text(
                        text = "Select which days of the week the cafeteria accepts pre-bookings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val daysOfWeek = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        daysOfWeek.forEach { day ->
                            DayChip(
                                day = day,
                                isSelected = activeDaysSet.contains(day),
                                onToggle = {
                                    if (activeDaysSet.contains(day)) {
                                        activeDaysSet = activeDaysSet - day
                                    } else {
                                        activeDaysSet = activeDaysSet + day
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppTimePickerDialog(
    initialTime: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val timeParts = initialTime.split(":")
    val initialHour = timeParts.getOrNull(0)?.toIntOrNull() ?: 9
    val initialMinute = timeParts.getOrNull(1)?.substringBefore(" ")?.toIntOrNull() ?: 0

    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = false
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val formattedHour = timePickerState.hour.toString().padStart(2, '0')
                val formattedMinute = timePickerState.minute.toString().padStart(2, '0')
                // Format with AM/PM for standard display if desired, or just 24-hour. We'll use 24-hour style for simplicity since it's just a string setting.
                onConfirm("$formattedHour:$formattedMinute")
            }) {
                Text("OK", color = EmeraldPrimary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        text = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TimePicker(state = timePickerState)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeSelectorUI(
    label: String,
    time: String,
    onTimeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        AppTimePickerDialog(
            initialTime = time,
            onDismiss = { showDialog = false },
            onConfirm = { 
                onTimeChange(it)
                showDialog = false 
            }
        )
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant)
        
        OutlinedTextField(
            value = time,
            onValueChange = {},
            readOnly = true,
            enabled = false,
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDialog = true },
            textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}

@Composable
private fun DayChip(
    day: String,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onToggle() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.take(1),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

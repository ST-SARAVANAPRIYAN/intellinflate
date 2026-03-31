package com.example.intellinflate.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.intellinflate.models.*
import com.example.intellinflate.ui.components.*
import com.example.intellinflate.viewmodel.VehicleUIState
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun VehicleDashboard(
    uiState: VehicleUIState,
    onDisconnect: () -> Unit,
    isDemoMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Connection controls
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isDemoMode) 
                    Color(0xFFF59E0B).copy(alpha = 0.2f)
                else
                    MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isDemoMode) "Demo Mode Active" else "ESP32 Connected",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = if (isDemoMode) "Displaying simulated data" else "Receiving live data",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Button(
                    onClick = onDisconnect,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(if (isDemoMode) "Stop Demo" else "Disconnect")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Overall Health Status
        OverallHealthCard(uiState.vehicleHealth)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Alerts Section
        if (uiState.vehicleHealth.alerts.isNotEmpty()) {
            AlertsSection(uiState.vehicleHealth.alerts)
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Vehicle Stats
        uiState.vehicleData?.let { vehicleData ->
            VehicleStatsCard(vehicleData)
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Tire Health Section
        TireHealthSection(uiState.tires)
    }
}

@Composable
fun OverallHealthCard(vehicleHealth: VehicleHealth) {
    val (color, icon, statusText) = when (vehicleHealth.overallStatus) {
        HealthStatus.EXCELLENT -> Triple(Color(0xFF4CAF50), Icons.Default.CheckCircle, "Excellent")
        HealthStatus.GOOD -> Triple(Color(0xFF8BC34A), Icons.Default.Check, "Good")
        HealthStatus.WARNING -> Triple(Color(0xFFFFC107), Icons.Default.Warning, "Warning")
        HealthStatus.CRITICAL -> Triple(Color(0xFFF44336), Icons.Default.Warning, "Critical")
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Overall Health",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.headlineMedium,
                    color = color
                )
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

@Composable
fun AlertsSection(alerts: List<Alert>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Active Alerts (${alerts.size})",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            alerts.forEach { alert ->
                AlertItem(alert)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun AlertItem(alert: Alert) {
    val (color, icon) = when (alert.severity) {
        AlertSeverity.CRITICAL -> Pair(MaterialTheme.colorScheme.error, Icons.Default.Warning)
        AlertSeverity.WARNING -> Pair(Color(0xFFFFC107), Icons.Default.Warning)
        AlertSeverity.INFO -> Pair(MaterialTheme.colorScheme.primary, Icons.Default.Info)
    }
    
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = alert.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = formatTimestamp(alert.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun VehicleStatsCard(vehicleData: VehicleData) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Vehicle Information",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                VehicleStatItem(
                    icon = Icons.Default.Speed,
                    label = "Speed",
                    value = String.format("%.1f km/h", vehicleData.speed),
                    modifier = Modifier.weight(1f)
                )
                VehicleStatItem(
                    icon = Icons.Default.LocalGasStation,
                    label = "Fuel",
                    value = String.format("%.0f%%", vehicleData.fuelLevel),
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                VehicleStatItem(
                    icon = Icons.Default.Thermostat,
                    label = "Engine Temp",
                    value = String.format("%.1f°C", vehicleData.engineTemperature),
                    modifier = Modifier.weight(1f)
                )
                VehicleStatItem(
                    icon = Icons.Default.BatteryFull,
                    label = "Battery",
                    value = String.format("%.1fV", vehicleData.batteryVoltage),
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                VehicleStatItem(
                    icon = Icons.Default.DirectionsCar,
                    label = "Odometer",
                    value = String.format("%.1f km", vehicleData.odometer),
                    modifier = Modifier.weight(1f)
                )
                VehicleStatItem(
                    icon = Icons.Default.Settings,
                    label = "Engine",
                    value = vehicleData.engineStatus.name,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun VehicleStatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
fun TireHealthSection(tires: Map<TirePosition, TireData>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Tire Health Monitor",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Front tires
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tires[TirePosition.FRONT_LEFT]?.let { tire ->
                    TireCard(tire, modifier = Modifier.weight(1f))
                } ?: PlaceholderTireCard("Front Left", modifier = Modifier.weight(1f))
                
                tires[TirePosition.FRONT_RIGHT]?.let { tire ->
                    TireCard(tire, modifier = Modifier.weight(1f))
                } ?: PlaceholderTireCard("Front Right", modifier = Modifier.weight(1f))
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Rear tires
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tires[TirePosition.REAR_LEFT]?.let { tire ->
                    TireCard(tire, modifier = Modifier.weight(1f))
                } ?: PlaceholderTireCard("Rear Left", modifier = Modifier.weight(1f))
                
                tires[TirePosition.REAR_RIGHT]?.let { tire ->
                    TireCard(tire, modifier = Modifier.weight(1f))
                } ?: PlaceholderTireCard("Rear Right", modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun TireCard(tire: TireData, modifier: Modifier = Modifier) {
    val (color, statusText) = when (tire.health) {
        TireHealth.GOOD -> Pair(Color(0xFF4CAF50), "Good")
        TireHealth.WARNING -> Pair(Color(0xFFFFC107), "Warning")
        TireHealth.CRITICAL -> Pair(Color(0xFFF44336), "Critical")
    }
    
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = tire.position.name.replace("_", " "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Icon(
                imageVector = Icons.Default.Album,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                color = color
            )
            Divider(modifier = Modifier.padding(vertical = 8.dp))
            Text(
                text = String.format("%.1f PSI", tire.pressure),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = String.format("%.1f°C", tire.temperature),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PlaceholderTireCard(label: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Icon(
                imageVector = Icons.Default.Album,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "No Data",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

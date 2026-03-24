package com.example.intellinflate.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.intellinflate.models.*
import com.example.intellinflate.viewmodel.PsiPilotUIState

@Composable
fun PsiPilotDashboard(
    uiState: PsiPilotUIState,
    onDetectVehicle: () -> Unit,
    onNavigateToScan: () -> Unit,
    onNavigateToVehicle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "IntelliInflate",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Tire Health Monitoring",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            val overallScore = if (uiState.tireScanResults.isEmpty()) null
                else uiState.tireScanResults.values.map { it.overallCondition.overallScore }.average().toFloat()
            if (overallScore != null) {
                OverallHealthBadge(score = overallScore)
            } else {
                Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                    Text(text = "No Data", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
        }
        VehicleInfoCard(vehicleProfile = uiState.currentVehicle, onDetectVehicle = onNavigateToVehicle)
        TireHealthMapCard(
            tireScanResults = uiState.tireScanResults,
            scanningTire = uiState.currentTireScanning,
            onTireTap = { onNavigateToScan() },
            onScanAll = { onNavigateToScan() }
        )
        val alerts = buildAlerts(uiState.tireScanResults)
        if (alerts.isNotEmpty()) { AlertsCard(alerts = alerts) }
        QuickActionsCard(
            hasTireData = uiState.tireScanResults.isNotEmpty(),
            hasVehicle = uiState.currentVehicle != null,
            onScanAll = onNavigateToScan,
            onDetectVehicle = onNavigateToVehicle
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun OverallHealthBadge(score: Float) {
    val color = healthColor(score)
    Surface(shape = RoundedCornerShape(20.dp), color = color.copy(alpha = 0.15f),
        modifier = Modifier.border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(20.dp))) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
            Text(text = healthLabel(score), style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun VehicleInfoCard(vehicleProfile: VehicleProfile?, onDetectVehicle: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        if (vehicleProfile != null) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.Default.DirectionsCar, contentDescription = null,
                        modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = vehicleProfile.vehicleType.displayName,
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(text = vehicleProfile.licensePlate, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 2.sp)
                }
                IconButton(onClick = onDetectVehicle) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Re-detect",
                        tint = MaterialTheme.colorScheme.primary)
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(imageVector = Icons.Default.DirectionsCar, contentDescription = null,
                    modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                Text(text = "No vehicle detected", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(onClick = onDetectVehicle) {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Detect Vehicle")
                }
            }
        }
    }
}

@Composable
private fun TireHealthMapCard(
    tireScanResults: Map<TirePosition, TireScanResult>,
    scanningTire: TirePosition?,
    onTireTap: (TirePosition) -> Unit,
    onScanAll: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Tire Health Map", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = onScanAll) {
                    Icon(Icons.Default.Scanner, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Scan All")
                }
            }
            Spacer(Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth().height(210.dp), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.width(72.dp).height(152.dp).clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)))
                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically) {
                        TireTile(TirePosition.FRONT_LEFT, "FL", tireScanResults[TirePosition.FRONT_LEFT], scanningTire == TirePosition.FRONT_LEFT) { onTireTap(TirePosition.FRONT_LEFT) }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f), modifier = Modifier.size(18.dp))
                            Text("FRONT", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), fontSize = 9.sp)
                        }
                        TireTile(TirePosition.FRONT_RIGHT, "FR", tireScanResults[TirePosition.FRONT_RIGHT], scanningTire == TirePosition.FRONT_RIGHT) { onTireTap(TirePosition.FRONT_RIGHT) }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically) {
                        TireTile(TirePosition.REAR_LEFT, "RL", tireScanResults[TirePosition.REAR_LEFT], scanningTire == TirePosition.REAR_LEFT) { onTireTap(TirePosition.REAR_LEFT) }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("REAR", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), fontSize = 9.sp)
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f), modifier = Modifier.size(18.dp))
                        }
                        TireTile(TirePosition.REAR_RIGHT, "RR", tireScanResults[TirePosition.REAR_RIGHT], scanningTire == TirePosition.REAR_RIGHT) { onTireTap(TirePosition.REAR_RIGHT) }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                listOf("Excellent" to Color(0xFF10B981), "Good" to Color(0xFF3B82F6),
                    "Fair" to Color(0xFFF59E0B), "Poor" to Color(0xFFEF4444)).forEach { (label, color) ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 5.dp)) {
                        Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(color))
                        Spacer(Modifier.width(3.dp))
                        Text(text = label, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun TireTile(
    position: TirePosition,
    label: String,
    scanResult: TireScanResult?,
    isScanning: Boolean,
    onClick: () -> Unit
) {
    val score = scanResult?.overallCondition?.overallScore
    val tileColor = if (score != null) healthColor(score) else MaterialTheme.colorScheme.outlineVariant
    val animatedColor by animateColorAsState(targetValue = tileColor, animationSpec = tween(400), label = "tc")
    val animatedScore by animateFloatAsState(targetValue = score ?: 0f, animationSpec = tween(600), label = "sc")

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable(onClick = onClick)) {
        Box(contentAlignment = Alignment.Center,
            modifier = Modifier.size(68.dp).clip(RoundedCornerShape(12.dp))
                .background(animatedColor.copy(alpha = 0.12f))
                .border(2.dp, animatedColor.copy(alpha = 0.6f), RoundedCornerShape(12.dp))) {
            if (isScanning) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
            } else if (score != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "${animatedScore.toInt()}", style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold, color = animatedColor)
                    Text(text = "/100", fontSize = 8.sp, color = animatedColor.copy(alpha = 0.7f))
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Scanner, contentDescription = null, modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    Text(text = "Scan", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                }
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(text = label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold,
            color = if (score != null) animatedColor else MaterialTheme.colorScheme.onSurfaceVariant)
        if (score != null) {
            Text(text = healthLabel(score), fontSize = 9.sp, color = animatedColor.copy(alpha = 0.8f))
        }
    }
}

data class TireAlert(val position: TirePosition, val message: String, val severity: AlertSeverity)

fun buildAlerts(scanResults: Map<TirePosition, TireScanResult>): List<TireAlert> {
    val alerts = mutableListOf<TireAlert>()
    scanResults.forEach { (pos, result) ->
        val score = result.overallCondition.overallScore
        val posName = pos.name.replace("_", " ")
        if (score < 50f) alerts.add(TireAlert(pos, "$posName tyre critically degraded (${score.toInt()}/100)", AlertSeverity.CRITICAL))
        else if (score < 70f) alerts.add(TireAlert(pos, "$posName tyre needs attention (${score.toInt()}/100)", AlertSeverity.WARNING))
        if (result.crackDetection.hasCracks && result.crackDetection.crackSeverity == CrackSeverity.CRITICAL)
            alerts.add(TireAlert(pos, "$posName — critical crack detected!", AlertSeverity.CRITICAL))
        if (result.treadAnalysis.hasForeignObjects)
            alerts.add(TireAlert(pos, "$posName — foreign object in tread", AlertSeverity.WARNING))
        if (result.sidewallAnalysis.hasDamage)
            alerts.add(TireAlert(pos, "$posName — sidewall damage detected", AlertSeverity.WARNING))
    }
    return alerts.sortedByDescending { it.severity == AlertSeverity.CRITICAL }
}

@Composable
private fun AlertsCard(alerts: List<TireAlert>) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.06f))) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(text = "Alerts (${alerts.size})", style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
            }
            HorizontalDivider(color = Color(0xFFEF4444).copy(alpha = 0.2f))
            alerts.take(5).forEach { alert ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape)
                        .background(if (alert.severity == AlertSeverity.CRITICAL) Color(0xFFEF4444) else Color(0xFFF59E0B)))
                    Spacer(Modifier.width(10.dp))
                    Text(text = alert.message, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun QuickActionsCard(
    hasTireData: Boolean,
    hasVehicle: Boolean,
    onScanAll: () -> Unit,
    onDetectVehicle: () -> Unit
) {
    Text(text = "Quick Actions", style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ElevatedCard(modifier = Modifier.weight(1f).clickable { onScanAll() }, shape = RoundedCornerShape(14.dp)) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.size(44.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Scanner, contentDescription = null, modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.primary)
                }
                Text(text = "Scan Tires", style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                Text(text = if (hasTireData) "Re-scan all 4" else "Start scan",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center, fontSize = 10.sp)
            }
        }
        ElevatedCard(modifier = Modifier.weight(1f).clickable { onDetectVehicle() }, shape = RoundedCornerShape(14.dp)) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.size(44.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.DirectionsCar, contentDescription = null, modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.secondary)
                }
                Text(text = "Detect Vehicle", style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                Text(text = if (hasVehicle) "Re-detect" else "Camera scan",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center, fontSize = 10.sp)
            }
        }
    }
}

fun healthColor(score: Float): Color = when {
    score >= 85f -> Color(0xFF10B981)
    score >= 70f -> Color(0xFF3B82F6)
    score >= 50f -> Color(0xFFF59E0B)
    else         -> Color(0xFFEF4444)
}

fun healthLabel(score: Float): String = when {
    score >= 85f -> "Excellent"
    score >= 70f -> "Good"
    score >= 50f -> "Fair"
    else         -> "Poor"
}

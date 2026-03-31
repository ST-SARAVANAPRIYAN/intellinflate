package com.example.intellinflate.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.intellinflate.models.*

/**
 * Tire Crack Detection and Health Analysis Screen
 * Displays comprehensive AI analysis of tire condition
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TireScanScreen(
    tireScanResults: Map<TirePosition, TireScanResult>,
    onScanTire: (TirePosition) -> Unit,
    onScanAllTires: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTire by remember { mutableStateOf<TirePosition?>(null) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tire Health Analysis",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Button(
                onClick = onScanAllTires,
                modifier = Modifier.height(36.dp)
            ) {
                Icon(imageVector = Icons.Default.Scanner, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Scan All")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Tire Selection Grid
        TireSelectionGrid(
            tireScanResults = tireScanResults,
            selectedTire = selectedTire,
            onTireSelected = { selectedTire = it },
            onScanTire = onScanTire
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Detailed Results for Selected Tire
        selectedTire?.let { tire ->
            tireScanResults[tire]?.let { scanResult ->
                TireScanDetailView(scanResult)
            } ?: run {
                ScanPromptCard(tire, onScanTire)
            }
        }
    }
}

@Composable
private fun TireSelectionGrid(
    tireScanResults: Map<TirePosition, TireScanResult>,
    selectedTire: TirePosition?,
    onTireSelected: (TirePosition) -> Unit,
    onScanTire: (TirePosition) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Select Tire to Analyze",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf(TirePosition.FRONT_LEFT, TirePosition.FRONT_RIGHT).forEach { tire ->
                    TireCard(
                        tire = tire,
                        scanResult = tireScanResults[tire],
                        isSelected = tire == selectedTire,
                        onSelect = { onTireSelected(tire) },
                        onScan = { onScanTire(tire) }
                    )
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf(TirePosition.REAR_LEFT, TirePosition.REAR_RIGHT).forEach { tire ->
                    TireCard(
                        tire = tire,
                        scanResult = tireScanResults[tire],
                        isSelected = tire == selectedTire,
                        onSelect = { onTireSelected(tire) },
                        onScan = { onScanTire(tire) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TireCard(
    tire: TirePosition,
    scanResult: TireScanResult?,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onScan: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .clickable { onSelect() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer
            else 
                MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) 
            CardDefaults.outlinedCardBorder().copy(width = 2.dp)
        else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Tire Icon with Status
            Box(
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Circle,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = scanResult?.let {
                        Color(it.overallCondition.overallStatus.color)
                    } ?: MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (scanResult != null) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier
                            .size(20.dp)
                            .align(Alignment.TopEnd),
                        tint = Color(0xFF10B981)
                    )
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            Text(
                text = tire.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            
            if (scanResult == null) {
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = onScan,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(4.dp)
                ) {
                    Text("Scan", style = MaterialTheme.typography.labelSmall)
                }
            } else {
                Text(
                    text = scanResult.overallCondition.overallStatus.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(scanResult.overallCondition.overallStatus.color)
                )
            }
        }
    }
}

@Composable
private fun ScanPromptCard(tire: TirePosition, onScanTire: (TirePosition) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Scanner,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(Modifier.height(16.dp))
            
            Text(
                text = "No scan data for ${tire.displayName}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(Modifier.height(8.dp))
            
            Text(
                text = "Start scanning to analyze tire condition",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(Modifier.height(16.dp))
            
            Button(
                onClick = { onScanTire(tire) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Camera, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Start Scanning")
            }
        }
    }
}

@Composable
private fun TireScanDetailView(scanResult: TireScanResult) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Overall Condition Card
        TireOverallConditionCard(scanResult.overallCondition)
        
        Spacer(Modifier.height(12.dp))
        
        // Tire Images
        if (scanResult.images.isNotEmpty()) {
            TireImagesCard(scanResult.images)
            Spacer(Modifier.height(12.dp))
        }
        
        // Crack Detection Results
        CrackDetectionCard(scanResult.crackDetection)
        
        Spacer(Modifier.height(12.dp))
        
        // Wear Analysis
        WearAnalysisCard(scanResult.wearAnalysis)
        
        Spacer(Modifier.height(12.dp))
        
        // Sidewall Analysis
        SidewallAnalysisCard(scanResult.sidewallAnalysis)
        
        Spacer(Modifier.height(12.dp))
        
        // Tread Analysis
        TreadAnalysisCard(scanResult.treadAnalysis)
    }
}

@Composable
private fun TireOverallConditionCard(assessment: TireConditionAssessment) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(assessment.overallStatus.color).copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Overall Condition",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = assessment.overallStatus.displayName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(assessment.overallStatus.color)
                    )
                }
                
                // Score Circle
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(assessment.overallStatus.color).copy(alpha = 0.2f))
                ) {
                    Text(
                        text = "${assessment.overallScore.toInt()}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(assessment.overallStatus.color)
                    )
                }
            }
            
            Spacer(Modifier.height(12.dp))
            Divider()
            Spacer(Modifier.height(12.dp))
            
            // Safety Rating
            SafetyRatingRow(assessment.safetyRating)
            
            Spacer(Modifier.height(12.dp))
            
            // Recommendations
            if (assessment.recommendations.isNotEmpty()) {
                Divider()
                Spacer(Modifier.height(12.dp))
                
                Text(
                    text = "Recommendations",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(Modifier.height(8.dp))
                
                assessment.recommendations.forEach { recommendation ->
                    RecommendationRow(recommendation)
                }
            }
        }
    }
}

@Composable
private fun SafetyRatingRow(safetyRating: SafetyRating) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = when (safetyRating) {
                    SafetyRating.SAFE -> Icons.Default.CheckCircle
                    SafetyRating.SAFE_WITH_CARE -> Icons.Default.Info
                    SafetyRating.LIMITED_USE -> Icons.Default.Warning
                    else -> Icons.Default.Error
                },
                contentDescription = null,
                tint = when (safetyRating) {
                    SafetyRating.SAFE -> Color(0xFF10B981)
                    SafetyRating.SAFE_WITH_CARE -> Color(0xFF3B82F6)
                    SafetyRating.LIMITED_USE -> Color(0xFFF59E0B)
                    else -> Color(0xFFEF4444)
                }
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Safety Rating",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        
        Text(
            text = safetyRating.name.replace("_", " "),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun RecommendationRow(recommendation: TireRecommendation) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(
                    when (recommendation.priority) {
                        RecommendationPriority.CRITICAL -> Color(0xFFEF4444)
                        RecommendationPriority.HIGH -> Color(0xFFF59E0B)
                        RecommendationPriority.MEDIUM -> Color(0xFF3B82F6)
                        else -> Color(0xFF6B7280)
                    }.copy(alpha = 0.2f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "!",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = when (recommendation.priority) {
                    RecommendationPriority.CRITICAL -> Color(0xFFEF4444)
                    RecommendationPriority.HIGH -> Color(0xFFF59E0B)
                    RecommendationPriority.MEDIUM -> Color(0xFF3B82F6)
                    else -> Color(0xFF6B7280)
                }
            )
        }
        
        Spacer(Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = recommendation.description,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Action: ${recommendation.timeframe.name.replace("_", " ")}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TireImagesCard(images: List<TireImage>) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .animateContentSize()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Image, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Captured Images (${images.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }
            
            if (expanded) {
                Spacer(Modifier.height(12.dp))
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.height(400.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(images) { image ->
                        TireImageItem(image)
                    }
                }
            }
        }
    }
}

@Composable
private fun TireImageItem(image: TireImage) {
    Card {
        Column {
            AsyncImage(
                model = image.imageUrl ?: image.imageBase64,
                contentDescription = image.captureAngle.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentScale = ContentScale.Crop
            )
            
            Text(
                text = image.captureAngle.name.replace("_", " "),
                modifier = Modifier.padding(8.dp),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
private fun CrackDetectionCard(crackDetection: CrackDetectionResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (crackDetection.crackSeverity) {
                CrackSeverity.NONE -> MaterialTheme.colorScheme.surfaceVariant
                CrackSeverity.MINOR -> Color(0xFF3B82F6).copy(alpha = 0.1f)
                CrackSeverity.MODERATE -> Color(0xFFF59E0B).copy(alpha = 0.1f)
                else -> Color(0xFFEF4444).copy(alpha = 0.1f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (crackDetection.hasCracks) Icons.Default.Warning else Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = when (crackDetection.crackSeverity) {
                        CrackSeverity.NONE -> Color(0xFF10B981)
                        CrackSeverity.MINOR -> Color(0xFF3B82F6)
                        CrackSeverity.MODERATE -> Color(0xFFF59E0B)
                        else -> Color(0xFFEF4444)
                    }
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Crack Detection",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoItem("Status", if (crackDetection.hasCracks) "Cracks Found" else "No Cracks")
                InfoItem("Severity", crackDetection.crackSeverity.name)
                InfoItem("Confidence", "${(crackDetection.confidence * 100).toInt()}%")
            }
            
            if (crackDetection.hasCracks) {
                Spacer(Modifier.height(12.dp))
                Divider()
                Spacer(Modifier.height(12.dp))
                
                InfoItem("Total Cracks", "${crackDetection.detectedCracks.size}")
                InfoItem("Total Length", "${crackDetection.totalCrackLength} mm")
                InfoItem("Crack Density", String.format("%.2f/cm²", crackDetection.crackDensity))
                
                if (crackDetection.detectedCracks.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Detected Cracks:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    
                    crackDetection.detectedCracks.take(5).forEach { crack ->
                        CrackItemRow(crack)
                    }
                }
            }
        }
    }
}

@Composable
private fun CrackItemRow(crack: DetectedCrack) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = crack.crackType.name.replace("_", " "),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "${crack.length}mm - ${crack.location.name}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = when (crack.severity) {
                CrackSeverity.MINOR -> Color(0xFF3B82F6)
                CrackSeverity.MODERATE -> Color(0xFFF59E0B)
                else -> Color(0xFFEF4444)
            }.copy(alpha = 0.2f)
        ) {
            Text(
                text = crack.severity.name,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                color = when (crack.severity) {
                    CrackSeverity.MINOR -> Color(0xFF3B82F6)
                    CrackSeverity.MODERATE -> Color(0xFFF59E0B)
                    else -> Color(0xFFEF4444)
                }
            )
        }
    }
}

@Composable
private fun WearAnalysisCard(wearAnalysis: WearAnalysisResult) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Speed, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Tread Wear Analysis",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(Modifier.height(12.dp))
            
            InfoItem("Wear Level", wearAnalysis.wearLevel.name)
            InfoItem("Wear Pattern", wearAnalysis.wearPattern.name.replace("_", " "))
            InfoItem("Avg Tread Depth", "${wearAnalysis.averageTreadDepth} mm")
            InfoItem("Min Tread Depth", "${wearAnalysis.minimumTreadDepth} mm")
            InfoItem("Wear Uniformity", "${(wearAnalysis.wearUniformity * 100).toInt()}%")
            
            wearAnalysis.estimatedRemainingLife.remainingKilometers?.let {
                InfoItem("Est. Remaining", "$it km")
            }
        }
    }
}

@Composable
private fun SidewallAnalysisCard(sidewallAnalysis: SidewallAnalysisResult) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Circle, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Sidewall Analysis",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(Modifier.height(12.dp))
            
            InfoItem("Condition", sidewallAnalysis.sidewallCondition.name)
            InfoItem("Damage Detected", if (sidewallAnalysis.hasDamage) "Yes" else "No")
            InfoItem("Anomalies", "${sidewallAnalysis.detectedAnomalies.size}")
        }
    }
}

@Composable
private fun TreadAnalysisCard(treadAnalysis: TreadAnalysisResult) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.GridOn, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Tread Analysis",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(Modifier.height(12.dp))
            
            InfoItem("Pattern Type", treadAnalysis.treadPattern.patternType.name)
            InfoItem("Pattern Condition", treadAnalysis.treadPattern.patternCondition.name)
            InfoItem("Foreign Objects", if (treadAnalysis.hasForeignObjects) "Yes" else "No")
            
            if (treadAnalysis.hasForeignObjects && treadAnalysis.detectedObjects.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Detected Objects:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                treadAnalysis.detectedObjects.forEach { obj ->
                    Text(
                        text = "• ${obj.objectType.name} - ${obj.riskLevel.name} risk",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

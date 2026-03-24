package com.example.intellinflate.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.intellinflate.models.*

/**
 * Vehicle Detection Results Screen
 * Displays AI-detected vehicle information including:
 * - Vehicle type classification
 * - License plate recognition
 * - Vehicle image with bounding boxes
 * - Confidence scores
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleDetectionScreen(
    detectionResult: VehicleDetectionResult?,
    vehicleProfile: VehicleProfile?,
    onDetectAgain: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                text = "Vehicle Detection",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            IconButton(onClick = onDetectAgain) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Detect Again"
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (detectionResult == null || vehicleProfile == null) {
            // No detection yet
            NoDetectionView(onDetect = onDetectAgain)
        } else {
            // Display detection results
            VehicleDetectionContent(
                detectionResult = detectionResult,
                vehicleProfile = vehicleProfile,
                onContinue = onContinue
            )
        }
    }
}

@Composable
private fun NoDetectionView(onDetect: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.DirectionsCar,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "No Vehicle Detected",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Position your vehicle in front of the camera",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onDetect,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Camera, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Start Detection")
            }
        }
    }
}

@Composable
private fun VehicleDetectionContent(
    detectionResult: VehicleDetectionResult,
    vehicleProfile: VehicleProfile,
    onContinue: () -> Unit
) {
    val detectedVehicle = detectionResult.vehicles.firstOrNull()
    
    // Vehicle Image with Bounding Boxes
    if (detectionResult.imageUrl != null || detectionResult.imageBase64 != null) {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            ) {
                AsyncImage(
                    model = detectionResult.imageUrl ?: detectionResult.imageBase64,
                    contentDescription = "Detected Vehicle",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                
                // Confidence Badge
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.9f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${(detectionResult.confidence * 100).toInt()}%",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
    
    // Vehicle Type Classification
    detectedVehicle?.let { vehicle ->
        VehicleTypeCard(vehicle.vehicleType)
        Spacer(modifier = Modifier.height(12.dp))
    }
    
    // License Plate Recognition
    detectedVehicle?.licensePlate?.let { plate ->
        LicensePlateCard(plate)
        Spacer(modifier = Modifier.height(12.dp))
    }
    
    // Vehicle Details
    VehicleDetailsCard(vehicleProfile, detectedVehicle)
    
    Spacer(modifier = Modifier.height(24.dp))
    
    // Continue Button
    Button(
        onClick = onContinue,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Text("Continue to Tire Inspection")
        Spacer(Modifier.width(8.dp))
        Icon(Icons.Default.ArrowForward, contentDescription = null)
    }
}

@Composable
private fun VehicleTypeCard(typeDetection: VehicleTypeDetection) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Vehicle Type",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = typeDetection.type.displayName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Confidence Score
                CircularConfidenceIndicator(
                    confidence = typeDetection.confidence,
                    size = 60.dp
                )
            }
            
            // Alternative Predictions
            if (typeDetection.alternativePredictions.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Divider()
                Spacer(Modifier.height(12.dp))
                
                Text(
                    text = "Alternative Classifications",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                
                typeDetection.alternativePredictions.take(3).forEach { prediction ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = prediction.type.displayName,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${(prediction.confidence * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LicensePlateCard(plate: LicensePlateDetection) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (plate.isValid) 
                MaterialTheme.colorScheme.primaryContainer
            else 
                MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (plate.isValid) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = if (plate.isValid) 
                    MaterialTheme.colorScheme.primary
                else 
                    MaterialTheme.colorScheme.error
            )
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "License Plate",
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = plate.plateNumber,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                plate.region?.let {
                    Text(
                        text = "Region: $it",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            
            // Confidence
            Text(
                text = "${(plate.confidence * 100).toInt()}%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun VehicleDetailsCard(
    vehicleProfile: VehicleProfile,
    detectedVehicle: DetectedVehicle?
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Vehicle Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(Modifier.height(12.dp))
            
            DetailRow("Type", vehicleProfile.vehicleType.displayName)
            DetailRow("License Plate", vehicleProfile.licensePlate)
            detectedVehicle?.color?.let {
                DetailRow("Color", it.primaryColor.name.lowercase().capitalize())
            }
            detectedVehicle?.orientation?.let {
                DetailRow("Orientation", it.name.replace("_", " ").lowercase().capitalize())
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
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

@Composable
private fun CircularConfidenceIndicator(
    confidence: Float,
    size: Dp
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(size)
    ) {
        CircularProgressIndicator(
            progress = confidence,
            modifier = Modifier.fillMaxSize(),
            strokeWidth = 4.dp,
            color = when {
                confidence >= 0.9f -> Color(0xFF10B981)
                confidence >= 0.7f -> Color(0xFFF59E0B)
                else -> Color(0xFFEF4444)
            }
        )
        Text(
            text = "${(confidence * 100).toInt()}%",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

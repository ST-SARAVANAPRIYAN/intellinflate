package com.example.intellinflate.models

/**
 * AI-Based Vehicle Detection Models
 * Data from ESP32-CAM and AI inference
 */

/**
 * Vehicle Detection Result - From ESP32-CAM AI
 */
data class VehicleDetectionResult(
    val detectionId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val imageUrl: String? = null,
    val imageBase64: String? = null, // Base64 encoded image from ESP32
    val vehicles: List<DetectedVehicle>,
    val processingTime: Long, // milliseconds
    val cameraId: String,
    val confidence: Float // Overall detection confidence
)

data class DetectedVehicle(
    val vehicleId: String,
    val boundingBox: BoundingBox,
    val vehicleType: VehicleTypeDetection,
    val licensePlate: LicensePlateDetection?,
    val color: VehicleColor?,
    val orientation: VehicleOrientation,
    val confidence: Float,
    val features: VehicleFeatures
)

data class BoundingBox(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val normalized: Boolean = true // If true, values are 0.0-1.0, else pixels
)

/**
 * Vehicle Type Detection with AI confidence
 */
data class VehicleTypeDetection(
    val type: VehicleType,
    val confidence: Float,
    val alternativePredictions: List<TypePrediction> = emptyList()
)

data class TypePrediction(
    val type: VehicleType,
    val confidence: Float
)

/**
 * License Plate Recognition
 */
data class LicensePlateDetection(
    val plateNumber: String,
    val boundingBox: BoundingBox,
    val confidence: Float,
    val region: String? = null, // State/Province code
    val characters: List<CharacterDetection> = emptyList(),
    val isValid: Boolean
)

data class CharacterDetection(
    val character: String,
    val confidence: Float,
    val boundingBox: BoundingBox
)

/**
 * Vehicle Color Detection
 */
data class VehicleColor(
    val primaryColor: ColorName,
    val secondaryColor: ColorName? = null,
    val confidence: Float
)

enum class ColorName {
    WHITE, BLACK, SILVER, GRAY, RED, BLUE, GREEN, YELLOW, ORANGE, BROWN, GOLD, BEIGE, UNKNOWN
}

enum class VehicleOrientation {
    FRONT,
    REAR,
    LEFT_SIDE,
    RIGHT_SIDE,
    FRONT_LEFT,
    FRONT_RIGHT,
    REAR_LEFT,
    REAR_RIGHT,
    UNKNOWN
}

/**
 * Additional vehicle features detected by AI
 */
data class VehicleFeatures(
    val hasRoofRack: Boolean = false,
    val hasSpareTire: Boolean = false,
    val doorCount: Int? = null,
    val bodyStyle: String? = null
)

/**
 * ==========================================
 * AI-Based Tire Crack/Damage Detection Models
 * ==========================================
 */

/**
 * Tire Scan Result - Complete AI analysis of tire
 */
data class TireScanResult(
    val scanId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val tirePosition: TirePosition,
    val images: List<TireImage>,
    val crackDetection: CrackDetectionResult,
    val wearAnalysis: WearAnalysisResult,
    val sidewallAnalysis: SidewallAnalysisResult,
    val treadAnalysis: TreadAnalysisResult,
    val overallCondition: TireConditionAssessment,
    val processingTime: Long, // milliseconds
    val aiModelVersion: String
)

/**
 * Tire Image captured during scanning
 */
data class TireImage(
    val imageId: String,
    val imageUrl: String? = null,
    val imageBase64: String? = null,
    val captureAngle: TireCaptureAngle,
    val imageType: TireImageType,
    val width: Int,
    val height: Int,
    val timestamp: Long = System.currentTimeMillis()
)

enum class TireCaptureAngle {
    TREAD_FRONT,      // Top view of tread
    SIDEWALL_OUTER,   // Outer sidewall
    SIDEWALL_INNER,   // Inner sidewall (if accessible)
    FULL_TIRE,        // Complete tire view
    VALVE_AREA,       // Close-up of valve stem area
    BEAD_AREA         // Tire bead/rim area
}

enum class TireImageType {
    ORIGINAL,         // Original captured image
    ANNOTATED,        // Image with AI annotations
    ENHANCED,         // Enhanced/processed image
    THERMAL           // Thermal imaging (if available)
}

/**
 * Crack Detection using AI Computer Vision
 */
data class CrackDetectionResult(
    val hasCracks: Boolean,
    val detectedCracks: List<DetectedCrack>,
    val crackSeverity: CrackSeverity,
    val totalCrackLength: Float, // in mm
    val crackDensity: Float, // cracks per square cm
    val confidence: Float
)

data class DetectedCrack(
    val crackId: String,
    val crackType: CrackType,
    val location: CrackLocation,
    val boundingBox: BoundingBox,
    val length: Float, // in mm
    val width: Float, // in mm
    val depth: DepthEstimate,
    val severity: CrackSeverity,
    val segmentationMask: String? = null, // Base64 encoded mask
    val confidence: Float
)

enum class CrackType {
    SURFACE_CRACK,        // Minor surface cracks
    DEEP_CRACK,           // Deep structural crack
    HAIRLINE_CRACK,       // Very thin surface crack
    SIDEWALL_CRACK,       // Crack on sidewall
    TREAD_SEPARATION,     // Tread separating from tire
    WEATHER_CRACK,        // Dry rot / weather cracking
    IMPACT_DAMAGE,        // Crack from impact
    RADIAL_CRACK,         // Crack along radial direction
    CIRCUMFERENTIAL_CRACK // Crack around tire circumference
}

enum class CrackLocation {
    TREAD_CENTER,
    TREAD_SHOULDER,
    SIDEWALL_UPPER,
    SIDEWALL_MIDDLE,
    SIDEWALL_LOWER,
    BEAD_AREA,
    GROOVE,
    SIPE
}

enum class CrackSeverity {
    NONE,           // No cracks detected
    MINOR,          // Superficial, monitor
    MODERATE,       // Needs attention soon
    SEVERE,         // Replace soon
    CRITICAL        // Replace immediately
}

data class DepthEstimate(
    val estimatedDepth: Float, // in mm
    val depthCategory: DepthCategory,
    val confidence: Float
)

enum class DepthCategory {
    SURFACE,      // < 1mm
    SHALLOW,      // 1-2mm
    MODERATE,     // 2-4mm
    DEEP,         // 4-6mm
    VERY_DEEP     // > 6mm
}

/**
 * Wear Analysis - AI-based tread wear pattern detection
 */
data class WearAnalysisResult(
    val wearPattern: WearPattern,
    val wearLevel: WearLevel,
    val treadDepthMeasurements: List<TreadDepthMeasurement>,
    val averageTreadDepth: Float, // in mm
    val minimumTreadDepth: Float, // in mm
    val wearUniformity: Float, // 0.0-1.0, 1.0 = perfectly uniform
    val estimatedRemainingLife: TireLifeEstimate,
    val confidence: Float
)

data class TreadDepthMeasurement(
    val location: TreadLocation,
    val depth: Float, // in mm
    val measurementPoint: Point2D,
    val confidence: Float
)

data class Point2D(
    val x: Float,
    val y: Float
)

enum class TreadLocation {
    CENTER,
    INNER_SHOULDER,
    OUTER_SHOULDER,
    INNER_RIB,
    OUTER_RIB,
    GROOVE
}

enum class WearLevel {
    NEW,          // > 8mm
    EXCELLENT,    // 6-8mm
    GOOD,         // 4-6mm
    FAIR,         // 3-4mm
    WORN,         // 1.6-3mm
    CRITICAL      // < 1.6mm (legal limit)
}

data class TireLifeEstimate(
    val remainingKilometers: Int?,
    val remainingMonths: Int?,
    val confidence: Float
)

/**
 * Sidewall Analysis - Damage, bulges, cuts
 */
data class SidewallAnalysisResult(
    val hasDamage: Boolean,
    val detectedAnomalies: List<SidewallAnomaly>,
    val sidewallCondition: SidewallCondition,
    val confidence: Float
)

data class SidewallAnomaly(
    val anomalyId: String,
    val type: SidewallAnomalyType,
    val boundingBox: BoundingBox,
    val severity: AnomalySeverity,
    val description: String,
    val confidence: Float
)

enum class SidewallAnomalyType {
    BULGE,              // Tire bulge (air bubble)
    CUT,                // Cut or slice
    PUNCTURE,           // Puncture hole
    ABRASION,           // Surface abrasion
    IMPACT_DAMAGE,      // Impact mark
    SCUFF,              // Scuff mark
    SIDEWALL_SEPARATION,// Separation from belt
    WEATHER_CHECKING,   // Dry rot pattern
    EXPOSED_CORD,       // Visible tire cord/belt
    UNEVEN_WEAR         // Irregular wear pattern
}

enum class AnomalySeverity {
    COSMETIC,   // Visual only, no structural concern
    MINOR,      // Monitor
    MODERATE,   // Address soon
    SEVERE,     // Immediate attention
    CRITICAL    // Unsafe, replace immediately
}

enum class SidewallCondition {
    EXCELLENT,
    GOOD,
    FAIR,
    POOR,
    UNSAFE
}

/**
 * Tread Analysis - Pattern, foreign objects
 */
data class TreadAnalysisResult(
    val hasForeignObjects: Boolean,
    val detectedObjects: List<ForeignObject>,
    val treadPattern: TreadPatternAnalysis,
    val confidence: Float
)

data class ForeignObject(
    val objectId: String,
    val objectType: ForeignObjectType,
    val boundingBox: BoundingBox,
    val isPenetrating: Boolean, // Is it embedded or just stuck?
    val riskLevel: RiskLevel,
    val confidence: Float
)

enum class ForeignObjectType {
    NAIL,
    SCREW,
    GLASS,
    METAL_FRAGMENT,
    STONE,
    WOOD,
    PLASTIC,
    WIRE,
    UNKNOWN
}

enum class RiskLevel {
    LOW,        // Surface, easy to remove
    MEDIUM,     // Partially embedded
    HIGH,       // Fully embedded, potential leak
    CRITICAL    // Active leak or major damage
}

data class TreadPatternAnalysis(
    val patternType: TreadPatternType,
    val patternCondition: PatternCondition,
    val blocksWorn: Boolean,
    val sipesVisible: Boolean,
    val groovesClean: Boolean
)

enum class TreadPatternType {
    SYMMETRIC,
    ASYMMETRIC,
    DIRECTIONAL,
    MULTI_DIRECTIONAL,
    UNKNOWN
}

enum class PatternCondition {
    EXCELLENT,  // Pattern clearly visible
    GOOD,       // Pattern still functional
    WORN,       // Pattern partially worn
    VERY_WORN,  // Pattern barely visible
    BALD        // No pattern visible
}

/**
 * Overall Tire Condition Assessment
 */
data class TireConditionAssessment(
    val overallScore: Float, // 0-100
    val overallStatus: TireHealthStatus,
    val safetyRating: SafetyRating,
    val recommendations: List<TireRecommendation>,
    val criticality: CriticalityLevel,
    val estimatedCost: CostEstimate?
)

enum class SafetyRating {
    SAFE,           // Safe to drive
    SAFE_WITH_CARE, // Safe with precautions
    LIMITED_USE,    // Limited use only
    UNSAFE,         // Not safe to drive
    DANGEROUS       // Immediate hazard
}

data class TireRecommendation(
    val priority: RecommendationPriority,
    val action: RecommendedAction,
    val description: String,
    val timeframe: ActionTimeframe
)

enum class RecommendationPriority {
    INFORMATIONAL,
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

enum class RecommendedAction {
    MONITOR,
    CLEAN,
    ROTATE,
    BALANCE,
    ALIGNMENT_CHECK,
    REPAIR,
    REPLACE,
    IMMEDIATE_REPLACEMENT,
    PROFESSIONAL_INSPECTION
}

enum class ActionTimeframe {
    IMMEDIATE,      // Within 24 hours
    WITHIN_WEEK,    // Within 7 days
    WITHIN_MONTH,   // Within 30 days
    NEXT_SERVICE,   // At next scheduled service
    MONITOR         // Continue monitoring
}

enum class CriticalityLevel {
    NORMAL,
    ATTENTION_NEEDED,
    URGENT,
    CRITICAL,
    EMERGENCY
}

data class CostEstimate(
    val minCost: Float,
    val maxCost: Float,
    val currency: String = "USD",
    val serviceType: String
)

/**
 * ==========================================
 * ESP32 Response Data Structure
 * ==========================================
 */

/**
 * Complete response from ESP32 IntelliInflate station
 */
data class ESP32Response(
    val timestamp: Long = System.currentTimeMillis(),
    val stationId: String,
    val sessionId: String?,
    val responseType: ESP32ResponseType,
    val vehicleDetection: VehicleDetectionResult? = null,
    val tireScan: TireScanResult? = null,
    val systemStatus: SystemStatus? = null,
    val error: ErrorInfo? = null
)

enum class ESP32ResponseType {
    VEHICLE_DETECTED,
    TIRE_SCAN_COMPLETE,
    SESSION_COMPLETE,
    SYSTEM_STATUS,
    ERROR
}

data class SystemStatus(
    val isOnline: Boolean,
    val isBusy: Boolean,
    val currentSession: String?,
    val hardwareStatus: HardwareStatus,
    val lastMaintenance: Long?,
    val softwareVersion: String,
    val aiModelVersion: String
)

data class HardwareStatus(
    val cameraOnline: Boolean,
    val temperature: Float? // ESP32 temperature
)

data class ErrorInfo(
    val errorCode: String,
    val errorMessage: String,
    val errorType: ErrorType,
    val timestamp: Long = System.currentTimeMillis()
)

enum class ErrorType {
    CAMERA_ERROR,
    SENSOR_ERROR,
    AI_MODEL_ERROR,
    COMMUNICATION_ERROR,
    HARDWARE_ERROR,
    CALIBRATION_ERROR,
    UNKNOWN_ERROR
}

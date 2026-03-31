package com.example.intellinflate.models

/**
 * Vehicle Profile - Detected and classified vehicle information
 */
data class VehicleProfile(
    val vehicleId: String,
    val licensePlate: String,
    val vehicleType: VehicleType,
    val make: String? = null,
    val model: String? = null,
    val year: Int? = null,
    val imageUrl: String? = null,
    val detectedAt: Long = System.currentTimeMillis()
)

enum class VehicleType(val displayName: String) {
    MOTORCYCLE("Motorcycle"),
    HATCHBACK("Hatchback"),
    SEDAN("Sedan"),
    SUV("SUV"),
    PICKUP_TRUCK("Pickup Truck"),
    VAN("Van"),
    TRUCK("Truck"),
    BUS("Bus"),
    UNKNOWN("Unknown")
}

enum class TirePosition(val displayName: String) {
    FRONT_LEFT("Front Left"),
    FRONT_RIGHT("Front Right"),
    REAR_LEFT("Rear Left"),
    REAR_RIGHT("Rear Right")
}

/**
 * Tire Health Report - Comprehensive tire diagnostics
 */
data class TireHealthReport(
    val reportId: String,
    val vehicle: VehicleProfile,
    val timestamp: Long = System.currentTimeMillis(),
    val tireConditions: Map<TirePosition, TireCondition>,
    val overallHealth: TireHealthStatus,
    val recommendations: List<String>,
    val warnings: List<TireWarning>
)

data class TireCondition(
    val position: TirePosition,
    val temperature: Float?,
    val treadDepth: TreadDepthStatus,
    val wearPattern: WearPattern,
    val condition: TireHealthStatus,
    val notes: String? = null
)

enum class TireHealthStatus(val color: Long, val displayName: String) {
    EXCELLENT(0xFF10B981, "Excellent"),
    GOOD(0xFF3B82F6, "Good"),
    FAIR(0xFFF59E0B, "Fair"),
    POOR(0xFFEF4444, "Poor"),
    CRITICAL(0xFF991B1B, "Critical")
}

enum class TreadDepthStatus {
    EXCELLENT,  // > 7mm
    GOOD,       // 5-7mm
    FAIR,       // 3-5mm
    WORN,       // 1.6-3mm (consider replacement)
    ILLEGAL     // < 1.6mm (must replace)
}

enum class WearPattern {
    NORMAL,
    CENTER_WEAR,        // Over-inflation
    EDGE_WEAR,          // Under-inflation
    ONE_SIDE_WEAR,      // Alignment issues
    CUPPING_SCALLOPING, // Suspension issues
    FEATHERING          // Alignment issues
}

/**
 * Tire Warning
 */
data class TireWarning(
    val type: WarningType,
    val message: String,
    val severity: WarningSeverity,
    val affectedTires: List<TirePosition>
)

enum class WarningType {
    WORN_TREAD,
    IRREGULAR_WEAR,
    HIGH_TEMPERATURE,
    SIDEWALL_DAMAGE,
    FOREIGN_OBJECT,
    CALIBRATION_NEEDED
}

enum class WarningSeverity {
    INFO,
    WARNING,
    CRITICAL
}

/**
 * Service History - Historical records of tire services
 */
data class ServiceHistory(
    val historyId: String,
    val vehicle: VehicleProfile,
    val services: List<ServiceRecord>
)

data class ServiceRecord(
    val recordId: String,
    val timestamp: Long,
    val serviceType: ServiceType,
    val report: TireHealthReport?,
    val notes: String? = null,
    val stationId: String? = null,
    val technician: String? = null
)

enum class ServiceType {
    TIRE_INSPECTION,
    FULL_DIAGNOSTIC
}

/**
 * IntelliInflate Station Information
 */
data class PsiPilotStation(
    val stationId: String,
    val name: String,
    val location: String,
    val ipAddress: String,
    val port: Int = 80,
    val status: StationStatus,
    val capabilities: List<StationCapability>,
    val lastSeen: Long = System.currentTimeMillis()
)

enum class StationStatus {
    ONLINE,
    OFFLINE,
    BUSY,
    MAINTENANCE,
    ERROR
}

enum class StationCapability {
    TIRE_HEALTH_SCAN,
    VEHICLE_DETECTION,
    NUMBER_PLATE_RECOGNITION,
    TEMPERATURE_MONITORING
}


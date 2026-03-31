package com.example.intellinflate.database

/**
 * MongoDB Atlas Collections for IntelliInflate
 *
 * Collection: tire_scans
 * Collection: vehicles
 * Collection: service_history
 *
 * These are plain data classes serialised to/from JSON by Gson.
 * Field names match the Atlas collection document schema.
 */

// ─── Collection: tire_scans ───────────────────────────────────────────────────

data class TireScanDocument(
    val _id: String = java.util.UUID.randomUUID().toString(),
    val scanId: String,
    val timestampMillis: Long = System.currentTimeMillis(),
    val tirePosition: String,          // TirePosition.name  e.g. "FRONT_LEFT"
    val overallScore: Float,           // 0–100
    val healthStatus: String,          // TireHealthStatus.name  e.g. "GOOD"
    val hasCracks: Boolean,
    val crackSeverity: String,         // CrackSeverity.name  e.g. "NONE"
    val hasForeignObjects: Boolean,
    val hasSidewallDamage: Boolean,
    val averageTreadDepth: Float,      // mm
    val estimatedRemainingLifeKm: Int,
    val aiModelVersion: String,
    val licensePlate: String,
    val vehicleId: String
)

// ─── Collection: vehicles ─────────────────────────────────────────────────────

data class VehicleDocument(
    val _id: String = java.util.UUID.randomUUID().toString(),
    val vehicleId: String,
    val licensePlate: String,
    val vehicleType: String,           // VehicleType.name  e.g. "SEDAN"
    val make: String,
    val model: String,
    val year: Int,
    val registeredAtMillis: Long = System.currentTimeMillis()
)

// ─── Collection: users ───────────────────────────────────────────────────────

data class UserDocument(
    val _id: String = java.util.UUID.randomUUID().toString(),
    val userId: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val email: String,
    val phone: String = "",
    val role: String = "TECHNICIAN",       // e.g. "ADMIN", "TECHNICIAN", "VIEWER"
    val assignedStationId: String = "",
    val createdAtMillis: Long = System.currentTimeMillis(),
    val lastLoginMillis: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)

// ─── Collection: service_history ─────────────────────────────────────────────

data class ServiceHistoryDocument(
    val _id: String = java.util.UUID.randomUUID().toString(),
    val serviceId: String = java.util.UUID.randomUUID().toString(),
    val vehicleId: String,
    val licensePlate: String,
    val serviceType: String,           // e.g. "TYRE_SCAN", "TYRE_REPLACE"
    val description: String,
    val technicianNote: String = "",
    val performedAtMillis: Long = System.currentTimeMillis(),
    val costEstimate: Float = 0f
)

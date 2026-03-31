package com.example.intellinflate.models

data class VehicleData(
    val speed: Float,  // in km/h
    val batteryVoltage: Float,  // in Volts
    val engineTemperature: Float,  // in Celsius
    val fuelLevel: Float,  // percentage
    val odometer: Float,  // in km
    val engineStatus: EngineStatus,
    val lastUpdated: Long = System.currentTimeMillis()
)

enum class EngineStatus {
    OFF,
    IDLE,
    RUNNING,
    WARNING,
    ERROR
}

data class VehicleHealth(
    val overallStatus: HealthStatus,
    val alerts: List<Alert> = emptyList()
)

enum class HealthStatus {
    EXCELLENT,
    GOOD,
    WARNING,
    CRITICAL
}

data class Alert(
    val type: AlertType,
    val message: String,
    val severity: AlertSeverity,
    val timestamp: Long = System.currentTimeMillis()
)

enum class AlertType {
    TIRE_PRESSURE,
    TIRE_TEMPERATURE,
    ENGINE_TEMPERATURE,
    BATTERY,
    FUEL,
    CONNECTION
}

enum class AlertSeverity {
    INFO,
    WARNING,
    CRITICAL
}

package com.example.intellinflate.models

data class TireData(
    val position: TirePosition,
    val pressure: Float,  // in PSI
    val temperature: Float,  // in Celsius
    val health: TireHealth,
    val lastUpdated: Long = System.currentTimeMillis()
)

enum class TireHealth {
    GOOD,
    WARNING,
    CRITICAL;
    
    companion object {
        fun fromPressure(pressure: Float, optimalPressure: Float = 32.0f): TireHealth {
            return when {
                pressure < optimalPressure * 0.8f -> CRITICAL
                pressure < optimalPressure * 0.9f -> WARNING
                pressure > optimalPressure * 1.2f -> WARNING
                pressure > optimalPressure * 1.3f -> CRITICAL
                else -> GOOD
            }
        }
    }
}

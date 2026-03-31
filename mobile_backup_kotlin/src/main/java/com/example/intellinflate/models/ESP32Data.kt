package com.example.intellinflate.models

import com.google.gson.annotations.SerializedName

/**
 * Data class representing the complete data packet from ESP32
 * Expected JSON format:
 * {
 *   "tires": [
 *     {"pos": 0, "pressure": 32.5, "temp": 25.0},
 *     {"pos": 1, "pressure": 31.8, "temp": 24.5},
 *     {"pos": 2, "pressure": 33.0, "temp": 26.0},
 *     {"pos": 3, "pressure": 32.2, "temp": 25.5}
 *   ],
 *   "vehicle": {
 *     "speed": 60.5,
 *     "battery": 12.6,
 *     "engineTemp": 85.0,
 *     "fuel": 75.0,
 *     "odometer": 12345.6,
 *     "engineStatus": 2
 *   }
 * }
 */
data class ESP32Data(
    @SerializedName("tires")
    val tires: List<TireDataRaw>,
    
    @SerializedName("vehicle")
    val vehicle: VehicleDataRaw
)

data class TireDataRaw(
    @SerializedName("pos")
    val position: Int,  // 0: FL, 1: FR, 2: RL, 3: RR
    
    @SerializedName("pressure")
    val pressure: Float,
    
    @SerializedName("temp")
    val temperature: Float
)

data class VehicleDataRaw(
    @SerializedName("speed")
    val speed: Float,
    
    @SerializedName("battery")
    val battery: Float,
    
    @SerializedName("engineTemp")
    val engineTemp: Float,
    
    @SerializedName("fuel")
    val fuel: Float,
    
    @SerializedName("odometer")
    val odometer: Float,
    
    @SerializedName("engineStatus")
    val engineStatus: Int  // 0: OFF, 1: IDLE, 2: RUNNING, 3: WARNING, 4: ERROR
)

// Extension functions to convert raw data to app models
fun TireDataRaw.toTireData(): TireData {
    val tirePosition = when (position) {
        0 -> TirePosition.FRONT_LEFT
        1 -> TirePosition.FRONT_RIGHT
        2 -> TirePosition.REAR_LEFT
        3 -> TirePosition.REAR_RIGHT
        else -> TirePosition.FRONT_LEFT
    }
    
    return TireData(
        position = tirePosition,
        pressure = pressure,
        temperature = temperature,
        health = TireHealth.fromPressure(pressure)
    )
}

fun VehicleDataRaw.toVehicleData(): VehicleData {
    val status = when (engineStatus) {
        0 -> EngineStatus.OFF
        1 -> EngineStatus.IDLE
        2 -> EngineStatus.RUNNING
        3 -> EngineStatus.WARNING
        4 -> EngineStatus.ERROR
        else -> EngineStatus.OFF
    }
    
    return VehicleData(
        speed = speed,
        batteryVoltage = battery,
        engineTemperature = engineTemp,
        fuelLevel = fuel,
        odometer = odometer,
        engineStatus = status
    )
}

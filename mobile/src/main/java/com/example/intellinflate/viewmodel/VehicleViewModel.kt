package com.example.intellinflate.viewmodel

import android.app.Application
import android.bluetooth.BluetoothDevice
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.intellinflate.bluetooth.BluetoothConnectionState
import com.example.intellinflate.bluetooth.ESP32BluetoothManager
import com.example.intellinflate.models.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlin.random.Random

data class VehicleUIState(
    val connectionState: BluetoothConnectionState = BluetoothConnectionState.Disconnected,
    val tires: Map<TirePosition, TireData> = emptyMap(),
    val vehicleData: VehicleData? = null,
    val vehicleHealth: VehicleHealth = VehicleHealth(HealthStatus.GOOD),
    val pairedDevices: List<BluetoothDevice> = emptyList(),
    val selectedDevice: BluetoothDevice? = null,
    val isBluetoothEnabled: Boolean = false,
    val isDemoMode: Boolean = false
)

class VehicleViewModel(application: Application) : AndroidViewModel(application) {
    
    private val bluetoothManager = ESP32BluetoothManager(application.applicationContext)
    
    private val _uiState = MutableStateFlow(VehicleUIState())
    val uiState: StateFlow<VehicleUIState> = _uiState.asStateFlow()
    
    init {
        // Observe connection state
        viewModelScope.launch {
            bluetoothManager.connectionState.collect { connectionState ->
                _uiState.update { it.copy(connectionState = connectionState) }
            }
        }
        
        // Observe ESP32 data
        viewModelScope.launch {
            bluetoothManager.esp32Data.collect { esp32Data ->
                esp32Data?.let {
                    updateVehicleData(it)
                }
            }
        }
        
        // Initialize
        refreshBluetoothDevices()
    }
    
    fun refreshBluetoothDevices() {
        val isEnabled = bluetoothManager.isBluetoothEnabled()
        val pairedDevices = if (isEnabled) {
            bluetoothManager.getPairedDevices()
        } else {
            emptyList()
        }
        
        // Auto-select ESP32 device if found
        val esp32Device = bluetoothManager.findESP32Device()
        
        _uiState.update {
            it.copy(
                isBluetoothEnabled = isEnabled,
                pairedDevices = pairedDevices,
                selectedDevice = esp32Device ?: it.selectedDevice
            )
        }
    }
    
    fun selectDevice(device: BluetoothDevice) {
        _uiState.update { it.copy(selectedDevice = device) }
    }
    
    fun connectToSelectedDevice() {
        val device = _uiState.value.selectedDevice ?: return
        viewModelScope.launch {
            bluetoothManager.connectToDevice(device)
        }
    }
    
    fun connectToDevice(device: BluetoothDevice) {
        viewModelScope.launch {
            bluetoothManager.connectToDevice(device)
        }
    }
    
    fun disconnect() {
        bluetoothManager.disconnect()
        stopDemoMode()
    }
    
    fun toggleDemoMode() {
        val newDemoState = !_uiState.value.isDemoMode
        _uiState.update { it.copy(isDemoMode = newDemoState) }
        
        if (newDemoState) {
            startDemoMode()
        } else {
            stopDemoMode()
        }
    }
    
    private fun startDemoMode() {
        viewModelScope.launch {
            while (_uiState.value.isDemoMode) {
                generateDemoData()
                delay(2000) // Update every 2 seconds
            }
        }
    }
    
    private fun stopDemoMode() {
        _uiState.update { it.copy(isDemoMode = false) }
    }
    
    private fun generateDemoData() {
        val tires = listOf(
            TireDataRaw(0, 30f + Random.nextFloat() * 5f, 25f + Random.nextFloat() * 15f),
            TireDataRaw(1, 30f + Random.nextFloat() * 5f, 25f + Random.nextFloat() * 15f),
            TireDataRaw(2, 30f + Random.nextFloat() * 5f, 25f + Random.nextFloat() * 15f),
            TireDataRaw(3, 30f + Random.nextFloat() * 5f, 25f + Random.nextFloat() * 15f)
        )
        
        val vehicle = VehicleDataRaw(
            speed = Random.nextFloat() * 120f,
            battery = 11.5f + Random.nextFloat() * 1.5f,
            engineTemp = 70f + Random.nextFloat() * 40f,
            fuel = 20f + Random.nextFloat() * 80f,
            odometer = 12345f + Random.nextFloat() * 100f,
            engineStatus = Random.nextInt(5)
        )
        
        updateVehicleData(ESP32Data(tires, vehicle))
    }
    
    fun sendCommand(command: String) {
        viewModelScope.launch {
            bluetoothManager.sendCommand(command)
        }
    }
    
    private fun updateVehicleData(esp32Data: ESP32Data) {
        // Convert tire data
        val tiresMap = esp32Data.tires.associate { tireRaw ->
            val tireData = tireRaw.toTireData()
            tireData.position to tireData
        }
        
        // Convert vehicle data
        val vehicleData = esp32Data.vehicle.toVehicleData()
        
        // Calculate health status
        val alerts = mutableListOf<Alert>()
        
        // Check tire pressures
        tiresMap.values.forEach { tire ->
            when (tire.health) {
                TireHealth.CRITICAL -> {
                    alerts.add(
                        Alert(
                            type = AlertType.TIRE_PRESSURE,
                            message = "${tire.position.name} tire pressure critical: ${tire.pressure} PSI",
                            severity = AlertSeverity.CRITICAL
                        )
                    )
                }
                TireHealth.WARNING -> {
                    alerts.add(
                        Alert(
                            type = AlertType.TIRE_PRESSURE,
                            message = "${tire.position.name} tire pressure warning: ${tire.pressure} PSI",
                            severity = AlertSeverity.WARNING
                        )
                    )
                }
                else -> {}
            }
            
            // Check tire temperature
            if (tire.temperature > 80.0f) {
                alerts.add(
                    Alert(
                        type = AlertType.TIRE_TEMPERATURE,
                        message = "${tire.position.name} tire temperature high: ${tire.temperature}°C",
                        severity = if (tire.temperature > 100.0f) AlertSeverity.CRITICAL else AlertSeverity.WARNING
                    )
                )
            }
        }
        
        // Check engine temperature
        if (vehicleData.engineTemperature > 100.0f) {
            alerts.add(
                Alert(
                    type = AlertType.ENGINE_TEMPERATURE,
                    message = "Engine temperature high: ${vehicleData.engineTemperature}°C",
                    severity = if (vehicleData.engineTemperature > 120.0f) 
                        AlertSeverity.CRITICAL else AlertSeverity.WARNING
                )
            )
        }
        
        // Check battery
        if (vehicleData.batteryVoltage < 11.5f) {
            alerts.add(
                Alert(
                    type = AlertType.BATTERY,
                    message = "Battery voltage low: ${vehicleData.batteryVoltage}V",
                    severity = if (vehicleData.batteryVoltage < 11.0f) 
                        AlertSeverity.CRITICAL else AlertSeverity.WARNING
                )
            )
        }
        
        // Check fuel level
        if (vehicleData.fuelLevel < 15.0f) {
            alerts.add(
                Alert(
                    type = AlertType.FUEL,
                    message = "Fuel level low: ${vehicleData.fuelLevel}%",
                    severity = if (vehicleData.fuelLevel < 5.0f) 
                        AlertSeverity.CRITICAL else AlertSeverity.WARNING
                )
            )
        }
        
        // Determine overall health status
        val overallStatus = when {
            alerts.any { it.severity == AlertSeverity.CRITICAL } -> HealthStatus.CRITICAL
            alerts.any { it.severity == AlertSeverity.WARNING } -> HealthStatus.WARNING
            alerts.any { it.severity == AlertSeverity.INFO } -> HealthStatus.GOOD
            else -> HealthStatus.EXCELLENT
        }
        
        val vehicleHealth = VehicleHealth(
            overallStatus = overallStatus,
            alerts = alerts
        )
        
        _uiState.update {
            it.copy(
                tires = tiresMap,
                vehicleData = vehicleData,
                vehicleHealth = vehicleHealth
            )
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        bluetoothManager.disconnect()
    }
}

package com.example.intellinflate.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.intellinflate.models.*
import com.example.intellinflate.network.ESP32WiFiManager
import com.example.intellinflate.network.NetworkConfig
import com.example.intellinflate.database.MongoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

/**
 * Main ViewModel for IntelliInflate Application
 * Tire Health Monitoring System
 */
class PsiPilotViewModel(application: Application) : AndroidViewModel(application) {
    
    private val TAG = "PsiPilotViewModel"
    private val wifiManager = ESP32WiFiManager()
    private val mongoRepository = MongoRepository.getInstance()
    
    private val _uiState = MutableStateFlow(PsiPilotUIState())
    val uiState: StateFlow<PsiPilotUIState> = _uiState.asStateFlow()
    
    init {
        // Initialize with default state
        _uiState.update { it.copy(selectedTab = NavigationTab.LOGIN) }
    }

    // ===== Authentication (Login / Register) =====

    fun login(identifier: String, password: String) {
        viewModelScope.launch {
            Log.e(TAG, 
 "Attempting login for $identifier")
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            val result = withContext(Dispatchers.IO) {
                try {
                    val url = URL(NetworkConfig.LOGIN_URL)
                    Log.e(TAG, 
 "Connecting to $url")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.connectTimeout = 5000
                    conn.readTimeout = 5000
                    conn.doOutput = true
                    
                    val jsonParam = JSONObject()
                    jsonParam.put("identifier", identifier)
                    jsonParam.put("password", password)
                    
                    val os = OutputStreamWriter(conn.outputStream)
                    os.write(jsonParam.toString())
                    os.flush()
                    os.close()
                    
                    val responseCode = conn.responseCode
                    Log.e(TAG, 
 "Login response code: $responseCode")
                    
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        val response = conn.inputStream.bufferedReader().readText()
                        val jsonResponse = JSONObject(response)
                        val userData = jsonResponse.getJSONObject("user")
                        
                        Result.success(userData)
                    } else {
                        val errorResponse = conn.errorStream?.bufferedReader()?.readText() ?: "Login failed"
                        Log.e(TAG, "Login error response: $errorResponse")
                        val errorMessage = try {
                            JSONObject(errorResponse).getString("error")
                        } catch (e: Exception) {
                            "Invalid credentials"
                        }
                        Result.failure(Exception(errorMessage))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Login exception", e)
                    Result.failure(e)
                }
            }
            
            result.onSuccess { userData ->
                val vehicleProfile = VehicleProfile(
                    vehicleId = userData.getString("id"),
                    licensePlate = userData.getString("numberPlate"),
                    vehicleType = VehicleType.UNKNOWN, // Default until updated
                    model = userData.optString("vehicleModel")
                )
                
                _uiState.update { it.copy(
                    isLoading = false,
                    isUserLoggedIn = true,
                    currentUser = vehicleProfile,
                    currentVehicle = vehicleProfile,
                    selectedTab = NavigationTab.DASHBOARD
                )}
                initializeMockData() // Initialize dashboard data
            }.onFailure { error ->
                _uiState.update { it.copy(
                    isLoading = false,
                    error = error.message ?: "Network error"
                )}
            }
        }
    }

    fun register(username: String, email: String, password: String, numberPlate: String, vehicleModel: String, phone: String) {
        viewModelScope.launch {
            Log.e(TAG, 
 "Starting registration for $email, plate: $numberPlate")
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            val result = withContext(Dispatchers.IO) {
                try {
                    val url = URL(NetworkConfig.REGISTER_URL)
                    Log.e(TAG, 
 "Connecting to $url")
                    val conn = url.openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.connectTimeout = 5000
                    conn.readTimeout = 5000
                    conn.doOutput = true
                    
                    val jsonParam = JSONObject()
                    jsonParam.put("username", username)
                    jsonParam.put("email", email)
                    jsonParam.put("password", password)
                    jsonParam.put("numberPlate", numberPlate)
                    jsonParam.put("vehicleModel", vehicleModel)
                    jsonParam.put("phone", phone)
                    
                    val os = OutputStreamWriter(conn.outputStream)
                    os.write(jsonParam.toString())
                    os.flush()
                    os.close()
                    
                    val responseCode = conn.responseCode
                    Log.e(TAG, 
 "Registration response code: $responseCode")
                    
                    if (responseCode == HttpURLConnection.HTTP_CREATED) {
                        Result.success(true)
                    } else {
                        val errorResponse = conn.errorStream?.bufferedReader()?.readText() ?: "Registration failed"
                        Log.e(TAG, "Registration error response: $errorResponse")
                        val errorMessage = try {
                            JSONObject(errorResponse).getString("error")
                        } catch (e: Exception) {
                            "Registration failed ($responseCode)"
                        }
                        Result.failure(Exception(errorMessage))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Registration exception: ${e.message}", e)
                    Result.failure(e)
                }
            }
            
            Log.e(TAG, 
 "Registration result: ${result.isSuccess}")
            if (result.isFailure) {
                Log.e(TAG, "Registration error: ${result.exceptionOrNull()?.message}")
            }

            result.onSuccess {
                _uiState.update { it.copy(
                    isLoading = false,
                    selectedTab = NavigationTab.LOGIN,
                    error = null
                )}
                // Show success message could be implemented with a separate UI state field
            }.onFailure { error ->
                _uiState.update { it.copy(
                    isLoading = false,
                    error = error.message ?: "Network error"
                )}
            }
        }
    }

    fun logout() {
        _uiState.update { PsiPilotUIState(selectedTab = NavigationTab.LOGIN) }
    }
    
    private fun initializeMockData() {
        _uiState.value = PsiPilotUIState(
            connectionState = ConnectionStatus.CONNECTED,
            stationInfo = PsiPilotStation(
                stationId = "INTELLI-INFLATE-001",
                name = "IntelliInflate Station",
                location = "Bay 1 - Development",
                ipAddress = "192.168.1.100",
                status = StationStatus.ONLINE,
                capabilities = listOf(
                    StationCapability.VEHICLE_DETECTION,
                    StationCapability.TIRE_HEALTH_SCAN,
                    StationCapability.NUMBER_PLATE_RECOGNITION
                )
            ),
            currentVehicle = VehicleProfile(
                vehicleId = "VEH-001",
                licensePlate = "ABC-1234",
                vehicleType = VehicleType.SEDAN
            ),
            tireScanResults = mapOf(
                TirePosition.FRONT_LEFT to mockScan(TirePosition.FRONT_LEFT, 88f, CrackSeverity.NONE, false),
                TirePosition.FRONT_RIGHT to mockScan(TirePosition.FRONT_RIGHT, 65f, CrackSeverity.MINOR, false),
                TirePosition.REAR_LEFT to mockScan(TirePosition.REAR_LEFT, 91f, CrackSeverity.NONE, false),
                TirePosition.REAR_RIGHT to mockScan(TirePosition.REAR_RIGHT, 42f, CrackSeverity.MODERATE, true)
            )
        )
    }

    private fun mockScan(pos: TirePosition, score: Float, crackSev: CrackSeverity, hasForeign: Boolean): TireScanResult {
        val hasCracks = crackSev != CrackSeverity.NONE
        return TireScanResult(
            scanId = "SCAN-${pos.name}",
            tirePosition = pos,
            images = emptyList(),
            crackDetection = CrackDetectionResult(
                hasCracks = hasCracks,
                detectedCracks = emptyList(),
                crackSeverity = crackSev,
                totalCrackLength = if (hasCracks) 18f else 0f,
                crackDensity = if (hasCracks) 0.12f else 0f,
                confidence = 0.93f
            ),
            wearAnalysis = WearAnalysisResult(
                wearPattern = WearPattern.NORMAL,
                wearLevel = if (score >= 80f) WearLevel.GOOD else if (score >= 60f) WearLevel.FAIR else WearLevel.WORN,
                treadDepthMeasurements = emptyList(),
                averageTreadDepth = if (score >= 80f) 6.5f else if (score >= 60f) 4.2f else 2.8f,
                minimumTreadDepth = if (score >= 80f) 5.8f else if (score >= 60f) 3.5f else 2.1f,
                wearUniformity = 0.88f,
                estimatedRemainingLife = TireLifeEstimate(remainingKilometers = (score * 200).toInt(), remainingMonths = (score / 10).toInt(), confidence = 0.85f),
                confidence = 0.91f
            ),
            sidewallAnalysis = SidewallAnalysisResult(
                hasDamage = score < 50f,
                detectedAnomalies = emptyList(),
                sidewallCondition = if (score >= 80f) SidewallCondition.GOOD else if (score >= 60f) SidewallCondition.FAIR else SidewallCondition.POOR,
                confidence = 0.89f
            ),
            treadAnalysis = TreadAnalysisResult(
                hasForeignObjects = hasForeign,
                detectedObjects = emptyList(),
                treadPattern = TreadPatternAnalysis(
                    patternType = TreadPatternType.SYMMETRIC,
                    patternCondition = if (score >= 80f) PatternCondition.GOOD else PatternCondition.WORN,
                    blocksWorn = score < 60f,
                    sipesVisible = score >= 50f,
                    groovesClean = !hasForeign
                ),
                confidence = 0.87f
            ),
            overallCondition = TireConditionAssessment(
                overallScore = score,
                overallStatus = if (score >= 85f) TireHealthStatus.EXCELLENT else if (score >= 70f) TireHealthStatus.GOOD
                    else if (score >= 50f) TireHealthStatus.FAIR else TireHealthStatus.POOR,
                safetyRating = if (score >= 70f) SafetyRating.SAFE else if (score >= 50f) SafetyRating.SAFE_WITH_CARE else SafetyRating.UNSAFE,
                recommendations = listOf(
                    TireRecommendation(
                        priority = if (score < 50f) RecommendationPriority.CRITICAL else RecommendationPriority.LOW,
                        action = if (score < 50f) RecommendedAction.REPLACE else RecommendedAction.MONITOR,
                        description = if (score < 50f) "Tyre requires immediate replacement" else "Continue regular monitoring",
                        timeframe = if (score < 50f) ActionTimeframe.IMMEDIATE else ActionTimeframe.NEXT_SERVICE
                    )
                ),
                criticality = if (score >= 70f) CriticalityLevel.NORMAL else if (score >= 50f) CriticalityLevel.ATTENTION_NEEDED else CriticalityLevel.URGENT,
                estimatedCost = null
            ),
            processingTime = 320L,
            aiModelVersion = "v2.1.0"
        )
    }

    // ===== MongoDB / Realm Database =====

    /**
     * Persist the current mock scan results to the local Realm database.
     * Called once on init; real scans should call [saveScanToDatabase] directly.
     */
    private fun persistMockDataToDatabase() {
        viewModelScope.launch {
            try {
                val state = _uiState.value
                val plate = state.currentVehicle?.licensePlate ?: ""
                state.currentVehicle?.let { mongoRepository.saveVehicle(it) }
                state.tireScanResults.values.forEach { scan ->
                    mongoRepository.saveTireScan(scan, plate)
                }
                Log.e(TAG, 
 "Mock data persisted to Realm database")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to persist mock data to Realm", e)
            }
        }
    }

    /**
     * Save a single tire scan result to the database and update UI state.
     */
    fun saveScanToDatabase(scan: TireScanResult) {
        viewModelScope.launch {
            try {
                val plate = _uiState.value.currentVehicle?.licensePlate ?: ""
                mongoRepository.saveTireScan(scan, plate)
                mongoRepository.addServiceEvent(
                    vehicleId = _uiState.value.currentVehicle?.vehicleId ?: "",
                    licensePlate = plate,
                    serviceType = "TYRE_SCAN",
                    description = "Tyre scan: ${scan.tirePosition.name} — score ${scan.overallCondition.overallScore.toInt()}/100",
                    costEstimate = scan.overallCondition.estimatedCost?.minCost ?: 0f
                )
                Log.e(TAG, 
 "Scan saved to Realm: ${scan.scanId}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save scan to Realm", e)
            }
        }
    }

    /**
     * Load the latest scan records per position from Realm and refresh UI state.
     */
    fun refreshFromDatabase() {
        viewModelScope.launch {
            try {
                val plate = _uiState.value.currentVehicle?.licensePlate ?: ""
                val latestScans = mongoRepository.getLatestScansPerPosition(plate)
                Log.e(TAG, 
 "Loaded ${latestScans.size} scan records from Realm")
                // Optionally map back to domain model here if needed
            } catch (e: Exception) {
                Log.e(TAG, "Failed to refresh from Realm", e)
            }
        }
    }

    /**
     * Retrieve all service history events for the current vehicle.
     */
    suspend fun getServiceHistory(): List<com.example.intellinflate.database.ServiceHistoryDocument> {
        val plate = _uiState.value.currentVehicle?.licensePlate ?: return emptyList()
        return try {
            mongoRepository.getServiceHistory(plate)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load service history", e)
            emptyList()
        }
    }

    // ===== Connection Management =====

    fun connectToStation(ipAddress: String, port: Int = 80) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            val config = ESP32WiFiManager.ConnectionConfig(
                ipAddress = ipAddress,
                port = port,
                enableAutoPolling = true
            )
            
            wifiManager.connect(config)
                .onSuccess { station ->
                    Log.e(TAG, 
 "Connected to station: ${station.name}")
                    _uiState.update { it.copy(isLoading = false) }
                }
                .onFailure { error ->
                    Log.e(TAG, "Connection failed", error)
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = "Connection failed: ${error.message}"
                    )}
                }
        }
    }
    
    fun disconnect() {
        wifiManager.disconnect()
        _uiState.value = PsiPilotUIState()
    }
    
    // ===== Vehicle Detection =====
    
    fun detectVehicle() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            wifiManager.requestVehicleDetection()
                .onSuccess { result ->
                    Log.e(TAG, 
 "Vehicle detected: ${result.vehicles.size} vehicle(s)")
                    
                    val detectedVehicle = result.vehicles.firstOrNull()
                    if (detectedVehicle != null) {
                        val vehicleProfile = VehicleProfile(
                            vehicleId = detectedVehicle.vehicleId,
                            licensePlate = detectedVehicle.licensePlate?.plateNumber ?: "UNKNOWN",
                            vehicleType = detectedVehicle.vehicleType.type,
                            imageUrl = result.imageUrl
                        )
                        
                        _uiState.update { it.copy(
                            vehicleDetectionResult = result,
                            currentVehicle = vehicleProfile,
                            isLoading = false
                        )}
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = "Vehicle detection failed: ${error.message}"
                    )}
                }
        }
    }
    
    // ===== Tire Scanning =====
    
    fun scanTire(tirePosition: TirePosition) {
        viewModelScope.launch {
            _uiState.update { it.copy(
                isLoading = true,
                currentTireScanning = tirePosition
            )}
            
            wifiManager.requestTireScan(tirePosition)
                .onSuccess { scanResult ->
                    Log.e(TAG, 
 "Tire scan complete for $tirePosition")
                    
                    val updatedScans = _uiState.value.tireScanResults.toMutableMap()
                    updatedScans[tirePosition] = scanResult
                    
                    _uiState.update { it.copy(
                        tireScanResults = updatedScans,
                        currentTireScanning = null,
                        isLoading = false
                    )}
                }
                .onFailure { error ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        currentTireScanning = null,
                        error = "Tire scan failed: ${error.message}"
                    )}
                }
        }
    }
    
    fun scanAllTires() {
        viewModelScope.launch {
            val tires = listOf(
                TirePosition.FRONT_LEFT,
                TirePosition.FRONT_RIGHT,
                TirePosition.REAR_LEFT,
                TirePosition.REAR_RIGHT
            )
            
            for (tire in tires) {
                scanTire(tire)
                delay(2000) // Wait between scans
            }
        }
    }
    
    // ===== Health Monitoring Only =====
    // Focus on AI-based tyre health analysis
    
    // ===== UI Actions =====
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    fun selectNavigationTab(tab: NavigationTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }
    
    override fun onCleared() {
        super.onCleared()
        wifiManager.cleanup()
    }
}

/**
 * UI State for IntelliInflate Application
 * Tire Health Monitoring System
 */
data class PsiPilotUIState(
    val connectionState: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val stationInfo: PsiPilotStation? = null,
    
    // User Authentication
    val isUserLoggedIn: Boolean = false,
    val currentUser: VehicleProfile? = null,
    
    // Vehicle Detection
    val vehicleDetectionResult: VehicleDetectionResult? = null,
    val currentVehicle: VehicleProfile? = null,
    
    // Tire Health Scanning
    val tireScanResults: Map<TirePosition, TireScanResult> = emptyMap(),
    val currentTireScanning: TirePosition? = null,
    
    // UI State
    val selectedTab: NavigationTab = NavigationTab.LOGIN,
    val isLoading: Boolean = false,
    val error: String? = null
)

enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

enum class NavigationTab(val displayName: String) {
    LOGIN("Login"),
    REGISTER("Register"),
    DASHBOARD("Dashboard"),
    VEHICLE_DETECTION("Vehicle Detection"),
    TIRE_SCAN("Health Scan"),
    HISTORY("History")
}

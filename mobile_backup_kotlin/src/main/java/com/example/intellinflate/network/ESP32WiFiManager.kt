package com.example.intellinflate.network

import android.util.Log
import com.example.intellinflate.models.*
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * ESP32 WiFi Communication Manager
 * Handles HTTP communication with ESP32 web server for IntelliInflate Health Scanner
 */
class ESP32WiFiManager {
    
    private val TAG = "ESP32WiFiManager"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()
    
    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    private val _latestResponse = MutableStateFlow<ESP32Response?>(null)
    val latestResponse: StateFlow<ESP32Response?> = _latestResponse.asStateFlow()
    
    private var baseUrl: String = ""
    private var pollingJob: Job? = null
    
    data class ConnectionConfig(
        val ipAddress: String,
        val port: Int = 80,
        val enableAutoPolling: Boolean = true,
        val pollingInterval: Long = 1000L // milliseconds
    )
    
    sealed class ConnectionState {
        object Disconnected : ConnectionState()
        object Connecting : ConnectionState()
        data class Connected(val stationInfo: PsiPilotStation) : ConnectionState()
        data class Error(val message: String) : ConnectionState()
    }
    
    /**
     * Connect to ESP32 station
     */
    suspend fun connect(config: ConnectionConfig): Result<PsiPilotStation> {
        return withContext(Dispatchers.IO) {
            try {
                _connectionState.value = ConnectionState.Connecting
                baseUrl = "http://${config.ipAddress}:${config.port}"
                
                Log.d(TAG, "Connecting to $baseUrl")
                
                // Test connection and get station info
                val stationInfo = getStationInfo().getOrThrow()
                
                _connectionState.value = ConnectionState.Connected(stationInfo)
                
                // Start auto-polling if enabled
                if (config.enableAutoPolling) {
                    startPolling(config.pollingInterval)
                }
                
                Result.success(stationInfo)
                
            } catch (e: Exception) {
                Log.e(TAG, "Connection failed", e)
                val errorMessage = "Failed to connect: ${e.message}"
                _connectionState.value = ConnectionState.Error(errorMessage)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Disconnect from ESP32
     */
    fun disconnect() {
        stopPolling()
        _connectionState.value = ConnectionState.Disconnected
        _latestResponse.value = null
        Log.d(TAG, "Disconnected from ESP32")
    }
    
    /**
     * Get station information
     */
    suspend fun getStationInfo(): Result<PsiPilotStation> {
        return makeRequest("/api/station/info", StationInfoResponse::class.java) { response ->
            PsiPilotStation(
                stationId = response.stationId,
                name = response.name,
                location = response.location,
                ipAddress = response.ipAddress,
                status = response.status,
                capabilities = response.capabilities
            )
        }
    }
    
    /**
     * Request vehicle detection
     */
    suspend fun requestVehicleDetection(): Result<VehicleDetectionResult> {
        return makeRequest("/api/vehicle/detect", VehicleDetectionResponse::class.java) { response ->
            _latestResponse.value = ESP32Response(
                stationId = response.stationId,
                sessionId = response.sessionId,
                responseType = ESP32ResponseType.VEHICLE_DETECTED,
                vehicleDetection = response.result
            )
            response.result
        }
    }
    
    /**
     * Request tire scan
     */
    suspend fun requestTireScan(tirePosition: TirePosition): Result<TireScanResult> {
        val json = gson.toJson(mapOf("tirePosition" to tirePosition.name))
        return makePostRequest("/api/tire/scan", json, TireScanResponse::class.java) { response ->
            _latestResponse.value = ESP32Response(
                stationId = response.stationId,
                sessionId = response.sessionId,
                responseType = ESP32ResponseType.TIRE_SCAN_COMPLETE,
                tireScan = response.result
            )
            response.result
        }
    }
    

    
    /**
     * Generic GET request
     */
    private suspend fun <T, R> makeRequest(
        endpoint: String,
        clazz: Class<T>,
        transform: (T) -> R
    ): Result<R> {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$baseUrl$endpoint")
                    .get()
                    .build()
                
                Log.d(TAG, "GET: $baseUrl$endpoint")
                
                val response = client.newCall(request).execute()
                
                if (!response.isSuccessful) {
                    throw IOException("HTTP ${response.code}: ${response.message}")
                }
                
                val body = response.body?.string() ?: throw IOException("Empty response body")
                Log.d(TAG, "Response: $body")
                
                val parsed = gson.fromJson(body, clazz)
                Result.success(transform(parsed))
                
            } catch (e: IOException) {
                Log.e(TAG, "Network error", e)
                Result.failure(e)
            } catch (e: JsonSyntaxException) {
                Log.e(TAG, "JSON parse error", e)
                Result.failure(e)
            } catch (e: Exception) {
                Log.e(TAG, "Request failed", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Generic POST request
     */
    private suspend fun <T, R> makePostRequest(
        endpoint: String,
        jsonBody: String,
        clazz: Class<T>,
        transform: (T) -> R
    ): Result<R> {
        return withContext(Dispatchers.IO) {
            try {
                val mediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = jsonBody.toRequestBody(mediaType)
                
                val request = Request.Builder()
                    .url("$baseUrl$endpoint")
                    .post(requestBody)
                    .build()
                
                Log.d(TAG, "POST: $baseUrl$endpoint")
                Log.d(TAG, "Body: $jsonBody")
                
                val response = client.newCall(request).execute()
                
                if (!response.isSuccessful) {
                    throw IOException("HTTP ${response.code}: ${response.message}")
                }
                
                val body = response.body?.string() ?: throw IOException("Empty response body")
                Log.d(TAG, "Response: $body")
                
                val parsed = gson.fromJson(body, clazz)
                Result.success(transform(parsed))
                
            } catch (e: IOException) {
                Log.e(TAG, "Network error", e)
                Result.failure(e)
            } catch (e: JsonSyntaxException) {
                Log.e(TAG, "JSON parse error", e)
                Result.failure(e)
            } catch (e: Exception) {
                Log.e(TAG, "Request failed", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Start automatic polling for station status
     */
    private fun startPolling(interval: Long) {
        stopPolling()
        pollingJob = scope.launch {
            while (isActive) {
                try {
                    // Polling reserved for future health-check pings
                } catch (e: Exception) {
                    Log.e(TAG, "Polling error", e)
                }
                delay(interval)
            }
        }
        Log.d(TAG, "Started polling with interval ${interval}ms")
    }
    
    /**
     * Stop automatic polling
     */
    private fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
        Log.d(TAG, "Stopped polling")
    }
    
    private inline fun <reified T> getTypeToken(): Class<T> = T::class.java
    
    fun cleanup() {
        disconnect()
        scope.cancel()
    }
    
    // ===== Response Data Classes =====
    
    private data class StationInfoResponse(
        val stationId: String,
        val name: String,
        val location: String,
        val ipAddress: String,
        val status: StationStatus,
        val capabilities: List<StationCapability>
    )
    
    private data class VehicleDetectionResponse(
        val stationId: String,
        val sessionId: String?,
        val result: VehicleDetectionResult
    )
    
    private data class TireScanResponse(
        val stationId: String,
        val sessionId: String?,
        val result: TireScanResult
    )
    
    private data class BasicResponse(
        val success: Boolean,
        val message: String? = null
    )
    
}

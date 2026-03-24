package com.example.intellinflate.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import com.example.intellinflate.models.ESP32Data
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

sealed class BluetoothConnectionState {
    object Disconnected : BluetoothConnectionState()
    object Connecting : BluetoothConnectionState()
    object Connected : BluetoothConnectionState()
    data class Error(val message: String) : BluetoothConnectionState()
}

class ESP32BluetoothManager(private val context: Context) {
    
    private val bluetoothManager: BluetoothManager? = 
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
    
    private var bluetoothSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private var isReading = false
    
    private val gson = Gson()
    
    private val _connectionState = MutableStateFlow<BluetoothConnectionState>(
        BluetoothConnectionState.Disconnected
    )
    val connectionState: StateFlow<BluetoothConnectionState> = _connectionState.asStateFlow()
    
    private val _esp32Data = MutableStateFlow<ESP32Data?>(null)
    val esp32Data: StateFlow<ESP32Data?> = _esp32Data.asStateFlow()
    
    companion object {
        private const val TAG = "ESP32BluetoothManager"
        // Standard UUID for SPP (Serial Port Profile)
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private const val ESP32_DEVICE_NAME = "ESP32"  // Change this to match your ESP32 device name
    }
    
    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true
    
    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<BluetoothDevice> {
        if (!hasBluetoothPermission()) return emptyList()
        return bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
    }
    
    @SuppressLint("MissingPermission")
    fun findESP32Device(): BluetoothDevice? {
        if (!hasBluetoothPermission()) return null
        return bluetoothAdapter?.bondedDevices?.find { 
            it.name?.contains(ESP32_DEVICE_NAME, ignoreCase = true) == true 
        }
    }
    
    @SuppressLint("MissingPermission")
    suspend fun connectToDevice(device: BluetoothDevice): Boolean = withContext(Dispatchers.IO) {
        if (!hasBluetoothPermission()) {
            _connectionState.value = BluetoothConnectionState.Error("Bluetooth permission not granted")
            return@withContext false
        }
        
        try {
            _connectionState.value = BluetoothConnectionState.Connecting
            
            // Close existing connection if any
            disconnect()
            
            // Create socket
            bluetoothSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            
            // Cancel discovery to improve connection
            bluetoothAdapter?.cancelDiscovery()
            
            // Connect to the device
            bluetoothSocket?.connect()
            
            // Get input and output streams
            inputStream = bluetoothSocket?.inputStream
            outputStream = bluetoothSocket?.outputStream
            
            _connectionState.value = BluetoothConnectionState.Connected
            Log.d(TAG, "Connected to ${device.name}")
            
            // Start reading data
            startReading()
            
            return@withContext true
        } catch (e: IOException) {
            Log.e(TAG, "Connection failed", e)
            _connectionState.value = BluetoothConnectionState.Error("Connection failed: ${e.message}")
            disconnect()
            return@withContext false
        }
    }
    
    private suspend fun startReading() = withContext(Dispatchers.IO) {
        isReading = true
        val buffer = ByteArray(1024)
        var stringBuilder = StringBuilder()
        
        try {
            while (isReading && inputStream != null) {
                val bytesRead = inputStream?.read(buffer) ?: -1
                
                if (bytesRead > 0) {
                    val data = String(buffer, 0, bytesRead)
                    stringBuilder.append(data)
                    
                    // Check if we have a complete JSON object
                    val jsonString = stringBuilder.toString()
                    if (jsonString.contains("{") && jsonString.contains("}")) {
                        try {
                            // Extract JSON from the string
                            val startIndex = jsonString.indexOf("{")
                            val endIndex = jsonString.lastIndexOf("}") + 1
                            val jsonData = jsonString.substring(startIndex, endIndex)
                            
                            // Parse JSON
                            val esp32Data = gson.fromJson(jsonData, ESP32Data::class.java)
                            _esp32Data.value = esp32Data
                            
                            Log.d(TAG, "Received data: $jsonData")
                            
                            // Clear the buffer after processing
                            stringBuilder = StringBuilder()
                            if (endIndex < jsonString.length) {
                                stringBuilder.append(jsonString.substring(endIndex))
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing JSON", e)
                            // Clear buffer on parse error
                            stringBuilder = StringBuilder()
                        }
                    }
                    
                    // Prevent buffer overflow
                    if (stringBuilder.length > 2048) {
                        stringBuilder = StringBuilder()
                    }
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error reading data", e)
            if (isReading) {
                _connectionState.value = BluetoothConnectionState.Error("Connection lost")
                disconnect()
            }
        }
    }
    
    suspend fun sendCommand(command: String): Boolean = withContext(Dispatchers.IO) {
        try {
            outputStream?.write((command + "\n").toByteArray())
            outputStream?.flush()
            true
        } catch (e: IOException) {
            Log.e(TAG, "Error sending command", e)
            false
        }
    }
    
    fun disconnect() {
        isReading = false
        
        try {
            inputStream?.close()
            outputStream?.close()
            bluetoothSocket?.close()
        } catch (e: IOException) {
            Log.e(TAG, "Error closing connection", e)
        } finally {
            inputStream = null
            outputStream = null
            bluetoothSocket = null
            _connectionState.value = BluetoothConnectionState.Disconnected
            _esp32Data.value = null
        }
    }
    
    private fun hasBluetoothPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
}

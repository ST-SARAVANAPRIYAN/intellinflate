/*
 * ESP32 IntelliInflate Example Code
 * 
 * This example demonstrates how to send vehicle and tire data to the IntelliInflate Android app
 * via Bluetooth Serial (SPP - Serial Port Profile).
 * 
 * Hardware Requirements:
 * - ESP32 development board
 * - Tire pressure sensors (4x) - optional for testing, can use simulated data
 * - Temperature sensors (4x) - optional for testing, can use simulated data
 * - OBD-II reader or vehicle CAN bus interface - optional
 * 
 * Libraries Required:
 * - BluetoothSerial (built-in with ESP32 package)
 * - ArduinoJson (install via Library Manager)
 * 
 * Author: IntelliInflate Team
 * Date: 2026
 */

#include "BluetoothSerial.h"
#include <ArduinoJson.h>

// Check if Bluetooth is available
#if !defined(CONFIG_BT_ENABLED) || !defined(CONFIG_BLUEDROID_ENABLED)
#error Bluetooth is not enabled! Please run `make menuconfig` to enable it
#endif

BluetoothSerial SerialBT;

// Bluetooth device name (must include "ESP32" for auto-detection in the app)
const char* BT_DEVICE_NAME = "ESP32-IntelliInflate";

// Timing
unsigned long lastSendTime = 0;
const unsigned long SEND_INTERVAL = 2000; // Send data every 2 seconds

// Simulated sensor data (replace with actual sensor readings)
struct TireData {
  int position;      // 0: FL, 1: FR, 2: RL, 3: RR
  float pressure;    // PSI
  float temperature; // Celsius
};

struct VehicleData {
  float speed;           // km/h
  float batteryVoltage;  // Volts
  float engineTemp;      // Celsius
  float fuelLevel;       // Percentage
  float odometer;        // km
  int engineStatus;      // 0: OFF, 1: IDLE, 2: RUNNING, 3: WARNING, 4: ERROR
};

TireData tires[4];
VehicleData vehicle;

void setup() {
  Serial.begin(115200);
  
  // Initialize Bluetooth
  SerialBT.begin(BT_DEVICE_NAME);
  Serial.println("Bluetooth device started, you can now pair it with your phone!");
  Serial.print("Device name: ");
  Serial.println(BT_DEVICE_NAME);
  
  // Initialize simulated data
  initializeSensorData();
}

void loop() {
  unsigned long currentTime = millis();
  
  // Send data at regular intervals
  if (currentTime - lastSendTime >= SEND_INTERVAL) {
    lastSendTime = currentTime;
    
    // Update sensor readings
    updateSensorData();
    
    // Send data via Bluetooth
    sendDataToApp();
  }
  
  // Check for incoming commands from the app
  if (SerialBT.available()) {
    String command = SerialBT.readStringUntil('\n');
    handleCommand(command);
  }
  
  delay(10);
}

void initializeSensorData() {
  // Initialize tire data with default values
  for (int i = 0; i < 4; i++) {
    tires[i].position = i;
    tires[i].pressure = 32.0;      // Default optimal pressure
    tires[i].temperature = 25.0;   // Room temperature
  }
  
  // Initialize vehicle data
  vehicle.speed = 0.0;
  vehicle.batteryVoltage = 12.6;   // Healthy battery
  vehicle.engineTemp = 70.0;       // Normal engine temp
  vehicle.fuelLevel = 100.0;       // Full tank
  vehicle.odometer = 0.0;
  vehicle.engineStatus = 0;        // OFF
}

void updateSensorData() {
  // TODO: Replace with actual sensor readings
  // This function simulates varying sensor data for demonstration
  
  // Simulate tire pressures (add some variation)
  for (int i = 0; i < 4; i++) {
    tires[i].pressure = 30.0 + random(-20, 50) / 10.0;
    tires[i].temperature = 25.0 + random(0, 300) / 10.0;
  }
  
  // Simulate vehicle data
  vehicle.speed = random(0, 1200) / 10.0;
  vehicle.batteryVoltage = 11.5 + random(0, 15) / 10.0;
  vehicle.engineTemp = 70.0 + random(0, 400) / 10.0;
  vehicle.fuelLevel = random(0, 1000) / 10.0;
  vehicle.odometer += vehicle.speed * (SEND_INTERVAL / 3600000.0); // Update odometer
  
  // Simulate engine status based on speed
  if (vehicle.speed == 0) {
    vehicle.engineStatus = 0; // OFF
  } else if (vehicle.speed < 5) {
    vehicle.engineStatus = 1; // IDLE
  } else if (vehicle.speed < 80) {
    vehicle.engineStatus = 2; // RUNNING
  } else {
    vehicle.engineStatus = vehicle.speed > 120 ? 3 : 2; // WARNING if too fast
  }
  
  // Simulate warning if engine temp is too high
  if (vehicle.engineTemp > 110) {
    vehicle.engineStatus = 3; // WARNING
  }
  if (vehicle.engineTemp > 125) {
    vehicle.engineStatus = 4; // ERROR
  }
}

void sendDataToApp() {
  // Create JSON document
  StaticJsonDocument<512> doc;
  
  // Add tire data
  JsonArray tiresArray = doc.createNestedArray("tires");
  for (int i = 0; i < 4; i++) {
    JsonObject tire = tiresArray.createNestedObject();
    tire["pos"] = tires[i].position;
    tire["pressure"] = round(tires[i].pressure * 10) / 10.0;
    tire["temp"] = round(tires[i].temperature * 10) / 10.0;
  }
  
  // Add vehicle data
  JsonObject vehicleObj = doc.createNestedObject("vehicle");
  vehicleObj["speed"] = round(vehicle.speed * 10) / 10.0;
  vehicleObj["battery"] = round(vehicle.batteryVoltage * 10) / 10.0;
  vehicleObj["engineTemp"] = round(vehicle.engineTemp * 10) / 10.0;
  vehicleObj["fuel"] = round(vehicle.fuelLevel * 10) / 10.0;
  vehicleObj["odometer"] = round(vehicle.odometer * 10) / 10.0;
  vehicleObj["engineStatus"] = vehicle.engineStatus;
  
  // Serialize and send via Bluetooth
  String jsonString;
  serializeJson(doc, jsonString);
  SerialBT.println(jsonString);
  
  // Also print to Serial for debugging
  Serial.println("Sent data:");
  serializeJsonPretty(doc, Serial);
  Serial.println();
}

void handleCommand(String command) {
  Serial.print("Received command: ");
  Serial.println(command);
  
  // Handle different commands from the app
  command.trim();
  
  if (command == "RESET_ODO") {
    vehicle.odometer = 0;
    SerialBT.println("{\"status\":\"Odometer reset\"}");
  }
  else if (command == "GET_STATUS") {
    sendDataToApp();
  }
  else if (command.startsWith("SET_FUEL:")) {
    float newFuel = command.substring(9).toFloat();
    if (newFuel >= 0 && newFuel <= 100) {
      vehicle.fuelLevel = newFuel;
      SerialBT.println("{\"status\":\"Fuel level updated\"}");
    }
  }
  else {
    SerialBT.println("{\"status\":\"Unknown command\"}");
  }
}

/*
 * INTEGRATION WITH REAL SENSORS:
 * 
 * For tire pressure sensors:
 * - Replace updateSensorData() with actual readings from pressure sensors
 * - Common sensors use I2C or analog output
 * - Example: BMP280, BMP180 for pressure
 * 
 * For temperature sensors:
 * - DS18B20 (digital), LM35 (analog), or DHT22
 * - Can use infrared sensors for tire temperature
 * 
 * For vehicle data:
 * - Use OBD-II reader connected via UART or CAN bus
 * - Libraries available: ELM327, ESP32-OBD2
 * - Read PIDs for speed, RPM, coolant temp, fuel level, etc.
 * 
 * Pin Connections Example:
 * - Pressure Sensors: I2C (SDA: GPIO21, SCL: GPIO22)
 * - Temperature Sensors: GPIO pins (configurable)
 * - OBD-II: UART2 (RX: GPIO16, TX: GPIO17)
 * 
 * Power Requirements:
 * - ESP32: 3.3V (can be powered from vehicle USB or 12V with regulator)
 * - Sensors: Check individual sensor specifications
 * - Use proper voltage regulation and protection circuits
 */

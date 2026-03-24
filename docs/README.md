# IntelliInflate - Vehicle & Tire Health Monitor

An Android application built with Kotlin and Jetpack Compose that receives real-time vehicle and tire health data from ESP32 via Bluetooth.

## Features

- **Real-time Bluetooth Communication** with ESP32 device
- **Comprehensive Vehicle Monitoring**:
  - Speed tracking
  - Engine temperature
  - Battery voltage
  - Fuel level
  - Odometer reading
  - Engine status
  
- **Advanced Tire Health Monitoring**:
  - Individual tire pressure for all 4 tires
  - Tire temperature monitoring
  - Visual health indicators (Good/Warning/Critical)
  - Position-specific alerts (Front Left, Front Right, Rear Left, Rear Right)

- **Smart Alert System**:
  - Real-time notifications for critical issues
  - Severity-based alerts (Info, Warning, Critical)
  - Customizable thresholds for tire pressure and temperature

- **Modern UI**:
  - Material Design 3
  - Dark/Light theme support
  - Intuitive dashboard with visual indicators
  - Real-time data updates

## Technical Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM (Model-View-ViewModel)
- **Bluetooth**: Android Bluetooth Classic API
- **Data Format**: JSON
- **Permissions**: Runtime permissions with Accompanist library

## Prerequisites

- Android device with Android 10 (API 30) or higher
- ESP32 device with Bluetooth capability
- Bluetooth permissions granted

## Installation

1. Clone this repository
2. Open the project in Android Studio
3. Sync Gradle dependencies
4. Build and run on your Android device

## ESP32 Setup

### Expected Data Format

The ESP32 should send data in the following JSON format via Bluetooth Serial:

```json
{
  "tires": [
    {"pos": 0, "pressure": 32.5, "temp": 25.0},
    {"pos": 1, "pressure": 31.8, "temp": 24.5},
    {"pos": 2, "pressure": 33.0, "temp": 26.0},
    {"pos": 3, "pressure": 32.2, "temp": 25.5}
  ],
  "vehicle": {
    "speed": 60.5,
    "battery": 12.6,
    "engineTemp": 85.0,
    "fuel": 75.0,
    "odometer": 12345.6,
    "engineStatus": 2
  }
}
```

### Tire Position Mapping:
- `0`: Front Left
- `1`: Front Right
- `2`: Rear Left
- `3`: Rear Right

### Engine Status Values:
- `0`: OFF
- `1`: IDLE
- `2`: RUNNING
- `3`: WARNING
- `4`: ERROR

### ESP32 Configuration

1. Set your ESP32's Bluetooth device name to include "ESP32" (e.g., "ESP32-Vehicle")
2. Use Bluetooth Serial Profile (SPP) with UUID: `00001101-0000-1000-8000-00805F9B34FB`
3. Pair your ESP32 with your Android device via Bluetooth settings
4. Send JSON data at regular intervals (recommended: 1-2 seconds)

See `ESP32_EXAMPLE_CODE.ino` for a complete Arduino sketch example.

## How to Use

1. **Launch the App**
   - Grant Bluetooth and Location permissions when prompted
   - Enable Bluetooth if not already enabled

2. **Connect to ESP32**
   - The app will show a list of paired Bluetooth devices
   - Select your ESP32 device from the list
   - Tap "Connect to Device"

3. **Monitor Your Vehicle**
   - View real-time tire pressure and temperature for all 4 tires
   - Check overall vehicle health status
   - Monitor speed, fuel level, battery voltage, and engine temperature
   - Receive instant alerts for any critical issues

4. **Disconnect**
   - Tap the "Disconnect" button to end the Bluetooth session

## Alert Thresholds

### Tire Pressure:
- **Critical**: < 25.6 PSI or > 41.6 PSI (80% or 130% of optimal 32 PSI)
- **Warning**: < 28.8 PSI or > 38.4 PSI (90% or 120% of optimal 32 PSI)
- **Good**: Within acceptable range

### Tire Temperature:
- **Critical**: > 100°C
- **Warning**: > 80°C

### Engine Temperature:
- **Critical**: > 120°C
- **Warning**: > 100°C

### Battery Voltage:
- **Critical**: < 11.0V
- **Warning**: < 11.5V

### Fuel Level:
- **Critical**: < 5%
- **Warning**: < 15%

## Project Structure

```
app/src/main/java/com/example/intellinflate/
├── MainActivity.kt                      # Main entry point
├── bluetooth/
│   └── ESP32BluetoothManager.kt        # Bluetooth communication handler
├── models/
│   ├── TireData.kt                     # Tire data models
│   ├── VehicleData.kt                  # Vehicle data models
│   └── ESP32Data.kt                    # ESP32 communication models
├── viewmodel/
│   └── VehicleViewModel.kt             # ViewModel for state management
└── ui/
    ├── screens/
    │   ├── MainScreen.kt               # App navigation and state handling
    │   └── VehicleDashboard.kt         # Main dashboard UI
    └── components/
        └── Components.kt               # Reusable UI components
```

## Troubleshooting

### Connection Issues:
1. Ensure ESP32 is powered on and in range
2. Verify Bluetooth is enabled on both devices
3. Check that ESP32 is paired in Android Bluetooth settings
4. Restart both devices if connection fails

### No Data Received:
1. Verify ESP32 is sending data in the correct JSON format
2. Check serial monitor on ESP32 to confirm data transmission
3. Ensure Bluetooth Serial Profile (SPP) is properly configured

### Permission Errors:
1. Go to Android Settings > Apps > IntelliInflate > Permissions
2. Grant all requested permissions (Bluetooth, Location)

## Future Enhancements

- [ ] Historical data logging and charts
- [ ] Export data to CSV/Excel
- [ ] Customizable alert thresholds
- [ ] Multiple vehicle profiles
- [ ] WiFi support alongside Bluetooth
- [ ] Voice alerts for critical issues
- [ ] Integration with vehicle OBD-II systems

## License

This project is open source and available under the MIT License.

## Support

For issues, questions, or contributions, please open an issue on the GitHub repository.

## Acknowledgments

- Built with Jetpack Compose
- Uses Material Design 3 guidelines
- Bluetooth communication based on Android Bluetooth Classic API

# IntelliInflate - Quick Start Guide

## 🚀 Getting Started in 5 Minutes

### Step 1: Build the Android App
1. Open Android Studio
2. Open this project
3. Wait for Gradle sync to complete
4. Connect your Android device via USB
5. Click "Run" (green play button)
6. Install the app on your device

### Step 2: Set Up ESP32
1. Open Arduino IDE
2. Install ESP32 board support (if not already installed)
3. Install ArduinoJson library: `Tools > Manage Libraries > Search "ArduinoJson"`
4. Open `ESP32_EXAMPLE_CODE.ino`
5. Select your ESP32 board: `Tools > Board > ESP32 Dev Module`
6. Select the correct COM port
7. Upload the sketch to your ESP32

### Step 3: Pair Devices
1. Turn on your ESP32 (it will start broadcasting)
2. On your Android device:
   - Go to Settings > Bluetooth
   - Look for "ESP32-IntelliInflate"
   - Tap to pair (PIN: 1234 or 0000 if asked)

### Step 4: Use the App
1. Open IntelliInflate app
2. Grant Bluetooth and Location permissions
3. The app will auto-detect your ESP32
4. Tap "Connect to Device"
5. View real-time vehicle and tire data!

## 📱 App Features at a Glance

### Connection Screen
- List of paired Bluetooth devices
- Auto-detection of ESP32 devices
- One-tap connection

### Dashboard
- **Overall Health Status**: Quick visual indicator
- **Active Alerts**: Real-time notifications
- **Vehicle Info**: Speed, battery, engine temp, fuel, odometer
- **Tire Monitor**: All 4 tires with pressure and temperature

### Alert System
- 🟢 **Green (Good)**: Everything is normal
- 🟡 **Yellow (Warning)**: Attention needed
- 🔴 **Red (Critical)**: Immediate action required

## 🔧 ESP32 Data Format

Your ESP32 must send JSON data via Bluetooth Serial:

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

## ⚙️ Customization

### Change ESP32 Device Name
In `ESP32_EXAMPLE_CODE.ino`, line 37:
```cpp
const char* BT_DEVICE_NAME = "ESP32-IntelliInflate";
```

### Change Data Send Interval
In `ESP32_EXAMPLE_CODE.ino`, line 42:
```cpp
const unsigned long SEND_INTERVAL = 2000; // milliseconds
```

### Adjust Alert Thresholds
In `VehicleViewModel.kt`, modify the threshold values in the `updateVehicleData()` function.

## 🐛 Common Issues

### App can't find ESP32
- ✅ Ensure ESP32 is powered on
- ✅ Check ESP32 is paired in Android Bluetooth settings
- ✅ Verify device name contains "ESP32"
- ✅ Try tapping "Refresh" in the app

### No data showing
- ✅ Check ESP32 Serial Monitor to verify data is being sent
- ✅ Ensure JSON format is correct
- ✅ Reconnect Bluetooth connection
- ✅ Restart both devices

### Permission errors
- ✅ Go to Settings > Apps > IntelliInflate > Permissions
- ✅ Enable all permissions
- ✅ Restart the app

### Connection keeps dropping
- ✅ Keep devices within 10 meters
- ✅ Reduce obstacles between devices
- ✅ Check ESP32 power supply is stable
- ✅ Ensure ESP32 code doesn't have blocking delays

## 📊 Understanding the Data

### Tire Positions
```
    [FL]         [FR]
     O             O
     
     
     O             O
    [RL]         [RR]
```

### Engine Status Codes
- `0` = OFF
- `1` = IDLE
- `2` = RUNNING
- `3` = WARNING
- `4` = ERROR

### Recommended Values
- **Tire Pressure**: 30-35 PSI (check your vehicle manual)
- **Tire Temperature**: < 80°C
- **Engine Temperature**: 80-100°C
- **Battery Voltage**: 12.4-12.8V (engine off), 13.5-14.5V (engine running)

## 🎯 Next Steps

1. **Test with Simulated Data**: The example code sends random data - perfect for testing!
2. **Add Real Sensors**: Integrate actual tire pressure and temperature sensors
3. **Connect to OBD-II**: Get real vehicle data using an OBD-II adapter
4. **Customize UI**: Modify the app theme and layout to your preference
5. **Add Features**: Implement data logging, charts, or export functionality

## 📚 Additional Resources

- **Android Bluetooth Guide**: https://developer.android.com/guide/topics/connectivity/bluetooth
- **ESP32 Documentation**: https://docs.espressif.com/projects/esp-idf/en/latest/esp32/
- **Jetpack Compose**: https://developer.android.com/jetpack/compose
- **ArduinoJson**: https://arduinojson.org/

## 💡 Tips

- Use a powerbank or car USB to power the ESP32 during testing
- Monitor ESP32 Serial output for debugging
- Keep the app in foreground for continuous data updates
- Battery voltage changes when engine is running vs off

## 🤝 Need Help?

- Check the main README.md for detailed documentation
- Review the code comments for implementation details
- Test individual components separately
- Use Serial Monitor to debug ESP32 communication

---

**Happy Monitoring! 🚗💨**

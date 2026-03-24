# IntelliInflate Web Dashboard 🚗📊

An interactive web-based dashboard for real-time vehicle and tire health monitoring. Features beautiful visualizations, real-time alerts, and multiple connection options for ESP32 hardware integration.

## ✨ Features

### 🎨 Beautiful Interactive UI
- **Real-time Dashboard** with live data updates
- **Modern Dark Theme** with smooth animations
- **Responsive Design** - works on desktop, tablet, and mobile
- **Interactive Vehicle Visualization** showing all 4 tires
- **Real-time Charts** for pressure and temperature history
- **Smart Alert System** with severity-based notifications

### 📡 Multiple Connection Options
1. **Demo Mode** - Simulated data for testing without hardware
2. **Web Bluetooth** - Direct connection to ESP32 (Chrome/Edge only)
3. **WebSocket Server** - Bridge for serial/Bluetooth connections
4. **Web Serial** - Direct USB connection (Chrome/Edge only)

### 📊 Data Visualization
- Live tire pressure monitoring (PSI)
- Tire temperature tracking (°C)
- Vehicle speed, fuel level, battery voltage
- Engine temperature and status
- Odometer reading
- Historical charts (last 20 data points)

### ⚠️ Intelligent Alerts
- **Critical Alerts** - Immediate action required
- **Warning Alerts** - Attention needed
- **Info Alerts** - General notifications
- Color-coded severity indicators
- Real-time threshold monitoring

## 🚀 Quick Start

### Option 1: Demo Mode (No Hardware Required)

1. **Open the Dashboard**
   ```bash
   # Simply open index.html in your browser
   # Or use a local server:
   python -m http.server 8000
   # Then visit: http://localhost:8000
   ```

2. **Click "Demo Mode"** button to see simulated data
3. Explore the interactive dashboard!

### Option 2: Web Bluetooth (Direct ESP32 Connection)

**Requirements:**
- Chrome or Edge browser
- ESP32 with Bluetooth enabled
- ESP32 running the IntelliInflate sketch

**Steps:**
1. Open `index.html` in Chrome/Edge
2. Click "Connect ESP32" button
3. Select "Web Bluetooth"
4. Choose your ESP32 device from the list
5. Real-time data will start flowing!

### Option 3: WebSocket Server (Recommended for Development)

**Requirements:**
- Node.js installed
- ESP32 connected via USB

**Steps:**

1. **Install Dependencies**
   ```bash
   cd web-dashboard
   npm install
   ```

2. **Configure Serial Port**
   Edit `server.js` line 6:
   ```javascript
   const SERIAL_PORT = 'COM3'; // Windows
   // or
   const SERIAL_PORT = '/dev/ttyUSB0'; // Linux/Mac
   ```

3. **Start Server**
   ```bash
   npm start
   ```

4. **Open Dashboard**
   - Open `index.html` in any browser
   - Click "Connect ESP32" → "WebSocket Server"
   - Server will automatically forward ESP32 data

### Option 4: Web Serial (USB Connection)

**Requirements:**
- Chrome or Edge browser
- ESP32 connected via USB

**Steps:**
1. Open `index.html` in Chrome/Edge
2. Click "Connect ESP32" button
3. Select "Web Serial"
4. Choose your ESP32 serial port
5. Data will stream directly from USB!

## 📁 Project Structure

```
web-dashboard/
├── index.html          # Main dashboard HTML
├── styles.css          # Complete styling and animations
├── app.js              # Dashboard logic and connections
├── server.js           # Optional WebSocket server
├── package.json        # Node.js dependencies
└── README.md           # This file
```

## 🔧 Hardware Integration

### ESP32 Data Format

Your ESP32 must send JSON data in this format:

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

### Tire Position Mapping
- `0` = Front Left
- `1` = Front Right
- `2` = Rear Left
- `3` = Rear Right

### Engine Status Values
- `0` = OFF
- `1` = IDLE
- `2` = RUNNING
- `3` = WARNING
- `4` = ERROR

### ESP32 Configuration

For Web Bluetooth, use this UUID setup:
```cpp
Service UUID: 0000ffe0-0000-1000-8000-00805f9b34fb
Characteristic UUID: 0000ffe1-0000-1000-8000-00805f9b34fb
```

See `../ESP32_EXAMPLE_CODE.ino` for complete Arduino sketch.

## 🎯 Alert Thresholds

### Tire Pressure
- **Critical**: < 25.6 PSI or > 41.6 PSI
- **Warning**: < 28.8 PSI or > 38.4 PSI
- **Good**: 28.8 - 38.4 PSI

### Tire Temperature
- **Critical**: > 100°C
- **Warning**: > 80°C
- **Good**: < 80°C

### Engine Temperature
- **Critical**: > 120°C
- **Warning**: > 100°C
- **Good**: < 100°C

### Battery Voltage
- **Critical**: < 11.0V
- **Warning**: < 11.5V
- **Good**: > 11.5V

### Fuel Level
- **Critical**: < 5%
- **Warning**: < 15%
- **Good**: > 15%

## 🌐 Browser Compatibility

### Full Support (All Features)
- ✅ Chrome 89+
- ✅ Edge 89+

### Limited Support (No Bluetooth/Serial)
- ⚠️ Firefox (Demo & WebSocket only)
- ⚠️ Safari (Demo & WebSocket only)

### Features by Connection Method

| Feature | Chrome/Edge | Firefox | Safari |
|---------|-------------|---------|--------|
| Demo Mode | ✅ | ✅ | ✅ |
| Web Bluetooth | ✅ | ❌ | ❌ |
| Web Serial | ✅ | ❌ | ❌ |
| WebSocket | ✅ | ✅ | ✅ |

## 💡 Tips & Best Practices

### Performance
- Dashboard updates every 2 seconds
- Charts maintain last 20 data points
- Smooth animations with CSS transitions
- Efficient data processing

### Development
- Use Demo Mode for UI development
- Use WebSocket server for testing
- Check browser console for debugging
- Monitor Network tab for data flow

### Production
- Web Bluetooth for direct connection
- WebSocket server for multiple clients
- Consider data logging
- Implement authentication for security

## 🛠️ Customization

### Change Update Interval
In `app.js`, line 46:
```javascript
this.demoInterval = setInterval(() => {
    this.generateDemoData();
}, 2000); // Change to your desired interval in ms
```

### Modify Alert Thresholds
In `app.js`, find the `getTireHealth()` and `checkAlerts()` functions to adjust thresholds.

### Change Color Scheme
In `styles.css`, modify CSS variables:
```css
:root {
    --primary-color: #2563eb;
    --success-color: #10b981;
    --warning-color: #f59e0b;
    --danger-color: #ef4444;
    /* ... more colors ... */
}
```

### Add New Metrics
1. Update HTML in `index.html`
2. Add styling in `styles.css`
3. Update data processing in `app.js`

## 🐛 Troubleshooting

### Dashboard not loading
- Check browser console for errors
- Ensure all files are in the same directory
- Try using a local web server instead of file://

### Web Bluetooth not working
- Only works in Chrome/Edge
- Ensure Bluetooth is enabled on your computer
- ESP32 must be advertising properly
- Check ESP32 device name contains "ESP32"

### WebSocket connection fails
- Ensure server is running (`npm start`)
- Check correct WebSocket URL (default: ws://localhost:8080)
- Verify serial port configuration in server.js
- Check firewall settings

### No data received
- Verify ESP32 is sending JSON in correct format
- Check serial monitor on ESP32
- Ensure baud rate matches (115200)
- Look for parse errors in browser console

### Charts not updating
- Check if data is being received (console logs)
- Verify Chart.js library loaded
- Ensure data format is correct

## 📈 Future Enhancements

Planned features:
- [ ] Data export to CSV/Excel
- [ ] Historical data storage
- [ ] Customizable dashboard layouts
- [ ] Voice alerts
- [ ] Mobile app integration
- [ ] Multi-vehicle support
- [ ] Cloud data sync
- [ ] Advanced analytics

## 🤝 Contributing

Feel free to:
- Report bugs
- Suggest features
- Submit pull requests
- Improve documentation

## 📄 License

MIT License - feel free to use in your projects!

## 🆘 Support

For issues or questions:
1. Check this README
2. Review browser console
3. Test with Demo Mode
4. Check ESP32 serial output
5. Verify connections

## 🎉 Acknowledgments

Built with:
- **Chart.js** - Beautiful charts
- **Web Bluetooth API** - Direct device connection
- **Web Serial API** - USB communication
- **WebSocket** - Real-time data streaming
- **Modern CSS** - Smooth animations and gradients

---

**Made with ❤️ for the IntelliInflate Project**

*Last updated: February 2026*

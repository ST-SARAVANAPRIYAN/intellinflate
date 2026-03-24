# IntelliInflate - AI-Powered Tire Health Monitoring System

## Overview

IntelliInflate is an intelligent tire health monitoring platform that combines **AI-based vehicle detection** and **tire crack detection** to provide comprehensive tire health assessments at service stations.

## ✨ Key Features

### 1. **AI-Powered Vehicle Detection**
- Automatic vehicle type classification (Motorcycle, Sedan, SUV, Truck, etc.)
- License plate recognition with OCR
- Confidence scoring for detection accuracy
- Vehicle color and orientation detection
- Automatic PSI recommendation based on vehicle type

### 2. **Advanced Tire Crack Detection**
- Computer vision-based crack analysis
- Detects multiple crack types:
  - Surface cracks
  - Deep/structural cracks
  - Sidewall damage
  - Tread separation
  - Weather checking (dry rot)
- Crack severity assessment (None, Minor, Moderate, Severe, Critical)
- Precise crack measurements (length, width, depth)

### 3. **Comprehensive Tire Health Analysis**
- **Wear Analysis**: Tread depth measurement, wear pattern detection
- **Sidewall Inspection**: Bulge, cut, puncture, abrasion detection
- **Tread Analysis**: Foreign object detection (nails, screws, glass)
- **Safety Rating**: Safe, Safe with Care, Limited Use, Unsafe, Dangerous
- **AI Recommendations**: Prioritized action items with timeframes

### 4. **Automated Inflation Control**
- Automatic PSI target setting based on vehicle type
- Real-time pressure monitoring during inflation
- Progress tracking with ETA
- Automatic shut-off at target pressure
- Multi-tire sequential inflation support

### 5. **Leak Detection**
- Pressure curve analysis for micro-leak detection
- Leak rate measurement (PSI per minute)
- Leak type classification (Slow, Moderate, Fast, Puncture)
- Confidence-based leak severity assessment

### 6. **Service History Tracking**
- Vehicle-linked service records
- Historical tire health reports
- Maintenance recommendations tracking
- Station and technician logging

## 📱 Mobile App Architecture

### Technology Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material 3)
- **Architecture**: MVVM (Model-View-ViewModel)
- **Networking**: OkHttp + Retrofit
- **Image Loading**: Coil
- **Data Serialization**: Gson
- **Async Operations**: Kotlin Coroutines + Flow

### App Structure

```
com.example.intellinflate/
├── models/
│   ├── PsiPilotModels.kt       # Core PSI PILOT data models
│   ├── AIDetectionModels.kt    # AI detection result models
│   ├── VehicleData.kt          # Legacy vehicle data
│   ├── TireData.kt            # Legacy tire data
│   └── ESP32Data.kt           # Legacy ESP32 data
├── network/
│   └── ESP32WiFiManager.kt     # WiFi communication with ESP32
├── viewmodel/
│   ├── PsiPilotViewModel.kt    # Main app ViewModel
│   └── VehicleViewModel.kt     # Legacy ViewModel
├── ui/
│   ├── screens/
│   │   ├── PsiPilotMainScreen.kt      # Main navigation screen
│   │   ├── PsiPilotDashboard.kt       # Dashboard overview
│   │   ├── VehicleDetectionScreen.kt  # Vehicle detection UI
│   │   ├── TireScanScreen.kt          # Tire health analysis UI
│   │   └── MainScreen.kt              # Legacy main screen
│   ├── components/
│   │   └── Components.kt              # Reusable UI components
│   └── theme/
│       ├── Theme.kt
│       ├── Color.kt
│       └── Type.kt
└── MainActivity.kt
```

## 🔌 Hardware Integration

### ESP32 Communication

The app communicates with ESP32-CAM via WiFi HTTP requests:

**Base URL**: `http://[ESP32_IP]:[PORT]` (Default: `http://192.168.4.1:80`)

### API Endpoints

#### Station Information
```
GET /api/station/info
Response: { stationId, name, location, status, capabilities }
```

#### Vehicle Detection
```
GET /api/vehicle/detect
Response: VehicleDetectionResult with:
  - Detected vehicles with bounding boxes
  - Vehicle type classification
  - License plate recognition
  - Captured image (Base64 or URL)
  - Confidence scores
```

#### Tire Scanning
```
POST /api/tire/scan
Body: { "tirePosition": "FRONT_LEFT" }
Response: TireScanResult with:
  - Crack detection results
  - Wear analysis
  - Sidewall analysis
  - Tread analysis
  - Multiple captured images
  - Overall condition assessment
```

#### Inflation Control
```
POST /api/inflation/settarget
Body: { frontLeft: 32, frontRight: 32, rearLeft: 32, rearRight: 32 }

POST /api/inflation/start
Body: { "tirePosition": "FRONT_LEFT" }

GET /api/inflation/live
Response: { currentPSI, targetPSI, progress, isInflating, ... }

GET /api/inflation/stop
```

#### Leak Testing
```
POST /api/tire/leaktest
Body: { "tirePosition": "FRONT_LEFT" }
Response: { hasLeak, leakRate, testDuration, pressureReadings, confidence }
```

## 🎨 UI Screens

### 1. **Dashboard Screen**
- Connection status
- Current session info
- Live inflation progress
- Quick statistics
- Session management

### 2. **Vehicle Detection Screen**
- Live camera feed with detection overlay
- Vehicle type with confidence score
- License plate display
- Alternative classification predictions
- Recommended PSI for all tires
- Color and orientation info

### 3. **Tire Scan Screen**
- Interactive tire selection (FL, FR, RL, RR)
- Tile-based tire status overview
- Detailed scan results:
  - Overall condition score (0-100)
  - Safety rating badge
  - Crack detection with images
  - Wear analysis visualization
  - Sidewall anomalies list
  - Foreign object detection
  - AI recommendations with priorities

### 4. **Inflation Control Screen**
- Real-time pressure monitoring
- Progress indicators
- Manual PSI adjustment
- Start/Stop controls
- ETA display

### 5. **Service History Screen**
- Past service records
- Tire health trends
- Maintenance recommendations
- Station and date logs

## 📊 Data Models

### Vehicle Detection Models
```kotlin
VehicleDetectionResult
├── vehicles: List<DetectedVehicle>
│   ├── vehicleType: VehicleTypeDetection
│   ├── licensePlate: LicensePlateDetection
│   ├── boundingBox: BoundingBox
│   ├── color: VehicleColor
│   └── confidence: Float
├── imageUrl: String?
├── processingTime: Long
└── confidence: Float
```

### Tire Scan Models
```kotlin
TireScanResult
├── crackDetection: CrackDetectionResult
│   ├── detectedCracks: List<DetectedCrack>
│   ├── crackSeverity: CrackSeverity
│   ├── totalCrackLength: Float
│   └── confidence: Float
├── wearAnalysis: WearAnalysisResult
│   ├── wearPattern: WearPattern
│   ├── treadDepthMeasurements: List<TreadDepthMeasurement>
│   └── estimatedRemainingLife: TireLifeEstimate
├── sidewallAnalysis: SidewallAnalysisResult
│   └── detectedAnomalies: List<SidewallAnomaly>
├── treadAnalysis: TreadAnalysisResult
│   └── detectedObjects: List<ForeignObject>
└── overallCondition: TireConditionAssessment
    ├── overallScore: Float (0-100)
    ├── safetyRating: SafetyRating
    └── recommendations: List<TireRecommendation>
```

## 🚀 Getting Started

### Prerequisites
- Android device with Android 10 (API 30) or higher
- ESP32-CAM with WiFi capability
- PSI PILOT hardware setup at station

### Installation

1. Clone the repository
2. Open in Android Studio
3. Sync Gradle dependencies
4. Build and install on Android device

### First Use

1. Launch the app
2. Tap "Connect to Station"
3. Enter ESP32 IP address (default: 192.168.4.1)
4. Wait for connection confirmation
5. Start a new service session

## 🔧 Configuration

### ESP32 WiFi Setup
By default, ESP32 creates an Access Point:
- **SSID**: PSI-PILOT-[StationID]
- **Password**: (configured on ESP32)
- **IP**: 192.168.4.1
- **Port**: 80

### App Settings
Configure in ConnectionDialog:
- IP Address
- Port number
- Auto-polling interval (default: 1 second)

## 📈 Future Enhancements

- Cloud sync for service history
- Fleet management dashboard
- Predictive maintenance alerts
- Tire pressure monitoring over time
- Integration with tire brands database
- Bluetooth OBD-II integration
- Multi-language support
- Offline mode with local caching

## 🎯 Use Cases

1. **Petrol Stations**: Fast, accurate tire service with minimal human intervention
2. **Automobile Service Centers**: Professional tire diagnostics
3. **Fleet Management**: Centralized tire health monitoring
4. **EV Charging Stations**: Value-added service during charging
5. **Smart City Infrastructure**: Data-driven mobility insights

## 📄 License

Copyright © 2026 PSI PILOT Development Team

## 🤝 Contributing

This is a prototype/demonstration project. For production deployment, additional safety certifications and hardware calibration are required.

## 📧 Contact

For hardware integration inquiries or ESP32 API documentation, refer to `ESP32_EXAMPLE_CODE.ino` and `WEB_DASHBOARD_GUIDE.md`.

---

**PSI PILOT** - *Revolutionizing tire maintenance through AI and automation* 🚗💨

# 🔌 ESP32 API Integration Guide

## Quick Reference for Hardware Team

This document shows the **exact data formats** the mobile app expects from ESP32-CAM.

---

## 📡 Connection

- **Default IP**: `192.168.4.1`
- **Default Port**: `80`
- **Protocol**: HTTP/JSON
- **Content-Type**: `application/json`

---

## 🚗 Vehicle Detection API

### Endpoint
```
GET /api/vehicle/detect
```

### Expected Response Format
```json
{
  "stationId": "STATION-001",
  "sessionId": "SESSION-12345",
  "result": {
    "detectionId": "DET-001",
    "timestamp": 1708646400000,
    "imageUrl": "http://192.168.4.1/images/vehicle.jpg",
    "imageBase64": null,
    "vehicles": [
      {
        "vehicleId": "VEH-001",
        "boundingBox": {
          "x": 0.2,
          "y": 0.3,
          "width": 0.6,
          "height": 0.5,
          "normalized": true
        },
        "vehicleType": {
          "type": "SEDAN",
          "confidence": 0.92,
          "alternativePredictions": [
            { "type": "HATCHBACK", "confidence": 0.05 },
            { "type": "SUV", "confidence": 0.03 }
          ]
        },
        "licensePlate": {
          "plateNumber": "KA01AB1234",
          "boundingBox": { "x": 0.4, "y": 0.6, "width": 0.2, "height": 0.1, "normalized": true },
          "confidence": 0.88,
          "region": "KA",
          "isValid": true,
          "characters": [
            { "character": "K", "confidence": 0.95, "boundingBox": { "x": 0.4, "y": 0.6, "width": 0.02, "height": 0.08, "normalized": true } }
          ]
        },
        "color": {
          "primaryColor": "WHITE",
          "secondaryColor": null,
          "confidence": 0.85
        },
        "orientation": "FRONT",
        "confidence": 0.92,
        "features": {
          "hasRoofRack": false,
          "hasSpareTire": false,
          "doorCount": 4,
          "bodyStyle": "sedan"
        }
      }
    ],
    "processingTime": 234,
    "cameraId": "CAM-01",
    "confidence": 0.92
  }
}
```

### Vehicle Types (Enum Values)
```
MOTORCYCLE, HATCHBACK, SEDAN, SUV, PICKUP_TRUCK, VAN, TRUCK, BUS, UNKNOWN
```

### Color Names (Enum Values)
```
WHITE, BLACK, SILVER, GRAY, RED, BLUE, GREEN, YELLOW, ORANGE, BROWN, GOLD, BEIGE, UNKNOWN
```

### Orientation Values
```
FRONT, REAR, LEFT_SIDE, RIGHT_SIDE, FRONT_LEFT, FRONT_RIGHT, REAR_LEFT, REAR_RIGHT, UNKNOWN
```

---

## 🔍 Tire Scan API

### Endpoint
```
POST /api/tire/scan
Content-Type: application/json
```

### Request Body
```json
{
  "tirePosition": "FRONT_LEFT"
}
```

### Tire Positions
```
FRONT_LEFT, FRONT_RIGHT, REAR_LEFT, REAR_RIGHT
```

### Expected Response Format
```json
{
  "stationId": "STATION-001",
  "sessionId": "SESSION-12345",
  "result": {
    "scanId": "SCAN-001",
    "timestamp": 1708646400000,
    "tirePosition": "FRONT_LEFT",
    "images": [
      {
        "imageId": "IMG-001",
        "imageUrl": "http://192.168.4.1/images/tire_fl_tread.jpg",
        "imageBase64": null,
        "captureAngle": "TREAD_FRONT",
        "imageType": "ORIGINAL",
        "width": 1920,
        "height": 1080,
        "timestamp": 1708646400000
      },
      {
        "imageId": "IMG-002",
        "imageUrl": "http://192.168.4.1/images/tire_fl_sidewall.jpg",
        "captureAngle": "SIDEWALL_OUTER",
        "imageType": "ANNOTATED",
        "width": 1920,
        "height": 1080,
        "timestamp": 1708646401000
      }
    ],
    "crackDetection": {
      "hasCracks": true,
      "detectedCracks": [
        {
          "crackId": "CRACK-001",
          "crackType": "SURFACE_CRACK",
          "location": "SIDEWALL_UPPER",
          "boundingBox": { "x": 0.3, "y": 0.4, "width": 0.15, "height": 0.02, "normalized": true },
          "length": 25.5,
          "width": 1.2,
          "depth": {
            "estimatedDepth": 0.8,
            "depthCategory": "SURFACE",
            "confidence": 0.75
          },
          "severity": "MINOR",
          "segmentationMask": null,
          "confidence": 0.87
        }
      ],
      "crackSeverity": "MINOR",
      "totalCrackLength": 25.5,
      "crackDensity": 0.15,
      "confidence": 0.87
    },
    "wearAnalysis": {
      "wearPattern": "NORMAL",
      "wearLevel": "GOOD",
      "treadDepthMeasurements": [
        {
          "location": "CENTER",
          "depth": 5.5,
          "measurementPoint": { "x": 0.5, "y": 0.5 },
          "confidence": 0.92
        },
        {
          "location": "INNER_SHOULDER",
          "depth": 5.3,
          "measurementPoint": { "x": 0.3, "y": 0.5 },
          "confidence": 0.88
        }
      ],
      "averageTreadDepth": 5.4,
      "minimumTreadDepth": 5.3,
      "wearUniformity": 0.95,
      "estimatedRemainingLife": {
        "remainingKilometers": 35000,
        "remainingMonths": 24,
        "confidence": 0.80
      },
      "confidence": 0.90
    },
    "sidewallAnalysis": {
      "hasDamage": false,
      "detectedAnomalies": [],
      "sidewallCondition": "EXCELLENT",
      "confidence": 0.95
    },
    "treadAnalysis": {
      "hasForeignObjects": false,
      "detectedObjects": [],
      "treadPattern": {
        "patternType": "SYMMETRIC",
        "patternCondition": "EXCELLENT",
        "blocksWorn": false,
        "sipesVisible": true,
        "groovesClean": true
      },
      "confidence": 0.93
    },
    "overallCondition": {
      "overallScore": 85.0,
      "overallStatus": "GOOD",
      "safetyRating": "SAFE",
      "recommendations": [
        {
          "priority": "LOW",
          "action": "MONITOR",
          "description": "Continue monitoring tread depth every 5000 km",
          "timeframe": "NEXT_SERVICE"
        }
      ],
      "criticality": "NORMAL",
      "estimatedCost": null
    },
    "processingTime": 1250,
    "aiModelVersion": "v1.2.3"
  }
}
```

### Crack Types
```
SURFACE_CRACK, DEEP_CRACK, HAIRLINE_CRACK, SIDEWALL_CRACK, TREAD_SEPARATION, 
WEATHER_CRACK, IMPACT_DAMAGE, RADIAL_CRACK, CIRCUMFERENTIAL_CRACK
```

### Crack Locations
```
TREAD_CENTER, TREAD_SHOULDER, SIDEWALL_UPPER, SIDEWALL_MIDDLE, SIDEWALL_LOWER, 
BEAD_AREA, GROOVE, SIPE
```

### Crack Severity
```
NONE, MINOR, MODERATE, SEVERE, CRITICAL
```

### Wear Patterns
```
NORMAL, CENTER_WEAR, EDGE_WEAR, ONE_SIDE_WEAR, CUPPING_SCALLOPING, FEATHERING
```

### Wear Levels
```
NEW, EXCELLENT, GOOD, FAIR, WORN, CRITICAL
```

### Sidewall Anomaly Types
```
BULGE, CUT, PUNCTURE, ABRASION, IMPACT_DAMAGE, SCUFF, SIDEWALL_SEPARATION, 
WEATHER_CHECKING, EXPOSED_CORD, UNEVEN_WEAR
```

### Foreign Object Types
```
NAIL, SCREW, GLASS, METAL_FRAGMENT, STONE, WOOD, PLASTIC, WIRE, UNKNOWN
```

### Safety Ratings
```
SAFE, SAFE_WITH_CARE, LIMITED_USE, UNSAFE, DANGEROUS
```

### Recommended Actions
```
MONITOR, CLEAN, INFLATE, ROTATE, BALANCE, ALIGNMENT_CHECK, REPAIR, REPLACE, 
IMMEDIATE_REPLACEMENT, PROFESSIONAL_INSPECTION
```

### Action Timeframes
```
IMMEDIATE, WITHIN_WEEK, WITHIN_MONTH, NEXT_SERVICE, MONITOR
```

---

## 💨 Inflation Control APIs

### Set Target PSI
```
POST /api/inflation/settarget
Content-Type: application/json

{
  "frontLeft": 32.0,
  "frontRight": 32.0,
  "rearLeft": 32.0,
  "rearRight": 32.0,
  "unit": "PSI"
}
```

### Start Inflation
```
POST /api/inflation/start
Content-Type: application/json

{
  "tirePosition": "FRONT_LEFT"
}
```

### Get Live Inflation Data
```
GET /api/inflation/live

Response:
{
  "data": {
    "timestamp": 1708646400000,
    "currentPSI": 28.5,
    "targetPSI": 32.0,
    "temperature": 25.3,
    "isInflating": true,
    "valveOpen": true,
    "compressorRunning": true,
    "progress": 0.65,
    "estimatedTimeRemaining": 15
  }
}
```

### Stop Inflation
```
GET /api/inflation/stop

Response:
{
  "success": true,
  "message": "Inflation stopped"
}
```

---

## 🔧 Leak Test API

### Endpoint
```
POST /api/tire/leaktest
Content-Type: application/json

{
  "tirePosition": "FRONT_LEFT"
}
```

### Response
```json
{
  "stationId": "STATION-001",
  "sessionId": "SESSION-12345",
  "result": {
    "testId": "LEAK-001",
    "tirePosition": "FRONT_LEFT",
    "hasLeak": true,
    "leakRate": 0.3,
    "testDuration": 60,
    "pressureReadings": [
      { "timestamp": 1708646400000, "pressure": 32.0, "temperature": 25.0, "tirePosition": "FRONT_LEFT" },
      { "timestamp": 1708646410000, "pressure": 31.95, "temperature": 25.1, "tirePosition": "FRONT_LEFT" },
      { "timestamp": 1708646420000, "pressure": 31.9, "temperature": 25.1, "tirePosition": "FRONT_LEFT" }
    ],
    "confidence": 0.88
  }
}
```

---

## 📊 Station Info API

### Endpoint
```
GET /api/station/info
```

### Response
```json
{
  "stationId": "STATION-001",
  "name": "PSI PILOT Station Alpha",
  "location": "Bay 3, Main Fuel Station",
  "ipAddress": "192.168.4.1",
  "status": "ONLINE",
  "capabilities": [
    "AUTO_INFLATE",
    "LEAK_DETECTION",
    "TIRE_HEALTH_SCAN",
    "VEHICLE_DETECTION",
    "NUMBER_PLATE_RECOGNITION",
    "TEMPERATURE_MONITORING"
  ]
}
```

### Station Status Values
```
ONLINE, OFFLINE, BUSY, MAINTENANCE, ERROR
```

### Capabilities
```
AUTO_INFLATE, LEAK_DETECTION, TIRE_HEALTH_SCAN, VEHICLE_DETECTION, 
NUMBER_PLATE_RECOGNITION, TEMPERATURE_MONITORING
```

---

## 🎯 Minimal Implementation (Just to Test App)

If full AI is not ready, send simplified test data:

### Minimal Vehicle Detection
```json
{
  "stationId": "TEST-001",
  "sessionId": null,
  "result": {
    "detectionId": "DET-001",
    "timestamp": 1708646400000,
    "imageUrl": null,
    "vehicles": [{
      "vehicleId": "V1",
      "vehicleType": { "type": "SEDAN", "confidence": 0.9, "alternativePredictions": [] },
      "licensePlate": { "plateNumber": "TEST123", "confidence": 0.85, "isValid": true },
      "color": { "primaryColor": "WHITE", "confidence": 0.8 },
      "orientation": "FRONT",
      "confidence": 0.9,
      "features": {},
      "boundingBox": { "x": 0.2, "y": 0.2, "width": 0.6, "height": 0.6, "normalized": true }
    }],
    "processingTime": 100,
    "cameraId": "CAM-1",
    "confidence": 0.9
  }
}
```

### Minimal Tire Scan
```json
{
  "stationId": "TEST-001",
  "sessionId": null,
  "result": {
    "scanId": "SCAN-001",
    "timestamp": 1708646400000,
    "tirePosition": "FRONT_LEFT",
    "images": [],
    "crackDetection": {
      "hasCracks": false,
      "detectedCracks": [],
      "crackSeverity": "NONE",
      "totalCrackLength": 0,
      "crackDensity": 0,
      "confidence": 0.95
    },
    "wearAnalysis": {
      "wearPattern": "NORMAL",
      "wearLevel": "GOOD",
      "treadDepthMeasurements": [
        { "location": "CENTER", "depth": 6.0, "measurementPoint": { "x": 0.5, "y": 0.5 }, "confidence": 0.9 }
      ],
      "averageTreadDepth": 6.0,
      "minimumTreadDepth": 6.0,
      "wearUniformity": 1.0,
      "estimatedRemainingLife": { "remainingKilometers": 40000, "remainingMonths": 30, "confidence": 0.8 },
      "confidence": 0.9
    },
    "sidewallAnalysis": {
      "hasDamage": false,
      "detectedAnomalies": [],
      "sidewallCondition": "EXCELLENT",
      "confidence": 0.95
    },
    "treadAnalysis": {
      "hasForeignObjects": false,
      "detectedObjects": [],
      "treadPattern": {
        "patternType": "SYMMETRIC",
        "patternCondition": "EXCELLENT",
        "blocksWorn": false,
        "sipesVisible": true,
        "groovesClean": true
      },
      "confidence": 0.93
    },
    "overallCondition": {
      "overallScore": 90.0,
      "overallStatus": "EXCELLENT",
      "safetyRating": "SAFE",
      "recommendations": [],
      "criticality": "NORMAL",
      "estimatedCost": null
    },
    "processingTime": 500,
    "aiModelVersion": "v1.0.0"
  }
}
```

---

## 🧪 Testing Tips

1. **Start Simple**: Implement station info API first
2. **Test with Postman**: Verify JSON structure
3. **Add Images Gradually**: Start without images, then add URLs
4. **Mock AI Data**: Use random confidence scores 0.8-0.95
5. **Test Each Endpoint**: Verify app displays data correctly

---

## 📞 Support

- Check `DEVELOPMENT_SUMMARY.md` for full architecture
- See `PSI_PILOT_README.md` for complete documentation
- All Kotlin data models are in `models/` folder

**The app is ready to receive and display your AI detection results!** 🚀

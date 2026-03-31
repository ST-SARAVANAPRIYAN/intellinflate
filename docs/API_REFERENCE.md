# IntellInflate API Reference - Production Ready

## Base URL
```
HTTP:  http://localhost:3000
WebSocket: ws://localhost:8080
Production: https://yourdomain.com
```

## Authentication
All API endpoints require JWT authentication (except /auth/register and /auth/login).

**Header:**
```
Authorization: Bearer <JWT_TOKEN>
```

---

## API Endpoints

### 1. Authentication

#### Register User
```http
POST /auth/register
Content-Type: application/json

{
  "username": "user@example.com",
  "password": "securePassword123"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userId": "507f1f77bcf86cd799439011"
}
```

#### Login
```http
POST /auth/login
Content-Type: application/json

{
  "username": "user@example.com",
  "password": "securePassword123"
}
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userId": "507f1f77bcf86cd799439011"
}
```

---

### 2. Vision Analysis

#### Tire Analysis
```http
POST /api/vision/analyze-tire
Content-Type: multipart/form-data

{
  "image": <FILE>,
  "analysisType": "cracks|tread|alignment"
}
```

**Response:**
```json
{
  "success": true,
  "analysisType": "cracks",
  "results": {
    "cracksDetected": true,
    "crackPercentage": 15.5,
    "severity": "moderate",
    "location": "outer_edge",
    "recommendations": [
      "Monitor crack growth",
      "Schedule maintenance within 1 month"
    ]
  },
  "timestamp": "2026-03-31T12:00:00Z"
}
```

#### Number Plate Recognition
```http
POST /api/vision/analyze-plate
Content-Type: multipart/form-data

{
  "image": <FILE>
}
```

**Response:**
```json
{
  "success": true,
  "plateNumber": "ABC-1234",
  "confidence": 0.95,
  "format": "US",
  "bboxCoordinates": {
    "x1": 100,
    "y1": 50,
    "x2": 300,
    "y2": 150
  },
  "timestamp": "2026-03-31T12:00:00Z"
}
```

---

### 3. Health & Status

#### Server Health
```http
GET /health
```

**Response:**
```json
{
  "status": "healthy",
  "uptime": 3600,
  "database": "connected",
  "pythonServices": "ready",
  "pythonVersion": "3.11.14",
  "mlModels": {
    "cracksModel": "loaded",
    "treadModel": "loaded",
    "yoloModel": "not_configured"
  }
}
```

---

## WebSocket Events

### Client → Server

#### Connect
```javascript
ws.send(JSON.stringify({
  type: 'auth',
  token: 'JWT_TOKEN'
}));
```

#### Image Stream
```javascript
ws.send(JSON.stringify({
  type: 'image_stream',
  data: base64EncodedImage,
  timestamp: Date.now()
}));
```

### Server → Client

#### Analysis Result
```javascript
{
  type: 'analysis_result',
  analysisId: 'uuid',
  success: true,
  result: { /* analysis data */ },
  processingTime: 234
}
```

#### Error
```javascript
{
  type: 'error',
  code: 'ANALYSIS_FAILED',
  message: 'Failed to process image'
}
```

---

## Error Codes

| Code | Status | Description |
|------|--------|-------------|
| 401 | Unauthorized | Missing or invalid JWT token |
| 400 | Bad Request | Invalid request parameters |
| 403 | Forbidden | User does not have permission |
| 404 | Not Found | Resource not found |
| 500 | Internal Server Error | Server processing error |
| 503 | Service Unavailable | Python services unavailable |

---

## Rate Limiting

- 100 requests per minute per IP
- 1000 requests per hour per user
- WebSocket connections limited to 5 per user

---

## Response Format

All responses follow this format:

```json
{
  "success": true|false,
  "data": { /* endpoint-specific data */ },
  "error": { /* only present if success=false */
    "code": "ERROR_CODE",
    "message": "Human readable error message"
  },
  "timestamp": "ISO8601 timestamp"
}
```

---

## Best Practices

1. **Authentication**: Store JWT tokens securely; refresh tokens before expiry
2. **Image upload**: Compress large images before sending (max 10MB)
3. **Error handling**: Implement retry logic with exponential backoff
4. **Monitoring**: Subscribe to /health endpoint for service status
5. **Rate limiting**: Implement client-side throttling to avoid 429 errors

---

## Example: Complete Analysis Workflow

```javascript
// 1. Login
const loginRes = await fetch('http://localhost:3000/auth/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    username: 'user@example.com',
    password: 'password123'
  })
});
const { token } = await loginRes.json();

// 2. Upload image and analyze
const formData = new FormData();
formData.append('image', imageFile);
formData.append('analysisType', 'cracks');

const analysisRes = await fetch(
  'http://localhost:3000/api/vision/analyze-tire',
  {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`
    },
    body: formData
  }
);

const result = await analysisRes.json();
console.log('Analysis results:', result.data);
```

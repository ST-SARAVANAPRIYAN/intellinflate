# Design Project - Unified Backend & Mobile Application

This project combines a Node.js backend, an Android mobile application, and a Python-based vehicle detection service.

## Project Structure

- `backend/`: Node.js Express server (Main coordinator).
- `python-services/`: Python scripts for number plate detection (using EasyOCR & OpenCV).
- `mobile/`: Android application source code.
- `frontend/`: Web dashboard for monitoring (Static files).

## Features

1. **User Registration**: Register users with their number plate and vehicle details (Stored in MongoDB).
2. **Vehicle Detection**: Scan a vehicle image to detect the number plate and automatically identify the registered user.
3. **IoT Integration**: WebSocket and HTTP endpoints for real-time tire pressure data.

## Setup

### 1. Backend
```bash
cd backend
npm install
node server.js
```
*Note: Ensure `.env` contains your `MONGODB_URI`.*

### 2. Python Services
Ensure you have the required dependencies:
```bash
cd backend/python-services
pip install -r requirements.txt
```

### 3. Mobile App
Open the `mobile/` directory in Android Studio to build and run the application.

## API Endpoints

- `POST /api/register`: Register a new user.
- `POST /api/detect`: Upload an image to detect the number plate and retrieve user info.
- `GET /health`: Check server and database status.

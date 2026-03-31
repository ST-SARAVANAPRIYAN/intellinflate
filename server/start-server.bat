@echo off
REM IntellInflate WebSocket Server Launcher
echo.
echo ========================================
echo   IntellInflate WebSocket Server
echo ========================================
echo.
echo Checking for Node.js...

where node >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Node.js is not installed!
    echo.
    echo Please install Node.js from: https://nodejs.org
    echo.
    pause
    exit /b 1
)

echo Node.js found: 
node --version
echo.

if not exist "node_modules" (
    echo Installing dependencies...
    call npm install
    echo.
)

echo Starting WebSocket server...
echo Server will run on: ws://localhost:8080
echo.
echo Press Ctrl+C to stop the server
echo.

node server.js

pause

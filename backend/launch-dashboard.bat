@echo off
REM IntelliInflate Web Dashboard Launcher
echo.
echo ========================================
echo   IntelliInflate Dashboard Launcher
echo ========================================
echo.
echo Opening dashboard in your default browser...
echo.

start index.html

echo.
echo Dashboard opened! Click "Demo Mode" to see it in action.
echo.
echo Connection Options:
echo   1. Demo Mode - Simulated data (no hardware needed)
echo   2. Web Bluetooth - Direct ESP32 connection (Chrome/Edge)
echo   3. WebSocket Server - Run 'start-server.bat' first
echo   4. Web Serial - USB connection (Chrome/Edge)
echo.
echo Press any key to exit...
pause >nul

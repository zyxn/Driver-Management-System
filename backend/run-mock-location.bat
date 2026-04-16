@echo off
echo ========================================
echo Mock Location Generator (3 Drivers)
echo ========================================
echo.
echo This will simulate 3 drivers moving in Jakarta area
echo No authentication required - direct WebSocket connection
echo.
echo Make sure backend server is running on port 8080
echo.
echo Press any key to start...
pause > nul

echo.
echo Starting mock location generator...
echo Press Ctrl+C to stop
echo.

go run cmd/mock-location/main.go

pause

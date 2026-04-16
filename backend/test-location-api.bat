@echo off
echo Testing Location API...
echo.

curl -X POST http://localhost:8080/api/v1/locations/track ^
  -H "Content-Type: application/json" ^
  -d "{\"user_id\":2,\"latitude\":-6.2088,\"longitude\":106.8456,\"speed\":45.5,\"accuracy\":10.2,\"heading\":180.5,\"recorded_at_utc\":\"2024-01-01T10:00:00Z\",\"timezone\":\"Asia/Jakarta\",\"recorded_at_local\":\"2024-01-01T17:00:00+07:00\"}"

echo.
echo.
echo If you see success:true, the API is working!
pause

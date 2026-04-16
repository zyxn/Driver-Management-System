# Notification System Setup Guide

## Overview
Sistem notifikasi overspeed yang mengirim alert ke driver dan semua operator ketika kecepatan melebihi batas yang ditentukan (default: 150 km/h).

## Backend Setup

### 1. Update Database Schema

**Option A: Menggunakan SQL Script (Recommended)**
```bash
cd backend
psql -U postgres -d driver_management -f MIGRATION_NOTIFICATION.sql
```

**Option B: Menggunakan Auto Migration**
```bash
cd backend
go run cmd/migrate/main.go
```

Ini akan menambahkan:
- `users.fcm_token` - untuk menyimpan FCM token
- `alerts.alert_type` - untuk tipe alert (overspeed, dll)
- `alerts.metadata` - untuk data tambahan (speed, limit, dll)

### 2. Re-seed Database (Optional)
Jika ingin data fresh dengan FCM token dummy:
```bash
cd backend
go run cmd/seed/main.go
```

Seeder akan membuat:
- 15 drivers dengan FCM token (50% dari active drivers)
- 3 operators dengan FCM token (60% dari operators)

### 3. Konfigurasi FCM (HTTP v1 API - Modern & Secure)

**a. Download Service Account JSON:**
1. Buka [Firebase Console](https://console.firebase.google.com/)
2. Pilih project Anda
3. Klik Settings (⚙️) > Project Settings
4. Pilih tab "Service Accounts"
5. Klik "Generate new private key"
6. Download file JSON → rename jadi `serviceAccount.json`
7. Letakkan di `backend/serviceAccount.json`

**b. Update .env file:**
```env
# FCM Configuration (HTTP v1 API)
FCM_PROJECT_ID=your-project-id-here
FCM_SERVICE_ACCOUNT_PATH=serviceAccount.json

# Alert Configuration
SPEED_LIMIT=150
```

**Cara dapat Project ID:**
- Lihat di Firebase Console > Project Settings > General
- Atau buka file `serviceAccount.json`, lihat field `project_id`

### 4. Jalankan Backend
```bash
cd backend
go run main.go
```

## Android Setup

### 1. Setup Firebase Project
1. Buka [Firebase Console](https://console.firebase.google.com/)
2. Buat project baru atau gunakan yang sudah ada
3. Tambahkan Android app:
   - Package name: `com.example.driver_management_system`
   - Download `google-services.json`
   - Letakkan di `mobile/app/google-services.json`

### 2. Enable Cloud Messaging
1. Di Firebase Console, buka "Cloud Messaging"
2. Enable "Cloud Messaging API (Legacy)"

### 3. Update Build Configuration
File `mobile/app/build.gradle.kts` sudah diupdate dengan:
- Firebase BOM
- Firebase Messaging dependency

Tambahkan di `mobile/build.gradle.kts` (root level):
```kotlin
plugins {
    id("com.google.gms.google-services") version "4.4.0" apply false
}
```

Dan di `mobile/app/build.gradle.kts`:
```kotlin
plugins {
    // ... existing plugins
    id("com.google.gms.google-services")
}
```

### 4. Sync & Build
```bash
cd mobile
./gradlew clean build
```

## Testing

### 1. Test FCM Token Registration
Setelah login di app, token akan otomatis dikirim ke backend. Check logs:

**Android:**
```
adb logcat | grep FCMManager
```

**Backend:**
```
# Check if token is saved
curl -X GET http://localhost:8080/api/v1/users/{userId} \
  -H "Authorization: Bearer {token}"
```

### 2. Test Overspeed Notification
Gunakan mock location untuk simulate overspeed:

```bash
cd backend
go run cmd/mock-location/main.go
```

Edit `cmd/mock-location/main.go` untuk set speed > 150 km/h:
```go
speed := 160.0 // Overspeed!
```

### 3. Manual Test FCM
Test FCM langsung menggunakan curl:

```bash
curl -X POST https://fcm.googleapis.com/fcm/send \
  -H "Authorization: key=YOUR_FCM_SERVER_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "to": "DEVICE_FCM_TOKEN",
    "notification": {
      "title": "Test Notification",
      "body": "This is a test"
    },
    "data": {
      "type": "overspeed",
      "speed": "160",
      "limit": "150"
    }
  }'
```

## How It Works

### Flow Diagram
```
1. Driver App sends location with speed > 150 km/h
   ↓
2. Backend LocationUseCase.TrackLocation() receives data
   ↓
3. checkOverspeedAndNotify() detects overspeed
   ↓
4. Create Alert in database
   ↓
5. Send FCM to Driver (if has token)
   ↓
6. Send FCM to all Operators (if have tokens)
   ↓
7. Android FCMService receives notification
   ↓
8. Display notification to user
```

### Backend Components
- `config/config.go` - FCM & Speed limit config
- `notification/fcm_service.go` - FCM sending logic
- `usecase/location_usecase.go` - Overspeed detection
- `entity/user.go` - FCM token field
- `entity/alert.go` - Alert type & metadata

### Android Components
- `service/FCMService.kt` - Receive & display notifications
- `data/manager/FCMManager.kt` - Token management
- `data/remote/ApiService.kt` - API endpoint for token update

## API Endpoints

### Update FCM Token
```
POST /api/v1/users/fcm-token
Authorization: Bearer {token}

{
  "fcm_token": "device_fcm_token_here"
}
```

### Get Alerts
```
GET /api/v1/alerts/driver/{driverId}
Authorization: Bearer {token}
```

## Notification Types

### 1. Overspeed Alert (Driver)
- **Title:** ⚠️ Peringatan Kecepatan Berlebih
- **Body:** Kecepatan Anda {speed} km/h melebihi batas {limit} km/h
- **Priority:** HIGH
- **Channel:** Speed Alerts

### 2. Overspeed Alert (Operator)
- **Title:** 🚨 Driver Overspeed Alert
- **Body:** {driver_name} berkendara dengan kecepatan {speed} km/h
- **Priority:** DEFAULT
- **Channel:** Operator Alerts

## Troubleshooting

### FCM Token tidak terkirim
1. Check internet connection
2. Check Firebase project configuration
3. Verify `google-services.json` is correct
4. Check logs: `adb logcat | grep FCM`

### Notification tidak muncul
1. Check notification permission di Android settings
2. Verify FCM Server Key di backend `.env`
3. Check if device token is registered in backend
4. Test with manual FCM curl command

### Backend error
1. Check if FCM_SERVER_KEY is set in `.env`
2. Verify database migration ran successfully
3. Check backend logs for errors

## Production Checklist

- [ ] Setup proper Firebase project for production
- [ ] Update FCM Server Key in production `.env`
- [ ] Set appropriate SPEED_LIMIT value
- [ ] Test notification delivery
- [ ] Setup notification analytics
- [ ] Configure notification channels properly
- [ ] Test on different Android versions
- [ ] Handle notification permission requests
- [ ] Implement notification history/logs
- [ ] Add notification preferences for users

## Future Enhancements

1. **Notification Preferences**
   - Allow users to customize notification settings
   - Mute notifications for specific times

2. **Rich Notifications**
   - Add action buttons (e.g., "View Location", "Dismiss")
   - Show map preview in notification

3. **Analytics**
   - Track notification delivery rate
   - Monitor overspeed incidents

4. **Multiple Alert Types**
   - Geofence alerts
   - Long idle time alerts
   - Low battery alerts

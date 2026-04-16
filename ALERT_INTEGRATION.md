# Alert Integration Documentation

## Overview
Alert system terintegrasi penuh antara Frontend (Svelte), Backend (Go), dan Mobile (Kotlin) dengan FCM notification.

## Backend Integration ✅

### Endpoints
- `POST /api/v1/alerts` - Create alert dan kirim FCM notification
- `GET /api/v1/alerts` - Get all alerts
- `GET /api/v1/alerts/driver/:id` - Get alerts by driver ID
- `PATCH /api/v1/alerts/:id/status` - Update alert status

### Alert Flow
1. Admin kirim alert dari frontend
2. Backend save alert ke database
3. Backend kirim FCM notification ke driver mobile
4. Mobile app terima dan tampilkan notification

### FCM Notification Payload
```json
{
  "notification": {
    "title": "Alert Title",
    "body": "Alert Message",
    "sound": "default"
  },
  "data": {
    "type": "alert",
    "driver_id": "123",
    "alert_id": "456"
  }
}
```

## Frontend Integration ✅

### Alert Page
- Location: `frontend/src/routes/(private)/alerts/+page.svelte`
- Features:
  - List semua drivers
  - Select driver untuk kirim alert
  - Form dengan title, message, dan priority
  - Real-time validation
  - Toast notification untuk feedback

### API Client
- Location: `frontend/src/lib/api/alerts.ts`
- Functions:
  - `sendAlert(alert)` - Kirim alert ke driver
  - `getAlerts()` - Get all alerts
  - `getAlertsByDriver(driverId)` - Get alerts by driver

### Types
```typescript
interface CreateAlertRequest {
  driver_id: number;
  title: string;
  message: string;
  priority: 'low' | 'medium' | 'high' | 'critical';
}
```

## Mobile Integration ✅

### FCM Service
- Location: `mobile/app/src/main/java/.../service/FCMService.kt`
- Handler: `handleAlertNotification()`
- Notification Channel: `speed_alerts` (High priority)

### Alert Notification
- Title: Dari FCM payload
- Body: Dari FCM payload
- Priority: HIGH
- Sound: Default notification sound
- Action: Open MainActivity

## Testing

### 1. Test Backend
```bash
cd backend
go run main.go
```

### 2. Test Frontend
```bash
cd frontend
npm run dev
```
Buka: http://localhost:5173/alerts

### 3. Test Alert Flow
1. Login sebagai admin/operator
2. Buka halaman Alerts
3. Pilih driver dari list
4. Isi form:
   - Title: "Test Alert"
   - Message: "This is a test alert"
   - Priority: "high"
5. Klik "Send Alert"
6. Check:
   - Toast success muncul
   - Database ada record baru
   - Mobile app terima notification (jika driver punya FCM token)

### 4. Check Database
```sql
SELECT * FROM alerts ORDER BY created_at DESC LIMIT 10;
```

### 5. Check Backend Logs
```
FCM notification sent successfully to driver X for alert Y
```
atau
```
Driver X has no FCM token, skipping notification
```

## Configuration

### Backend (.env)
```env
PORT=3000
FCM_PROJECT_ID=your-project-id
FCM_SERVICE_ACCOUNT_PATH=./serviceAccount.json
```

### Frontend (.env)
```env
VITE_API_URL=http://localhost:3000/api/v1
```

### Mobile (google-services.json)
Pastikan file sudah ada dan valid.

## Troubleshooting

### Alert tidak terkirim
- Check backend logs untuk error
- Pastikan driver exists dan role = "driver"
- Check network request di browser DevTools

### FCM notification tidak muncul
- Check driver punya FCM token di database
- Check backend logs untuk FCM error
- Pastikan serviceAccount.json valid
- Check mobile app permission untuk notifications

### Frontend error
- Check API_BASE_URL di .env
- Check CORS settings di backend
- Check authentication (cookies)

## Next Steps
- [ ] Add alert history page
- [ ] Add alert templates
- [ ] Add bulk alert sending
- [ ] Add alert scheduling
- [ ] Add alert analytics

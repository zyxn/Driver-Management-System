# FCM HTTP v1 API Setup Guide

## ✅ Modern & Secure (Recommended)

Sistem ini menggunakan **FCM HTTP v1 API** dengan OAuth2, bukan Legacy Server Key yang sudah deprecated.

## 📥 Download Service Account

1. **Buka Firebase Console:**
   - https://console.firebase.google.com/
   - Pilih project kamu

2. **Download Service Account:**
   - Klik ⚙️ (Settings) > **Project Settings**
   - Tab **Service Accounts**
   - Klik **Generate new private key**
   - Download file JSON

3. **Rename & Letakkan:**
   ```bash
   # Rename file yang didownload
   mv ~/Downloads/your-project-firebase-adminsdk-xxxxx.json backend/serviceAccount.json
   ```

## ⚙️ Konfigurasi Backend

Edit `backend/.env`:

```env
# FCM Configuration (HTTP v1 API)
FCM_PROJECT_ID=your-project-id
FCM_SERVICE_ACCOUNT_PATH=serviceAccount.json

# Alert Configuration
SPEED_LIMIT=150
```

**Cara dapat Project ID:**
- Buka `serviceAccount.json`, lihat field `project_id`
- Atau di Firebase Console > Project Settings > General

## 🔒 Security

**PENTING:**
- ❌ **JANGAN** commit `serviceAccount.json` ke Git
- ✅ File sudah ada di `.gitignore`
- ✅ Setiap developer download sendiri dari Firebase Console

## 🆚 Legacy vs Modern

| Feature | Legacy API (❌ Deprecated) | HTTP v1 API (✅ Recommended) |
|---------|---------------------------|------------------------------|
| Auth | Server Key | OAuth2 Token |
| Endpoint | `/fcm/send` | `/v1/projects/{id}/messages:send` |
| Security | ⚠️ Key bisa bocor | ✅ Token expire otomatis |
| Future | ❌ Akan dihapus | ✅ Supported long-term |

## 🧪 Testing

1. **Build backend:**
   ```bash
   cd backend
   go run main.go
   ```

2. **Test overspeed notification:**
   ```bash
   # Edit cmd/mock-location/main.go, set speed > 150
   go run cmd/mock-location/main.go
   ```

3. **Check logs:**
   - Backend: Lihat console untuk FCM send status
   - Android: `adb logcat | grep FCM`

## 🐛 Troubleshooting

### Error: "failed to read service account file"
- ✅ Pastikan `serviceAccount.json` ada di folder `backend/`
- ✅ Check path di `.env`: `FCM_SERVICE_ACCOUNT_PATH=serviceAccount.json`

### Error: "failed to get access token"
- ✅ Pastikan service account punya permission "Firebase Cloud Messaging API"
- ✅ Re-download service account dari Firebase Console

### Error: "FCM request failed with status 404"
- ✅ Check `FCM_PROJECT_ID` di `.env` sudah benar
- ✅ Pastikan project ID sama dengan yang di `serviceAccount.json`

## 📚 Resources

- [FCM HTTP v1 API Docs](https://firebase.google.com/docs/cloud-messaging/migrate-v1)
- [Service Account Setup](https://firebase.google.com/docs/admin/setup)

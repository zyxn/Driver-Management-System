# GPS Tracking Berdasarkan Role

## Fitur
GPS tracking otomatis akan dimulai **hanya untuk user dengan role "driver"** setelah login berhasil.

## Cara Kerja

### 1. Login sebagai Driver
- Ketika user login dengan role "driver", aplikasi akan:
  - Meminta permission lokasi (Fine Location, Coarse Location)
  - Meminta permission background location (Android 10+)
  - Meminta permission notifikasi (Android 13+)
  - Meminta battery optimization exemption
  - Memulai LocationTrackingService secara otomatis

### 2. Login sebagai Non-Driver (Admin/Manager)
- Ketika user login dengan role selain "driver", aplikasi akan:
  - **TIDAK** meminta permission lokasi
  - **TIDAK** memulai GPS tracking
  - Langsung masuk ke home screen tanpa tracking

### 3. Auto-Resume Tracking
- Jika app direstart dan user sudah login sebagai driver sebelumnya:
  - GPS tracking akan otomatis dimulai kembali
  - Tidak perlu login ulang

### 4. Logout
- Ketika user logout:
  - GPS tracking service akan dihentikan
  - Semua data user dihapus dari preferences
  - Notifikasi tracking akan hilang

## Validasi di Backend
LocationTrackingService akan memvalidasi role sebelum mengirim data lokasi:
```kotlin
// Hanya driver yang bisa mengirim lokasi
if (!userRole.equals("driver", ignoreCase = true)) {
    Log.w(TAG, "User is not a driver, skipping location send")
    return
}
```

## Testing

### Test sebagai Driver
1. Login dengan akun driver (role: "driver")
2. Aplikasi akan meminta permission lokasi
3. Setelah permission diberikan, notifikasi tracking akan muncul
4. Lokasi akan dikirim ke backend setiap 30 detik

### Test sebagai Admin/Manager
1. Login dengan akun admin/manager (role: "admin" atau "manager")
2. Aplikasi **TIDAK** akan meminta permission lokasi
3. **TIDAK** ada notifikasi tracking
4. Langsung masuk ke home screen

### Test Auto-Resume
1. Login sebagai driver
2. Tutup aplikasi (swipe dari recent apps)
3. Buka aplikasi kembali
4. GPS tracking akan otomatis berjalan tanpa perlu login ulang

### Test Logout
1. Login sebagai driver (tracking aktif)
2. Klik logout
3. GPS tracking akan berhenti
4. Notifikasi tracking akan hilang

## File yang Dimodifikasi

1. **MainActivity.kt**
   - Menambahkan parameter `role` pada callback `onLoginSuccess`
   - Menambahkan callback `onLogoutAction` untuk stop tracking service
   - Hanya request permission jika role = "driver"
   - Auto-start tracking saat app restart untuk driver
   - Stop tracking saat logout melalui callback

2. **PreferencesManager.kt**
   - Menambahkan method `getUserRole()` untuk akses synchronous
   - Menambahkan method `saveRoleSync()` untuk menyimpan role

3. **AuthRepositoryImpl.kt**
   - Menyimpan role secara synchronous untuk akses dari Service

4. **LocationTrackingService.kt**
   - Validasi role sebelum mengirim lokasi ke backend
   - Hanya driver yang bisa mengirim data lokasi

## Role yang Didukung
- **driver**: GPS tracking aktif
- **admin**: GPS tracking tidak aktif
- **manager**: GPS tracking tidak aktif
- **supervisor**: GPS tracking tidak aktif

## Catatan Penting
- GPS tracking hanya berjalan untuk role "driver" (case-insensitive)
- Service akan tetap berjalan di background meskipun app ditutup
- Lokasi dikirim ke backend setiap 30 detik
- Battery optimization exemption diperlukan untuk tracking yang stabil

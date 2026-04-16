# Setup Autentikasi Mobile App

## Perubahan yang Dilakukan

### 1. Login menggunakan Email (bukan Username)
- Field login diubah dari username ke email
- Validasi email menggunakan `Patterns.EMAIL_ADDRESS`
- UI menggunakan icon email dan placeholder yang sesuai

### 2. Integrasi dengan Backend API

#### Dependencies yang Ditambahkan
```kotlin
// Retrofit untuk HTTP client
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.retrofit2:converter-gson:2.9.0")
implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

// DataStore untuk menyimpan token dan user data
implementation("androidx.datastore:datastore-preferences:1.0.0")
```

#### Struktur Baru yang Dibuat

**Data Layer:**
- `data/remote/ApiConfig.kt` - Konfigurasi API (base URL, timeout)
- `data/remote/ApiService.kt` - Interface Retrofit untuk endpoint
- `data/remote/RetrofitClient.kt` - Singleton Retrofit client
- `data/remote/dto/LoginRequest.kt` - DTO untuk request login
- `data/remote/dto/LoginResponse.kt` - DTO untuk response login
- `data/remote/dto/ErrorResponse.kt` - DTO untuk error response
- `data/local/PreferencesManager.kt` - Manager untuk DataStore (simpan token & user data)
- `data/repository/AuthRepositoryImpl.kt` - Implementasi repository dengan API call

**Domain Layer:**
- `domain/model/User.kt` - Model User
- `domain/model/LoginCredentials.kt` - Diubah dari username ke email
- `domain/model/LoginResult.kt` - Ditambahkan User object
- `domain/usecase/LoginUseCase.kt` - Validasi input dan business logic

**Presentation Layer:**
- `presentation/login/LoginScreen.kt` - UI diubah ke email field
- `presentation/login/LoginViewModel.kt` - Diubah untuk handle email
- `presentation/login/LoginUiState.kt` - Ditambahkan User object

### 3. Fitur yang Diimplementasikan

✅ Login dengan email dan password
✅ Validasi email format
✅ Validasi password minimal 6 karakter
✅ Koneksi ke backend API (http://10.0.2.2:8080/api/v1/)
✅ Parsing response JSON dari backend
✅ Extract JWT token dari Set-Cookie header
✅ Simpan token dan user data ke DataStore
✅ Error handling untuk network error dan API error
✅ Loading state saat login
✅ Success state setelah login berhasil

## Cara Testing

### 1. Pastikan Backend Berjalan
```bash
cd backend
go run main.go
```

Backend harus berjalan di `http://localhost:8080`

### 2. Setup Database
Pastikan database sudah ada user dengan email dan password yang di-hash. Atau gunakan endpoint register terlebih dahulu.

### 3. Testing di Emulator
- Base URL sudah diset ke `http://10.0.2.2:8080/api/v1/` (localhost dari emulator)
- Build dan run aplikasi
- Masukkan email dan password yang valid
- Klik tombol "Masuk"

### 4. Testing di Device Fisik
Ubah base URL di `ApiConfig.kt`:
```kotlin
const val BASE_URL = "http://192.168.1.XXX:8080/api/v1/"
```
Ganti `192.168.1.XXX` dengan IP komputer Anda di jaringan lokal.

Cara cek IP komputer:
- Windows: `ipconfig` (lihat IPv4 Address)
- Mac/Linux: `ifconfig` atau `ip addr`

### 5. Test Credentials
Gunakan user yang sudah terdaftar di database, contoh:
- Email: `driver1@example.com`
- Password: `password123`

## Troubleshooting

### Error: "Koneksi gagal"
- Pastikan backend berjalan
- Pastikan base URL sudah benar
- Cek firewall tidak memblokir port 8080
- Untuk device fisik, pastikan di jaringan yang sama dengan komputer

### Error: "Invalid credentials"
- Pastikan email dan password benar
- Cek database apakah user sudah terdaftar
- Pastikan password di-hash dengan bcrypt

### Token tidak tersimpan
- Cek logcat untuk melihat response dari backend
- Pastikan backend mengirim Set-Cookie header dengan token

## Alur Autentikasi

1. User input email dan password
2. `LoginViewModel` memanggil `LoginUseCase`
3. `LoginUseCase` validasi input, lalu panggil `AuthRepository`
4. `AuthRepositoryImpl` membuat `LoginRequest` dan kirim ke API
5. Backend memvalidasi credentials dan return response dengan Set-Cookie
6. Mobile app extract token dari cookie header
7. Token dan user data disimpan ke DataStore
8. UI update ke success state
9. Navigate ke HomeScreen

## Next Steps

- [ ] Implement auto-login (cek token saat app start)
- [ ] Implement logout (clear DataStore)
- [ ] Implement token refresh
- [ ] Add interceptor untuk attach token ke setiap request
- [ ] Implement register screen
- [ ] Handle token expiration

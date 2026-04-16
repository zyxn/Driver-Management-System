# Fitur Daftar Driver

## Deskripsi
Fitur ini menampilkan daftar semua driver dengan informasi lokasi GPS terakhir mereka, kecepatan, dan alamat yang dikonversi dari koordinat menggunakan OpenStreetMap Nominatim API.

## Komponen yang Dibuat

### Mobile App (Android - Kotlin)

1. **DriverListScreen.kt**
   - Screen utama untuk menampilkan daftar driver
   - Fitur pull-to-refresh untuk memuat ulang data
   - Loading state dan error handling
   - Empty state ketika tidak ada driver

2. **DriverCard.kt**
   - Card component untuk menampilkan informasi driver
   - Menampilkan:
     - Avatar dengan inisial nama
     - Nama dan nomor telepon
     - Status (Aktif/Offline)
     - Koordinat GPS (latitude, longitude)
     - Alamat (dikonversi dari koordinat)
     - Kecepatan terakhir
     - Waktu update terakhir

3. **DriverListViewModel.kt**
   - ViewModel untuk mengelola state dan logic
   - Mengambil data driver dari API
   - Melakukan reverse geocoding untuk konversi koordinat ke alamat
   - Caching alamat untuk menghindari request berulang

4. **Domain Layer**
   - `Driver.kt` - Model domain untuk driver
   - `DriverLocation.kt` - Model untuk lokasi driver
   - `DriverRepository.kt` - Interface repository
   - `GetAllDriversUseCase.kt` - Use case untuk mengambil semua driver

5. **Data Layer**
   - `DriverRepositoryImpl.kt` - Implementasi repository
   - `DriverDto.kt` - Data transfer object
   - `NominatimService.kt` - Service untuk reverse geocoding menggunakan OpenStreetMap

### Backend (Go)

1. **user_usecase.go**
   - Method `GetAllDriversWithLocation()` untuk mengambil semua driver dengan lokasi terakhir
   - Menggabungkan data user dengan lokasi GPS terakhir

2. **user_handler.go**
   - Endpoint handler untuk `/users` yang mengembalikan daftar driver dengan lokasi

3. **app.go**
   - Update dependency injection untuk UserUseCase dengan LocationRepository

## API Endpoint

### GET /users
Mengambil daftar semua driver dengan lokasi terakhir mereka.

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "John Doe",
      "email": "john@example.com",
      "phone": "08123456789",
      "role": "driver",
      "last_location": {
        "id": 123,
        "latitude": -6.2088,
        "longitude": 106.8456,
        "speed": 45.5,
        "timestamp": "2024-01-15T10:30:00"
      }
    }
  ]
}
```

## Fitur Utama

1. **List Driver dengan Card**
   - Setiap driver ditampilkan dalam card yang informatif
   - Avatar dengan inisial nama
   - Status online/offline berdasarkan ketersediaan lokasi

2. **Informasi Lokasi GPS**
   - Koordinat latitude dan longitude
   - Alamat lengkap (dikonversi dari koordinat menggunakan OpenStreetMap)
   - Loading indicator saat mengambil alamat

3. **Informasi Kecepatan**
   - Menampilkan kecepatan terakhir dalam km/h
   - Icon speedometer untuk visualisasi

4. **Waktu Update**
   - Menampilkan waktu update lokasi terakhir
   - Format waktu yang mudah dibaca (HH:mm)

5. **Pull to Refresh**
   - Swipe down untuk memuat ulang data
   - Loading indicator saat refresh

6. **Error Handling**
   - Menampilkan pesan error jika gagal memuat data
   - Tombol "Coba Lagi" untuk retry

## Teknologi yang Digunakan

### Mobile
- **Jetpack Compose** - UI framework
- **Retrofit** - HTTP client untuk API calls
- **Coroutines & Flow** - Asynchronous programming
- **Accompanist SwipeRefresh** - Pull to refresh functionality
- **OpenStreetMap Nominatim API** - Reverse geocoding

### Backend
- **Go Fiber** - Web framework
- **GORM** - ORM untuk database
- **PostgreSQL** - Database

## Cara Menggunakan

1. Buka aplikasi mobile
2. Login sebagai user
3. Navigasi ke tab "Driver" di bottom navigation
4. Lihat daftar driver dengan informasi lokasi mereka
5. Pull down untuk refresh data

## Catatan Penting

1. **Rate Limiting Nominatim API**
   - Nominatim API memiliki rate limit 1 request per detik
   - Implementasi sudah menambahkan delay 1 detik antar request
   - Alamat di-cache untuk menghindari request berulang

2. **Dependency**
   - Pastikan menambahkan dependency `accompanist-swiperefresh` di `build.gradle.kts`
   - Backend memerlukan LocationRepository di UserUseCase

3. **Navigation**
   - Tab "Driver" ditambahkan di bottom navigation (index 2)
   - Tab "Aksi" dipindah ke index 3

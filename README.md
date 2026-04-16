# Driver Management System

Sistem manajemen driver dengan fitur GPS tracking real-time, alert management, dan reporting.

## 📋 Struktur Project

```
Driver Management System/
├── backend/          # Go Backend (Clean Architecture)
├── frontend/         # SvelteKit Web Dashboard
└── mobile/          # Android Kotlin Mobile App
```

## 🚀 Tech Stack

### Backend
- **Language**: Go 1.21+
- **Framework**: Gin
- **Database**: PostgreSQL
- **Architecture**: Clean Architecture
- **Real-time**: WebSocket
- **Notifications**: Firebase Cloud Messaging (FCM)

### Frontend
- **Framework**: SvelteKit
- **UI Library**: Tailwind CSS + shadcn-svelte
- **Maps**: Leaflet
- **State Management**: Svelte Stores

### Mobile
- **Language**: Kotlin
- **UI**: Jetpack Compose
- **Architecture**: Clean Architecture + MVVM
- **Maps**: Google Maps
- **Notifications**: FCM

## 🔧 Setup & Installation

### Backend Setup

```bash
cd backend

# Copy environment file
cp .env.example .env

# Edit .env dengan konfigurasi Anda
# - Database credentials
# - JWT secret
# - FCM credentials

# Install dependencies
go mod download

# Run migrations
go run cmd/migrate/main.go

# Seed database (optional)
go run cmd/seed/main.go

# Run server
go run main.go
```

### Frontend Setup

```bash
cd frontend

# Copy environment file
cp .env.example .env

# Edit .env dengan API URL backend

# Install dependencies
npm install

# Run development server
npm run dev
```

### Mobile Setup

1. Buka project `mobile/` di Android Studio
2. Copy `google-services.json` dari Firebase Console ke `mobile/app/`
3. Sync Gradle
4. Edit `ApiConfig.kt` untuk mengatur BASE_URL ke backend Anda
5. Run aplikasi

## 📱 Fitur Utama

### Backend
- ✅ Authentication & Authorization (JWT)
- ✅ User Management (Admin, Manager, Driver)
- ✅ Real-time GPS Tracking
- ✅ Alert Management
- ✅ Report Management
- ✅ WebSocket untuk real-time updates
- ✅ FCM Push Notifications

### Frontend Dashboard
- ✅ Login & Authentication
- ✅ Real-time Driver Tracking Map
- ✅ Driver List & Management
- ✅ Alert Management
- ✅ Report Management
- ✅ Responsive Design

### Mobile App
- ✅ Driver Login
- ✅ GPS Tracking Service (Background)
- ✅ Trip History
- ✅ Weather Information
- ✅ Push Notifications
- ✅ Mock Location Detection
- ✅ Offline Support

## 🔐 Environment Variables

### Backend (.env)
```env
DB_HOST=localhost
DB_PORT=5432
DB_USER=postgres
DB_PASSWORD=your_password
DB_NAME=driver_management
JWT_SECRET=your_jwt_secret
FCM_CREDENTIALS_PATH=./serviceAccount.json
```

### Frontend (.env)
```env
PUBLIC_API_URL=http://localhost:8080
```

## 📚 Dokumentasi

- [Backend README](./backend/README.md)
- [Frontend README](./frontend/README.md)
- [Mobile GPS Tracking](./mobile/README-GPS-TRACKING.md)
- [FCM Setup](./backend/FCM_SETUP.md)
- [Alert Integration](./ALERT_INTEGRATION.md)

## 🧪 Testing

### Backend
```bash
cd backend
go test ./...
```

### Frontend
```bash
cd frontend
npm run test
```

## 📦 Build Production

### Backend
```bash
cd backend
go build -o driver-management-backend main.go
```

### Frontend
```bash
cd frontend
npm run build
```

### Mobile
Build APK melalui Android Studio:
- Build > Build Bundle(s) / APK(s) > Build APK(s)

## 🤝 Contributing

1. Fork repository
2. Create feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to branch (`git push origin feature/AmazingFeature`)
5. Open Pull Request

## 📄 License

This project is private and proprietary.

## 👥 Team

- Backend Developer
- Frontend Developer
- Mobile Developer

## 📞 Support

Untuk pertanyaan atau dukungan, silakan hubungi tim development.

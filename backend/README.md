# Driver Management System - Backend

Backend API untuk Driver Management System menggunakan Go Fiber dengan Clean Architecture.

## Struktur Project

```
backend/
├── cmd/                          # Entry points
├── internal/
│   ├── app/                      # Application initialization
│   ├── domain/                   # Domain layer (entities, repositories interface)
│   │   ├── entity/              # Business entities
│   │   └── repository/          # Repository interfaces
│   ├── usecase/                 # Use cases / Business logic
│   ├── infrastructure/          # Infrastructure layer
│   │   ├── config/             # Configuration
│   │   ├── database/           # Database connection
│   │   ├── repository/         # Repository implementations
│   │   └── security/           # JWT, password hashing
│   └── interface/              # Interface layer
│       └── http/
│           ├── handler/        # HTTP handlers
│           ├── middleware/     # HTTP middleware
│           └── router/         # Route definitions
├── .env                        # Environment variables (create from .env.example)
├── .env.example               # Example environment variables
├── go.mod                     # Go module file
└── main.go                    # Application entry point
```

## Setup

### 1. Install Dependencies

```bash
cd backend
go mod download
```

### 2. Setup Database

Install PostgreSQL dan buat database:

```sql
CREATE DATABASE driver_management;
```

### 3. Environment Variables

Copy `.env.example` ke `.env` dan sesuaikan:

```bash
cp .env.example .env
```

Edit `.env`:
```
PORT=8080
DB_HOST=localhost
DB_PORT=5432
DB_USER=postgres
DB_PASSWORD=your_password
DB_NAME=driver_management
DB_SSLMODE=disable

JWT_SECRET=your-secret-key-change-this
JWT_EXPIRY=24h

ALLOWED_ORIGINS=*
```

### 4. Database Migration

Jalankan migration untuk membuat tabel:

```bash
go run cmd/migrate/main.go
```

### 5. Database Seeding (Optional)

Isi database dengan data dummy untuk testing:

```bash
go run cmd/seed/main.go
```

Seeder akan membuat:
- **2 Operator**: operator1, operator2
- **3 Driver**: driver1, driver2, driver3 (1 inactive)
- **Location records**: Data tracking lokasi untuk driver
- **Report records**: Laporan aktivitas driver

**Default Password untuk semua user**: `password123`

### 6. Run Application

```bash
go run main.go
```

Server akan berjalan di `http://localhost:8080`

## API Endpoints

### Authentication

- `POST /api/v1/auth/register` - Register driver baru
- `POST /api/v1/auth/login` - Login driver

### Trips (Protected)

- `POST /api/v1/trips/start` - Mulai perjalanan
- `POST /api/v1/trips/end` - Akhiri perjalanan
- `GET /api/v1/trips/driver/:driverId` - Riwayat perjalanan
- `GET /api/v1/trips/driver/:driverId/active` - Perjalanan aktif

### Locations (Protected)

- `POST /api/v1/locations/track` - Track lokasi
- `GET /api/v1/locations/driver/:driverId` - Riwayat lokasi
- `GET /api/v1/locations/driver/:driverId/latest` - Lokasi terakhir

### Health Check

- `GET /api/v1/health` - Health check

## Testing dengan cURL

### Register
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "driver1",
    "email": "driver1@example.com",
    "password": "password123",
    "full_name": "Driver Satu",
    "phone": "081234567890",
    "license_no": "B1234567"
  }'
```

### Login
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "driver1",
    "password": "password123"
  }'
```

### Start Trip (dengan token)
```bash
curl -X POST http://localhost:8080/api/v1/trips/start \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN_HERE" \
  -d '{
    "driver_id": 1,
    "start_lat": -6.2088,
    "start_lng": 106.8456,
    "notes": "Trip ke kantor"
  }'
```

## Clean Architecture Layers

1. **Domain Layer** (`internal/domain/`)
   - Entities: Business objects
   - Repository Interfaces: Contracts untuk data access

2. **Use Case Layer** (`internal/usecase/`)
   - Business logic
   - Orchestration antar repositories

3. **Infrastructure Layer** (`internal/infrastructure/`)
   - Database implementation
   - External services
   - Security utilities

4. **Interface Layer** (`internal/interface/`)
   - HTTP handlers
   - Request/Response mapping
   - Middleware

## Development

### Build
```bash
go build -o bin/server main.go
```

### Run Binary
```bash
./bin/server
```

### Format Code
```bash
go fmt ./...
```

### Run Tests (coming soon)
```bash
go test ./...
```

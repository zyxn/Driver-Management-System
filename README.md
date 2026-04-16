# Driver Management System

Sistem manajemen driver dengan tracking lokasi real-time dan pelaporan.

## Tech Stack

### Backend
- **Go** (Golang) dengan Fiber framework
- **PostgreSQL** database
- **JWT** authentication dengan httpOnly cookies
- Clean Architecture pattern

### Frontend
- **SvelteKit** 
- **TypeScript**
- **Tailwind CSS**
- **Shadcn-svelte** UI components
- **Leaflet** untuk maps

## Features

- ✅ Authentication dengan JWT httpOnly cookies
- ✅ User management (Driver & Operator roles)
- ✅ Real-time location tracking
- ✅ Report management
- ✅ Interactive maps
- ✅ Responsive dashboard

## Quick Start

### Prerequisites
- Go 1.21+
- Node.js 18+
- PostgreSQL 14+

### Backend Setup

```bash
cd backend

# Install dependencies
go mod download

# Copy environment file
cp .env.example .env

# Edit .env dengan konfigurasi database Anda

# Run migrations
go run cmd/migrate/main.go

# (Optional) Seed data
go run cmd/seed/main.go

# Run server
go run main.go

# Atau dengan hot reload (install air terlebih dahulu)
air
```

Backend akan berjalan di `http://localhost:3000`

### Frontend Setup

```bash
cd frontend

# Install dependencies
npm install

# Copy environment file
cp .env.example .env

# Run dev server
npm run dev
```

Frontend akan berjalan di `http://localhost:5173`

## Authentication

Sistem menggunakan JWT yang disimpan dalam **httpOnly cookies** untuk keamanan maksimal.

### Login
```bash
POST /api/v1/auth/login
{
  "email": "user@example.com",
  "password": "password123"
}
```

### Register
```bash
POST /api/v1/auth/register
{
  "email": "user@example.com",
  "password": "password123",
  "username": "username",
  "full_name": "Full Name",
  "phone": "08123456789",
  "role": "driver"
}
```

Lihat [INTEGRATION.md](./INTEGRATION.md) untuk detail lengkap integrasi frontend-backend.

## API Endpoints

### Public
- `POST /api/v1/auth/login` - Login
- `POST /api/v1/auth/register` - Register

### Protected (Requires Authentication)
- `POST /api/v1/auth/logout` - Logout
- `GET /api/v1/auth/me` - Get current user
- `GET /api/v1/reports` - Get all reports
- `POST /api/v1/reports` - Create report
- `GET /api/v1/reports/:id` - Get report by ID
- `GET /api/v1/reports/user/:userId` - Get user reports
- `POST /api/v1/locations/track` - Track location
- `GET /api/v1/locations/user/:userId` - Get location history
- `GET /api/v1/locations/user/:userId/latest` - Get latest location

## Project Structure

### Backend
```
backend/
├── cmd/
│   ├── migrate/     # Database migrations
│   └── seed/        # Database seeding
├── internal/
│   ├── app/         # Application setup
│   ├── domain/      # Domain entities & repositories
│   ├── infrastructure/  # External dependencies
│   ├── interface/   # HTTP handlers & middleware
│   └── usecase/     # Business logic
└── main.go
```

### Frontend
```
frontend/
├── src/
│   ├── lib/
│   │   ├── api/         # API clients
│   │   ├── components/  # UI components
│   │   └── stores/      # Svelte stores
│   └── routes/
│       ├── (private)/   # Protected routes
│       └── login/       # Public routes
└── package.json
```

## Security Features

- ✅ JWT stored in httpOnly cookies (XSS protection)
- ✅ Secure & SameSite cookie flags (CSRF protection)
- ✅ Password hashing with bcrypt
- ✅ CORS configuration
- ✅ Input validation
- ✅ SQL injection protection (GORM)

## Development

### Backend Hot Reload
```bash
# Install air
go install github.com/cosmtrek/air@latest

# Run with hot reload
cd backend
air
```

### Frontend Dev Server
```bash
cd frontend
npm run dev
```

## Testing

### Test Backend API
```bash
# Register
curl -X POST http://localhost:3000/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123",
    "username": "testuser",
    "full_name": "Test User",
    "phone": "08123456789",
    "role": "driver"
  }'

# Login
curl -X POST http://localhost:3000/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -c cookies.txt \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }'

# Get current user
curl -X GET http://localhost:3000/api/v1/auth/me \
  -b cookies.txt
```

## Production Deployment

Lihat [INTEGRATION.md](./INTEGRATION.md) untuk production checklist.

## License

MIT

## Contributors

- Your Team

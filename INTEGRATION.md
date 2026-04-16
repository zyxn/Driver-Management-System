# Integrasi Frontend & Backend dengan JWT HttpOnly Cookies

## Overview
Sistem ini menggunakan JWT (JSON Web Token) yang disimpan dalam **httpOnly cookies** untuk keamanan maksimal. Token tidak dapat diakses melalui JavaScript, mencegah serangan XSS.

## Backend Configuration

### 1. Environment Variables (.env)
```env
PORT=8080
DB_HOST=localhost
DB_PORT=5432
DB_USER=postgres
DB_PASSWORD=postgres
DB_NAME=driver_management
DB_SSLMODE=disable

JWT_SECRET=your-secret-key-change-this-in-production
JWT_EXPIRY=24h

# CORS - Harus sesuai dengan URL frontend
ALLOWED_ORIGINS=http://localhost:5173
```

### 2. CORS Configuration
Backend sudah dikonfigurasi dengan:
- `AllowCredentials: true` - Mengizinkan cookies
- `AllowOrigins` - Sesuai dengan frontend URL
- Headers yang diperlukan untuk authentication

### 3. Auth Endpoints

#### Login
```
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}

Response:
{
  "success": true,
  "data": {
    "user": {
      "id": 1,
      "email": "user@example.com",
      "username": "user",
      "full_name": "User Name",
      "role": "driver"
    }
  }
}

Set-Cookie: token=<jwt_token>; HttpOnly; Secure; SameSite=Lax; Max-Age=86400; Path=/
```

#### Register
```
POST /api/v1/auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123",
  "username": "username",
  "full_name": "Full Name",
  "phone": "08123456789"
}
```

#### Logout
```
POST /api/v1/auth/logout
Cookie: token=<jwt_token>

Response:
{
  "success": true,
  "message": "Logged out successfully"
}
```

#### Get Current User
```
GET /api/v1/auth/me
Cookie: token=<jwt_token>

Response:
{
  "success": true,
  "data": {
    "id": 1,
    "username": "user"
  }
}
```

### 4. Protected Endpoints
Semua endpoint yang memerlukan autentikasi akan membaca token dari:
1. Cookie `token` (prioritas utama)
2. Header `Authorization: Bearer <token>` (fallback untuk API clients)

## Frontend Configuration

### 1. Environment Variables (.env)
```env
VITE_API_URL=http://localhost:8080/api/v1
```

### 2. API Client
Semua request ke backend menggunakan `credentials: 'include'` untuk mengirim cookies:

```typescript
fetch(url, {
  credentials: 'include',
  // ... other options
})
```

### 3. Auth Store
Store Svelte untuk mengelola state autentikasi:
- `authStore.setUser(user)` - Set user setelah login
- `authStore.logout()` - Logout dan redirect ke login
- `authStore.checkAuth()` - Cek status autentikasi

### 4. Protected Routes
Layout `(private)` otomatis mengecek autentikasi:
- Jika tidak ada user, redirect ke `/login`
- Jika ada user, tampilkan konten

## Security Features

### 1. HttpOnly Cookies
- Token disimpan dalam httpOnly cookie
- Tidak dapat diakses via JavaScript
- Mencegah XSS attacks

### 2. Secure Flag
- Cookie hanya dikirim via HTTPS (production)
- Set `Secure: true` di production

### 3. SameSite
- `SameSite: Lax` mencegah CSRF attacks
- Cookie hanya dikirim untuk same-site requests

### 4. CORS
- Strict CORS policy
- Hanya origin yang diizinkan dapat mengakses API

## Development Setup

### Backend
```bash
cd backend

# Install dependencies
go mod download

# Setup database
go run cmd/migrate/main.go

# Seed data (optional)
go run cmd/seed/main.go

# Run server
go run main.go
# atau dengan hot reload
air
```

### Frontend
```bash
cd frontend

# Install dependencies
npm install

# Run dev server
npm run dev
```

## Testing Authentication

### 1. Register User
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123",
    "username": "testuser",
    "full_name": "Test User",
    "phone": "08123456789"
  }'
```

### 2. Login
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -c cookies.txt \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }'
```

### 3. Access Protected Endpoint
```bash
curl -X GET http://localhost:8080/api/v1/auth/me \
  -b cookies.txt
```

### 4. Logout
```bash
curl -X POST http://localhost:8080/api/v1/auth/logout \
  -b cookies.txt
```

## Production Checklist

- [ ] Ganti `JWT_SECRET` dengan secret key yang kuat
- [ ] Set `Secure: true` untuk cookies (HTTPS only)
- [ ] Update `ALLOWED_ORIGINS` dengan domain production
- [ ] Enable HTTPS/TLS
- [ ] Set proper `SameSite` policy
- [ ] Implement rate limiting
- [ ] Add refresh token mechanism
- [ ] Setup proper logging
- [ ] Configure database connection pooling
- [ ] Add monitoring and alerting

## Troubleshooting

### Cookie tidak terkirim
- Pastikan `credentials: 'include'` di frontend
- Pastikan `AllowCredentials: true` di backend CORS
- Cek domain dan path cookie

### CORS Error
- Pastikan `ALLOWED_ORIGINS` sesuai dengan frontend URL
- Jangan gunakan wildcard `*` dengan credentials
- Cek browser console untuk detail error

### Token expired
- Token expired setelah 24 jam (default)
- User harus login ulang
- Implementasi refresh token untuk UX lebih baik

### 401 Unauthorized
- Cek apakah cookie terkirim (Network tab)
- Cek JWT secret sama di backend
- Cek token belum expired

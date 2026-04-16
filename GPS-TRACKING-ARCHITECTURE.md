# GPS Tracking System Architecture

## 🏗️ System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         MOBILE APP                               │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌────────────────────────────────────────────────────────┐    │
│  │              Presentation Layer                         │    │
│  │  ┌──────────────────────────────────────────────────┐  │    │
│  │  │         TrackingScreen.kt                        │  │    │
│  │  │  - Start/Stop Button                             │  │    │
│  │  │  - Display Speed, Distance, Location             │  │    │
│  │  │  - Permission Handling                            │  │    │
│  │  └──────────────────────────────────────────────────┘  │    │
│  └────────────────────────────────────────────────────────┘    │
│                           ↓ bind                                │
│  ┌────────────────────────────────────────────────────────┐    │
│  │              Service Layer                              │    │
│  │  ┌──────────────────────────────────────────────────┐  │    │
│  │  │    LocationTrackingService.kt                    │  │    │
│  │  │  - FusedLocationProvider (GPS)                   │  │    │
│  │  │  - Update every 5 seconds                        │  │    │
│  │  │  - Send to backend every 30 seconds              │  │    │
│  │  │  - Calculate distance                            │  │    │
│  │  │  - Foreground notification                       │  │    │
│  │  └──────────────────────────────────────────────────┘  │    │
│  └────────────────────────────────────────────────────────┘    │
│                           ↓ uses                                │
│  ┌────────────────────────────────────────────────────────┐    │
│  │              Domain Layer                               │    │
│  │  ┌──────────────────────────────────────────────────┐  │    │
│  │  │      SendLocationUseCase.kt                      │  │    │
│  │  │  - Business logic for sending location           │  │    │
│  │  └──────────────────────────────────────────────────┘  │    │
│  └────────────────────────────────────────────────────────┘    │
│                           ↓ calls                               │
│  ┌────────────────────────────────────────────────────────┐    │
│  │              Data Layer                                 │    │
│  │  ┌──────────────────────────────────────────────────┐  │    │
│  │  │    LocationRepositoryImpl.kt                     │  │    │
│  │  │  - Convert timestamp (UTC & Local)               │  │    │
│  │  │  - Convert speed (m/s → km/h)                    │  │    │
│  │  │  - Handle nullable fields                        │  │    │
│  │  └──────────────────────────────────────────────────┘  │    │
│  │                       ↓ uses                            │    │
│  │  ┌──────────────────────────────────────────────────┐  │    │
│  │  │         ApiService.kt (Retrofit)                 │  │    │
│  │  │  - POST /locations/track                         │  │    │
│  │  │  - TrackLocationRequest DTO                      │  │    │
│  │  └──────────────────────────────────────────────────┘  │    │
│  └────────────────────────────────────────────────────────┘    │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
                              ↓ HTTP POST
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                         BACKEND (Go)                             │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌────────────────────────────────────────────────────────┐    │
│  │              HTTP Handler                               │    │
│  │  ┌──────────────────────────────────────────────────┐  │    │
│  │  │      location_handler.go                         │  │    │
│  │  │  - TrackLocation(c *fiber.Ctx)                   │  │    │
│  │  │  - Parse request body                            │  │    │
│  │  │  - Validate input                                │  │    │
│  │  └──────────────────────────────────────────────────┘  │    │
│  └────────────────────────────────────────────────────────┘    │
│                           ↓ calls                               │
│  ┌────────────────────────────────────────────────────────┐    │
│  │              Use Case                                   │    │
│  │  ┌──────────────────────────────────────────────────┐  │    │
│  │  │      location_usecase.go                         │  │    │
│  │  │  - TrackLocation(req TrackLocationRequest)       │  │    │
│  │  │  - Parse timestamps                              │  │    │
│  │  │  - Create Location entity                        │  │    │
│  │  └──────────────────────────────────────────────────┘  │    │
│  └────────────────────────────────────────────────────────┘    │
│                           ↓ calls                               │
│  ┌────────────────────────────────────────────────────────┐    │
│  │              Repository                                 │    │
│  │  ┌──────────────────────────────────────────────────┐  │    │
│  │  │   location_repository_impl.go                    │  │    │
│  │  │  - Create(location *entity.Location)             │  │    │
│  │  │  - GORM database operations                      │  │    │
│  │  └──────────────────────────────────────────────────┘  │    │
│  └────────────────────────────────────────────────────────┘    │
│                           ↓ saves to                            │
│  ┌────────────────────────────────────────────────────────┐    │
│  │              Database                                   │    │
│  │  ┌──────────────────────────────────────────────────┐  │    │
│  │  │         PostgreSQL                               │  │    │
│  │  │  Table: locations                                │  │    │
│  │  │  - id, user_id, latitude, longitude              │  │    │
│  │  │  - speed, accuracy, heading, altitude            │  │    │
│  │  │  - recorded_at_utc, timezone                     │  │    │
│  │  │  - recorded_at_local, created_at                 │  │    │
│  │  └──────────────────────────────────────────────────┘  │    │
│  └────────────────────────────────────────────────────────┘    │
│                           ↓ broadcasts                          │
│  ┌────────────────────────────────────────────────────────┐    │
│  │              WebSocket                                  │    │
│  │  ┌──────────────────────────────────────────────────┐  │    │
│  │  │      websocket_handler.go                        │  │    │
│  │  │  - BroadcastLocationUpdate()                     │  │    │
│  │  │  - Real-time updates to dashboard                │  │    │
│  │  └──────────────────────────────────────────────────┘  │    │
│  └────────────────────────────────────────────────────────┘    │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
                              ↓ WebSocket
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                      FRONTEND DASHBOARD                          │
├─────────────────────────────────────────────────────────────────┤
│  - Real-time location updates                                   │
│  - Map view with driver positions                               │
│  - Speed and distance monitoring                                │
└─────────────────────────────────────────────────────────────────┘
```

## 📊 Data Flow Diagram

```
┌──────────────┐
│   GPS        │
│  Satellite   │
└──────┬───────┘
       │ Location Data
       ↓
┌──────────────────────────────────────────────────────────┐
│  FusedLocationProvider (Google Play Services)            │
│  - latitude, longitude, speed, accuracy                  │
│  - bearing, altitude, timestamp                          │
└──────────────────────┬───────────────────────────────────┘
                       │ Every 5 seconds
                       ↓
┌──────────────────────────────────────────────────────────┐
│  LocationTrackingService                                 │
│  ┌────────────────────────────────────────────────────┐ │
│  │  1. Receive Location                               │ │
│  │  2. Calculate Distance                             │ │
│  │  3. Update Notification                            │ │
│  │  4. Check Throttle (10 seconds)                    │ │
│  │  5. Send to Backend                                │ │
│  └────────────────────────────────────────────────────┘ │
└──────────────────────┬───────────────────────────────────┘
                       │ Every 30 seconds
                       ↓
┌──────────────────────────────────────────────────────────┐
│  LocationRepositoryImpl                                  │
│  ┌────────────────────────────────────────────────────┐ │
│  │  Data Transformation:                              │ │
│  │  ┌──────────────────────────────────────────────┐ │ │
│  │  │ timestamp → UTC (RFC3339)                    │ │ │
│  │  │ timestamp → Local (RFC3339 + Timezone)       │ │ │
│  │  │ speed (m/s) → speed (km/h)                   │ │ │
│  │  │ bearing → heading (nullable)                 │ │ │
│  │  │ altitude → altitude (nullable)               │ │ │
│  │  └──────────────────────────────────────────────┘ │ │
│  └────────────────────────────────────────────────────┘ │
└──────────────────────┬───────────────────────────────────┘
                       │ HTTP POST
                       ↓
┌──────────────────────────────────────────────────────────┐
│  Backend API: POST /locations/track                      │
│  ┌────────────────────────────────────────────────────┐ │
│  │  Request Body (JSON):                              │ │
│  │  {                                                 │ │
│  │    "user_id": 1,                                   │ │
│  │    "latitude": -6.200000,                          │ │
│  │    "longitude": 106.816666,                        │ │
│  │    "speed": 45.5,                                  │ │
│  │    "accuracy": 12.5,                               │ │
│  │    "heading": 180.0,                               │ │
│  │    "altitude": 25.5,                               │ │
│  │    "recorded_at_utc": "2024-01-15T10:30:45Z",     │ │
│  │    "timezone": "Asia/Jakarta",                     │ │
│  │    "recorded_at_local": "2024-01-15T17:30:45+07"  │ │
│  │  }                                                 │ │
│  └────────────────────────────────────────────────────┘ │
└──────────────────────┬───────────────────────────────────┘
                       │
                       ↓
┌──────────────────────────────────────────────────────────┐
│  PostgreSQL Database                                     │
│  ┌────────────────────────────────────────────────────┐ │
│  │  INSERT INTO locations (...)                       │ │
│  │  VALUES (...)                                      │ │
│  └────────────────────────────────────────────────────┘ │
└──────────────────────┬───────────────────────────────────┘
                       │
                       ↓
┌──────────────────────────────────────────────────────────┐
│  WebSocket Broadcast                                     │
│  - Notify all connected dashboard clients                │
│  - Real-time location update                             │
└──────────────────────────────────────────────────────────┘
```

## 🔄 Sequence Diagram

```
User          TrackingScreen    LocationService    Repository    Backend    Database
 │                 │                  │                │            │           │
 │  Tap Start      │                  │                │            │           │
 │────────────────>│                  │                │            │           │
 │                 │  startTracking() │                │            │           │
 │                 │─────────────────>│                │            │           │
 │                 │                  │  Request GPS   │            │           │
 │                 │                  │───────────────>│            │           │
 │                 │                  │                │            │           │
 │                 │                  │  GPS Update    │            │           │
 │                 │                  │<───────────────│            │           │
 │                 │                  │                │            │           │
 │                 │  Location Update │                │            │           │
 │                 │<─────────────────│                │            │           │
 │                 │                  │                │            │           │
 │  Display Speed  │                  │  (10s passed)  │            │           │
 │<────────────────│                  │                │            │           │
 │                 │                  │  sendLocation()│            │           │
 │                 │                  │───────────────>│            │           │
 │                 │                  │                │ POST /track│           │
 │                 │                  │                │───────────>│           │
 │                 │                  │                │            │  INSERT   │
 │                 │                  │                │            │──────────>│
 │                 │                  │                │            │  Success  │
 │                 │                  │                │            │<──────────│
 │                 │                  │                │  201 OK    │           │
 │                 │                  │                │<───────────│           │
 │                 │                  │  Success       │            │           │
 │                 │                  │<───────────────│            │           │
 │                 │                  │                │            │           │
 │                 │                  │  (Next GPS)    │            │           │
 │                 │                  │<───────────────│            │           │
 │                 │                  │  ...           │            │           │
 │                 │                  │                │            │           │
 │  Tap Stop       │                  │                │            │           │
 │────────────────>│                  │                │            │           │
 │                 │  stopTracking()  │                │            │           │
 │                 │─────────────────>│                │            │           │
 │                 │                  │  Stop GPS      │            │           │
 │                 │                  │───────────────>│            │           │
 │                 │                  │                │            │           │
```

## 🎯 Component Responsibilities

### Mobile App

#### LocationTrackingService
- ✅ Manage GPS updates (FusedLocationProvider)
- ✅ Calculate distance traveled
- ✅ Throttle backend requests (10s interval)
- ✅ Show foreground notification
- ✅ Handle service lifecycle
- ✅ Persist state across restarts

#### SendLocationUseCase
- ✅ Business logic for sending location
- ✅ Coordinate between service and repository

#### LocationRepositoryImpl
- ✅ Convert Android Location to API format
- ✅ Transform timestamps (UTC & Local)
- ✅ Convert speed units (m/s → km/h)
- ✅ Handle nullable fields
- ✅ Make HTTP request to backend

#### TrackingScreen
- ✅ UI for start/stop tracking
- ✅ Display real-time stats
- ✅ Handle permissions
- ✅ Bind to service

### Backend

#### LocationHandler
- ✅ Receive HTTP POST request
- ✅ Parse and validate request body
- ✅ Return appropriate HTTP responses

#### LocationUseCase
- ✅ Parse timestamp strings
- ✅ Create Location entity
- ✅ Coordinate with repository

#### LocationRepository
- ✅ Database operations (GORM)
- ✅ Save location to PostgreSQL
- ✅ Query location history

#### WebSocketHandler
- ✅ Broadcast location updates
- ✅ Real-time communication with dashboard

## 📈 Performance Characteristics

### Mobile App
- **GPS Update Frequency**: 5 seconds
- **Backend Send Frequency**: 30 seconds
- **Battery Impact**: Low (optimized with throttling)
- **Network Usage**: ~1 request per 30 seconds (~120 requests/hour)
- **Data Size**: ~200 bytes per request

### Backend
- **Response Time**: < 100ms (typical)
- **Database Write**: < 50ms (typical)
- **WebSocket Broadcast**: < 10ms (typical)
- **Concurrent Users**: Scalable (stateless API)

## 🔐 Security

### Mobile
- ✅ HTTPS only
- ✅ JWT token authentication
- ✅ Encrypted local storage (DataStore)
- ✅ Permission-based access

### Backend
- ✅ JWT validation
- ✅ Input validation
- ✅ SQL injection prevention (GORM)
- ✅ CORS configuration
- ✅ Rate limiting (optional)

## 🎉 Summary

Sistem GPS tracking ini dirancang dengan:
- **Clean Architecture**: Separation of concerns
- **Scalability**: Stateless API, efficient database
- **Reliability**: Auto-restart, error handling
- **Performance**: Throttling, optimized queries
- **Real-time**: WebSocket for live updates
- **Battery Efficient**: Adaptive intervals

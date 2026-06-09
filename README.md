# 🚂 RailConnect — Advanced Train Booking System

> Enterprise-grade Spring Boot + PostgreSQL train booking platform inspired by IRCTC, with advanced features including real-time GPS tracking, Tatkal/Premium Tatkal, waitlist management, and multi-payment support.

---

## ✨ Features

| Feature | Details |
|---|---|
| 🔐 Authentication | JWT + Google OAuth2, BCrypt passwords, account lockout |
| 🔍 Train Search | Multi-station search, flexible dates, class filtering |
| 🎫 Booking | General / Tatkal / Premium Tatkal / Ladies / Senior Citizen quotas |
| 💺 Seat Management | Optimistic locking (`@Version`), pessimistic lock on booking, RAC, Waitlist auto-promotion |
| 💰 Payment | Razorpay integration — GPay UPI intent, PhonePe UPI intent, Credit/Debit Cards |
| 🗺️ Live Tracking | WebSocket (STOMP) real-time GPS, Google Maps embed, 30-second broadcast |
| ❌ Cancellation | Partial / full cancellation, automatic refund via Razorpay, cancellation charge matrix |
| 📋 PNR Status | Real-time PNR check with passenger seat/status details |
| 📧 Notifications | HTML email: booking confirmation, cancellation, waitlist upgrade |
| 🛡️ Security | Rate limiting (Bucket4j), CORS, HTTPS-ready, audit log table |
| 📊 Admin Panel | Dashboard stats, train/user management, manual seat seeding |
| 📖 API Docs | Swagger UI at `/swagger-ui.html`, OpenAPI 3.0 |

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    RailConnect Backend                   │
│                                                         │
│  Spring Boot 3.2  │  Spring Security  │  Spring Data JPA│
│  JWT + OAuth2     │  WebSocket STOMP  │  Flyway          │
│  Redis Cache      │  Razorpay SDK     │  Bucket4j        │
└─────────────────────────────────────────────────────────┘
         │                    │                  │
    PostgreSQL 16         Redis 7          Razorpay API
    (Primary DB)          (Cache)          (GPay/PhonePe/Cards)
```

---

## 🚀 Quick Start

### Prerequisites
- Java 21+
- Docker & Docker Compose
- PostgreSQL 16 (or use Docker)
- Redis 7 (or use Docker)

### 1. Clone & Configure
```bash
git clone https://github.com/YOUR_USERNAME/railconnect.git
cd railconnect
cp .env.example .env
# Edit .env with your API keys
```

### 2. Run with Docker Compose (Recommended)
```bash
docker-compose up -d
```
App starts at **http://localhost:8080**

### 3. Run Locally
```bash
# Start dependencies
docker-compose up -d postgres redis

# Run the app
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

---

## 🔑 API Keys Required

| Key | Where to Get |
|---|---|
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | [Google Cloud Console](https://console.cloud.google.com) → OAuth 2.0 |
| `RAZORPAY_KEY_ID` / `RAZORPAY_KEY_SECRET` | [Razorpay Dashboard](https://dashboard.razorpay.com) |
| `GOOGLE_MAPS_API_KEY` | [Google Cloud Console](https://console.cloud.google.com) → Maps JavaScript API |
| `JWT_SECRET` | Generate: `openssl rand -base64 32` |

---

## 📡 Key API Endpoints

### Authentication
```
POST /api/v1/auth/register      - Register new user
POST /api/v1/auth/login         - Login → JWT tokens
POST /api/v1/auth/refresh       - Refresh access token
GET  /oauth2/authorization/google - Google OAuth2 login
```

### Trains
```
GET  /api/v1/trains/search                         - Search trains
GET  /api/v1/trains/{id}/availability?date=&class= - Seat availability
GET  /api/v1/trains/{number}/live-status           - Live GPS status
GET  /api/v1/trains/stations/search?query=         - Station autocomplete
```

### Bookings
```
POST /api/v1/bookings           - Create booking
GET  /api/v1/bookings/pnr/{pnr} - PNR status check
GET  /api/v1/bookings/my-bookings - User's bookings
POST /api/v1/bookings/cancel    - Cancel booking
```

### Payments
```
POST /api/v1/payments/initiate  - Create Razorpay order (returns UPI intent URLs)
POST /api/v1/payments/verify    - Verify payment signature
POST /api/v1/payments/webhook   - Razorpay webhook
```

### WebSocket (Live Tracking)
```
WS   /ws                              - STOMP endpoint (SockJS)
SUB  /topic/train/{trainNumber}       - Subscribe for live GPS updates
PUB  /app/track/{trainNumber}/request - Request immediate update
```

---

## 💳 Payment Flow (GPay / PhonePe / Cards)

```
1. POST /api/v1/payments/initiate
   → Returns { razorpayOrderId, upiIntentUrl }
   
2a. [GPay / PhonePe] Redirect user to upiIntentUrl  
    gpay://upi/pay?pa=railconnect@razorpay&...
    phonepe://pay?pa=railconnect@razorpay&...

2b. [Cards] Use Razorpay JS SDK with razorpayOrderId

3. POST /api/v1/payments/verify
   { razorpayOrderId, razorpayPaymentId, razorpaySignature }
   → Signature verified server-side (HMAC-SHA256)
   → Booking confirmed + email sent
```

---

## 🗺️ Live Train Tracking Flow

```
Frontend                    Backend                  WebSocket
   │                           │                        │
   │── GET /live-status ───────►│                        │
   │◄─ Initial position ────────│                        │
   │                           │                        │
   │── WS Connect /ws ─────────────────────────────────►│
   │── Subscribe /topic/train/12301 ───────────────────►│
   │                           │                        │
   │                    Scheduler (30s)                  │
   │                           │── Broadcast GPS ───────►│
   │◄────────────── Live update (lat/lng/speed/delay) ───│
```

---

## 🎫 Tatkal Booking Rules (Implemented)

| Quota | Opens | Charges |
|---|---|---|
| General | 120 days before | Base fare |
| Tatkal | 10:00 AM, 1 day before | Base + tatkal charge |
| Premium Tatkal | 11:00 AM, 1 day before | Base + 1.3× tatkal charge |

---

## 🗃️ Database Schema Highlights

- **Optimistic locking** (`version` column on `seats`) prevents double-booking
- **Pessimistic write lock** on `findAvailableSeatsWithLock` for the critical booking path
- **Automatic waitlist promotion** when a confirmed booking is cancelled
- **Flyway migrations** for schema versioning (V1 = schema, V2 = seed data)
- **Audit log** table for all user actions
- **pg_trgm** extension for fuzzy station name search

---

## 🧪 Testing

```bash
./mvnw test
```

---

## 📁 Project Structure

```
railconnect/
├── src/main/java/com/railconnect/
│   ├── config/          # Security, Redis, WebSocket, Razorpay, OpenAPI
│   ├── controller/      # Auth, Train, Booking, Payment, Admin, User
│   ├── dto/             # Request/Response DTOs
│   ├── entity/          # JPA entities
│   ├── enums/           # BookingStatus, SeatClass, QuotaType, etc.
│   ├── exception/       # Global exception handler
│   ├── repository/      # Spring Data JPA repositories
│   ├── security/        # JWT, OAuth2, UserDetails
│   ├── service/         # Business logic
│   ├── util/            # FareCalculator, PnrGenerator
│   └── websocket/       # STOMP controllers
├── src/main/resources/
│   ├── application.yml
│   └── db/migration/    # Flyway V1 schema + V2 seed data
├── docker-compose.yml
├── Dockerfile
└── .env.example
```

---

## 🛡️ Security Features

- JWT with 24h expiry + 7-day refresh tokens
- Account lockout after 5 failed login attempts (30-min lock)
- Rate limiting: 100 requests/minute per IP (Bucket4j)
- HMAC-SHA256 Razorpay signature verification
- `@PreAuthorize` on all admin endpoints
- `@Version` optimistic locking on seats
- CORS configured (update allowed origins for production)

---

*Built with ❤️ | RailConnect © 2026*

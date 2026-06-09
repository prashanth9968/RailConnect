# 🚂 RailConnect — Advanced Train Booking System (Microservices)

> Enterprise-grade train booking platform inspired by IRCTC, built on a highly scalable Spring Boot Microservices architecture. Features real-time GPS tracking, Tatkal/Premium Tatkal booking, asynchronous messaging via Kafka, waitlist management, and multi-payment support.

---

## ✨ Features

| Feature | Details |
|---|---|
| 🔐 Authentication | JWT + Google OAuth2, BCrypt passwords, account lockout via `auth-service` |
| 🔍 Train Search | Multi-station search, flexible dates, class filtering via `inventory-service` |
| 🎫 Booking | General / Tatkal / Premium Tatkal quotas with concurrency control in `booking-service` |
| 💺 Seat Management | Optimistic locking (`@Version`) + pessimistic locks, RAC, and Waitlist auto-promotion |
| 💰 Payment | Razorpay integration — UPI intent (GPay/PhonePe) & cards via `payment-service` |
| 🗺️ Live Tracking | WebSocket (STOMP) real-time GPS, Google Maps embed, 30s broadcast via `tracking-service` |
| 📧 Notifications | Asynchronous HTML email confirmations & waitlist updates via Kafka + `notification-service` |
| 🛡️ Security | API Gateway routing, CORS config, rate limiting (Bucket4j), audit logging |
| 📊 Front-End UI | Modern React dashboard running at `http://localhost:3000` |

---

## 🏗️ Architecture & Modules

```
                        ┌────────────────────────┐
                        │     React Frontend     │
                        │ (localhost:3000 / Vite)│
                        └───────────┬────────────┘
                                    │
                        ┌───────────▼────────────┐
                        │      API Gateway       │ (Port 8080)
                        └───────────┬────────────┘
                                    │ (Routing & Rate Limiting)
       ┌────────────────────────────┼────────────────────────────┐
┌──────▼──────┐              ┌──────▼──────┐              ┌──────▼──────┐
│auth-service │ (Port 8081)  │booking-serv.│ (Port 8082)  │invent.-serv.│ (Port 8083)
└──────┬──────┘              └──────┬──────┘              └──────┬──────┘
       │                            │                            │
       └──────────────┬─────────────┴─────────────┬──────────────┘
                      │                           │
              ┌──────▼──────┐             ┌──────▼──────┐
              │payment-serv.│ (Port 8084) │tracking-ser.│ (Port 8085)
              └──────┬──────┘             └──────┬──────┘
                     │ (Kafka Events)            │
              ┌──────▼──────┐                    │
              │notific.-ser.│ (Port 8086)        │
              └─────────────┘                    │
                                                 │
 ┌───────────────────────────────────────────────┼───────────────────────────────┐
 │                                               │                               │
┌▼──────────┐                               ┌────▼─────┐                    ┌────▼─────┐
│ PostgreSQL│ (Port 5433)                   │  Redis   │ (Port 6379)        │  Kafka   │ (Port 29092)
└───────────┘                               └──────────┘                    └──────────┘
```

This project is organized as a Maven Multi-Module Reactor project:
- **`railconnect-common`**: Shared models, utilities, DTOs, and global configurations.
- **`api-gateway`**: Routes HTTP/WebSocket traffic and enforces JWT validation & security checks.
- **`auth-service`**: Manages credentials, tokens, registration, and locking.
- **`booking-service`**: Drives the core ticketing, quota management, and waitlists.
- **`inventory-service`**: Seeds and displays seat availability and train searches.
- **`payment-service`**: Integrates payments via Razorpay API and fires success hooks.
- **`tracking-service`**: Handles WS STOMP updates and coordinates GPS maps tracking.
- **`notification-service`**: Listens to Kafka messages to send HTML booking emails.
- **`frontend`**: Modern React single-page application.

---

## 🚀 Quick Start (Running Locally)

### Prerequisites
- Java 21+ (OpenJDK 25.0.2 recommended)
- Maven 3.9+
- Node.js (for React frontend)
- Docker Desktop (for Postgres, Redis, Zookeeper, and Kafka)

### 1. Configure Environment Variables
Copy the `.env.example` file to `.env` in the root directory and update it with your credentials:
```bash
cp .env.example .env
```

### 2. Launch Infrastructure
Start PostgreSQL, Redis, Kafka, and Zookeeper using Docker Compose:
```bash
docker-compose up -d
```

### 3. Initialize Database
Initialize the database tables and seed them with trains, stations, and seat quotas:
```cmd
# Runs Flyway V1 Schema + V2 Seeds
.\setup_db.cmd
```

### 4. Build and Package Services
Compile the multi-module Maven reactor project:
```powershell
.\build.ps1
```

### 5. Launch Services & Frontend
Start all the microservices in the correct order:
```powershell
.\start_services.ps1
```
In a separate terminal, launch the React development frontend:
```powershell
.\start_frontend.ps1
```
Once everything is up, open **[http://localhost:3000](http://localhost:3000)** in your browser!

---

## 📡 Ports Directory

- `3000` — Frontend (React/Vite)
- `8080` — API Gateway
- `8081` — Auth Service
- `8082` — Booking Service
- `8083` — Inventory Service
- `8084` — Payment Service
- `8085` — Tracking Service
- `8086` — Notification Service
- `5433` — PostgreSQL DB
- `6379` — Redis Cache
- `29092` — Kafka Broker

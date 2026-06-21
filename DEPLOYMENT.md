# 🚀 RailConnect — Railway.app Deployment Guide

Deploy RailConnect with a live demo URL. No Kafka needed.

---

## Prerequisites

- [Railway.app](https://railway.app) account (sign up with GitHub)
- Your RailConnect repo pushed to GitHub

---

## Step 1: Create Railway Project

1. Go to [railway.app/new](https://railway.app/new)
2. Click **"Deploy from GitHub Repo"**
3. Select your `railconnect` repository
4. Railway creates a project — don't deploy anything yet, click **"Add a Service"** instead

---

## Step 2: Add PostgreSQL & Redis

1. In your Railway project, click **"+ New"** → **"Database"** → **"PostgreSQL"**
2. Click **"+ New"** → **"Database"** → **"Redis"**

Railway auto-provisions these with connection variables you can reference.

---

## Step 3: Create Backend Services

For **each** of these 7 services, repeat:

| Service | Dockerfile Path | Port |
|---|---|---|
| api-gateway | `api-gateway/Dockerfile` | 8080 |
| auth-service | `auth-service/Dockerfile` | 8081 |
| booking-service | `booking-service/Dockerfile` | 8082 |
| inventory-service | `inventory-service/Dockerfile` | 8083 |
| payment-service | `payment-service/Dockerfile` | 8084 |
| tracking-service | `tracking-service/Dockerfile` | 8085 |
| notification-service | `notification-service/Dockerfile` | 8086 |

**Steps for each:**

1. Click **"+ New"** → **"GitHub Repo"** → select your repo
2. Go to **Settings** → **Build** section:
   - Set **Dockerfile Path** to `<service-name>/Dockerfile`
   - Set **Docker Build Context** to `.` (root — needed because Dockerfiles copy from root)
3. Go to **Settings** → **Networking**:
   - Set **Port** to the port listed above

---

## Step 4: Create Frontend Service

1. Click **"+ New"** → **"GitHub Repo"** → select your repo
2. Go to **Settings** → **Build**:
   - Set **Dockerfile Path** to `frontend/Dockerfile`
   - Set **Docker Build Context** to `frontend`
3. Go to **Settings** → **Networking**:
   - Set **Port** to `80`

---

## Step 5: Configure Environment Variables

### Shared Variables (set on ALL 7 backend services)

Use Railway's **Shared Variables** feature or set on each service individually.

Click on each service → **Variables** tab → **"+ New Variable"**:

```
DB_HOST=${{Postgres.PGHOST}}
DB_PORT=${{Postgres.PGPORT}}
DB_NAME=${{Postgres.PGDATABASE}}
DB_USER=${{Postgres.PGUSER}}
DB_PASS=${{Postgres.PGPASSWORD}}
REDIS_HOST=${{Redis.REDISHOST}}
REDIS_PORT=${{Redis.REDISPORT}}
SPRING_MAIN_ALLOW_BEAN_DEFINITION_OVERRIDING=true
```

> **Note:** The `${{Postgres.PGHOST}}` syntax is Railway's reference variable — it auto-resolves to the actual value.

### Disable Kafka (set on ALL 7 backend services)

```
SPRING_KAFKA_BOOTSTRAP_SERVERS=dummy:9092
APP_KAFKA_ENABLED=false
```

This tells Spring Kafka not to connect to any real broker, and disables the `KafkaConfig` and `NotificationMessageListener` beans.

### Shared Secret (set on ALL 7 backend services)

```
JWT_SECRET=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
```

### API Gateway Only (additional variables)

```
AUTH_SERVICE_URI=http://auth-service.railway.internal:8081
BOOKING_SERVICE_URI=http://booking-service.railway.internal:8082
INVENTORY_SERVICE_URI=http://inventory-service.railway.internal:8083
PAYMENT_SERVICE_URI=http://payment-service.railway.internal:8084
TRACKING_SERVICE_URI=http://tracking-service.railway.internal:8085
TRACKING_WEBSOCKET_URI=ws://tracking-service.railway.internal:8085
NOTIFICATION_SERVICE_URI=http://notification-service.railway.internal:8086
```

### Booking Service Only (additional)

```
INVENTORY_SERVICE_URI=http://inventory-service.railway.internal:8083
```

### Payment Service Only (additional)

```
RAZORPAY_KEY_ID=rzp_test_xxxxxxxxxxxx
RAZORPAY_KEY_SECRET=your_razorpay_key_secret
```

### Auth Service Only (additional)

```
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret
OAUTH2_REDIRECT_URI=https://<your-frontend>.railway.app/oauth2/callback
```

### Tracking Service Only (additional)

```
GOOGLE_MAPS_API_KEY=your_google_maps_api_key
```

### Notification Service Only (additional)

```
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USER=your-email@gmail.com
MAIL_PASS=your-app-password
```

---

## Step 6: Expose Public URLs

Only **two** services need public domains:

### API Gateway
1. Click on `api-gateway` → **Settings** → **Networking**
2. Click **"Generate Domain"** — you'll get something like `api-gateway-production-xxxx.railway.app`

### Frontend
1. Click on `frontend` → **Settings** → **Networking**
2. Click **"Generate Domain"** — you'll get something like `frontend-production-xxxx.railway.app`

All other services communicate over Railway's **private network** (e.g., `auth-service.railway.internal:8081`).

---

## Step 7: Deploy

Railway auto-deploys when you push to `main`. To trigger a manual deploy:

1. Click on any service → **Deployments** tab → **"Deploy"**
2. Watch the build logs — each Java service takes ~3-5 minutes to build

### Recommended deploy order:
1. PostgreSQL & Redis (already running)
2. auth-service, inventory-service (no dependencies on other services)
3. booking-service, payment-service, tracking-service, notification-service
4. api-gateway (depends on all services being up)
5. frontend (depends on api-gateway)

---

## Step 8: Verify

1. Open `https://<your-frontend>.railway.app` — you should see the RailConnect UI
2. Try registering a new account
3. Search for trains
4. Check the API directly: `https://<your-api-gateway>.railway.app/api/v1/trains`

---

## Database Seeding

Your Flyway migrations (`V1__init_schema.sql`, etc.) in `railconnect-common/src/main/resources/db/migration/` run **automatically** when the first service starts. The database will be created and seeded with train data.

---

## Troubleshooting

### Service won't start
- Check **Deployments** → click the failed deploy → view **Build Logs** and **Deploy Logs**
- Common issue: missing environment variables

### "Connection refused" errors
- Ensure the service name in Railway matches what's used in the URI (e.g., `auth-service`)
- Railway private networking uses the **service name** as the hostname

### Kafka-related log warnings
- You'll see warnings like `Failed to publish booking created event to Kafka` — **this is expected and harmless**
- The booking/payment flows complete successfully; only async notifications are skipped

---

## Resume Line

Once deployed, add this to your resume:

> **RailConnect** — [railconnect.railway.app](https://railconnect.railway.app)  
> Enterprise-grade train booking system built with Spring Boot Microservices, React, PostgreSQL, Redis, and Kafka.  
> Features JWT auth, Razorpay payments, real-time GPS tracking, waitlist auto-promotion, and multi-quota booking.

---

## Phase 2 (Later — Optional)

When you're ready to add Kafka back:
1. Add Zookeeper and Kafka as Docker image services on Railway
2. Remove `APP_KAFKA_ENABLED=false` and set `SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka.railway.internal:9092`
3. Set `APP_KAFKA_ENABLED=true` on notification-service to enable email listeners

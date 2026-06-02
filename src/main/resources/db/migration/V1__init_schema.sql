-- ============================================================
-- RailConnect Database Schema - V1 Initial Migration
-- ============================================================

-- Extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm"; -- For fuzzy station search

-- ── USERS ────────────────────────────────────────────────────
CREATE TABLE users (
    id                    UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email                 VARCHAR(255) UNIQUE NOT NULL,
    password              VARCHAR(255),
    first_name            VARCHAR(100) NOT NULL,
    last_name             VARCHAR(100),
    phone                 VARCHAR(15) UNIQUE,
    date_of_birth         DATE,
    gender                VARCHAR(10),
    profile_picture       TEXT,
    role                  VARCHAR(30) NOT NULL DEFAULT 'ROLE_USER',
    provider              VARCHAR(20) NOT NULL DEFAULT 'LOCAL',
    provider_id           VARCHAR(255),
    email_verified        BOOLEAN NOT NULL DEFAULT FALSE,
    phone_verified        BOOLEAN NOT NULL DEFAULT FALSE,
    enabled               BOOLEAN NOT NULL DEFAULT TRUE,
    account_non_locked    BOOLEAN NOT NULL DEFAULT TRUE,
    failed_login_attempts INT NOT NULL DEFAULT 0,
    lock_time             TIMESTAMP,
    aadhaar_number        VARCHAR(20),
    aadhaar_verified      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at            TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_user_email  ON users(email);
CREATE INDEX idx_user_phone  ON users(phone);
CREATE INDEX idx_user_provider ON users(provider, provider_id);

-- ── STATIONS ─────────────────────────────────────────────────
CREATE TABLE stations (
    id            BIGSERIAL PRIMARY KEY,
    station_code  VARCHAR(10) UNIQUE NOT NULL,
    station_name  VARCHAR(200) NOT NULL,
    city          VARCHAR(100),
    state         VARCHAR(100),
    zone          VARCHAR(10),
    latitude      DOUBLE PRECISION DEFAULT 0,
    longitude     DOUBLE PRECISION DEFAULT 0,
    active        BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_station_code ON stations(station_code);
CREATE INDEX idx_station_name_trgm ON stations USING gin(station_name gin_trgm_ops);

-- ── TRAINS ───────────────────────────────────────────────────
CREATE TABLE trains (
    id            BIGSERIAL PRIMARY KEY,
    train_number  VARCHAR(10) UNIQUE NOT NULL,
    train_name    VARCHAR(200) NOT NULL,
    train_type    VARCHAR(50),
    running_days  INT NOT NULL DEFAULT 127, -- All 7 days (binary 1111111)
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_train_number ON trains(train_number);

-- ── TRAIN ROUTES ─────────────────────────────────────────────
CREATE TABLE train_routes (
    id                    BIGSERIAL PRIMARY KEY,
    train_id              BIGINT NOT NULL REFERENCES trains(id) ON DELETE CASCADE,
    station_id            BIGINT NOT NULL REFERENCES stations(id),
    stop_number           INT NOT NULL,
    arrival_time          TIME,
    departure_time        TIME,
    distance_from_source  INT NOT NULL DEFAULT 0,
    day_number            INT NOT NULL DEFAULT 1,
    halt_minutes          INT NOT NULL DEFAULT 0,
    UNIQUE(train_id, station_id),
    UNIQUE(train_id, stop_number)
);

CREATE INDEX idx_route_train   ON train_routes(train_id);
CREATE INDEX idx_route_station ON train_routes(station_id);

-- ── TRAIN COACHES ─────────────────────────────────────────────
CREATE TABLE train_coaches (
    id                      BIGSERIAL PRIMARY KEY,
    train_id                BIGINT NOT NULL REFERENCES trains(id) ON DELETE CASCADE,
    coach_number            VARCHAR(10) NOT NULL,
    seat_class              VARCHAR(10) NOT NULL,
    total_seats             INT NOT NULL DEFAULT 72,
    tatkal_quota            INT NOT NULL DEFAULT 6,
    premium_tatkal_quota    INT NOT NULL DEFAULT 3,
    ladies_quota            INT NOT NULL DEFAULT 6,
    senior_citizen_quota    INT NOT NULL DEFAULT 4,
    max_rac_seats           INT NOT NULL DEFAULT 18,
    max_waitlist_seats      INT NOT NULL DEFAULT 50,
    UNIQUE(train_id, coach_number)
);

-- ── SEATS ────────────────────────────────────────────────────
CREATE TABLE seats (
    id            BIGSERIAL PRIMARY KEY,
    coach_id      BIGINT NOT NULL REFERENCES train_coaches(id) ON DELETE CASCADE,
    seat_number   VARCHAR(10) NOT NULL,
    berth_type    VARCHAR(20),
    status        VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    journey_date  DATE NOT NULL,
    version       BIGINT NOT NULL DEFAULT 0,
    UNIQUE(coach_id, seat_number, journey_date)
);

CREATE INDEX idx_seat_coach_date ON seats(coach_id, journey_date);
CREATE INDEX idx_seat_status     ON seats(status, journey_date);

-- ── TRAIN SCHEDULES (Live GPS) ────────────────────────────────
CREATE TABLE train_schedules (
    id                   BIGSERIAL PRIMARY KEY,
    train_id             BIGINT NOT NULL REFERENCES trains(id) ON DELETE CASCADE,
    journey_date         DATE NOT NULL,
    status               VARCHAR(20) NOT NULL DEFAULT 'ON_TIME',
    delay_minutes        INT NOT NULL DEFAULT 0,
    current_latitude     DOUBLE PRECISION DEFAULT 0,
    current_longitude    DOUBLE PRECISION DEFAULT 0,
    current_station_code VARCHAR(10),
    next_station_code    VARCHAR(10),
    speed_kmph           INT DEFAULT 0,
    last_updated         TIMESTAMP DEFAULT NOW(),
    UNIQUE(train_id, journey_date)
);

-- ── BOOKINGS ─────────────────────────────────────────────────
CREATE TABLE bookings (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    pnr_number              VARCHAR(10) UNIQUE NOT NULL,
    user_id                 UUID NOT NULL REFERENCES users(id),
    train_id                BIGINT NOT NULL REFERENCES trains(id),
    journey_date            DATE NOT NULL,
    source_station_id       BIGINT REFERENCES stations(id),
    destination_station_id  BIGINT REFERENCES stations(id),
    seat_class              VARCHAR(10) NOT NULL,
    quota_type              VARCHAR(20) NOT NULL DEFAULT 'GENERAL',
    status                  VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    base_fare               NUMERIC(10,2) NOT NULL DEFAULT 0,
    tatkal_charge           NUMERIC(10,2) NOT NULL DEFAULT 0,
    service_tax             NUMERIC(10,2) NOT NULL DEFAULT 0,
    total_amount            NUMERIC(10,2) NOT NULL DEFAULT 0,
    refund_amount           NUMERIC(10,2) DEFAULT 0,
    boarding_station_code   VARCHAR(10),
    sms_alert               BOOLEAN NOT NULL DEFAULT TRUE,
    email_alert             BOOLEAN NOT NULL DEFAULT TRUE,
    booked_at               TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_booking_user       ON bookings(user_id);
CREATE INDEX idx_booking_pnr        ON bookings(pnr_number);
CREATE INDEX idx_booking_train_date ON bookings(train_id, journey_date);
CREATE INDEX idx_booking_status     ON bookings(status);

-- ── PASSENGERS ───────────────────────────────────────────────
CREATE TABLE passengers (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID REFERENCES users(id),
    booking_id      UUID REFERENCES bookings(id) ON DELETE CASCADE,
    name            VARCHAR(100) NOT NULL,
    age             INT NOT NULL,
    gender          VARCHAR(10) NOT NULL,
    nationality     VARCHAR(50) DEFAULT 'Indian',
    id_type         VARCHAR(20),
    id_number       VARCHAR(30),
    seat_number     VARCHAR(10),
    coach_number    VARCHAR(10),
    berth_preference VARCHAR(20),
    booking_status  VARCHAR(20) DEFAULT 'CNF',
    waitlist_number INT DEFAULT 0
);

CREATE INDEX idx_passenger_booking ON passengers(booking_id);
CREATE INDEX idx_passenger_user    ON passengers(user_id);

-- ── PAYMENTS ─────────────────────────────────────────────────
CREATE TABLE payments (
    id                   UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    booking_id           UUID UNIQUE NOT NULL REFERENCES bookings(id),
    payment_method       VARCHAR(30),
    status               VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    amount               NUMERIC(10,2) NOT NULL,
    currency             VARCHAR(5) NOT NULL DEFAULT 'INR',
    razorpay_order_id    VARCHAR(100),
    razorpay_payment_id  VARCHAR(100),
    razorpay_signature   TEXT,
    upi_transaction_id   VARCHAR(100),
    upi_vpa              VARCHAR(100),
    failure_reason       TEXT,
    refunded_amount      NUMERIC(10,2) DEFAULT 0,
    refund_id            VARCHAR(100),
    refunded_at          TIMESTAMP,
    created_at           TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payment_booking      ON payments(booking_id);
CREATE INDEX idx_payment_razorpay_oid ON payments(razorpay_order_id);

-- ── CANCELLATIONS ─────────────────────────────────────────────
CREATE TABLE cancellations (
    id                   UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    booking_id           UUID UNIQUE NOT NULL REFERENCES bookings(id),
    status               VARCHAR(20) NOT NULL DEFAULT 'REQUESTED',
    reason               TEXT,
    cancellation_charge  NUMERIC(10,2),
    refund_amount        NUMERIC(10,2),
    cancelled_passenger_ids TEXT,
    requested_at         TIMESTAMP NOT NULL DEFAULT NOW(),
    processed_at         TIMESTAMP
);

-- ── AUDIT LOG ────────────────────────────────────────────────
CREATE TABLE audit_logs (
    id          BIGSERIAL PRIMARY KEY,
    user_id     UUID REFERENCES users(id),
    action      VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50),
    entity_id   VARCHAR(100),
    ip_address  VARCHAR(50),
    user_agent  TEXT,
    details     JSONB,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_user   ON audit_logs(user_id);
CREATE INDEX idx_audit_action ON audit_logs(action);
CREATE INDEX idx_audit_time   ON audit_logs(created_at);

-- ── REFRESH TOKENS ────────────────────────────────────────────
CREATE TABLE refresh_tokens (
    id          BIGSERIAL PRIMARY KEY,
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token       TEXT UNIQUE NOT NULL,
    expires_at  TIMESTAMP NOT NULL,
    revoked     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_token ON refresh_tokens(token);

COMMENT ON TABLE bookings IS 'Core booking table with PNR, fare, quota and status';
COMMENT ON TABLE seats IS 'Per-date seat availability with optimistic locking (version col)';
COMMENT ON TABLE train_schedules IS 'Live GPS and delay info per train per date';

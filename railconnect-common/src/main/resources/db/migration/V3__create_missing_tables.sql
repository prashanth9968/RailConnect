-- ============================================================
-- RailConnect Database Schema - V3 Add Missing Tables
-- ============================================================

-- ── NOTIFICATIONS ───────────────────────────────────────────
CREATE TABLE notifications (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    user_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title               VARCHAR(255) NOT NULL,
    message             TEXT NOT NULL,
    notification_type   VARCHAR(100),
    reference_id        UUID,
    is_read             BOOLEAN NOT NULL DEFAULT FALSE,
    sent_via_email      BOOLEAN NOT NULL DEFAULT FALSE,
    sent_via_sms        BOOLEAN NOT NULL DEFAULT FALSE
);

-- ── OTP TOKENS ───────────────────────────────────────────────
CREATE TABLE otp_tokens (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    identifier          VARCHAR(150) NOT NULL,
    otp_code            VARCHAR(10) NOT NULL,
    purpose             VARCHAR(50),
    is_used             BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at          TIMESTAMP NOT NULL
);

-- ── SEAT INVENTORY ───────────────────────────────────────────
CREATE TABLE seat_inventory (
    id                      UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    schedule_id             BIGINT NOT NULL REFERENCES train_schedules(id) ON DELETE CASCADE,
    class_type              VARCHAR(20) NOT NULL,
    from_station_id         BIGINT REFERENCES stations(id),
    to_station_id           BIGINT REFERENCES stations(id),
    total_seats             INT NOT NULL,
    available_seats         INT NOT NULL,
    waitlisted_count        INT NOT NULL DEFAULT 0,
    tatkal_available        INT NOT NULL DEFAULT 0,
    premium_tatkal_available INT NOT NULL DEFAULT 0,
    fare                    NUMERIC(10,2),
    tatkal_fare             NUMERIC(10,2),
    premium_tatkal_fare     NUMERIC(10,2)
);

-- ── SEAT LOCKS ───────────────────────────────────────────────
CREATE TABLE seat_locks (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    schedule_id BIGINT REFERENCES train_schedules(id) ON DELETE CASCADE,
    class_type  VARCHAR(20),
    seat_number VARCHAR(10),
    coach_number VARCHAR(10),
    user_id     UUID REFERENCES users(id) ON DELETE SET NULL,
    locked_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at  TIMESTAMP NOT NULL,
    lock_token  VARCHAR(255) UNIQUE
);

-- ── TRAIN AVAILABILITY ────────────────────────────────────────
CREATE TABLE train_availability (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    train_id            BIGINT NOT NULL REFERENCES trains(id) ON DELETE CASCADE,
    journey_date        DATE NOT NULL,
    class_type          VARCHAR(20) NOT NULL,
    quota_type          VARCHAR(20) NOT NULL,
    total_seats         INT NOT NULL,
    available_seats     INT NOT NULL,
    waiting_list_count  INT NOT NULL DEFAULT 0,
    rac_count           INT NOT NULL DEFAULT 0,
    base_fare           NUMERIC(10,2) NOT NULL,
    tatkal_fare         NUMERIC(10,2) NOT NULL DEFAULT 0,
    tatkal_open         BOOLEAN NOT NULL DEFAULT FALSE,
    booking_open        BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE UNIQUE INDEX idx_avail_train_date_class ON train_availability(train_id, journey_date, class_type, quota_type);

-- ── TRAIN FARES ──────────────────────────────────────────────
CREATE TABLE train_fares (
    id                  BIGSERIAL PRIMARY KEY,
    train_id            BIGINT NOT NULL REFERENCES trains(id) ON DELETE CASCADE,
    from_stop           INT NOT NULL,
    to_stop             INT NOT NULL,
    coach_class         VARCHAR(50) NOT NULL,
    base_fare           NUMERIC(10,2) NOT NULL,
    reservation_charge  NUMERIC(10,2) NOT NULL DEFAULT 0,
    UNIQUE(train_id, from_stop, to_stop, coach_class)
);

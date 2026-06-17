-- ============================================================
-- RailConnect Database Schema - V4 Add System Metadata & Indexes
-- ============================================================

CREATE TABLE IF NOT EXISTS system_metadata (
    metadata_key VARCHAR(255) PRIMARY KEY,
    metadata_value VARCHAR(255) NOT NULL
);

-- Ensure index coverage for fast searches
CREATE INDEX IF NOT EXISTS idx_stations_code ON stations(station_code);
CREATE INDEX IF NOT EXISTS idx_stations_name ON stations(station_name);
CREATE INDEX IF NOT EXISTS idx_stations_city ON stations(city);
CREATE INDEX IF NOT EXISTS idx_trains_number ON trains(train_number);

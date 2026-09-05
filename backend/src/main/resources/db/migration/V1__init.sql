CREATE TABLE vendors (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    sla_ota_pct DECIMAL(5, 2) NOT NULL DEFAULT 90
);

CREATE TABLE trips (
    id BIGINT PRIMARY KEY,
    business_unit VARCHAR(100),
    office VARCHAR(255),
    mode VARCHAR(50),
    vendor_id VARCHAR(255) NOT NULL,
    route_id VARCHAR(255),
    corridor VARCHAR(255),
    shift_id VARCHAR(50),
    trip_date DATE,
    scheduled_at TIMESTAMPTZ,
    actual_at TIMESTAMPTZ,
    occupancy INT,
    capacity INT,
    cost DECIMAL(15, 2) DEFAULT 0,
    on_time BOOLEAN NOT NULL,
    delay_minutes INT DEFAULT 0,
    delay_reason VARCHAR(50)
);

CREATE INDEX idx_trips_vendor ON trips(vendor_id);
CREATE INDEX idx_trips_date ON trips(trip_date);
CREATE INDEX idx_trips_vendor_date ON trips(vendor_id, trip_date);

CREATE TABLE delay_records (
    id BIGSERIAL PRIMARY KEY,
    trip_id BIGINT NOT NULL REFERENCES trips(id),
    reason_code VARCHAR(50) NOT NULL,
    delay_minutes INT DEFAULT 0
);

CREATE TABLE vendor_monthly_stats (
    vendor_id VARCHAR(255) NOT NULL,
    month_year VARCHAR(7) NOT NULL,
    ota_pct DECIMAL(5, 2) NOT NULL,
    trip_count BIGINT NOT NULL,
    PRIMARY KEY (vendor_id, month_year)
);

CREATE TABLE findings (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(100) NOT NULL,
    severity VARCHAR(50) NOT NULL,
    metric_json TEXT,
    benchmark_json TEXT,
    narration TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE agent_actions (
    id BIGSERIAL PRIMARY KEY,
    finding_id BIGINT REFERENCES findings(id),
    action_type VARCHAR(100) NOT NULL,
    payload_json TEXT,
    drafted_message TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    confirmed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE agent_audit_log (
    id BIGSERIAL PRIMARY KEY,
    run_id UUID NOT NULL,
    stage VARCHAR(50) NOT NULL,
    message TEXT NOT NULL,
    timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE app_metadata (
    key VARCHAR(100) PRIMARY KEY,
    value VARCHAR(255) NOT NULL
);

CREATE TABLE live_feed_events (
    id BIGSERIAL PRIMARY KEY,
    sentiment VARCHAR(20) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    title VARCHAR(255) NOT NULL,
    detail TEXT,
    office VARCHAR(255),
    shift_id VARCHAR(50),
    metric_value DECIMAL(12, 2),
    source VARCHAR(50) NOT NULL DEFAULT 'SIMULATOR',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_live_feed_events_created_at ON live_feed_events(created_at DESC);

CREATE TABLE live_action_items (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT REFERENCES live_feed_events(id),
    severity VARCHAR(20) NOT NULL,
    title VARCHAR(255) NOT NULL,
    ai_insight TEXT NOT NULL,
    recommended_action TEXT NOT NULL,
    action_type VARCHAR(100),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    openai_model VARCHAR(100),
    confirmed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_live_action_items_status ON live_action_items(status, created_at DESC);

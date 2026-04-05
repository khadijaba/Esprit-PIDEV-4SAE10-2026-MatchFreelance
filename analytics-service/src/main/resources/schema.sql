CREATE TABLE IF NOT EXISTS platform_metric (
    id SERIAL PRIMARY KEY,
    metric_key VARCHAR(128) NOT NULL UNIQUE,
    metric_value NUMERIC(19, 4) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

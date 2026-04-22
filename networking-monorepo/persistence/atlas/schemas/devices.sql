CREATE TABLE devices (
    id UUID PRIMARY KEY,
    os TEXT NULL,
    screen TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX devices_updated_at_idx ON devices (updated_at);

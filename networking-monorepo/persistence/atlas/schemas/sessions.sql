CREATE TABLE sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    tenant_id UUID NULL,
    device_id UUID NULL,
    created_at TIMESTAMPTZ NOT NULL,
    absolute_expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ NULL
);

CREATE INDEX sessions_user_id_idx ON sessions (user_id);
CREATE INDEX sessions_absolute_expires_at_idx ON sessions (absolute_expires_at);
CREATE INDEX sessions_revoked_at_idx ON sessions (revoked_at);

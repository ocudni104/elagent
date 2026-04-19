CREATE TABLE users (
    id UUID PRIMARY KEY,
    email TEXT NOT NULL,
    provider TEXT NOT NULL,
    provider_subject TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    last_login_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT users_provider_subject_key UNIQUE (provider, provider_subject),
    CONSTRAINT users_email_key UNIQUE (email)
);

CREATE INDEX users_provider_subject_idx ON users (provider, provider_subject);

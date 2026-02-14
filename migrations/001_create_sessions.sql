-- Create sessions table for user authentication
CREATE TABLE sessions (
    id UUID PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    language VARCHAR(10) NOT NULL DEFAULT 'en',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_active_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_sessions_username ON sessions(username);

CREATE TABLE chat_session (
    id          UUID PRIMARY KEY,
    user_id     VARCHAR(100) NOT NULL,
    title       VARCHAR(200) NOT NULL DEFAULT 'New Chat',
    is_favorite BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_chat_session_user_id ON chat_session (user_id);

CREATE TABLE chat_message (
    id         UUID PRIMARY KEY,
    session_id UUID NOT NULL REFERENCES chat_session (id) ON DELETE CASCADE,
    sender     VARCHAR(20) NOT NULL,
    content    TEXT NOT NULL,
    context    JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_chat_message_session_id ON chat_message (session_id);
CREATE INDEX idx_chat_message_session_created_at ON chat_message (session_id, created_at);

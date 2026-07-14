ALTER TABLE session ADD COLUMN user_id BIGINT NULL REFERENCES users(id);
CREATE INDEX idx_session_user_id ON session(user_id);

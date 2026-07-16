-- The legacy questionnaire table is disconnected from the live, anonymous session flow.
-- Current assessments are stored through session, answer, and risk_assessment instead.
DROP TABLE IF EXISTS questionnaire;

-- Protect the one-answer-per-question invariant and support session history lookups.
CREATE UNIQUE INDEX IF NOT EXISTS answer_session_question_key_key
    ON answer (session_id, question_key);
CREATE INDEX IF NOT EXISTS answer_session_created_at_idx
    ON answer (session_id, created_at);

-- Support result retrieval and the risk distribution used by the admin dashboard.
CREATE INDEX IF NOT EXISTS risk_assessment_session_created_at_idx
    ON risk_assessment (session_id, created_at DESC);
CREATE INDEX IF NOT EXISTS risk_assessment_risk_level_idx
    ON risk_assessment (risk_level);

-- Support cleanup and status reporting without scanning all anonymous sessions.
CREATE INDEX IF NOT EXISTS session_status_created_at_idx
    ON session (status, created_at);

-- Support active-question ordering and the administration views.
CREATE INDEX IF NOT EXISTS question_active_display_order_idx
    ON question (is_active, display_order);
CREATE INDEX IF NOT EXISTS users_created_at_idx
    ON users (created_at DESC);
CREATE INDEX IF NOT EXISTS users_enabled_verified_idx
    ON users (enabled, email_verified);

-- Tokens are looked up directly and must identify only one account action.
CREATE UNIQUE INDEX IF NOT EXISTS verification_token_token_key
    ON verification_token (token) WHERE token IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS password_reset_tokens_token_key
    ON password_reset_tokens (token) WHERE token IS NOT NULL;

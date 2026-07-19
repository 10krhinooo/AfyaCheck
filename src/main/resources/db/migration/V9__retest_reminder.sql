-- Opt-in retest reminders. Deliberately NOT linked to any session/assessment: the email
-- address is the only stored datum (plus when to send), the reminder content is generic
-- ("time to retest"), and the row is deleted after the send attempt — preserving the
-- anonymous-by-design stance as far as a scheduled email allows.
CREATE TABLE retest_reminder (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(320) NOT NULL,
    due_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_retest_reminder_due_at ON retest_reminder (due_at);

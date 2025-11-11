-- Migration: Add user_id field to Session and AuthAttempt tables
-- Purpose: Avoid NULL issues when Device is unlinked/changed
-- This is a denormalization strategy to maintain user references even after device deletion

-- ============================================================
-- PART 1: Add user_id to attempt_auth table
-- ============================================================

-- Add user_id column (nullable initially for data migration)
ALTER TABLE attempt_auth ADD COLUMN user_id BIGINT;

-- Migrate existing data: populate user_id from the Device -> User chain
UPDATE attempt_auth aa
SET user_id = (
    SELECT d.useId
    FROM device d
    WHERE d.dev_fingerprint = aa.dev_fingerprint
)
WHERE aa.dev_fingerprint IS NOT NULL;

-- Make user_id NOT NULL after data migration
ALTER TABLE attempt_auth ALTER COLUMN user_id SET NOT NULL;

-- Optional: Add index for better query performance
CREATE INDEX idx_attempt_auth_user_id ON attempt_auth(user_id);

-- ============================================================
-- PART 2: Add user_id to session table
-- ============================================================

-- Add user_id column (nullable initially for data migration)
ALTER TABLE session ADD COLUMN user_id BIGINT;

-- Migrate existing data: populate user_id from Session -> AuthAttempt -> Device -> User chain
UPDATE session s
SET user_id = (
    SELECT d.useId
    FROM attempt_auth aa
    JOIN device d ON aa.dev_fingerprint = d.dev_fingerprint
    WHERE aa.att_id = s.att_id_initial
)
WHERE s.att_id_initial IS NOT NULL;

-- Make user_id NOT NULL after data migration
ALTER TABLE session ALTER COLUMN user_id SET NOT NULL;

-- Optional: Add index for better query performance
CREATE INDEX idx_session_user_id ON session(user_id);

-- ============================================================
-- VERIFICATION QUERIES (Optional - for testing)
-- ============================================================

-- Check for any rows that still have NULL user_id (should be 0)
-- SELECT COUNT(*) FROM attempt_auth WHERE user_id IS NULL;
-- SELECT COUNT(*) FROM session WHERE user_id IS NULL;

-- Verify data migration worked correctly
-- SELECT aa.att_id, aa.user_id, d.useId as expected_user_id
-- FROM attempt_auth aa
-- LEFT JOIN device d ON aa.dev_fingerprint = d.dev_fingerprint
-- WHERE aa.user_id != d.useId OR (aa.user_id IS NULL AND d.useId IS NOT NULL);

-- SELECT s.ses_id, s.user_id, d.useId as expected_user_id
-- FROM session s
-- LEFT JOIN attempt_auth aa ON s.att_id_initial = aa.att_id
-- LEFT JOIN device d ON aa.dev_fingerprint = d.dev_fingerprint
-- WHERE s.user_id != d.useId OR (s.user_id IS NULL AND d.useId IS NOT NULL);

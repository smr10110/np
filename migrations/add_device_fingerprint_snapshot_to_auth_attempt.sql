-- Migration: Add dev_fingerprint_snapshot column to attempt_auth
-- Purpose: Preserve device fingerprint in AuthAttempt records even after Device deletion
-- Context: Currently when Device is deleted, dev_fingerprint FK is set to NULL (ON DELETE SET NULL)
--          This migration adds a denormalized snapshot field to preserve fingerprint for audit trail

-- ============================================================
-- STEP 1: Add new column dev_fingerprint_snapshot
-- ============================================================

ALTER TABLE attempt_auth
ADD COLUMN IF NOT EXISTS dev_fingerprint_snapshot VARCHAR(100);

-- ============================================================
-- STEP 2: Backfill existing records with current device fingerprints
-- ============================================================

-- For existing records that still have a device reference, copy the fingerprint
UPDATE attempt_auth
SET dev_fingerprint_snapshot = dev_fingerprint
WHERE dev_fingerprint IS NOT NULL
  AND dev_fingerprint_snapshot IS NULL;

-- ============================================================
-- STEP 3: Add index for performance (optional but recommended)
-- ============================================================

-- Index on dev_fingerprint_snapshot for audit queries
CREATE INDEX IF NOT EXISTS idx_attempt_auth_fingerprint_snapshot
ON attempt_auth(dev_fingerprint_snapshot);

-- ============================================================
-- VERIFICATION (Optional - for testing)
-- ============================================================

-- Check that the column was added correctly:
-- SELECT column_name, data_type, character_maximum_length, is_nullable
-- FROM information_schema.columns
-- WHERE table_name = 'attempt_auth' AND column_name = 'dev_fingerprint_snapshot';

-- Verify backfill worked:
-- SELECT COUNT(*) as total_attempts,
--        COUNT(dev_fingerprint) as with_device_fk,
--        COUNT(dev_fingerprint_snapshot) as with_snapshot
-- FROM attempt_auth;

-- Test audit trail after device deletion (ONLY IN DEVELOPMENT!):
-- 1. Create a test AuthAttempt with a device
-- 2. Note the fingerprint value
-- 3. Delete the device
-- 4. Verify that dev_fingerprint is NULL but dev_fingerprint_snapshot still has the value

-- Example test:
-- SELECT att_id, user_id, dev_fingerprint, dev_fingerprint_snapshot
-- FROM attempt_auth WHERE user_id = <test_user_id>;
-- Expected: After device deletion, dev_fingerprint = NULL, dev_fingerprint_snapshot = preserved value

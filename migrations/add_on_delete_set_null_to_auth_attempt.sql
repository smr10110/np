-- Migration: Add ON DELETE SET NULL constraint to attempt_auth.dev_fingerprint
-- Purpose: Automatically set device to NULL when Device is deleted (instead of manual detach)
-- This simplifies DeviceService code and lets PostgreSQL handle referential integrity

-- ============================================================
-- STEP 1: Drop existing foreign key constraint (if exists)
-- ============================================================

-- Find the current FK constraint name (it might vary)
-- Commonly it's named: attempt_auth_dev_fingerprint_fkey or similar
-- You can check with: \d attempt_auth in psql

DO $$
BEGIN
    -- Try to drop the FK constraint if it exists
    -- Replace 'attempt_auth_dev_fingerprint_fkey' with actual constraint name if different
    IF EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'attempt_auth_dev_fingerprint_fkey'
        AND table_name = 'attempt_auth'
    ) THEN
        ALTER TABLE attempt_auth DROP CONSTRAINT attempt_auth_dev_fingerprint_fkey;
    END IF;
END $$;

-- ============================================================
-- STEP 2: Add new foreign key WITH ON DELETE SET NULL
-- ============================================================

ALTER TABLE attempt_auth
ADD CONSTRAINT fk_attempt_device
FOREIGN KEY (dev_fingerprint)
REFERENCES device(dev_fingerprint)
ON DELETE SET NULL;

-- ============================================================
-- VERIFICATION (Optional - for testing)
-- ============================================================

-- Check that the constraint was added correctly:
-- SELECT conname, confdeltype FROM pg_constraint
-- WHERE conname = 'fk_attempt_device';
-- Expected: confdeltype = 'n' (meaning SET NULL)

-- Test the constraint (ONLY IN DEVELOPMENT!):
-- 1. Create a test user and device
-- 2. Create an AuthAttempt with that device
-- 3. Delete the device
-- 4. Verify that AuthAttempt.dev_fingerprint is NULL but AuthAttempt.user_id is NOT NULL

-- Example test:
-- SELECT att_id, user_id, dev_fingerprint FROM attempt_auth WHERE user_id = <test_user_id>;
-- DELETE FROM device WHERE dev_fingerprint = '<test_fingerprint>';
-- SELECT att_id, user_id, dev_fingerprint FROM attempt_auth WHERE user_id = <test_user_id>;
-- Expected: dev_fingerprint should be NULL, user_id should still have value

-- ========================================================
-- Migration: Normalize ON DELETE CASCADE Rules
-- Description: Add ON DELETE CASCADE/SET NULL/RESTRICT rules to all foreign keys
--              to ensure referential integrity and proper data cleanup
-- Date: 2025-11-11
-- Author: Claude AI
-- ========================================================

-- ========================================================
-- 1. Device → User: CASCADE
-- When a user is deleted, delete all their devices
-- ========================================================
ALTER TABLE device
    DROP CONSTRAINT IF EXISTS fk_dev_user;

ALTER TABLE device
    ADD CONSTRAINT fk_dev_user
    FOREIGN KEY (use_id)
    REFERENCES app_user(use_id)
    ON DELETE CASCADE;

-- ========================================================
-- 2. Session → AuthAttempt: SET NULL
-- When an auth attempt is deleted, preserve sessions but set reference to NULL
-- ========================================================
ALTER TABLE session
    DROP CONSTRAINT IF EXISTS fk_session_auth_attempt;

ALTER TABLE session
    ADD CONSTRAINT fk_session_auth_attempt
    FOREIGN KEY (att_id_initial)
    REFERENCES attempt_auth(att_id)
    ON DELETE SET NULL;

-- ========================================================
-- 3. DeviceLog → User: CASCADE
-- When a user is deleted, delete all their device logs
-- ========================================================
ALTER TABLE device_log
    DROP CONSTRAINT IF EXISTS fk_devlog_user;

ALTER TABLE device_log
    ADD CONSTRAINT fk_devlog_user
    FOREIGN KEY (use_id)
    REFERENCES app_user(use_id)
    ON DELETE CASCADE;

-- ========================================================
-- 4. DeviceLog → Device: SET NULL
-- When a device is deleted, preserve logs but set device reference to NULL
-- ========================================================
ALTER TABLE device_log
    DROP CONSTRAINT IF EXISTS fk_devlog_device;

ALTER TABLE device_log
    ADD CONSTRAINT fk_devlog_device
    FOREIGN KEY (dev_fingerprint)
    REFERENCES device(dev_fingerprint)
    ON DELETE SET NULL;

-- ========================================================
-- 5. DeviceRecovery → User: CASCADE
-- When a user is deleted, delete all their device recovery requests
-- ========================================================
ALTER TABLE device_recovery
    DROP CONSTRAINT IF EXISTS fk_devrec_user;

ALTER TABLE device_recovery
    ADD CONSTRAINT fk_devrec_user
    FOREIGN KEY (use_id)
    REFERENCES app_user(use_id)
    ON DELETE CASCADE;

-- ========================================================
-- 6. PasswordRecovery → User: CASCADE
-- When a user is deleted, delete all their password recovery requests
-- ========================================================
ALTER TABLE password_recovery
    DROP CONSTRAINT IF EXISTS fk_password_recovery_user;

ALTER TABLE password_recovery
    ADD CONSTRAINT fk_password_recovery_user
    FOREIGN KEY (use_id)
    REFERENCES app_user(use_id)
    ON DELETE CASCADE;

-- ========================================================
-- 7. FundTransaction → Account (origin): RESTRICT
-- Prevent deletion of accounts that have associated transactions
-- ========================================================
ALTER TABLE transactions
    DROP CONSTRAINT IF EXISTS fk_transaction_origin_account;

ALTER TABLE transactions
    ADD CONSTRAINT fk_transaction_origin_account
    FOREIGN KEY (origin_account_id)
    REFERENCES accounts(id)
    ON DELETE RESTRICT;

-- ========================================================
-- 8. FundTransaction → Account (destination): RESTRICT
-- Prevent deletion of accounts that have associated transactions
-- ========================================================
ALTER TABLE transactions
    DROP CONSTRAINT IF EXISTS fk_transaction_destination_account;

ALTER TABLE transactions
    ADD CONSTRAINT fk_transaction_destination_account
    FOREIGN KEY (destination_account_id)
    REFERENCES accounts(id)
    ON DELETE RESTRICT;

-- ========================================================
-- Summary of Changes:
-- ========================================================
-- CASCADE rules (child deleted when parent deleted):
--   - device → app_user
--   - device_log → app_user
--   - device_recovery → app_user
--   - password_recovery → app_user
--
-- SET NULL rules (preserve child, set FK to NULL):
--   - session → attempt_auth
--   - device_log → device
--
-- RESTRICT rules (prevent parent deletion if children exist):
--   - transactions → accounts (origin and destination)
-- ========================================================

-- Verification queries:
-- SELECT
--     tc.constraint_name,
--     tc.table_name,
--     kcu.column_name,
--     ccu.table_name AS foreign_table_name,
--     ccu.column_name AS foreign_column_name,
--     rc.delete_rule
-- FROM information_schema.table_constraints AS tc
-- JOIN information_schema.key_column_usage AS kcu
--     ON tc.constraint_name = kcu.constraint_name
--     AND tc.table_schema = kcu.table_schema
-- JOIN information_schema.constraint_column_usage AS ccu
--     ON ccu.constraint_name = tc.constraint_name
--     AND ccu.table_schema = tc.table_schema
-- JOIN information_schema.referential_constraints AS rc
--     ON rc.constraint_name = tc.constraint_name
--     AND rc.constraint_schema = tc.table_schema
-- WHERE tc.constraint_type = 'FOREIGN KEY'
-- ORDER BY tc.table_name, tc.constraint_name;

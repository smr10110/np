-- ============================================
-- NaivePay - Datos Iniciales
-- ============================================
-- Este archivo se ejecuta automáticamente al arrancar Spring Boot
-- Solo funciona si spring.jpa.defer-datasource-initialization=true
--
-- IMPORTANTE: Este script es ALTERNATIVO a AdminUserInitializer.java
-- Si usas AdminUserInitializer, puedes borrar este archivo.
-- ============================================

-- ============================================
-- USUARIO ADMINISTRADOR
-- ============================================
-- Email: admin@naivepay.cl
-- RUT: 11111111-1
-- Password: Admin@2025
-- ============================================

-- 1. Crear registro de admin
INSERT INTO register (reg_id, reg_email, reg_hashed_login_password, reg_register_date, reg_verified, reg_verification_code, reg_verification_code_expiration)
SELECT
    1000,
    'admin@naivepay.cl',
    '$2a$10$N9qo8uLOickgx2ZMRZoMye.IjefYl/ZqH7VzWtKEm7UIJB8C6/Kmi', -- Hash BCrypt de "Admin@2025"
    CURRENT_TIMESTAMP,
    TRUE,
    NULL,
    NULL
WHERE NOT EXISTS (SELECT 1 FROM register WHERE reg_email = 'admin@naivepay.cl');

-- 2. Crear credencial de admin (sin claves RSA por simplicidad)
INSERT INTO credencial (cre_id, cre_creation_date, cre_denied, cre_active_dinamic_key, cre_private_key_rsa, cre_public_key_rsa)
SELECT
    1000,
    CURRENT_TIMESTAMP,
    FALSE,
    FALSE,
    NULL, -- Las claves RSA se pueden agregar después si es necesario
    NULL
WHERE NOT EXISTS (SELECT 1 FROM credencial WHERE cre_id = 1000);

-- 3. Crear usuario admin
INSERT INTO app_user (use_id, use_names, use_last_names, use_rut_general, use_verification_digit, use_birth_date, use_phone_number, use_profession, use_adress, use_state, use_role, credencial_id, register_id)
SELECT
    1000,
    'Admin',
    'Sistema',
    11111111,
    '1',
    '1990-01-01',
    56912345678,
    'Administrador',
    'UFRO',
    'ACTIVE',
    'ADMIN',
    1000, -- FK a credencial
    1000  -- FK a register
WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE use_rut_general = 11111111 AND use_verification_digit = '1');

-- 4. Crear cuenta de fondos para admin
INSERT INTO accounts (user_id, balance, created_at, updated_at)
SELECT
    1000,
    0.00,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM accounts WHERE user_id = 1000);

-- ============================================
-- NOTA IMPORTANTE
-- ============================================
-- Este script NO genera claves RSA (cre_private_key_rsa, cre_public_key_rsa)
--
-- Opciones:
-- 1. Usar AdminUserInitializer.java (RECOMENDADO) - genera RSA automáticamente
-- 2. Agregar las claves RSA manualmente después del primer login
-- 3. Modificar el sistema para que las claves RSA sean opcionales para admin
-- ============================================

-- ============================================
-- Hash BCrypt incluido:
-- ============================================
-- Password: Admin@2025
-- Hash: $2a$10$N9qo8uLOickgx2ZMRZoMye.IjefYl/ZqH7VzWtKEm7UIJB8C6/Kmi
--
-- Para generar un nuevo hash:
-- BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
-- String hash = encoder.encode("Admin@2025");
-- System.out.println(hash);
-- ============================================

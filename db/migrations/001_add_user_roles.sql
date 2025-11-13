-- ============================================
-- Migración: Agregar roles de usuario
-- Fecha: 2025-11-13
-- Descripción: Agrega columna use_role y crea usuario admin
-- ============================================

-- 1. Agregar columna use_role a la tabla app_user
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS use_role VARCHAR(20) NOT NULL DEFAULT 'USER';

-- 2. Actualizar usuarios existentes (asegurar que tengan rol USER)
UPDATE app_user SET use_role = 'USER' WHERE use_role IS NULL;

-- 3. Crear usuario admin si no existe
-- Nota: Este SQL usa el hash BCrypt de la contraseña "Admin@2025"
-- Hash generado con: new BCryptPasswordEncoder().encode("Admin@2025")
INSERT INTO app_user (
    use_names,
    use_last_names,
    use_rut_general,
    use_verification_digit,
    use_birth_date,
    use_phone_number,
    use_profession,
    use_adress,
    use_state,
    use_role
)
SELECT
    'Admin',
    'Sistema',
    11111111,
    '1',
    '1990-01-01',
    56912345678,
    'Administrador',
    'UFRO',
    'ACTIVE',
    'ADMIN'
WHERE NOT EXISTS (
    SELECT 1 FROM app_user WHERE use_rut_general = 11111111 AND use_verification_digit = '1'
);

-- 4. Crear credencial para admin (BCrypt hash de "Admin@2025")
-- IMPORTANTE: Ejecutar este script después de generar el hash real
-- Este es un hash de ejemplo, debes reemplazarlo con uno generado en tu entorno
INSERT INTO credencial (
    cre_password,
    cre_key_status
)
SELECT
    '$2a$10$XqZJ3k5YqN4LKW8.Zr5Ovu.rGxJxFmYKkZ9K1XqYZJ3k5YqN4LKW8', -- Reemplazar con hash real
    'ACTIVE'
WHERE NOT EXISTS (
    SELECT 1 FROM app_user WHERE use_rut_general = 11111111 AND use_verification_digit = '1'
);

-- 5. Vincular credencial con usuario admin
-- Nota: Ajustar IDs según tu base de datos
-- UPDATE app_user
-- SET credencial_id = (SELECT cre_id FROM credencial WHERE ...)
-- WHERE use_rut_general = 11111111 AND use_verification_digit = '1';

-- ============================================
-- CREDENCIALES ADMIN (Solo para desarrollo)
-- ============================================
-- RUT: 11111111-1
-- Email: admin@naivepay.cl (agregar en registro)
-- Password: Admin@2025
-- Rol: ADMIN
-- ============================================

-- Para generar un nuevo hash BCrypt, ejecuta este código Java:
-- BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
-- String hash = encoder.encode("Admin@2025");
-- System.out.println(hash);

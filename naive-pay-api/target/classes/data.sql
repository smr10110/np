-- ---------------------------------------------------------------------------
-- Datos semilla para el entorno de desarrollo.
-- Crea un administrador y su cuenta de fondos solo si aún no existe.
-- ---------------------------------------------------------------------------

-- 1) Registro base con credenciales de acceso.
INSERT INTO register (
    reg_id,
    reg_email,
    reg_hashed_login_password,
    reg_register_date,
    reg_verified,
    reg_verification_code,
    reg_verification_code_expiration
)
SELECT
    1000,
    'hola122@yopmail.com',
    '$2b$12$Edh0PwDTKmM.0uFsrzZLFOSjdxdWatHVIYglqdySdjxb/ZmjCSLU6', -- Admin@2025
    CURRENT_TIMESTAMP,
    TRUE,
    NULL,
    NULL
WHERE NOT EXISTS (
    SELECT 1 FROM register WHERE reg_email = 'hola122@yopmail.com'
);

-- 2) Credencial asociada (sin llaves RSA por simplicidad en dev).
INSERT INTO credencial (
    cre_id,
    cre_creation_date,
    cre_denied,
    cre_active_dinamic_key,
    cre_private_key_rsa,
    cre_public_key_rsa
)
SELECT
    1000,
    CURRENT_TIMESTAMP,
    FALSE,
    FALSE,
    NULL,
    NULL
WHERE NOT EXISTS (
    SELECT 1 FROM credencial WHERE cre_id = 1000
);

-- 3) Usuario administrador enlazado al registro y credencial creados.
INSERT INTO app_user (
    use_id,
    use_names,
    use_last_names,
    use_rut_general,
    use_verification_digit,
    use_birth_date,
    use_phone_number,
    use_profession,
    use_state,
    use_adress,
    use_role,
    credencial_id,
    register_id
)
SELECT
    1000,
    'Admin',
    'Sistema',
    11111111,
    '1',
    DATE '1990-01-01',
    56912345678,
    'Administrador',
    'ACTIVE',
    'UFRO',
    'ADMIN',
    1000,
    1000
WHERE NOT EXISTS (
    SELECT 1 FROM app_user WHERE use_role = 'ADMIN'
);

-- 4) Cuenta de fondos asociada al administrador.
INSERT INTO accounts (
    id,
    user_id,
    available_balance,
    creation_date,
    last_update
)
SELECT
    1000,
    1000,
    0.00,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
WHERE NOT EXISTS (
    SELECT 1 FROM accounts WHERE user_id = 1000
);

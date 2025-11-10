# Análisis de Redundancia de Datos: Naive-Pay

## Objetivo: Eliminar Redundancia y Normalizar Base de Datos

---

## 🔍 REDUNDANCIAS IDENTIFICADAS

### Redundancia 1: User ↔ Credencial ↔ Register (Datos Divididos) 🔴 CRÍTICA

**Problema Actual**:
```
User (app_user)
├─ useId, useNames, useLastNames, useRutGeneral, ...
├─ credencial_id (FK) → Credencial
└─ register_id (FK) → Register

Credencial
├─ creId
├─ crePrivateKeyRsa
├─ crePublicKeyRsa
├─ creCreationDate
├─ creDenied
└─ creActiveDinamicKey

Register
├─ regId
├─ regEmail            ← ❌ DATO DE AUTENTICACIÓN
├─ regHashedLoginPassword ← ❌ DATO DE AUTENTICACIÓN
├─ regRegisterDate
└─ regVerified
```

**Redundancias**:
1. ✅ **User + Credencial + Register = 3 tablas relacionadas 1:1:1** → Ineficiente
2. ✅ **Email en Register** pero debería estar en módulo autentificación
3. ✅ **Password en Register** pero es dato de login (autentificación)
4. ✅ **3 JOINs necesarios** para obtener datos completos de usuario

**Impacto en Queries**:
```sql
-- Query actual (3 JOINs):
SELECT u.use_id, u.use_names,
       c.cre_public_key_rsa, c.cre_private_key_rsa,
       r.reg_email, r.reg_hashed_login_password
FROM app_user u
INNER JOIN credencial c ON u.credencial_id = c.cre_id
INNER JOIN register r ON u.register_id = r.reg_id
WHERE r.reg_email = 'user@example.com';

-- Query ideal (0 JOINs):
SELECT use_id, use_names, use_email, use_hashed_password,
       use_public_key_rsa, use_private_key_rsa
FROM app_user
WHERE use_email = 'user@example.com';
```

**Solución**: Merge total en tabla `app_user`

---

### Redundancia 2: Session → Device (Snapshot de Fingerprint) 🟡 MODERADA

**Problema Actual**:
```java
// Session.java
@ManyToOne(fetch = FetchType.LAZY, optional = true)
@JoinColumn(name = "dev_fingerprint", referencedColumnName = "dev_fingerprint")
private Device device;  // ← Relación FK

@Column(name = "ses_dev_fp", length = 255)
private String sesDeviceFingerprint;  // ← ❌ DUPLICADO: mismo fingerprint
```

**Redundancia**:
- `device.fingerprint` ya tiene el valor
- `sesDeviceFingerprint` duplica la misma información

**¿Es necesario el snapshot?**
- ✅ **SÍ** si quieres auditoría histórica (qué fingerprint tenía device cuando se creó sesión)
- ❌ **NO** si solo necesitas saber qué device usó (FK suficiente)

**Soluciones**:

**Opción A - Eliminar snapshot** (simplicidad):
```java
// Session.java
@ManyToOne(fetch = FetchType.LAZY, optional = true)
@JoinColumn(name = "dev_id")  // Solo FK
private Device device;

// ELIMINAR:
// private String sesDeviceFingerprint;
```

**Opción B - Mantener snapshot** (auditoría completa):
```java
// Session.java
@ManyToOne(fetch = FetchType.LAZY, optional = true)
@JoinColumn(name = "dev_id")
private Device device;

@Column(name = "ses_dev_fp_snapshot", length = 255)
private String deviceFingerprintSnapshot;  // Snapshot al momento de crear sesión

// ✅ Usar solo para auditoría histórica, NO para validación
```

**Recomendación**: **Opción B** si necesitas saber exactamente qué fingerprint tenía el device cuando se creó la sesión (útil si device cambia fingerprint o se elimina).

---

### Redundancia 3: AuthAttempt → Session/Device (Snapshot de Fingerprint) 🟡 MODERADA

**Problema Actual**:
```java
// AuthAttempt.java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "use_id", referencedColumnName = "useId")
private User user;  // ← FK a User

@Column(name = "att_dev_fp", length = 255)
private String attDeviceFingerprint;  // ← Snapshot de fingerprint

@ManyToOne(fetch = FetchType.LAZY, optional = true)
@JoinColumn(name = "ses_id")
private Session session;  // ← FK a Session (que ya tiene device)
```

**Redundancia**:
- Si tienes `session` → ya sabes `device` → ya sabes `fingerprint`
- `attDeviceFingerprint` puede derivarse de `session.device.fingerprint`

**¿Es necesario?**
- ✅ **SÍ** para intentos fallidos sin sesión (login rechazado antes de crear session)
- ✅ **SÍ** para auditoría histórica (snapshot del fingerprint al momento exacto)

**Soluciones**:

**Opción A - Eliminar snapshot y derivar de Session**:
```java
// AuthAttempt.java
@ManyToOne(fetch = FetchType.LAZY)
private User user;

@ManyToOne(fetch = FetchType.LAZY, optional = true)
private Session session;  // Puede ser null si login falló antes de crear sesión

// ELIMINAR:
// private String attDeviceFingerprint;

// Método helper:
public String getDeviceFingerprint() {
    return session != null && session.getDevice() != null
        ? session.getDevice().getFingerprint()
        : null;
}
```

**❌ Problema**: Si login falla (usuario no existe), no hay sesión → pierdes fingerprint del atacante.

**Opción B - Mantener snapshot SOLO para intentos sin sesión**:
```java
// AuthAttempt.java
@ManyToOne(fetch = FetchType.LAZY)
private User user;

@ManyToOne(fetch = FetchType.LAZY, optional = true)
private Session session;

@Column(name = "att_dev_fp_snapshot", length = 255)
private String deviceFingerprintSnapshot;  // Solo si session = null

// Lógica al crear:
// if (session != null) → snapshot = session.device.fingerprint (para redundancia)
// if (session == null) → snapshot = fingerprint del request (único registro)
```

**Recomendación**: **Opción B** - Mantener snapshot porque es crítico para seguridad (detectar ataques de fuerza bruta desde dispositivos desconocidos).

---

### Redundancia 4: User ↔ Register ↔ Credencial (IDs Cruzadas) 🔴 CRÍTICA

**Problema Actual**:
```java
// User.java
@OneToOne(cascade = CascadeType.ALL)
@JoinColumn(name = "credencial_id", referencedColumnName = "creId")
public Credencial credencial;  // ← FK: user.credencial_id

@OneToOne
@JoinColumn(name = "register_id", referencedColumnName = "regId")
public Register register;  // ← FK: user.register_id

// Credencial.java
@OneToOne(mappedBy = "credencial")
private User user;  // ← Relación inversa

// Register.java
@OneToOne(mappedBy = "register")
private User user;  // ← Relación inversa
```

**Redundancia**:
```
app_user table:
├─ use_id (PK)
├─ credencial_id (FK) → credencial.cre_id
└─ register_id (FK) → register.reg_id

credencial table:
└─ cre_id (PK)

register table:
└─ reg_id (PK)
```

**Problema**: 3 PKs para un solo usuario → almacenamiento redundante

**Impacto**:
- Cada usuario = 3 IDs separados (use_id, credencial_id, register_id)
- 2 FKs adicionales en `app_user`
- Complejidad en consultas

**Solución**: Eliminar tablas separadas, mergear todo en `app_user`

---

### Redundancia 5: Change (Logs duplicados) 🟡 MODERADA

**Problema Actual**:
```java
// Change.java
@ManyToOne
@JoinColumn(name = "credencial_id", referencedColumnName = "creId")
public Credencial credencial;  // ← FK a Credencial

@ManyToOne
@JoinColumn(name = "register_id", referencedColumnName = "regId")
public Register register;  // ← FK a Register
```

**Redundancia**:
- Si Credencial y Register se mergean en User → estas FKs no tienen sentido
- Cada cambio referencia SOLO una de las dos, nunca ambas

**Solución**: Simplificar a FK única a User

```java
// Change.java (simplificado)
@ManyToOne
@JoinColumn(name = "use_id")
private User user;

@Column(name = "cha_entity_type")
private String entityType;  // "CREDENTIAL" | "REGISTER" | "USER"

@Column(name = "cha_field_changed")
private String fieldChanged;  // "email" | "publicKey" | "password"

@Column(name = "cha_old_value")
private String oldValue;

@Column(name = "cha_new_value")
private String newValue;
```

---

## 🎯 SOLUCIÓN INTEGRAL: MODELO SIN REDUNDANCIA

### Diseño Normalizado Propuesto

```
app_user (tabla única consolidada)
├─ use_id (PK)
├─ use_names
├─ use_last_names
├─ use_rut_general (UNIQUE)
├─ use_verification_digit
├─ use_birth_date
├─ use_phone_number
├─ use_profession
├─ use_state
├─ use_address
│
├─ Campos de Credencial (merge):
├─ use_private_key_rsa
├─ use_public_key_rsa
├─ use_key_creation_date
├─ use_key_denied
├─ use_active_dinamic_key
│
├─ Campos de Autenticación (merge):
├─ use_email (UNIQUE)
├─ use_hashed_password
├─ use_password_changed_at
├─ use_last_login_at
│
└─ Campos de Registro (merge):
    ├─ use_registered_at
    ├─ use_verified
    ├─ use_verification_code
    └─ use_verification_code_expiration
```

**Ventajas**:
- ✅ **0 JOINs** para login
- ✅ **1 tabla** en lugar de 3
- ✅ **1 PK** en lugar de 3
- ✅ **Sin FKs redundantes**
- ✅ **Transacciones atómicas** (todo en 1 fila)

---

## 📋 IMPLEMENTACIÓN: ELIMINAR REDUNDANCIAS

### Fase 1: Consolidar User + Credencial + Register

#### Paso 1.1: Modificar User.java

```java
package cl.ufro.dci.naivepayapi.registro.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "app_user")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class User {

    // ========== IDENTIFICACIÓN ==========
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "use_id")
    private Long useId;

    @Column(name = "use_names", nullable = false, length = 255)
    private String useNames;

    @Column(name = "use_last_names", nullable = false, length = 255)
    private String useLastNames;

    @Column(name = "use_rut_general", nullable = false, unique = true)
    private Long useRutGeneral;

    @Column(name = "use_verification_digit", nullable = false, length = 1)
    private Character useVerificationDigit;

    @Column(name = "use_birth_date", nullable = false)
    private LocalDate useBirthDate;

    @Column(name = "use_phone_number")
    private Long usePhoneNumber;

    @Column(name = "use_profession", length = 255)
    private String useProfession;

    @Enumerated(EnumType.STRING)
    @Column(name = "use_state", nullable = false, length = 20)
    private AccountState useState;

    @Column(name = "use_address", length = 500)
    private String useAddress;

    // ========== AUTENTICACIÓN (merge de Register) ==========
    @Column(name = "use_email", nullable = false, unique = true, length = 255)
    private String useEmail;

    @Column(name = "use_hashed_password", nullable = false, length = 255)
    private String useHashedPassword;

    @Column(name = "use_password_changed_at")
    private Instant usePasswordChangedAt;

    @Column(name = "use_last_login_at")
    private Instant useLastLoginAt;

    // ========== CREDENCIAL (merge de Credencial) ==========
    @Lob
    @Column(name = "use_private_key_rsa", columnDefinition = "TEXT")
    private String usePrivateKeyRsa;

    @Lob
    @Column(name = "use_public_key_rsa", columnDefinition = "TEXT")
    private String usePublicKeyRsa;

    @Column(name = "use_key_creation_date")
    private Instant useKeyCreationDate;

    @Column(name = "use_key_denied")
    private Boolean useKeyDenied = false;

    @Column(name = "use_active_dinamic_key")
    private Boolean useActiveDinamicKey = true;

    // ========== REGISTRO (merge de Register) ==========
    @Column(name = "use_registered_at", nullable = false)
    private Instant useRegisteredAt;

    @Column(name = "use_verified", nullable = false)
    private Boolean useVerified = false;

    @Column(name = "use_verification_code", length = 10)
    private String useVerificationCode;

    @Column(name = "use_verification_code_expiration")
    private Instant useVerificationCodeExpiration;

    // ========== RELACIONES (sin Credencial ni Register) ==========
    // ELIMINAR estas relaciones:
    // @OneToOne private Credencial credencial;  ❌
    // @OneToOne private Register register;      ❌
}
```

#### Paso 1.2: Eliminar Credencial.java

```bash
# Borrar archivo:
rm naive-pay-api/src/main/java/cl/ufro/dci/naivepayapi/registro/domain/Credencial.java
```

#### Paso 1.3: Eliminar Register.java

```bash
# Borrar archivo:
rm naive-pay-api/src/main/java/cl/ufro/dci/naivepayapi/registro/domain/Register.java
```

#### Paso 1.4: Actualizar Change.java

```java
package cl.ufro.dci.naivepayapi.registro.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "change_log")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Change {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cha_id")
    private Long chaId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "use_id", nullable = false)
    private User user;  // ✅ FK única a User

    @Column(name = "cha_date", nullable = false)
    private Instant chaDate;

    @Column(name = "cha_entity_type", length = 50)
    private String entityType;  // "CREDENTIAL" | "AUTH" | "PROFILE"

    @Column(name = "cha_field_changed", length = 100)
    private String fieldChanged;  // "email", "publicKey", "password", etc.

    @Column(name = "cha_old_value", columnDefinition = "TEXT")
    private String oldValue;

    @Column(name = "cha_new_value", columnDefinition = "TEXT")
    private String newValue;

    @Column(name = "cha_description", length = 500)
    private String description;

    // ELIMINAR:
    // @ManyToOne private Credencial credencial;  ❌
    // @ManyToOne private Register register;      ❌
}
```

---

### Fase 2: Optimizar Session (Eliminar/Mantener Snapshot)

#### Opción A: Sin Snapshot (simplicidad)

```java
// Session.java
@ManyToOne(fetch = FetchType.LAZY, optional = true)
@JoinColumn(name = "dev_id")
private Device device;

// ELIMINAR:
// @Column(name = "ses_dev_fp", length = 255)
// private String sesDeviceFingerprint;
```

#### Opción B: Con Snapshot (auditoría)

```java
// Session.java
@ManyToOne(fetch = FetchType.LAZY, optional = true)
@JoinColumn(name = "dev_id")
private Device device;

@Column(name = "ses_dev_fp_snapshot", length = 255)
private String deviceFingerprintSnapshot;  // Snapshot al crear sesión

// Al crear sesión:
public static Session create(User user, Device device, ...) {
    return Session.builder()
        .user(user)
        .device(device)
        .deviceFingerprintSnapshot(device != null ? device.getFingerprint() : null)  // ✅ Snapshot
        // ...
        .build();
}
```

**Recomendación**: **Opción B** (mantener snapshot para auditoría)

---

### Fase 3: Optimizar AuthAttempt (Mantener Snapshot para Seguridad)

```java
// AuthAttempt.java
@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "use_id", nullable = false)
private User user;

@ManyToOne(fetch = FetchType.LAZY, optional = true)
@JoinColumn(name = "ses_id")
private Session session;  // Puede ser null si login falló

@Column(name = "att_dev_fp_snapshot", length = 255)
private String deviceFingerprintSnapshot;  // ✅ MANTENER para seguridad

@Column(name = "att_success", nullable = false)
private boolean attSuccess;

@Enumerated(EnumType.STRING)
@Column(name = "att_reason", nullable = false, length = 40)
private AuthAttemptReason attReason;

@Column(name = "att_occurred", nullable = false)
private Instant attOccurred;

// Al crear intento:
public static AuthAttempt create(User user, Session session, String fingerprint, ...) {
    return AuthAttempt.builder()
        .user(user)
        .session(session)
        .deviceFingerprintSnapshot(fingerprint)  // ✅ Siempre guardar
        // ...
        .build();
}
```

**Justificación**: Necesario para detectar ataques de fuerza bruta desde dispositivos desconocidos.

---

## 📊 COMPARACIÓN: ANTES vs DESPUÉS

### Antes (Con Redundancia)

```
Tablas: 3
├─ app_user (8 campos + 2 FKs)
├─ credencial (6 campos)
└─ register (6 campos)

Total columnas: 20
JOINs para login: 2
Espacio por usuario: ~500 bytes (estimado)
```

### Después (Sin Redundancia)

```
Tablas: 1
└─ app_user (22 campos)

Total columnas: 22
JOINs para login: 0
Espacio por usuario: ~450 bytes (estimado)
Reducción: 10% espacio + 100% menos JOINs
```

---

## 🔧 SCRIPT DE MIGRACIÓN POSTGRESQL

### Crear Tabla Consolidada

```sql
-- 1. Crear nueva tabla app_user consolidada
CREATE TABLE app_user_new (
    -- IDENTIFICACIÓN
    use_id BIGSERIAL PRIMARY KEY,
    use_names VARCHAR(255) NOT NULL,
    use_last_names VARCHAR(255) NOT NULL,
    use_rut_general BIGINT NOT NULL UNIQUE,
    use_verification_digit CHAR(1) NOT NULL,
    use_birth_date DATE NOT NULL,
    use_phone_number BIGINT,
    use_profession VARCHAR(255),
    use_state VARCHAR(20) NOT NULL,
    use_address VARCHAR(500),

    -- AUTENTICACIÓN
    use_email VARCHAR(255) NOT NULL UNIQUE,
    use_hashed_password VARCHAR(255) NOT NULL,
    use_password_changed_at TIMESTAMP,
    use_last_login_at TIMESTAMP,

    -- CREDENCIAL
    use_private_key_rsa TEXT,
    use_public_key_rsa TEXT,
    use_key_creation_date TIMESTAMP,
    use_key_denied BOOLEAN DEFAULT FALSE,
    use_active_dinamic_key BOOLEAN DEFAULT TRUE,

    -- REGISTRO
    use_registered_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    use_verified BOOLEAN NOT NULL DEFAULT FALSE,
    use_verification_code VARCHAR(10),
    use_verification_code_expiration TIMESTAMP,

    CONSTRAINT chk_verification_digit
        CHECK (use_verification_digit IN ('0','1','2','3','4','5','6','7','8','9','K','k'))
);

-- 2. Migrar datos desde tablas antiguas
INSERT INTO app_user_new (
    use_id, use_names, use_last_names, use_rut_general, use_verification_digit,
    use_birth_date, use_phone_number, use_profession, use_state, use_address,
    use_email, use_hashed_password,
    use_private_key_rsa, use_public_key_rsa, use_key_creation_date, use_key_denied, use_active_dinamic_key,
    use_registered_at, use_verified, use_verification_code, use_verification_code_expiration
)
SELECT
    u.use_id, u.use_names, u.use_last_names, u.use_rut_general, u.use_verification_digit,
    u.use_birth_date, u.use_phone_number, u.use_profession, u.use_state, u.use_adress,
    r.reg_email, r.reg_hashed_login_password,
    c.cre_private_key_rsa, c.cre_public_key_rsa, c.cre_creation_date, c.cre_denied, c.cre_active_dinamic_key,
    r.reg_register_date, r.reg_verified, r.reg_verification_code, r.reg_verification_code_expiration
FROM app_user u
INNER JOIN credencial c ON u.credencial_id = c.cre_id
INNER JOIN register r ON u.register_id = r.reg_id;

-- 3. Actualizar FKs de otras tablas para apuntar a app_user_new
-- (Session, AuthAttempt, PasswordRecovery, DeviceRecovery ya apuntan a use_id, están OK)

-- 4. Eliminar tablas antiguas
DROP TABLE IF EXISTS change_log;  -- Primero eliminar tabla que referencia credencial/register
DROP TABLE IF EXISTS credencial;
DROP TABLE IF EXISTS register;

-- 5. Renombrar tabla nueva
DROP TABLE IF EXISTS app_user;
ALTER TABLE app_user_new RENAME TO app_user;

-- 6. Crear índices
CREATE INDEX idx_user_email ON app_user(use_email);
CREATE INDEX idx_user_rut ON app_user(use_rut_general);
CREATE INDEX idx_user_state ON app_user(use_state);
CREATE INDEX idx_user_verified ON app_user(use_verified);

-- 7. Recrear tabla change_log simplificada
CREATE TABLE change_log (
    cha_id BIGSERIAL PRIMARY KEY,
    use_id BIGINT NOT NULL,
    cha_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cha_entity_type VARCHAR(50),
    cha_field_changed VARCHAR(100),
    cha_old_value TEXT,
    cha_new_value TEXT,
    cha_description VARCHAR(500),

    CONSTRAINT fk_change_user FOREIGN KEY (use_id)
        REFERENCES app_user(use_id) ON DELETE CASCADE
);

CREATE INDEX idx_change_user_date ON change_log(use_id, cha_date DESC);
```

---

## ✅ RESUMEN DE REDUNDANCIAS ELIMINADAS

| Redundancia | Antes | Después | Ahorro |
|-------------|-------|---------|--------|
| **Tablas para User** | 3 (User, Credencial, Register) | 1 (User) | 66% menos tablas |
| **JOINs en login** | 2 JOINs | 0 JOINs | 100% menos JOINs |
| **PKs por usuario** | 3 IDs | 1 ID | 66% menos overhead |
| **FKs en app_user** | 2 FKs (credencial_id, register_id) | 0 FKs | 100% menos FKs |
| **Consultas simplificadas** | 3 tablas | 1 tabla | ✅ |
| **Transacciones atómicas** | 3 INSERTs | 1 INSERT | ✅ |

---

## 🚀 PRÓXIMOS PASOS

### Opción 1: Implementar Todo (Recomendado)
1. ✅ Consolidar User + Credencial + Register → 1 tabla
2. ✅ Mantener snapshots en Session y AuthAttempt (auditoría/seguridad)
3. ✅ Simplificar Change a FK única
4. ✅ Migrar datos con script SQL
5. ✅ Actualizar servicios (AuthService, RegisterService, etc.)

### Opción 2: Solo Crítico (Rápido)
1. ✅ Consolidar User + Credencial + Register
2. ⚠️ Dejar snapshots como están (sin optimizar)

**¿Quieres que implemente la consolidación ahora?** 🛠️

Puedo:
- Modificar las entidades Java
- Generar el script SQL de migración
- Actualizar los servicios afectados

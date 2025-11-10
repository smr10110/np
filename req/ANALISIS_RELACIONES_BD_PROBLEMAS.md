# Análisis de Relaciones de Base de Datos: Registro, Autentificación y Dispositivos

## Análisis como Experto en Bases de Datos

### Estado Actual del Modelo

```
User (app_user)
├─ 1:1 → Credencial
├─ 1:1 → Register
├─ 1:1 ← Device (optional = false)
├─ 1:N ← Session (optional = false)
├─ 1:N ← AuthAttempt
├─ 1:N ← PasswordRecovery (optional = false)
└─ 1:N ← DeviceRecovery (optional = false)
```

---

## 🚨 PROBLEMAS CRÍTICOS IDENTIFICADOS

### Problema 1: Relación Incorrecta User ↔ Register ❌

**Estado Actual**:
```java
// User.java
@OneToOne
@JoinColumn(name = "register_id", referencedColumnName = "regId")
public Register register;

// Register.java
@OneToOne(mappedBy = "register")
private User user;
```

**Diagnóstico**: ❌ **RELACIÓN BIDIRECCIONAL MAL CONFIGURADA**

#### Problemas:
1. **Ambigüedad**: No está claro cuál tabla tiene la FK
2. **User tiene la FK** (`register_id`) pero Register también mapea con `mappedBy`
3. **Inconsistencia conceptual**: ¿Qué entidad es la dueña de la relación?

#### Impacto al migrar a PostgreSQL:
```sql
-- Tabla user tendrá:
CREATE TABLE app_user (
    use_id BIGSERIAL PRIMARY KEY,
    register_id BIGINT,  -- ❌ FK duplicada e innecesaria
    ...
);

-- Tabla register:
CREATE TABLE register (
    reg_id BIGSERIAL PRIMARY KEY,
    ...
    -- ❌ No tiene FK, pero User sí (confuso)
);
```

#### Solución Recomendada:

**Opción A - Register es hijo de User** (RECOMENDADA):
```java
// User.java
@OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
private Register register;

// Register.java
@OneToOne(optional = false)
@JoinColumn(name = "use_id", nullable = false, unique = true)
private User user;
```

**Justificación**:
- Register ES el proceso de registro DE UN usuario
- User existe primero (conceptualmente)
- Register almacena datos del proceso de registro

**SQL generado**:
```sql
CREATE TABLE register (
    reg_id BIGSERIAL PRIMARY KEY,
    use_id BIGINT NOT NULL UNIQUE,
    ...
    CONSTRAINT fk_register_user FOREIGN KEY (use_id) REFERENCES app_user(use_id)
);
```

---

### Problema 2: Relación User ↔ Credencial Redundante ❌

**Estado Actual**:
```java
// User.java
@OneToOne(cascade = CascadeType.ALL)
@JoinColumn(name = "credencial_id", referencedColumnName = "creId")
public Credencial credencial;

// Credencial.java
@OneToOne(mappedBy = "credencial")
private User user;
```

**Diagnóstico**: ❌ **SEPARACIÓN INNECESARIA - CANDIDATO A MERGE**

#### Problemas:
1. **User y Credencial son 1:1 obligatorio** → No hay razón para separarlos
2. **Credencial siempre existe con User** → No tiene sentido independiente
3. **Genera JOINs innecesarios** en cada consulta de autenticación
4. **Complejidad adicional** sin beneficio

#### Impacto en rendimiento:
```sql
-- Cada login requiere JOIN:
SELECT u.*, c.cre_public_key_rsa, c.cre_private_key_rsa
FROM app_user u
INNER JOIN credencial c ON u.credencial_id = c.cre_id
WHERE u.use_id = ?;

-- vs diseño optimizado (sin JOIN)
SELECT use_id, use_public_key_rsa, use_private_key_rsa
FROM app_user
WHERE use_id = ?;
```

#### Solución Recomendada:

**Opción B - Merge Credencial en User** (RECOMENDADA):
```java
// User.java - Eliminar relación, agregar campos directamente:
@Lob
@Column(name = "use_private_key_rsa")
private String usePrivateKeyRsa;

@Lob
@Column(name = "use_public_key_rsa")
private String usePublicKeyRsa;

@Column(name = "use_cred_creation_date")
private Instant useCredCreationDate;

@Column(name = "use_cred_denied")
private Boolean useCredDenied;

@Column(name = "use_active_dinamic_key")
private Boolean useActiveDinamicKey;

// ELIMINAR tabla Credencial completamente
```

**Justificación**:
- **Sin relación 1:1 obligatoria** → No se necesita tabla separada
- **Mejor rendimiento**: 0 JOINs en autenticación
- **Simplicidad**: Menos tablas, menos complejidad
- **Integridad**: Todo en una transacción

---

### Problema 3: Relación User ↔ Device Incorrecta ❌

**Estado Actual**:
```java
// Device.java
@OneToOne(optional = false)
@JoinColumn(name = "useId", foreignKey = @ForeignKey(name = "fk_dev_user"))
private User user;
```

**Diagnóstico**: ❌ **RELACIÓN 1:1 PERO DEBERÍA SER 1:N (Múltiples Devices Históricos)**

#### Problemas:

1. **Requisito de Naive-Pay**:
   > "usuario vinculará un único dispositivo... En caso de registrar un nuevo dispositivo, el dispositivo anterior quedará bloqueado"

2. **Problema con 1:1**:
   - No puedes mantener histórico de dispositivos anteriores
   - Al cambiar dispositivo, ¿borras el anterior? ❌ Pierdes auditoría
   - ¿Cómo sabes cuándo cambió de dispositivo?

3. **Device usa `fingerprint` como PK**:
   - ¿Qué pasa si mismo fingerprint se registra 2 veces? (usuario elimina y vuelve a registrar)
   - PK natural vs surrogate key

#### Solución Recomendada:

**Opción C - Cambiar a 1:N con campo `active`** (RECOMENDADA):

```java
// User.java
@OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
private List<Device> devices = new ArrayList<>();

// Método helper
public Optional<Device> getActiveDevice() {
    return devices.stream()
        .filter(Device::isActive)
        .findFirst();
}

// Device.java
@Entity
@Table(name = "device")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dev_id")
    private Long devId;  // ✅ Surrogate key

    @Column(name = "dev_fingerprint", length = 100, nullable = false)
    private String fingerprint;  // ✅ Ya NO es PK

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "use_id", nullable = false)
    private User user;  // ✅ Cambio de OneToOne a ManyToOne

    @Column(name = "dev_active", nullable = false)
    private boolean active = true;  // ✅ Campo nuevo para device activo

    @Column(name = "dev_blocked_at")
    private Instant blockedAt;  // ✅ Cuándo se bloqueó

    // ... resto de campos existentes
}
```

**Constraint único**:
```java
@Table(name = "device",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_user_active_device",
        columnNames = {"use_id", "dev_active"}
    )
)
// Esto garantiza que solo 1 device por usuario tenga active = true
```

**SQL generado**:
```sql
CREATE TABLE device (
    dev_id BIGSERIAL PRIMARY KEY,
    dev_fingerprint VARCHAR(100) NOT NULL,
    use_id BIGINT NOT NULL,
    dev_active BOOLEAN NOT NULL DEFAULT TRUE,
    dev_blocked_at TIMESTAMP,
    ...
    CONSTRAINT fk_dev_user FOREIGN KEY (use_id) REFERENCES app_user(use_id),
    CONSTRAINT uk_user_active_device UNIQUE (use_id, dev_active) WHERE dev_active = TRUE
);

CREATE INDEX idx_device_user_active ON device(use_id, dev_active);
```

**Ventajas**:
- ✅ Histórico completo de dispositivos
- ✅ Auditoría: ves cuándo cambió de device
- ✅ Solo 1 device activo garantizado por constraint
- ✅ No pierdes datos al cambiar dispositivo

---

### Problema 4: Session ↔ Device con Referencia Débil ⚠️

**Estado Actual**:
```java
// Session.java
@ManyToOne(fetch = FetchType.LAZY, optional = true)
@JoinColumn(name = "dev_fingerprint", referencedColumnName = "dev_fingerprint")
private Device device;

@Column(name = "ses_dev_fp", length = 255)
private String sesDeviceFingerprint;  // Snapshot
```

**Diagnóstico**: ⚠️ **DISEÑO INCONSISTENTE CON PROBLEMA 3**

#### Problemas:

1. **FK apunta a `dev_fingerprint`** (que actualmente es PK)
2. Si cambias Device a tener `dev_id` como PK → esta relación se rompe
3. **Snapshot redundante**: `sesDeviceFingerprint` duplica info

#### Solución Recomendada:

**Después de aplicar Solución C del Problema 3**:

```java
// Session.java
@ManyToOne(fetch = FetchType.LAZY, optional = true)
@JoinColumn(name = "dev_id")  // ✅ Cambiar a dev_id (surrogate key)
private Device device;

@Column(name = "ses_dev_fp_snapshot", length = 255)
private String sesDeviceFingerprintSnapshot;  // ✅ Renombrar para claridad

// Eliminar campo duplicado o mantener snapshot para auditoría histórica
```

**SQL**:
```sql
CREATE TABLE session (
    ses_id BIGSERIAL PRIMARY KEY,
    use_id BIGINT NOT NULL,
    dev_id BIGINT,  -- ✅ Cambio: FK a device.dev_id
    ses_dev_fp_snapshot VARCHAR(255),  -- Snapshot para auditoría
    ...
    CONSTRAINT fk_session_device FOREIGN KEY (dev_id) REFERENCES device(dev_id) ON DELETE SET NULL
);
```

---

### Problema 5: Nomenclatura Inconsistente de PKs/FKs ⚠️

**Estado Actual**:
```java
// User.java
@JoinColumn(name = "use_id", referencedColumnName = "useId")  // ❌ Inconsistente

// Session.java
@JoinColumn(name = "use_id", nullable = false, referencedColumnName = "useId")  // ✅ Correcto

// DeviceRecovery.java
@JoinColumn(name = "useId", ...)  // ❌ Inconsistente (sin guion bajo)
```

**Diagnóstico**: ⚠️ **CONVENCIÓN DE NOMBRES INCONSISTENTE**

#### Problemas:
1. Unas veces usa `use_id`, otras `useId`
2. En PostgreSQL, nombres sensibles a mayúsculas requieren comillas dobles
3. Dificulta queries manuales

#### Solución Recomendada:

**Estandarizar en snake_case**:
```java
// Todas las FK deben ser:
@JoinColumn(name = "use_id", referencedColumnName = "use_id")
```

---

### Problema 6: Register tiene Datos de Autenticación ❌

**Estado Actual**:
```java
// Register.java
private String regEmail;
private String regHashedLoginPassword;  // ❌ Password en tabla de registro
```

**Diagnóstico**: ❌ **SEPARACIÓN DE RESPONSABILIDADES VIOLADA**

#### Problemas:
1. **Register** es el módulo de REGISTRO (proceso de creación de cuenta)
2. **Password** es dato de AUTENTICACIÓN (módulo autentificación)
3. **Violación de SRP**: Register no debería manejar autenticación

#### Solución Recomendada:

**Opción D - Crear tabla Authentication** (RECOMENDADA):

```java
// Nueva entidad: Authentication.java
@Entity
@Table(name = "authentication")
@Getter @Setter
public class Authentication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "auth_id")
    private Long authId;

    @OneToOne(optional = false)
    @JoinColumn(name = "use_id", nullable = false, unique = true)
    private User user;

    @Column(name = "auth_email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "auth_hashed_password", nullable = false, length = 255)
    private String hashedPassword;

    @Column(name = "auth_password_changed_at")
    private Instant passwordChangedAt;

    @Column(name = "auth_last_login_at")
    private Instant lastLoginAt;
}

// User.java
@OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
private Authentication authentication;

// Register.java - ELIMINAR campos de autenticación:
// ❌ private String regEmail;
// ❌ private String regHashedLoginPassword;
```

**Justificación**:
- ✅ Separación clara: Register = proceso, Authentication = credenciales
- ✅ Módulo autentificación autocontenido
- ✅ Facilita cambios futuros (ej: agregar 2FA)
- ✅ Mejor seguridad: datos sensibles aislados

---

## 📊 RESUMEN DE PROBLEMAS Y SOLUCIONES

| # | Problema | Severidad | Solución | Impacto BD |
|---|----------|-----------|----------|------------|
| 1 | User ↔ Register relación mal configurada | 🔴 CRÍTICO | Register tiene FK a User | 1 tabla modificada |
| 2 | Credencial separada innecesariamente | 🟡 MEDIO | Merge en User | 1 tabla eliminada |
| 3 | Device como 1:1 sin histórico | 🔴 CRÍTICO | Cambiar a 1:N con `active` | 1 tabla modificada |
| 4 | Session → Device FK incorrecta | 🟡 MEDIO | FK a dev_id en vez de fingerprint | 1 tabla modificada |
| 5 | Nomenclatura inconsistente | 🟢 BAJO | Estandarizar snake_case | Todas las FKs |
| 6 | Register con datos de auth | 🟡 MEDIO | Crear tabla Authentication | 1 tabla nueva |

---

## 🎯 MODELO CORREGIDO PROPUESTO

### Estructura Recomendada

```
app_user (tabla central)
├─ 1:1 ← authentication (datos de login)
├─ 1:1 ← register (datos de proceso de registro)
├─ 1:N ← device (histórico de dispositivos, 1 activo)
├─ 1:N ← session (sesiones activas/cerradas)
├─ 1:N ← auth_attempt (intentos de autenticación)
├─ 1:N ← password_recovery (recuperaciones de contraseña)
└─ 1:N ← device_recovery (recuperaciones de dispositivo)

Campos de Credencial → Movidos a app_user directamente
```

---

## 🔧 IMPLEMENTACIÓN: ORDEN DE CAMBIOS

### Fase 1: Correcciones Críticas (Impactan Lógica)

#### 1.1 Arreglar User ↔ Register
```java
// Register.java
@OneToOne(optional = false)
@JoinColumn(name = "use_id", nullable = false, unique = true)
private User user;

// User.java
@OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
private Register register;
```

#### 1.2 Cambiar Device a 1:N con histórico
```java
// Device.java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long devId;  // Nuevo PK

@Column(name = "dev_fingerprint")
private String fingerprint;  // Ya NO es PK

@ManyToOne
@JoinColumn(name = "use_id")
private User user;

@Column(name = "dev_active")
private boolean active = true;

// User.java
@OneToMany(mappedBy = "user")
private List<Device> devices = new ArrayList<>();
```

#### 1.3 Actualizar Session → Device FK
```java
// Session.java
@ManyToOne(fetch = FetchType.LAZY, optional = true)
@JoinColumn(name = "dev_id")  // Cambio de dev_fingerprint a dev_id
private Device device;
```

---

### Fase 2: Mejoras de Diseño (Opcionales pero Recomendadas)

#### 2.1 Merge Credencial en User
```java
// User.java - Agregar campos:
@Lob private String usePrivateKeyRsa;
@Lob private String usePublicKeyRsa;
private Instant useCredCreationDate;
private Boolean useCredDenied;
private Boolean useActiveDinamicKey;

// Eliminar tabla Credencial
// Migrar datos: INSERT INTO app_user SELECT * FROM credencial JOIN app_user...
```

#### 2.2 Crear tabla Authentication
```java
// Nuevo archivo: Authentication.java
@Entity
@Table(name = "authentication")
public class Authentication {
    @Id @GeneratedValue private Long authId;
    @OneToOne @JoinColumn(name = "use_id") private User user;
    private String email;
    private String hashedPassword;
    private Instant passwordChangedAt;
    private Instant lastLoginAt;
}

// Migrar datos de Register.regEmail y Register.regHashedLoginPassword
```

#### 2.3 Estandarizar nomenclatura
```sql
-- Renombrar columnas en PostgreSQL:
ALTER TABLE device_recovery RENAME COLUMN "useId" TO use_id;
-- ... etc para todas las inconsistencias
```

---

## 📋 SCRIPT DE MIGRACIÓN POSTGRESQL

### Crear Tablas Corregidas

```sql
-- 1. Tabla central User (sin cambios en estructura, solo nomenclatura)
CREATE TABLE app_user (
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
    -- Campos de Credencial (merge):
    use_private_key_rsa TEXT,
    use_public_key_rsa TEXT,
    use_cred_creation_date TIMESTAMP,
    use_cred_denied BOOLEAN DEFAULT FALSE,
    use_active_dinamic_key BOOLEAN DEFAULT TRUE,

    CONSTRAINT chk_verification_digit CHECK (use_verification_digit IN ('0','1','2','3','4','5','6','7','8','9','K','k'))
);

-- 2. Tabla Register (FK a User)
CREATE TABLE register (
    reg_id BIGSERIAL PRIMARY KEY,
    use_id BIGINT NOT NULL UNIQUE,  -- ✅ FK a User
    reg_register_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reg_verified BOOLEAN NOT NULL DEFAULT FALSE,
    reg_verification_code VARCHAR(10),
    reg_verification_code_expiration TIMESTAMP,

    CONSTRAINT fk_register_user FOREIGN KEY (use_id)
        REFERENCES app_user(use_id) ON DELETE CASCADE
);

-- 3. Tabla Authentication (nueva)
CREATE TABLE authentication (
    auth_id BIGSERIAL PRIMARY KEY,
    use_id BIGINT NOT NULL UNIQUE,
    auth_email VARCHAR(255) NOT NULL UNIQUE,
    auth_hashed_password VARCHAR(255) NOT NULL,
    auth_password_changed_at TIMESTAMP,
    auth_last_login_at TIMESTAMP,

    CONSTRAINT fk_auth_user FOREIGN KEY (use_id)
        REFERENCES app_user(use_id) ON DELETE CASCADE
);

CREATE INDEX idx_auth_email ON authentication(auth_email);

-- 4. Tabla Device (1:N con histórico)
CREATE TABLE device (
    dev_id BIGSERIAL PRIMARY KEY,  -- ✅ Nuevo surrogate key
    use_id BIGINT NOT NULL,
    dev_fingerprint VARCHAR(100) NOT NULL,
    dev_type VARCHAR(100) NOT NULL,
    dev_os VARCHAR(100) NOT NULL,
    dev_browser VARCHAR(100) NOT NULL,
    dev_reg_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    dev_last_login TIMESTAMP,
    dev_active BOOLEAN NOT NULL DEFAULT TRUE,  -- ✅ Campo nuevo
    dev_blocked_at TIMESTAMP,  -- ✅ Campo nuevo

    CONSTRAINT fk_dev_user FOREIGN KEY (use_id)
        REFERENCES app_user(use_id) ON DELETE CASCADE
);

-- Constraint parcial: solo 1 device activo por usuario
CREATE UNIQUE INDEX uk_user_active_device
    ON device(use_id)
    WHERE dev_active = TRUE;

CREATE INDEX idx_device_user ON device(use_id);
CREATE INDEX idx_device_fingerprint ON device(dev_fingerprint);

-- 5. Tabla Session (FK actualizada)
CREATE TABLE session (
    ses_id BIGSERIAL PRIMARY KEY,
    ses_jti UUID NOT NULL UNIQUE,
    use_id BIGINT NOT NULL,
    dev_id BIGINT,  -- ✅ Cambio: FK a device.dev_id
    ses_dev_fp_snapshot VARCHAR(255),  -- Snapshot para auditoría
    ses_created TIMESTAMP NOT NULL,
    ses_expires TIMESTAMP NOT NULL,
    ses_closed TIMESTAMP,
    ses_status VARCHAR(16) NOT NULL,

    CONSTRAINT fk_session_user FOREIGN KEY (use_id)
        REFERENCES app_user(use_id) ON DELETE CASCADE,
    CONSTRAINT fk_session_device FOREIGN KEY (dev_id)
        REFERENCES device(dev_id) ON DELETE SET NULL
);

CREATE INDEX idx_session_jti ON session(ses_jti);
CREATE INDEX idx_session_user_status ON session(use_id, ses_status);

-- 6. Tabla AuthAttempt
CREATE TABLE attempt_auth (
    att_id BIGSERIAL PRIMARY KEY,
    use_id BIGINT NOT NULL,
    att_dev_fp VARCHAR(255),
    ses_id BIGINT,
    att_success BOOLEAN NOT NULL,
    att_reason VARCHAR(40) NOT NULL,
    att_occurred TIMESTAMP NOT NULL,

    CONSTRAINT fk_attempt_user FOREIGN KEY (use_id)
        REFERENCES app_user(use_id) ON DELETE CASCADE,
    CONSTRAINT fk_attempt_session FOREIGN KEY (ses_id)
        REFERENCES session(ses_id) ON DELETE SET NULL
);

CREATE INDEX idx_attempt_user_time ON attempt_auth(use_id, att_occurred DESC);

-- 7. Tabla PasswordRecovery
CREATE TABLE password_recovery (
    pas_id BIGSERIAL PRIMARY KEY,
    use_id BIGINT NOT NULL,
    pas_code VARCHAR(40) NOT NULL,
    pas_created TIMESTAMP NOT NULL,
    pas_expired TIMESTAMP NOT NULL,
    pas_last_sent TIMESTAMP,
    pas_used TIMESTAMP,
    pas_resend_count INT NOT NULL DEFAULT 0,
    pas_status VARCHAR(16) NOT NULL,

    CONSTRAINT fk_password_recovery_user FOREIGN KEY (use_id)
        REFERENCES app_user(use_id) ON DELETE CASCADE
);

CREATE INDEX idx_pass_recovery_user_status ON password_recovery(use_id, pas_status);

-- 8. Tabla DeviceRecovery
CREATE TABLE device_recovery (
    dev_recover_id UUID PRIMARY KEY,
    use_id BIGINT NOT NULL,  -- ✅ Estandarizado a use_id
    dev_rec_fp VARCHAR(100) NOT NULL,
    dev_rec_code VARCHAR(6) NOT NULL,
    dev_rec_status VARCHAR(16) NOT NULL,
    dev_rec_requested TIMESTAMP NOT NULL,
    dev_rec_expire TIMESTAMP NOT NULL,
    dev_rec_verified TIMESTAMP,

    CONSTRAINT fk_device_recovery_user FOREIGN KEY (use_id)
        REFERENCES app_user(use_id) ON DELETE CASCADE
);

CREATE INDEX idx_dev_recovery_user ON device_recovery(use_id);

-- 9. Tabla Change (para auditoría)
CREATE TABLE change_log (
    cha_id BIGSERIAL PRIMARY KEY,
    use_id BIGINT,
    credencial_id BIGINT,  -- ⚠️ Eliminar si mergeamos Credencial
    register_id BIGINT,
    cha_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cha_description TEXT,

    CONSTRAINT fk_change_user FOREIGN KEY (use_id)
        REFERENCES app_user(use_id) ON DELETE CASCADE,
    CONSTRAINT fk_change_register FOREIGN KEY (register_id)
        REFERENCES register(reg_id) ON DELETE CASCADE
);
```

---

## ✅ CHECKLIST DE VALIDACIÓN

Antes de migrar a PostgreSQL, verifica:

### Estructura
- [ ] Todas las relaciones 1:1 tienen sentido (o se pueden mergear)
- [ ] Todas las FK están correctamente definidas
- [ ] No hay dependencias circulares
- [ ] PKs son consistentes (preferir surrogate keys)

### Nomenclatura
- [ ] Todos los nombres de columna en snake_case
- [ ] FKs siguen convención: `{tabla}_id`
- [ ] Nombres descriptivos y consistentes

### Integridad
- [ ] Constraints de NOT NULL correctos
- [ ] Constraints UNIQUE donde corresponda
- [ ] ON DELETE CASCADE/SET NULL apropiados
- [ ] Índices en FKs y columnas de búsqueda frecuente

### Rendimiento
- [ ] Sin JOINs innecesarios (merge tablas 1:1 obligatorias)
- [ ] Índices compuestos donde sea necesario
- [ ] FKs indexadas automáticamente

---

## 🚀 PRÓXIMOS PASOS RECOMENDADOS

1. **Revisar este análisis** con tu equipo
2. **Decidir qué problemas corregir**:
   - Mínimo: Problemas 1, 3, 4 (críticos)
   - Recomendado: Todos (diseño óptimo)
3. **Crear branch de refactoring**: `feature/db-relationship-fix`
4. **Implementar cambios por fases**
5. **Migrar datos de H2 a PostgreSQL**
6. **Validar con tests end-to-end**

¿Quieres que implemente las correcciones ahora? 🛠️

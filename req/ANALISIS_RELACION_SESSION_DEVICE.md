# Análisis: ¿Qué Relación Debería Haber Entre Session y Device?

## Pregunta Clave

**¿Cuál es la cardinalidad correcta entre Session y Device?**

Opciones:
- A) `Session` N:1 `Device` (Muchas sesiones, 1 device) - **ACTUAL**
- B) `Session` 1:1 `Device` (1 sesión, 1 device único)
- C) `Session` N:M `Device` (Muchas sesiones, muchos devices)
- D) Sin relación JPA (solo `deviceId` o `deviceFingerprint`)

---

## 🔍 ANÁLISIS DEL CONTEXTO NAIVE-PAY

### Requisito de Naive-Pay

> "Para acceder a la App Naive-Pay, el usuario deberá ingresar su clave secreta y **su acceso estará restringido sólo al equipo registrado por el usuario**"

> "usuario vinculará **un único dispositivo**"

### Flujo de Uso Real

```
1. Usuario registra Device A
   ↓
2. Usuario hace login desde Device A
   → Sesión 1 creada (device = Device A)
   ↓
3. Usuario hace logout
   → Sesión 1 cerrada (device = Device A)
   ↓
4. Usuario hace login nuevamente desde Device A
   → Sesión 2 creada (device = Device A)
   ↓
5. Usuario registra nuevo Device B (Device A bloqueado)
   → Device A desacoplado de sesiones históricas
   ↓
6. Sesiones 1 y 2 quedan con device = null (auditoría)
```

### Preguntas Críticas

**¿Cuántas sesiones puede tener 1 device?**
- ✅ **MUCHAS**: Usuario puede hacer login/logout múltiples veces desde mismo device

**¿Una sesión puede tener múltiples devices?**
- ❌ **NO**: Cada sesión es creada desde UN solo device

**¿Una sesión puede existir sin device?**
- ✅ **SÍ** (histórico): Cuando device es reemplazado, sesiones antiguas quedan con device = null

---

## 📊 ANÁLISIS DE OPCIONES

### Opción A: Session N:1 Device (ACTUAL)

```java
// Session.java
@ManyToOne(fetch = FetchType.LAZY, optional = true)
@JoinColumn(name = "dev_fingerprint", referencedColumnName = "dev_fingerprint")
private Device device;

// Device.java (relación inversa)
@OneToMany(mappedBy = "device")
private List<Session> sessions = new ArrayList<>();
```

**Diagrama**:
```
Device "abc123"
├─ Session #1 (2024-01-10, CLOSED)
├─ Session #2 (2024-01-15, CLOSED)
├─ Session #3 (2024-01-20, ACTIVE)
└─ Session #4 (2024-01-25, ACTIVE)
```

**Ventajas**:
- ✅ Realista: 1 device puede tener múltiples sesiones
- ✅ Auditoría: Ves todas las sesiones de un device
- ✅ Navegación bidireccional: `device.getSessions()` útil
- ✅ Permite device = null (sesiones históricas)

**Desventajas**:
- ⚠️ Device es PK natural (`dev_fingerprint`) → problemático
- ⚠️ Si cambias Device a surrogate key (`dev_id`), rompe FK actual

**Cardinalidad**: ✅ **CORRECTA** (muchas sesiones por device)

---

### Opción B: Session 1:1 Device

```java
// Session.java
@OneToOne(fetch = FetchType.LAZY, optional = true)
@JoinColumn(name = "dev_id", unique = true)
private Device device;

// Device.java
@OneToOne(mappedBy = "device")
private Session session;
```

**Diagrama**:
```
Device "abc123" ←→ Session #1 (única sesión)
Device "xyz789" ←→ Session #2 (única sesión)
```

**Ventajas**:
- ✅ Sin duplicados: Device solo tiene 1 sesión

**Desventajas**:
- ❌ **INCORRECTA**: Usuario puede hacer login múltiples veces desde mismo device
- ❌ Solo 1 sesión activa posible por device (no realista)
- ❌ No permite histórico (cada login sobrescribe sesión anterior)

**Cardinalidad**: ❌ **INCORRECTA** para Naive-Pay

---

### Opción C: Session N:M Device

```java
// Session.java
@ManyToMany
@JoinTable(
    name = "session_device",
    joinColumns = @JoinColumn(name = "ses_id"),
    inverseJoinColumns = @JoinColumn(name = "dev_id")
)
private Set<Device> devices;

// Device.java
@ManyToMany(mappedBy = "devices")
private Set<Session> sessions;
```

**Ventajas**:
- ✅ Máxima flexibilidad

**Desventajas**:
- ❌ **OVERKILL**: Una sesión NUNCA tiene múltiples devices
- ❌ Complejidad innecesaria (tabla intermedia)
- ❌ No refleja realidad del negocio

**Cardinalidad**: ❌ **INCORRECTA** (sesión solo tiene 1 device)

---

### Opción D: Sin Relación JPA (deviceId o deviceFingerprint)

```java
// Session.java
@Column(name = "dev_id")
private Long deviceId;  // Solo el ID, sin relación

// O bien
@Column(name = "dev_fingerprint")
private String deviceFingerprint;  // Solo el fingerprint
```

**Ventajas**:
- ✅ Desacoplamiento máximo
- ✅ Sin lazy loading
- ✅ Performance óptima

**Desventajas**:
- ❌ **Pierdes navegación útil**: No puedes hacer `session.getDevice().getOs()`
- ❌ Sin metadata de device (os, browser, type)
- ❌ Queries manuales siempre

**Decisión**: ❌ **No recomendada** (pierdes info útil de device)

---

## 🚨 PROBLEMA ACTUAL: Device con PK Natural

### Estado Actual (Problemático)

```java
// Device.java
@Id
@Column(name = "dev_fingerprint", length = 100, nullable = false)
private String fingerprint;  // ← PK NATURAL

// Session.java
@ManyToOne
@JoinColumn(name = "dev_fingerprint", referencedColumnName = "dev_fingerprint")
private Device device;  // ← FK a PK natural
```

**Problemas**:
1. ❌ **PK natural mutable**: Si fingerprint cambia, FK se rompe
2. ❌ **VARCHAR como PK**: Performance pobre vs BIGINT
3. ❌ **FK VARCHAR**: Índices más lentos
4. ❌ **Desacoplamiento complejo**: Requiere UPDATE con string matching

### Solución: Device con Surrogate Key

```java
// Device.java (MEJORADO)
@Entity
@Table(name = "device")
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dev_id")
    private Long devId;  // ← SURROGATE KEY (PK)

    @Column(name = "dev_fingerprint", length = 100, nullable = false)
    private String fingerprint;  // ← Ya NO es PK

    @ManyToOne(optional = false)
    @JoinColumn(name = "use_id", nullable = false)
    private User user;

    // ... otros campos
}

// Session.java (MEJORADO)
@ManyToOne(fetch = FetchType.LAZY, optional = true)
@JoinColumn(name = "dev_id")  // ← FK a dev_id (BIGINT)
private Device device;
```

**Ventajas del Surrogate Key**:
- ✅ PK inmutable (Long)
- ✅ Performance: BIGINT vs VARCHAR(100)
- ✅ Índices más eficientes
- ✅ Desacoplamiento simple: `SET dev_id = NULL`

---

## 🎯 RECOMENDACIÓN FINAL

### ✅ Relación Correcta: **Session N:1 Device** (ManyToOne)

**Pero con mejoras**:

```java
// Device.java (MEJORADO)
@Entity
@Table(name = "device")
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dev_id")
    private Long devId;  // ✅ Surrogate key

    @Column(name = "dev_fingerprint", length = 100, nullable = false, unique = true)
    private String fingerprint;  // ✅ Unique pero NO PK

    @Column(name = "use_id", nullable = false)
    private Long userId;  // ✅ Sin relación JPA (como acordamos con User)

    @Column(name = "dev_type", nullable = false, length = 100)
    private String type;

    @Column(name = "dev_os", nullable = false, length = 100)
    private String os;

    @Column(name = "dev_browser", nullable = false, length = 100)
    private String browser;

    @Column(name = "dev_reg_date", nullable = false)
    private Instant registeredAt;

    @Column(name = "dev_last_login")
    private Instant lastLoginAt;

    @Column(name = "dev_active", nullable = false)
    private boolean active = true;  // ✅ Para múltiples devices (1:N)

    @Column(name = "dev_blocked_at")
    private Instant blockedAt;

    // SIN relación @OneToMany a Session (unidireccional)
}

// Session.java (MEJORADO)
@Entity
@Table(name = "session")
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ses_id")
    private Long sesId;

    @Column(name = "use_id", nullable = false)
    private Long userId;  // ✅ Sin relación JPA a User

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "dev_id")  // ✅ FK a dev_id (surrogate key)
    private Device device;

    @Column(name = "ses_dev_fp_snapshot", length = 255)
    private String deviceFingerprintSnapshot;  // ✅ Snapshot para auditoría

    @Column(name = "ses_jti", nullable = false, unique = true)
    private UUID sesJti;

    @Column(name = "ses_created", nullable = false)
    private Instant sesCreated;

    @Column(name = "ses_expires", nullable = false)
    private Instant sesExpires;

    @Column(name = "ses_closed")
    private Instant sesClosed;

    @Enumerated(EnumType.STRING)
    @Column(name = "ses_status", nullable = false, length = 16)
    private SessionStatus status;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AuthAttempt> attempts = new ArrayList<>();
}
```

---

## 📋 JUSTIFICACIÓN DE CADA DECISIÓN

### 1. Session N:1 Device (ManyToOne)

**Razón**: Un device puede tener MUCHAS sesiones a lo largo del tiempo
```
Device A:
├─ Session 1 (10/01/2025 08:00 - 08:15) CLOSED
├─ Session 2 (10/01/2025 14:00 - 14:30) CLOSED
├─ Session 3 (11/01/2025 09:00 - 09:45) CLOSED
└─ Session 4 (11/01/2025 15:00 - ACTIVE) ACTIVE
```

### 2. Device con Surrogate Key (dev_id)

**Razón**: PK natural (fingerprint) es problemático
- Performance: BIGINT vs VARCHAR(100)
- Inmutabilidad: Si fingerprint cambia, no rompe FKs
- Simplicidad: Desacoplar con `SET dev_id = NULL`

### 3. Device.userId sin @ManyToOne

**Razón**: Desacoplar Device del módulo User
- Device no necesita cargar User entity
- Consistente con Session.userId (mismo patrón)

### 4. Device sin @OneToMany sessions

**Razón**: Relación unidireccional suficiente
- Session → Device (necesario para navegación)
- Device → Sessions (NO necesario, evita lazy loading pesado)
- Queries: `sessionRepo.findByDevice(device)` suficiente

### 5. Session.device `optional = true`

**Razón**: Device puede ser null en sesiones históricas
- Cuando device se reemplaza, sesiones antiguas quedan con device = null
- Preserva auditoría: `deviceFingerprintSnapshot` tiene snapshot

### 6. Snapshot deviceFingerprintSnapshot

**Razón**: Auditoría histórica cuando device = null
```java
Session histórica:
├─ device: null (desacoplado)
└─ deviceFingerprintSnapshot: "abc123" ✅ (preservado)
```

---

## 🔧 CAMBIOS NECESARIOS

### Fase 1: Refactorizar Device (Surrogate Key)

```java
// 1. Device.java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
private Long devId;  // Nuevo PK

@Column(name = "dev_fingerprint", unique = true)
private String fingerprint;  // Ya NO es PK

@Column(name = "use_id", nullable = false)
private Long userId;  // Sin @ManyToOne

// ELIMINAR:
// @OneToOne private User user;
```

**Migration SQL**:
```sql
-- Crear tabla temporal con surrogate key
CREATE TABLE device_new (
    dev_id BIGSERIAL PRIMARY KEY,
    dev_fingerprint VARCHAR(100) NOT NULL UNIQUE,
    use_id BIGINT NOT NULL,
    dev_type VARCHAR(100) NOT NULL,
    dev_os VARCHAR(100) NOT NULL,
    dev_browser VARCHAR(100) NOT NULL,
    dev_reg_date TIMESTAMP NOT NULL,
    dev_last_login TIMESTAMP,
    dev_active BOOLEAN NOT NULL DEFAULT TRUE,
    dev_blocked_at TIMESTAMP,

    CONSTRAINT fk_device_user FOREIGN KEY (use_id)
        REFERENCES app_user(use_id) ON DELETE CASCADE
);

-- Migrar datos
INSERT INTO device_new (dev_fingerprint, use_id, dev_type, dev_os, dev_browser, dev_reg_date, dev_last_login)
SELECT dev_fingerprint, use_id, dev_type, dev_os, dev_browser, dev_reg_date, dev_last_login
FROM device;

-- Reemplazar tabla
DROP TABLE device CASCADE;
ALTER TABLE device_new RENAME TO device;

-- Índices
CREATE INDEX idx_device_user ON device(use_id);
CREATE INDEX idx_device_fingerprint ON device(dev_fingerprint);
CREATE UNIQUE INDEX uk_user_active_device ON device(use_id) WHERE dev_active = TRUE;
```

### Fase 2: Actualizar Session FK

```java
// Session.java
@ManyToOne(fetch = FetchType.LAZY, optional = true)
@JoinColumn(name = "dev_id")  // ✅ Cambiar de dev_fingerprint a dev_id
private Device device;

@Column(name = "ses_dev_fp_snapshot", length = 255)
private String deviceFingerprintSnapshot;  // ✅ Renombrar para claridad
```

**Migration SQL**:
```sql
-- Agregar nueva columna dev_id
ALTER TABLE session ADD COLUMN dev_id BIGINT;

-- Migrar datos: Buscar dev_id por fingerprint
UPDATE session s
SET dev_id = (
    SELECT d.dev_id
    FROM device d
    WHERE d.dev_fingerprint = s.dev_fingerprint
);

-- Eliminar columna vieja
ALTER TABLE session DROP COLUMN dev_fingerprint;

-- Agregar FK
ALTER TABLE session
ADD CONSTRAINT fk_session_device
FOREIGN KEY (dev_id) REFERENCES device(dev_id)
ON DELETE SET NULL;

-- Renombrar snapshot
ALTER TABLE session RENAME COLUMN ses_dev_fp TO ses_dev_fp_snapshot;

-- Índice
CREATE INDEX idx_session_device ON session(dev_id);
```

### Fase 3: Actualizar DeviceService

```java
// DeviceService.java
// ANTES: Desacoplar por fingerprint
sessionRepo.detachDeviceByFingerprint(oldDevice.getFingerprint());

// DESPUÉS: Desacoplar por dev_id
sessionRepo.detachDeviceById(oldDevice.getDevId());
```

**SessionRepository.java**:
```java
// ANTES
@Query("UPDATE Session s SET s.device = null WHERE s.device.fingerprint = :fp")
int detachDeviceByFingerprint(@Param("fp") String fingerprint);

// DESPUÉS
@Query("UPDATE Session s SET s.device = null WHERE s.device.devId = :devId")
int detachDeviceById(@Param("devId") Long devId);
```

---

## 📊 COMPARACIÓN: ANTES vs DESPUÉS

| Aspecto | ANTES | DESPUÉS |
|---------|-------|---------|
| **Device PK** | VARCHAR(100) fingerprint | BIGINT dev_id |
| **Session FK** | dev_fingerprint | dev_id |
| **Performance FK** | Lento (VARCHAR) | Rápido (BIGINT) |
| **Device → User** | @OneToOne User | Long userId |
| **Session → User** | @ManyToOne User | Long userId |
| **Session → Device** | @ManyToOne (fingerprint) | @ManyToOne (dev_id) |
| **Relación inversa** | Device.sessions | ❌ No (unidireccional) |
| **Desacoplamiento** | UPDATE WHERE fingerprint = ? | UPDATE WHERE dev_id = ? |

---

## ✅ RESUMEN EJECUTIVO

### Relación Correcta: **Session N:1 Device** (ManyToOne)

**Con mejoras clave**:
1. ✅ Device con **surrogate key** (`dev_id`) en lugar de PK natural
2. ✅ Device con `userId` (Long) sin relación JPA a User
3. ✅ Session con `userId` (Long) sin relación JPA a User
4. ✅ Session → Device con FK a `dev_id` (BIGINT)
5. ✅ Relación **unidireccional** (Session → Device, no inversa)
6. ✅ Snapshot `deviceFingerprintSnapshot` para auditoría

**Beneficios**:
- ✅ Performance: FK BIGINT vs VARCHAR
- ✅ Desacoplamiento: Sin relaciones a User entity
- ✅ Navegación útil: `session.getDevice()` disponible
- ✅ Auditoría completa: userId + snapshot preservados
- ✅ Múltiples devices: User puede tener histórico (1:N)

---

¿Quieres que implemente todos estos cambios? 🚀

Incluiría:
1. Refactorizar Device (surrogate key + userId)
2. Actualizar Session FK (dev_id)
3. Actualizar DeviceService
4. Crear migrations SQL completos
5. Actualizar tests
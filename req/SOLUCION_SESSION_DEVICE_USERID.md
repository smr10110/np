# Solución: Session con Relación a Device + userId (sin relación a User)

## Diseño Propuesto

```java
// Session.java
@Entity
@Table(name = "session")
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ses_id")
    private Long sesId;

    // ========== USUARIO (SIN RELACIÓN JPA) ==========
    @Column(name = "use_id", nullable = false)
    private Long userId;  // ✅ Solo el ID, sin @ManyToOne

    // ========== DEVICE (CON RELACIÓN JPA) ==========
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "dev_fingerprint", referencedColumnName = "dev_fingerprint")
    private Device device;  // ✅ MANTENER relación JPA

    @Column(name = "ses_dev_fp", length = 255)
    private String sesDeviceFingerprint;  // Snapshot

    // ========== OTROS CAMPOS ==========
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

## 🎯 VENTAJAS DE ESTE DISEÑO

### 1. Mejor Normalización

```
Session
├─ userId (Long)          → Usuario dueño de la sesión
└─ device (Device)        → Device usado (puede derivar user también)
   └─ user (User)         → Usuario dueño del device

Ventaja: userId directo + derivable vía device.user si device != null
```

### 2. Funciona con Device = null (Auditoría Histórica)

```java
// Sesión activa
Session {
    userId: 123L,        // ✅ Usuario identificado
    device: Device#456   // ✅ Device activo
    device.user.useId: 123L  // ✅ Mismo usuario
}

// Sesión histórica (device desacoplado)
Session {
    userId: 123L,        // ✅ Usuario SIGUE identificado
    device: null         // ⚠️ Device desacoplado (cambio de dispositivo)
}
```

✅ **Auditoría preservada**: Siempre sabes el userId, incluso sin device

### 3. Navegación Útil a Device

```java
Session session = sessionRepo.findById(1L);

// Acceder a device (cuando existe)
if (session.getDevice() != null) {
    String fingerprint = session.getDevice().getFingerprint();
    String os = session.getDevice().getOs();

    // También puedes derivar user de device
    User userFromDevice = session.getDevice().getUser();
}

// Acceder a userId SIEMPRE (sin necesidad de device)
Long userId = session.getUserId();
```

✅ **Flexibilidad**: Navegas a device cuando existe, usas userId cuando no

### 4. Consistency Check Posible

```java
// Validar que userId coincide con device.user.useId
public void validateSession(Session session) {
    if (session.getDevice() != null) {
        Long userIdFromDevice = session.getDevice().getUser().getUseId();
        if (!session.getUserId().equals(userIdFromDevice)) {
            throw new IllegalStateException("Session userId mismatch with device user");
        }
    }
}
```

✅ **Validación**: Puedes verificar consistencia cuando device existe

### 5. Queries Flexibles

```sql
-- Por userId directo (rápido, sin JOINs)
SELECT * FROM session WHERE use_id = ?

-- Por device (con navegación JPA)
SELECT s FROM Session s WHERE s.device.fingerprint = ?

-- Por device.user (derivado)
SELECT s FROM Session s WHERE s.device.user.useId = ?
```

✅ **Opciones**: Query por userId directo O por device

---

## 📊 COMPARACIÓN CON OTRAS OPCIONES

| Aspecto | user + device (actual) | solo userId | solo device | **userId + device** |
|---------|------------------------|-------------|-------------|---------------------|
| **Auditoría con device=null** | ✅ user disponible | ✅ userId disponible | ❌ Pierde usuario | ✅ userId disponible |
| **Navegación a Device** | ✅ Sí | ❌ No | ✅ Sí | ✅ Sí |
| **Queries por userId** | ✅ Directo | ✅ Directo | ❌ Requiere JOIN | ✅ Directo |
| **Integridad JPA User** | ✅ FK automática | ❌ Manual | ✅ Vía device | ❌ Manual |
| **Integridad JPA Device** | ✅ FK automática | ❌ No | ✅ FK automática | ✅ FK automática |
| **Acoplamiento User** | ❌ Alto | ✅ Bajo | ✅ Bajo | ✅ Bajo |
| **Acoplamiento Device** | ✅ Bajo | ✅ Bajo | ❌ Alto | ✅ Bajo |
| **Redundancia** | ⚠️ user derivable de device | ✅ Sin redundancia | ⚠️ userId derivable de device | ⚠️ userId derivable de device |

**Ganador**: ✅ **userId + device** (balance perfecto)

---

## 🔧 CAMBIOS NECESARIOS

### Cambio 1: Session.java

```java
// ELIMINAR:
@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "use_id", nullable = false, referencedColumnName = "useId")
private User user;

// AGREGAR:
@Column(name = "use_id", nullable = false)
private Long userId;

// MANTENER (sin cambios):
@ManyToOne(fetch = FetchType.LAZY, optional = true)
@JoinColumn(name = "dev_fingerprint", referencedColumnName = "dev_fingerprint")
private Device device;
```

### Cambio 2: AuthSessionService.java

```java
// ANTES:
@Transactional
public Session saveActiveSession(UUID jti, User user, Device device, Instant expiresAt) {
    Session auth = Session.builder()
            .sesJti(jti)
            .user(user)  // ← CAMBIAR
            .device(device)  // ← MANTENER
            .sesDeviceFingerprint(device != null ? device.getFingerprint() : null)
            .sesCreated(Instant.now())
            .sesExpires(expiresAt)
            .status(SessionStatus.ACTIVE)
            .build();

    return authRepo.save(auth);
}

// DESPUÉS:
@Transactional
public Session saveActiveSession(UUID jti, User user, Device device, Instant expiresAt) {
    Session auth = Session.builder()
            .sesJti(jti)
            .userId(user.getUseId())  // ← CAMBIAR a userId
            .device(device)  // ← MANTENER (sin cambios)
            .sesDeviceFingerprint(device != null ? device.getFingerprint() : null)
            .sesCreated(Instant.now())
            .sesExpires(expiresAt)
            .status(SessionStatus.ACTIVE)
            .build();

    return authRepo.save(auth);
}
```

### Cambio 3: SessionRepository.java (Agregar Query Útil)

```java
public interface SessionRepository extends JpaRepository<Session, Long> {

    // Existentes (mantener):
    Optional<Session> findBySesJtiAndStatus(UUID sesJti, SessionStatus status);
    Optional<Session> findBySesJti(UUID sesJti);

    @Modifying
    @Query("update Session s set s.device = null where s.device.fingerprint = :fp")
    int detachDeviceByFingerprint(@Param("fp") String fingerprint);

    // AGREGAR (útil para auditoría):
    @Query("SELECT s FROM Session s WHERE s.userId = :userId ORDER BY s.sesCreated DESC")
    List<Session> findByUserId(@Param("userId") Long userId);

    @Query("SELECT s FROM Session s WHERE s.userId = :userId AND s.status = :status")
    List<Session> findByUserIdAndStatus(@Param("userId") Long userId,
                                        @Param("status") SessionStatus status);
}
```

### Cambio 4: Migration SQL

```sql
-- V1__refactor_session_user_relationship.sql

-- 1. Eliminar constraint FK de User (si existe)
ALTER TABLE session DROP CONSTRAINT IF EXISTS fk_session_user;

-- 2. Agregar constraint FK manual para integridad (RECOMENDADO)
ALTER TABLE session
ADD CONSTRAINT fk_session_user
FOREIGN KEY (use_id) REFERENCES app_user(use_id)
ON DELETE CASCADE;

-- 3. Crear índice en use_id (si no existe)
CREATE INDEX IF NOT EXISTS idx_session_user_id ON session(use_id);

-- 4. Mantener constraint de Device (sin cambios)
-- FK dev_fingerprint ya existe y se mantiene

-- 5. Comentario en BD
COMMENT ON COLUMN session.use_id IS 'User ID (FK manual, no JPA relation for decoupling)';
COMMENT ON COLUMN session.dev_fingerprint IS 'Device fingerprint (JPA relation, can be null for historical sessions)';
```

---

## ✅ VENTAJAS ESPECÍFICAS vs DISEÑO ACTUAL

| Aspecto | Diseño Actual | Diseño Propuesto | Mejora |
|---------|---------------|------------------|--------|
| **Acoplamiento módulo User** | Alto (`@ManyToOne User`) | Bajo (solo `Long userId`) | ✅ Desacoplado |
| **Navegación a Device** | Sí | Sí | 🟰 Igual |
| **Queries por userId** | `WHERE user.useId` (JOIN) | `WHERE userId` (directo) | ✅ Más rápido |
| **Auditoría device=null** | user disponible | userId disponible | 🟰 Igual |
| **Lazy loading User** | ⚠️ Puede cargar entity | ✅ No carga entity | ✅ Mejor performance |
| **Código actual** | No usa `session.getUser()` | No necesita cambios | 🟰 Compatible |

---

## 🎯 VALIDACIÓN: ¿Por Qué Mantener Device?

### Device Tiene Información Útil

```java
Session session = sessionRepo.findById(1L);

if (session.getDevice() != null) {
    // Información del dispositivo
    String os = session.getDevice().getOs();           // "Windows 11"
    String browser = session.getDevice().getBrowser(); // "Chrome 120"
    String type = session.getDevice().getType();       // "Desktop"

    // Timestamp útil
    Instant lastLogin = session.getDevice().getLastLoginAt();

    // Navegación a user (si necesitas)
    User deviceOwner = session.getDevice().getUser();
}
```

✅ **Utilidad**: Device tiene metadata que userId solo no tiene

### Queries Útiles con Device

```java
// Todas las sesiones de un device específico
@Query("SELECT s FROM Session s WHERE s.device = :device")
List<Session> findByDevice(@Param("device") Device device);

// Sesiones activas de un fingerprint
@Query("SELECT s FROM Session s WHERE s.device.fingerprint = :fp AND s.status = 'ACTIVE'")
List<Session> findActiveByFingerprint(@Param("fp") String fingerprint);
```

✅ **Flexibilidad**: Queries por device son útiles para auditoría

---

## 📋 RESUMEN DE IMPLEMENTACIÓN

### Paso 1: Modificar Session.java

```java
// ELIMINAR
@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "use_id", nullable = false, referencedColumnName = "useId")
private User user;

// AGREGAR
@Column(name = "use_id", nullable = false)
private Long userId;

// MANTENER (sin cambios)
@ManyToOne(fetch = FetchType.LAZY, optional = true)
@JoinColumn(name = "dev_fingerprint")
private Device device;
```

### Paso 2: Modificar AuthSessionService.java

```java
// Línea ~35: Cambiar
.user(user)

// Por:
.userId(user.getUseId())
```

### Paso 3: Agregar Queries en SessionRepository.java

```java
// Para auditoría
@Query("SELECT s FROM Session s WHERE s.userId = :userId")
List<Session> findByUserId(@Param("userId") Long userId);
```

### Paso 4: Crear Migration SQL

```sql
-- Eliminar FK JPA, agregar FK manual
ALTER TABLE session DROP CONSTRAINT IF EXISTS fk_session_user;
ALTER TABLE session ADD CONSTRAINT fk_session_user
    FOREIGN KEY (use_id) REFERENCES app_user(use_id) ON DELETE CASCADE;
```

### Paso 5: Testing

```java
@Test
void testSessionWithUserIdAndDevice() {
    User user = createAndSaveUser();
    Device device = createAndSaveDevice(user);

    Session session = Session.builder()
        .sesJti(UUID.randomUUID())
        .userId(user.getUseId())  // ✅ userId simple
        .device(device)           // ✅ device relación JPA
        .sesCreated(Instant.now())
        .sesExpires(Instant.now().plusSeconds(900))
        .status(SessionStatus.ACTIVE)
        .build();

    Session saved = sessionRepo.save(session);

    // Validaciones
    assertEquals(user.getUseId(), saved.getUserId());
    assertNotNull(saved.getDevice());
    assertEquals(device.getFingerprint(), saved.getDevice().getFingerprint());

    // Consistency check
    assertEquals(saved.getUserId(), saved.getDevice().getUser().getUseId());
}

@Test
void testSessionWithUserIdButNoDevice() {
    User user = createAndSaveUser();

    Session session = Session.builder()
        .sesJti(UUID.randomUUID())
        .userId(user.getUseId())  // ✅ userId disponible
        .device(null)             // ✅ device null (histórico)
        .sesDeviceFingerprint("old_fp_snapshot")
        .sesCreated(Instant.now())
        .sesExpires(Instant.now().plusSeconds(900))
        .sesClosed(Instant.now())
        .status(SessionStatus.CLOSED)
        .build();

    Session saved = sessionRepo.save(session);

    assertEquals(user.getUseId(), saved.getUserId());
    assertNull(saved.getDevice());  // ✅ Auditoría funciona sin device
}
```

---

## 🎯 DECISIÓN FINAL

### ✅ **SÍ, IMPLEMENTAR** este diseño

**Justificación**:

1. ✅ **Mejor balance**: Mantiene navegación útil (device) + desacopla módulo user
2. ✅ **Auditoría completa**: userId siempre disponible (device = null OK)
3. ✅ **Performance**: Sin lazy loading innecesario de User entity
4. ✅ **Flexibilidad**: Queries por userId O por device
5. ✅ **Cambios mínimos**: 3 archivos Java + 1 SQL
6. ✅ **Sin romper código**: Nadie usa `session.getUser()`

---

## 📊 COMPARACIÓN FINAL

| Criterio | Actual (user + device) | Propuesto (userId + device) |
|----------|------------------------|----------------------------|
| **Relaciones JPA** | 2 (@ManyToOne user + device) | 1 (@ManyToOne device) |
| **Acoplamiento User** | Alto | Bajo |
| **Acoplamiento Device** | Bajo | Bajo |
| **Auditoría** | ✅ Completa | ✅ Completa |
| **Queries userId** | JOIN necesario | Directo |
| **Navegación Device** | ✅ Sí | ✅ Sí |
| **Integridad User** | JPA automática | FK manual (SQL) |
| **Integridad Device** | JPA automática | JPA automática |
| **Performance** | Lazy load User | Sin lazy load |

**Resultado**: ✅ **Propuesto es superior**

---

¿Quieres que implemente los cambios ahora? 🚀

Modificaré:
1. Session.java (eliminar `@ManyToOne User user`, agregar `Long userId`)
2. AuthSessionService.java (cambiar `.user(user)` → `.userId(user.getUseId())`)
3. SessionRepository.java (agregar queries por userId)
4. Crear script SQL de migración

**¿Procedo con la implementación?**

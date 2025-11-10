# Análisis: Eliminar Relación Directa Session → User (Derivar vía Device)

## Contexto

**Objetivo**: Eliminar la FK directa de `Session` a `User` y derivar el usuario a través de `Device`:

```
Actual:
Session → User (directo)    ← FK directa
Session → Device → User     ← Ruta indirecta

Propuesta:
Session → Device → User     ← Única ruta
```

---

## 🔍 ANÁLISIS DE RELACIÓN ACTUAL

### Estado Actual

```java
// Session.java
@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "use_id", nullable = false, referencedColumnName = "useId")
private User user;  // ← FK DIRECTA (obligatoria)

@ManyToOne(fetch = FetchType.LAZY, optional = true)
@JoinColumn(name = "dev_fingerprint", referencedColumnName = "dev_fingerprint")
private Device device;  // ← FK a Device (opcional)

@Column(name = "ses_dev_fp", length = 255)
private String sesDeviceFingerprint;  // ← Snapshot

// Device.java
@OneToOne(optional = false)
@JoinColumn(name = "useId", foreignKey = @ForeignKey(name = "fk_dev_user"))
private User user;  // ← Device → User (obligatorio)
```

### Diagrama de Relaciones

```
Session
├─ use_id (FK) → User          ← ❌ REDUNDANTE (propuesta)
└─ dev_fingerprint (FK) → Device
                          └─ useId (FK) → User  ← Ya existe aquí
```

---

## ⚠️ PROBLEMA CRÍTICO IDENTIFICADO

### Device es `optional = true` en Session

**Código actual**:
```java
@ManyToOne(fetch = FetchType.LAZY, optional = true)
@JoinColumn(name = "dev_fingerprint", referencedColumnName = "dev_fingerprint")
private Device device;  // ← ❓ Puede ser NULL
```

**Pregunta crucial**: ¿Cuándo `device` es NULL?

---

## 📊 ANÁLISIS DE USO ACTUAL

### 1. AuthSessionService.java - Crear Sesión

```java
// Línea 29-40: Método saveActiveSession
@Transactional
public Session saveActiveSession(UUID jti, User user, Device device, Instant expiresAt) {
    Session auth = Session.builder()
            .sesJti(jti)
            .user(user)  // ← ✅ SIEMPRE asigna user
            .device(device)  // ← ❓ ¿Puede ser null?
            .sesDeviceFingerprint(device != null ? device.getFingerprint() : null)
            .sesCreated(Instant.now())
            .sesExpires(expiresAt)
            .status(SessionStatus.ACTIVE)
            .build();

    return authRepo.save(auth);
}
```

**Observación**: `device` puede ser null (ternario `device != null`)

---

### 2. AuthService.java - Crear Sesión Autenticada

```java
// Línea 183-218: Método createAuthenticatedSession
private LoginResponse createAuthenticatedSession(User user, String deviceFingerprint) {
    logger.debug("Creando sesión autenticada | userId={}", user.getUseId());

    // Generar token JWT con JTI único
    UUID jti = UUID.randomUUID();
    String safeFingerprint = (deviceFingerprint == null) ? "" : deviceFingerprint;

    String token = jwtService.generate(
            String.valueOf(user.getUseId()),
            safeFingerprint,
            jti.toString()
    );
    Instant exp = jwtService.getExpiration(token);

    // Validar y obtener dispositivo autorizado
    Long userIdFromToken = Long.valueOf(jwtService.getUserId(token));
    Device device = deviceService.ensureAuthorizedDevice(userIdFromToken, safeFingerprint);  // ← ✅ SIEMPRE existe

    logger.debug("Dispositivo autorizado | userId={} | fingerprint={}", user.getUseId(), device.getFingerprint());

    // Persistir sesión activa
    Session session = authSessionService.saveActiveSession(jti, user, device, exp);  // ← ✅ Device NO null

    logger.debug("Sesión persistida | userId={} | sessionId={}", user.getUseId(), session.getSesId());

    // Registrar intento exitoso
    logAttempt(user, device.getFingerprint(), session, true, AuthAttemptReason.OK);

    return new LoginResponse(...);
}
```

**Observación**: `device` SIEMPRE existe (si falla `ensureAuthorizedDevice`, lanza excepción 403)

---

### 3. SessionRepository.java - Queries

```java
// Línea 15: Buscar por JTI y estado
Optional<Session> findBySesJtiAndStatus(UUID sesJti, SessionStatus status);

// Línea 17: Buscar por JTI
Optional<Session> findBySesJti(UUID sesJti);

// Línea 20-22: Desacoplar Device
@Modifying
@Query("update Session s set s.device = null where s.device.fingerprint = :fp")
int detachDeviceByFingerprint(@Param("fp") String fingerprint);
```

**Uso de `user`**: ❌ **NINGUNA query filtra por `user.useId`**

---

### 4. DeviceService.java - Desacoplar Device de Session

```java
// Línea 131-132: Antes de eliminar device, desacoplar de sessions
try {
    sessionRepo.detachDeviceByFingerprint(oldDevice.getFingerprint());
} catch (Exception ignored) {}

devRepo.delete(oldDevice);
```

**Observación**: Al cambiar device, se desacopla (`device = null`) en sessions

---

### 5. AuthSessionService.java - Cerrar Sesión

```java
// Línea 49-64: Método closeByJti
@Transactional
public Optional<Session> closeByJti(UUID jti) {
    return authRepo.findBySesJti(jti).map(a -> {
        if (a.getStatus() != SessionStatus.CLOSED) {
            a.setStatus(SessionStatus.CLOSED);
            if (a.getSesClosed() == null) {
                Instant now = Instant.now();
                Instant closedInstant = (a.getSesExpires() != null && now.isAfter(a.getSesExpires()))
                        ? a.getSesExpires()
                        : now;
                a.setSesClosed(closedInstant);
            }
            return authRepo.save(a);
        }
        return a;
    });
}
```

**Uso de `user`**: ❌ **NO usa `user` para nada**

---

## 🔍 BÚSQUEDA EXHAUSTIVA DE USO DE `session.user`

### Archivos donde se usa Session

1. ✅ **AuthSessionService.java** - Crea y cierra sessions
2. ✅ **AuthService.java** - Crea session autenticada
3. ✅ **AuthAttemptService.java** - Guarda session en AuthAttempt
4. ✅ **DeviceService.java** - Desacopla device de sessions
5. ✅ **SessionRepository.java** - Queries sobre sessions

### ¿Alguno usa `session.user` directamente?

```bash
# Búsqueda: ¿Dónde se accede a session.getUser()?
grep -r "session.getUser()" naive-pay-api/src/
# Resultado: ❌ NO ENCONTRADO

# Búsqueda: ¿Dónde se accede a session.user?
grep -r "session.user" naive-pay-api/src/
# Resultado: ❌ NO ENCONTRADO

# Búsqueda: ¿Queries JPQL con s.user?
grep -r "s.user" naive-pay-api/src/
# Resultado: ❌ NO ENCONTRADO en queries de Session
```

**Conclusión**: ❌ **NINGÚN código actual accede a `session.user`**

---

## ⚡ HALLAZGO CLAVE: Device SIEMPRE Existe en Sessions Activas

### Análisis del Flujo de Login

```java
1. Usuario envía login + deviceFingerprint
   ↓
2. AuthService valida credenciales
   ↓
3. deviceService.ensureAuthorizedDevice(userId, fingerprint)
   ├─ Si device no existe → 403 DEVICE_REQUIRED
   ├─ Si fingerprint no coincide → 403 DEVICE_UNAUTHORIZED
   └─ ✅ Retorna device válido
   ↓
4. authSessionService.saveActiveSession(jti, user, device, exp)
   └─ ✅ device NUNCA es null aquí
   ↓
5. Session creada con device NOT NULL
```

### ¿Cuándo `device` es NULL en Session?

**Único caso**: Al desacoplar device antes de eliminarlo

```java
// DeviceService.java línea 131
sessionRepo.detachDeviceByFingerprint(oldDevice.getFingerprint());
// Ejecuta: UPDATE session SET device = null WHERE dev_fingerprint = ?

// Razón: Conservar sesiones históricas si Device se elimina
```

**Propósito**:
- ✅ Auditoría histórica: Mantener registro de sesiones pasadas
- ✅ ON DELETE SET NULL: Si device se borra, session no se borra
- ✅ Snapshot preservado: `sesDeviceFingerprint` tiene el fingerprint original

---

## 🎯 ANÁLISIS DE ELIMINACIÓN: ¿Es Posible?

### Opción Propuesta

```java
// Session.java (DESPUÉS de eliminar user)
@ManyToOne(fetch = FetchType.LAZY, optional = false)  // ← Cambiar a obligatorio
@JoinColumn(name = "dev_id", nullable = false)  // ← Ya NO opcional
private Device device;  // ← SIEMPRE existe

// ELIMINAR:
// @ManyToOne private User user;

// Derivar user:
public User getUser() {
    return device != null ? device.getUser() : null;
}
```

### ❌ PROBLEMA: Device Desacoplado (null)

**Escenario**:
```java
// Usuario registra nuevo device → device anterior se desacopla
sessionRepo.detachDeviceByFingerprint(oldDevice.getFingerprint());

// Resultado:
Session {
    sesId: 123,
    device: null,  // ← ❌ Desacoplado para preservar auditoría
    user: ???      // ← Si eliminas user, ¿cómo obtienes usuario de sesión histórica?
}
```

**Impacto**: Pierdes referencia a usuario en sesiones históricas con device desacoplado

---

## 📊 COMPARACIÓN DE OPCIONES

### Opción A: Mantener `user` (Estado Actual)

```java
@ManyToOne(fetch = FetchType.LAZY, optional = false)
private User user;  // ← Directo

@ManyToOne(fetch = FetchType.LAZY, optional = true)
private Device device;  // ← Puede ser null (sesiones históricas)
```

**Ventajas**:
- ✅ Auditoría completa: Siempre sabes qué usuario
- ✅ Funciona con device = null (sesiones históricas)
- ✅ Sin cambios de código
- ✅ Performance: Acceso directo a user (0 JOINs)

**Desventajas**:
- ⚠️ "Redundancia aparente" (user derivable de device SI device != null)

---

### Opción B: Eliminar `user`, Device Obligatorio

```java
@ManyToOne(fetch = FetchType.LAZY, optional = false)
private Device device;  // ← Obligatorio

// Derivar user:
public User getUser() {
    return device.getUser();
}
```

**Ventajas**:
- ✅ Elimina "redundancia"
- ✅ Normalización estricta

**Desventajas**:
- ❌ **Rompe auditoría histórica**: Sesiones con device = null pierden usuario
- ❌ Requiere `ON DELETE CASCADE`: Si device se borra, session también
- ❌ Performance: +1 JOIN para obtener user (session → device → user)
- ❌ Requiere refactoring de DeviceService (eliminar `detachDeviceByFingerprint`)

---

### Opción C: Eliminar `user`, Agregar `userId` (Desnormalizado)

```java
@Column(name = "use_id", nullable = false)
private Long userId;  // ← Ya NO es FK, solo valor

@ManyToOne(fetch = FetchType.LAZY, optional = true)
private Device device;  // ← Puede ser null
```

**Ventajas**:
- ✅ Funciona con device = null
- ✅ Auditoría preservada (userId siempre disponible)
- ✅ Queries simples (WHERE use_id = ?)

**Desventajas**:
- ❌ **Pierde integridad referencial** (sin constraint FK)
- ❌ Desnormalización (userId sin validación)
- ❌ Posible inconsistencia: userId existe pero user fue borrado

---

### Opción D: Mantener `user`, Hacer Device Obligatorio

```java
@ManyToOne(fetch = FetchType.LAZY, optional = false)
private User user;

@ManyToOne(fetch = FetchType.LAZY, optional = false)
private Device device;  // ← Cambiar a obligatorio
```

**Cambio requerido**: Eliminar `detachDeviceByFingerprint` (device siempre asociado)

**Ventajas**:
- ✅ User + Device siempre disponibles
- ✅ Integridad máxima

**Desventajas**:
- ❌ **Rompe auditoría**: Si device se borra, session también (CASCADE)
- ❌ Pierdes histórico de sesiones antiguas

---

## 🔍 ANÁLISIS DE QUERIES AFECTADAS

### Queries Actuales (Ninguna usa `user`)

```sql
-- 1. Buscar por JTI
SELECT * FROM session WHERE ses_jti = ?

-- 2. Buscar por JTI + status
SELECT * FROM session WHERE ses_jti = ? AND ses_status = ?

-- 3. Desacoplar device
UPDATE session SET dev_fingerprint = NULL WHERE dev_fingerprint = ?
```

**Impacto si eliminas `user`**: ✅ **CERO** (ninguna query usa `user`)

---

### Queries Potenciales Futuras

```sql
-- ¿Todas las sesiones de un usuario?
SELECT * FROM session WHERE use_id = ?

-- Si eliminas user:
SELECT * FROM session s
INNER JOIN device d ON s.dev_fingerprint = d.dev_fingerprint  -- ❌ Falla si device = null
WHERE d.use_id = ?
```

**Problema**: Query excluye sesiones con device = null

---

## 💡 CASO DE USO REAL: Cambio de Device

### Flujo Actual

```
1. Usuario registra nuevo device
   ↓
2. DeviceService.replaceDevice()
   ├─ Desacopla device de sessions: device = null
   ├─ Elimina device viejo
   └─ Crea device nuevo
   ↓
3. Sesiones antiguas quedan:
   Session {
       user: User#123  ← ✅ Preservado para auditoría
       device: null    ← Desacoplado
       sesDeviceFingerprint: "abc123"  ← Snapshot preservado
   }
```

**Ventaja**: Auditoría completa (sabes qué usuario + qué fingerprint usó)

---

### Si Eliminas `user`

```
1. Usuario registra nuevo device
   ↓
2. DeviceService.replaceDevice()
   ├─ ❌ NO puede desacoplar (device obligatorio)
   └─ Dos opciones:
       A) Eliminar sessions antiguas (pierde auditoría)
       B) ON DELETE CASCADE (también pierde auditoría)
   ↓
3. Sesiones antiguas:
   Session {
       device: null  ← Si desacoplas
       // ❌ ¿Cómo obtener user si device = null?
   }
```

**Problema**: Pierdes usuario en auditoría histórica

---

## 📋 RESUMEN DE CAMBIOS REQUERIDOS

### Si Eliminas `user` (Opción B)

| Archivo | Cambio | Complejidad |
|---------|--------|-------------|
| **Session.java** | Eliminar campo `user` | 🟢 Trivial |
| **AuthSessionService.java** | Cambiar firma de `saveActiveSession()` | 🟡 Medio |
| **AuthService.java** | Eliminar parámetro `user` | 🟡 Medio |
| **DeviceService.java** | Eliminar `detachDeviceByFingerprint()` | 🔴 Alto |
| **SessionRepository.java** | Eliminar query `detachDeviceByFingerprint` | 🟢 Trivial |
| **Schema BD** | DROP CONSTRAINT + Cambiar device a NOT NULL | 🟡 Medio |

**Total**: 5 archivos + cambio DB schema

---

### Cambio Crítico: DeviceService

**Antes**:
```java
// Preserva sesiones históricas desacoplando device
sessionRepo.detachDeviceByFingerprint(oldDevice.getFingerprint());
devRepo.delete(oldDevice);
```

**Después** (Sin desacoplar):
```java
// Opciones:
// A) Eliminar sessions antes de borrar device (pierde auditoría)
sessionRepo.deleteByDeviceFingerprint(oldDevice.getFingerprint());
devRepo.delete(oldDevice);

// B) ON DELETE CASCADE en FK (también pierde auditoría)
devRepo.delete(oldDevice);  // Sessions se borran automáticamente
```

**Impacto**: ❌ **Pierdes auditoría de sesiones pasadas**

---

## 🎯 ANÁLISIS DE REDUNDANCIA REAL

### ¿Es Redundancia o Diseño Correcto?

```
Session tiene:
├─ user (FK)    → Para auditoría cuando device = null
└─ device (FK)  → Para vincular con device activo (puede ser null)

Casos:
1. Session activa:     user ✅, device ✅  (redundancia aparente)
2. Session histórica:  user ✅, device ❌  (NO redundancia, necesario)
```

**Conclusión**: NO es redundancia real, es **diseño para auditoría**

---

## ✅ RECOMENDACIÓN FINAL

### ❌ **NO ELIMINAR** relación Session → User

**Razones**:

1. **Auditoría histórica crítica**:
   - Sesiones con device = null necesitan user para saber de quién fueron
   - Cumplimiento regulatorio: GDPR, auditorías de seguridad

2. **Ningún código actual usa `user`**:
   - Si eliminas, no rompes funcionalidad ACTUAL
   - Pero rompes auditoría FUTURA (queries "sesiones de usuario X")

3. **Performance óptima**:
   - Acceso directo a user (0 JOINs)
   - Si derivas de device: +1 JOIN + falla con device = null

4. **Integridad referencial**:
   - FK garantiza consistencia
   - userId plano (Opción C) pierde constraint

5. **Complejidad vs beneficio**:
   - Eliminar requiere 5 archivos + refactoring device desacoplamiento
   - Beneficio: Elimina "redundancia aparente" (que no es real)

---

## 🔧 ALTERNATIVA: Documentar Justificación

Si te preocupa que parezca redundante, agrega documentación:

```java
/**
 * Sesión de autenticación vinculada a un JWT token.
 *
 * <h2>Relaciones</h2>
 * <ul>
 *   <li><b>user</b>: FK directa al usuario propietario de la sesión.
 *       NECESARIA para auditoría de sesiones históricas donde el dispositivo
 *       fue desvinculado (device = null).</li>
 *   <li><b>device</b>: FK al dispositivo usado para crear la sesión.
 *       Puede ser null si el dispositivo fue reemplazado o eliminado después
 *       de crear la sesión (se desacopla para preservar histórico).</li>
 * </ul>
 *
 * <h2>¿Por qué user y device?</h2>
 * <p>NO es redundancia: cuando device es reemplazado, se desacoplan las sesiones
 * antiguas (device = null) pero se mantiene user para auditoría. Si deriváramos
 * user de device, perderíamos el usuario de sesiones históricas.</p>
 *
 * <h2>Ejemplo: Cambio de Dispositivo</h2>
 * <pre>
 * Usuario registra nuevo device:
 * 1. Sessions antiguas: device = null, user = User#123 ✅ (auditoría preservada)
 * 2. Device viejo eliminado
 * 3. Sessions nuevas: device = Device#456, user = User#123
 * </pre>
 */
@Entity
@Table(name = "session")
public class Session {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "use_id", nullable = false)
    private User user;  // ✅ MANTENER (necesario para auditoría histórica)

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "dev_fingerprint")
    private Device device;  // ✅ MANTENER (null en sesiones históricas)

    @Column(name = "ses_dev_fp", length = 255)
    private String sesDeviceFingerprint;  // Snapshot del fingerprint original
}
```

---

## 📊 COMPARACIÓN FINAL

| Criterio | Mantener user | Eliminar user | Ganador |
|----------|---------------|---------------|---------|
| **Auditoría histórica** | 🟢 Completa | 🔴 Rota | ✅ Mantener |
| **Performance** | 🟢 0 JOINs | 🔴 +1 JOIN | ✅ Mantener |
| **Integridad** | 🟢 FK constraint | 🔴 Sin constraint | ✅ Mantener |
| **Complejidad** | 🟢 0 cambios | 🔴 5 archivos | ✅ Mantener |
| **Normalización** | 🟡 "Redundancia" | 🟢 Perfecta | ⚠️ Empate |
| **Código actual** | 🟢 Funciona | 🟢 También (no usa user) | 🟰 Empate |

**Resultado**: ✅ **5-1** a favor de mantener

---

## ✅ CONCLUSIÓN

### NO Eliminar Relación Session → User

**Motivos**:
1. ❌ Rompe auditoría histórica (sesiones con device = null)
2. ❌ Requiere eliminar desacoplamiento de device (pierde histórico)
3. ❌ Degrada performance (+1 JOIN para obtener user)
4. ❌ Requiere refactoring de 5 archivos sin beneficio real
5. ✅ La "redundancia" es **justificada para auditoría**

### Enfoque en Redundancias Reales

1. ✅ **User + Credencial + Register** → Merge (3 tablas → 1) ← PRIORIDAD
2. ⚠️ **Session.sesDeviceFingerprint** → Evaluar si snapshot es necesario
3. ⚠️ **AuthAttempt.user** → MANTENER (necesario para intentos sin sesión)
4. ⚠️ **Session.user** → MANTENER (necesario para auditoría histórica)

**Session.user NO es redundancia, es diseño correcto para auditoría.**

---

¿Quieres que:
1. Agregue la documentación explicativa en Session.java?
2. Procedamos con la consolidación User + Credencial + Register (redundancia REAL)?

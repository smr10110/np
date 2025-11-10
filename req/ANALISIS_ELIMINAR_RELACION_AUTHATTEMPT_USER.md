# Análisis: Eliminar Relación Directa AuthAttempt → User

## Contexto

**Objetivo**: Eliminar la FK directa de `AuthAttempt` a `User` para evitar redundancia, ya que:
```
AuthAttempt → Session → User  (redundante)
AuthAttempt → User             (directo)
```

Si `AuthAttempt` ya tiene `Session`, y `Session` ya tiene `User`, ¿para qué tener FK directa a `User`?

---

## 🔍 ANÁLISIS DE RELACIÓN ACTUAL

### Estado Actual

```java
// AuthAttempt.java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "use_id", referencedColumnName = "useId")
private User user;  // ← FK DIRECTA

@ManyToOne(fetch = FetchType.LAZY, optional = true)
@JoinColumn(name = "ses_id")
private Session session;  // ← FK a Session (que ya tiene User)

@Column(name = "att_dev_fp", length = 255)
private String attDeviceFingerprint;
```

### Diagrama de Relaciones

```
AuthAttempt
├─ use_id (FK) → User  ← ❌ REDUNDANTE
└─ ses_id (FK) → Session
                 └─ use_id (FK) → User  ← Ya existe aquí
```

---

## ⚠️ PROBLEMA CRÍTICO IDENTIFICADO

### Intentos Fallidos SIN Sesión

**Escenario**: Usuario no existe o password incorrecto → No se crea Session

```java
// AuthService.java línea 67
if (userOpt.isEmpty()) {
    logger.warn("Login rechazado: usuario no encontrado");
    logAttempt(null, deviceFingerprint, null, false, AuthAttemptReason.USER_NOT_FOUND);
    //         ^^^^ user = null
    //                                   ^^^^ session = null
    return unauthorized(AuthAttemptReason.USER_NOT_FOUND);
}
```

**Casos donde `session = null`**:
1. ✅ Usuario no encontrado (línea 67)
2. ✅ Password incorrecto (línea 86)
3. ✅ Cuenta bloqueada (línea 79)
4. ✅ Device no autorizado (línea 237)

**Problema**: Si eliminas `user`, ¿cómo guardas el intento fallido SIN sesión?

---

## 📊 ANÁLISIS DE USO ACTUAL

### Lugares que Usan `user` en AuthAttempt

#### 1. AuthAttemptRepository.java

```java
// Línea 22-26: Obtener últimos intentos de un usuario
@Query("""
    SELECT a FROM AuthAttempt a
    WHERE a.user.useId = :userId  ← ❌ USA user DIRECTAMENTE
    ORDER BY a.attOccurred DESC
""")
List<AuthAttempt> findLatestAttemptsByUser(@Param("userId") Long userId, Pageable pageable);

// Línea 39-44: Contar intentos fallidos
@Query("""
    SELECT COUNT(a) FROM AuthAttempt a
    WHERE a.user.useId = :userId  ← ❌ USA user DIRECTAMENTE
    AND a.attSuccess = false
    AND a.attOccurred > :since
""")
long countFailedAttemptsSince(@Param("userId") Long userId, @Param("since") Instant since);

// Línea 51-54: Último intento exitoso
@Query("""
    SELECT MAX(a.attOccurred) FROM AuthAttempt a
    WHERE a.user.useId = :userId AND a.attSuccess = true  ← ❌ USA user DIRECTAMENTE
""")
Instant findLastSuccessAt(@Param("userId") Long userId);
```

**Uso**: 3 queries que filtran por `a.user.useId`

#### 2. AuthAttemptService.java

```java
// Línea 21-29: Registrar intento
@Transactional
public void log(User user, String attDeviceFingerprint, Session session, boolean success, AuthAttemptReason reason) {
    var attempt = AuthAttempt.builder()
            .user(user)  ← ❌ ASIGNA user DIRECTAMENTE
            .attDeviceFingerprint(attDeviceFingerprint)
            .session(session)
            .attSuccess(success)
            .attReason(reason)
            .attOccurred(Instant.now())
            .build();
    repo.save(attempt);
}
```

**Uso**: Asigna `user` al crear AuthAttempt

#### 3. AuthService.java

```java
// Línea 67: Usuario no encontrado
logAttempt(null, deviceFingerprint, null, false, AuthAttemptReason.USER_NOT_FOUND);
//         ^^^^ user = null

// Línea 86: Password incorrecto
logFailedAttempt(user, AuthAttemptReason.BAD_CREDENTIALS);

// Línea 211: Login exitoso
logAttempt(user, device.getFingerprint(), session, true, AuthAttemptReason.OK);
```

**Uso**: Pasa `user` (o `null`) al registrar intentos

#### 4. AccountLockService.java

```java
// Línea 109-112: Obtener últimos N intentos
List<AuthAttempt> recentAttempts = authAttemptRepository.findLatestAttemptsByUser(
        user.getUseId(),  ← ❌ Usa userId para filtrar
        PageRequest.of(0, maxFailedAttempts)
);
```

**Uso**: Filtra intentos por `userId`

#### 5. PasswordRecoveryService.java

```java
// Línea 86: Password reset exitoso
authAttemptService.logPasswordResetAsSuccess(user);
```

**Uso**: Registra intento de tipo PASSWORD_RESET con `user`

---

## 🚨 IMPACTO DE ELIMINAR RELACIÓN

### Cambios Requeridos si Eliminas `user`

| Archivo | Líneas Afectadas | Tipo de Cambio | Complejidad |
|---------|------------------|----------------|-------------|
| **AuthAttempt.java** | 1 campo | Eliminar `private User user;` | 🟢 Trivial |
| **AuthAttemptRepository.java** | 3 queries | Cambiar `a.user.useId` → `a.session.user.useId` | 🔴 PROBLEMA |
| **AuthAttemptService.java** | 1 método | Cambiar firma + lógica | 🟡 Medio |
| **AuthService.java** | 3 llamadas | Cambiar llamadas a log | 🟡 Medio |
| **AccountLockService.java** | 1 query | Cambiar query | 🔴 PROBLEMA |
| **PasswordRecoveryService.java** | 1 llamada | Cambiar llamada | 🟡 Medio |
| **Schema BD** | 1 FK | ALTER TABLE DROP CONSTRAINT | 🟢 Trivial |

**Total**: 6 archivos Java + 1 cambio SQL

---

## ❌ PROBLEMAS CRÍTICOS

### Problema 1: Intentos sin Sesión (Usuario No Encontrado)

**Caso actual**:
```java
// Usuario no existe en BD
logAttempt(null, deviceFingerprint, null, false, AuthAttemptReason.USER_NOT_FOUND);
//         ^^^^ user = null
//                               ^^^^ session = null
```

**Si eliminas `user`**:
```java
AuthAttempt {
    user: null,        ← ❌ ELIMINADO
    session: null,     ← También null
    // ¿Cómo saber de qué usuario fue el intento?
}
```

**❌ No puedes derivar user de session si ambos son null**

---

### Problema 2: Queries que Filtran por userId

**Query actual**:
```sql
SELECT a FROM AuthAttempt a
WHERE a.user.useId = :userId
ORDER BY a.attOccurred DESC
```

**Si cambias a derivar de session**:
```sql
SELECT a FROM AuthAttempt a
WHERE a.session.user.useId = :userId  ← ❌ FALLA si session = null
ORDER BY a.attOccurred DESC
```

**Problema**: Excluye intentos fallidos sin sesión (los más importantes para seguridad)

---

### Problema 3: Performance (JOINs Adicionales)

**Query actual**:
```sql
-- JOIN directo: AuthAttempt → User
SELECT * FROM attempt_auth a
INNER JOIN app_user u ON a.use_id = u.use_id
WHERE u.use_id = ?
```

**Si cambias a derivar de session**:
```sql
-- 2 JOINs: AuthAttempt → Session → User
SELECT * FROM attempt_auth a
LEFT JOIN session s ON a.ses_id = s.ses_id  ← LEFT porque puede ser null
LEFT JOIN app_user u ON s.use_id = u.use_id
WHERE u.use_id = ?  ← ❌ No funciona si session = null
```

**Impacto**: +1 JOIN extra + queries más lentas

---

## ✅ SOLUCIONES POSIBLES

### Opción A: Mantener Relación (RECOMENDADA)

**Justificación**:
- ✅ Necesaria para intentos sin sesión
- ✅ Performance óptima (1 JOIN menos)
- ✅ Queries simples
- ✅ Código actual funciona

**Cambios**: ❌ Ninguno

---

### Opción B: Agregar Campo `userId` Desnormalizado

**Idea**: En lugar de FK, guardar `userId` como valor plano

```java
// AuthAttempt.java
@Column(name = "use_id")
private Long userId;  // ← Ya NO es FK, solo valor

@ManyToOne(fetch = FetchType.LAZY, optional = true)
private Session session;
```

**Ventajas**:
- ✅ Funciona con intentos sin sesión (userId != null, session = null)
- ✅ Queries simples (WHERE userId = ?)
- ✅ No requiere JOINs

**Desventajas**:
- ❌ Pierde integridad referencial (no hay constraint FK)
- ❌ Posible data inconsistente (userId sin user en BD)
- ❌ Violación de normalización

---

### Opción C: Tabla Separada para Intentos sin Sesión

**Idea**: Dividir en 2 tablas

```java
// AuthAttempt (con sesión)
@ManyToOne(optional = false)
private Session session;  // ← Obligatorio

// AuthAttemptAnonymous (sin sesión)
@Column(name = "email_attempted")
private String emailAttempted;
@Column(name = "device_fingerprint")
private String deviceFingerprint;
```

**Ventajas**:
- ✅ Normalización perfecta
- ✅ Separa casos con/sin sesión

**Desventajas**:
- ❌ Complejidad: 2 tablas + 2 repositorios + 2 servicios
- ❌ Queries complicadas (UNION para ver todos los intentos)
- ❌ Mantenimiento difícil

---

### Opción D: Session Obligatoria (Crear Sesión Temporal)

**Idea**: Crear session incluso para intentos fallidos

```java
// Siempre crear session (aunque sea temporal sin token)
Session tempSession = authSessionService.createTemporarySession(user);
authAttemptService.log(tempSession, false, reason);
```

**Ventajas**:
- ✅ Elimina redundancia (AuthAttempt solo tiene session)
- ✅ Queries simples (siempre JOIN a session)

**Desventajas**:
- ❌ Overhead: inserta session inútil en BD
- ❌ Polución de tabla session con datos temporales
- ❌ Complejidad: gestionar sesiones temporales

---

## 📋 RECOMENDACIÓN FINAL

### ✅ **Opción A: MANTENER RELACIÓN User**

**Razones**:

1. **Necesidad funcional**: Intentos sin sesión son CRÍTICOS para seguridad
   ```
   - Usuario no encontrado → Detectar escaneo de emails
   - Password incorrecto → Contar intentos fallidos
   - Cuenta bloqueada → Auditoría de ataques
   ```

2. **Performance**: FK directa = 1 JOIN menos que derivar de session

3. **Simplicidad**: Código actual funciona, no requiere refactoring

4. **Integridad**: FK garantiza consistencia de datos

5. **La "redundancia" es justificada**:
   ```
   user → Para intentos SIN sesión (fallidos antes de auth)
   session → Para intentos CON sesión (exitosos o fallidos post-auth)
   ```

---

## 🔧 ALTERNATIVA: Documentar Justificación

Si te preocupa que parezca redundante, agrega documentación:

```java
/**
 * Intento de autenticación registrado en el sistema.
 *
 * <h2>Relaciones</h2>
 * <ul>
 *   <li><b>user</b>: FK directa para intentos fallidos sin sesión
 *       (ej: password incorrecto, usuario no encontrado).
 *       NECESARIA porque no hay session en estos casos.</li>
 *   <li><b>session</b>: FK a sesión creada si el intento fue exitoso.
 *       Puede ser null para intentos fallidos.</li>
 * </ul>
 *
 * <h2>¿Por qué user y session?</h2>
 * <p>NO es redundancia: user es obligatorio, session es opcional.
 * Si deriváramos user de session, perderíamos intentos fallidos
 * (críticos para seguridad y detección de ataques).</p>
 */
@Entity
@Table(name = "attempt_auth")
public class AuthAttempt {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "use_id", nullable = false)
    private User user;  // ✅ MANTENER (necesario para intentos sin sesión)

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "ses_id")
    private Session session;  // ✅ MANTENER (null si intento falló antes de auth)
}
```

---

## 📊 COMPARACIÓN DE OPCIONES

| Criterio | A: Mantener | B: userId Plano | C: 2 Tablas | D: Session Obligatoria |
|----------|-------------|-----------------|-------------|------------------------|
| **Complejidad** | 🟢 Baja | 🟡 Media | 🔴 Alta | 🟡 Media |
| **Performance** | 🟢 Óptima | 🟢 Óptima | 🔴 Mala | 🟡 Media |
| **Integridad** | 🟢 FK garantiza | 🔴 Sin constraint | 🟢 FK garantiza | 🟢 FK garantiza |
| **Normalización** | 🟡 "Redundancia" | 🔴 Desnormalizado | 🟢 Perfecta | 🟡 Overhead |
| **Cambios necesarios** | 🟢 0 archivos | 🟡 6 archivos | 🔴 15 archivos | 🟡 8 archivos |
| **Mantenibilidad** | 🟢 Simple | 🟡 Tolerable | 🔴 Compleja | 🟡 Tolerable |

**Ganador**: ✅ **Opción A (Mantener relación actual)**

---

## 🎯 CONCLUSIÓN

### NO Eliminar Relación AuthAttempt → User

**Motivos**:
1. ❌ Rompe funcionalidad crítica (intentos sin sesión)
2. ❌ Complica queries (+ JOINs)
3. ❌ Requiere refactoring de 6 archivos
4. ❌ Degrada performance
5. ✅ La "redundancia" es **justificada y necesaria**

### Si Quieres Eliminar Redundancias, Enfócate en:

1. ✅ **User + Credencial + Register** → Merge (3 tablas → 1)
2. ✅ **Session.sesDeviceFingerprint** → Evaluar si es snapshot necesario
3. ✅ **Change FKs** → Simplificar a FK única a User

**AuthAttempt.user NO es redundancia, es diseño correcto.**

---

## ✅ RESUMEN EJECUTIVO

| Pregunta | Respuesta |
|----------|-----------|
| ¿Eliminar AuthAttempt → User? | ❌ **NO** |
| ¿Es redundancia? | ❌ No, es necesaria |
| ¿Cambios requeridos? | 6 archivos + queries complejas |
| ¿Impacto? | 🔴 Rompe intentos sin sesión |
| ¿Recomendación? | ✅ **MANTENER como está** |

**Acción**: Documentar justificación en Javadoc de AuthAttempt.

---

¿Quieres que agregue la documentación explicativa o prefieres analizar otra redundancia? 🛠️

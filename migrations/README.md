# Refactorización: Campos userId en Session y AuthAttempt

## 🔴 Problema Original

Cuando se desvinculaba o cambiaba un dispositivo, múltiples campos se ponían en `NULL`, causando pérdida de información crítica.

### Flujo problemático:

```
Session → AuthAttempt → Device → User
```

Al desvincular un dispositivo:
1. `authAttemptRepo.detachAuthAttemptsFromDevice(...)` → Pone `AuthAttempt.device = NULL`
2. `devRepo.delete(oldDevice)` → Elimina el Device

**Consecuencias:**
- ❌ Sesiones activas pierden referencia al usuario
- ❌ `Session.getUser()` retorna `NULL`
- ❌ `AuthAttempt.getUser()` retorna `NULL`
- ❌ No se puede auditar intentos de autenticación históricos
- ❌ Imposible identificar a qué usuario pertenece una sesión activa

## ✅ Solución Implementada

**Desnormalización estratégica:** Agregar campo `userId` directo en `Session` y `AuthAttempt`.

### Arquitectura mejorada:

```java
Session {
    Long sesId
    UUID sesJti
    Long userId           // 👈 NUEVO: Campo desnormalizado
    AuthAttempt initialAuthAttempt  // Mantiene relación para auditoría
    ...
}

AuthAttempt {
    Long attId
    Long userId           // 👈 NUEVO: Campo desnormalizado
    Device device         // Mantiene relación para auditoría (optional=true)
    ...
}
```

### Ventajas:

- ✅ **Sesiones inmutables:** Siempre conocen su usuario, sin importar cambios en dispositivos
- ✅ **Auditoría completa:** Historial de AuthAttempts preservado incluso sin Device
- ✅ **Mejor rendimiento:** Queries directos sin JOINs innecesarios
- ✅ **Simplicidad:** No se necesitan soft deletes ni complejidad adicional
- ✅ **Resilencia:** Sin cascadas de NULLs al eliminar dispositivos

## 📋 Cambios Realizados

### 1. Entidades actualizadas:
- ✅ `Session.java` - Campo `userId` agregado
- ✅ `AuthAttempt.java` - Campo `userId` agregado

### 2. Servicios actualizados:
- ✅ `AuthSessionService.saveActiveSession()` - Establece `userId` al crear sesión
- ✅ `AuthAttemptService.log()` - Establece `userId` al crear intento

### 3. Repositorios optimizados:
- ✅ `AuthAttemptRepository` - Queries simplificados usando `userId` directo

### 4. Migraciones SQL:
- ✅ `add_user_id_to_session_and_auth_attempt.sql` - Script de migración completo

## 🚀 Cómo Aplicar la Migración

### Opción 1: JPA Auto-Create (Desarrollo)

Si tu `application.properties` tiene:
```properties
spring.jpa.hibernate.ddl-auto=update
```

JPA creará las columnas automáticamente. **Luego ejecuta manualmente:**

```sql
-- Migrar datos existentes
UPDATE attempt_auth aa
SET user_id = (
    SELECT d.useId FROM device d WHERE d.dev_fingerprint = aa.dev_fingerprint
)
WHERE aa.dev_fingerprint IS NOT NULL;

UPDATE session s
SET user_id = (
    SELECT d.useId
    FROM attempt_auth aa
    JOIN device d ON aa.dev_fingerprint = d.dev_fingerprint
    WHERE aa.att_id = s.att_id_initial
)
WHERE s.att_id_initial IS NOT NULL;
```

### Opción 2: Migración SQL Manual (Producción)

Ejecuta el script completo:
```bash
psql -U your_user -d naivepay < migrations/add_user_id_to_session_and_auth_attempt.sql
```

## 🔍 Verificación Post-Migración

```sql
-- Verificar que NO haya NULLs
SELECT COUNT(*) FROM attempt_auth WHERE user_id IS NULL;  -- Debe ser 0
SELECT COUNT(*) FROM session WHERE user_id IS NULL;       -- Debe ser 0

-- Verificar consistencia de datos
SELECT s.ses_id, s.user_id, d.useId as device_user_id
FROM session s
LEFT JOIN attempt_auth aa ON s.att_id_initial = aa.att_id
LEFT JOIN device d ON aa.dev_fingerprint = d.dev_fingerprint
WHERE s.user_id != d.useId
LIMIT 10;  -- Debe retornar 0 filas
```

## 📊 Impacto en el Sistema

### Antes:
```java
// ❌ Esto retornaba NULL después de desvincular device
session.getUser()  // → NULL
authAttempt.getUser()  // → NULL
```

### Después:
```java
// ✅ Siempre funciona, sin importar el estado del device
session.getUserId()  // → 123 (siempre disponible)
authAttempt.getUserId()  // → 123 (siempre disponible)

// Los métodos helper siguen funcionando para auditoría (cuando device existe)
session.getUser()  // → User (si device existe) o NULL (si fue eliminado)
authAttempt.getUser()  // → User (si device existe) o NULL (si fue eliminado)
```

## 🎯 Recomendaciones

1. **Usa `userId` para lógica de negocio** (autenticación, autorización)
2. **Usa las relaciones navegables** solo para auditoría y reporting
3. **No dependas de `getUser()` navegando la cadena** - usa `getUserId()` en su lugar

## 🔧 Compatibilidad

- ✅ Compatible con código existente
- ✅ Los métodos helper `getUser()` siguen funcionando (retornan NULL si device fue eliminado)
- ✅ Mejora rendimiento de queries existentes
- ✅ No rompe contratos de API

# Plan de Refactorización - Servicios de Autenticación
## Análisis Actualizado - Código Actual

---

## 🟡 CÓDIGO DUPLICADO

### 1. **Método `isBlank()` - Triplicado**

**Ubicaciones:**
1. `AuthService.java:313-314`
2. `LoginRequestValidator.java:79-80`
3. `DeviceController.java:149`

**Código duplicado:**
```java
// AuthService.java:313-314
private static boolean isBlank(String s) {
    return s == null || s.trim().isEmpty();
}

// LoginRequestValidator.java:79-80
private boolean isBlank(String s) {
    return s == null || s.trim().isEmpty();
}

// DeviceController.java:149
private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
```

**Impacto:** Medio - Código trivial pero innecesariamente triplicado.

**Solución:**
Opción A (recomendada): Usar utilidad existente de Spring
```java
import org.springframework.util.StringUtils;

// Reemplazar isBlank(str) por !StringUtils.hasText(str)
```

Opción B: Crear clase utilitaria propia
```java
// Crear: autentificacion/util/StringUtil.java
public class StringUtil {
    public static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
```

---

### 2. **Extracción de Bearer Token - Triplicado**

**Ubicaciones:**
1. `AuthService.java:323-328` → método `extractBearer()`
2. `DeviceTokenUtil.java:49-57` → método `extractBearerTokenFromHeader()`
3. `JwtAuthFilter.java:78-85` → implementación inline

**Código duplicado:**

```java
// AuthService.java:323-328
private String extractBearer(String authHeader) {
    if (isBlank(authHeader) || !authHeader.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
        return null;
    }
    return authHeader.substring(BEARER_PREFIX.length()).trim();
}

// DeviceTokenUtil.java:49-57
public String extractBearerTokenFromHeader(String authorizationHeaderValue) {
    if (authorizationHeaderValue == null || authorizationHeaderValue.isBlank()) {
        throw new IllegalArgumentException("Missing Authorization header");
    }
    if (!authorizationHeaderValue.startsWith(BEARER_PREFIX)) {
        throw new IllegalArgumentException("Invalid Authorization format (expected: 'Bearer <token>')");
    }
    return authorizationHeaderValue.substring(BEARER_PREFIX.length()).trim();
}

// JwtAuthFilter.java:78-85
final String header = request.getHeader(AUTH_HEADER);
if (header == null || !header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
    chain.doFilter(request, response);
    return;
}
final String token = header.substring(BEARER_PREFIX.length()).trim();
```

**Problema:**
- Tres implementaciones casi idénticas
- Diferencia: manejo de errores (null vs excepción)
- `BEARER_PREFIX` también está duplicado

**Impacto:** Medio - Dificulta el mantenimiento y genera inconsistencias.

**Solución:**
Crear clase utilitaria `BearerTokenExtractor` en `autentificacion.util`:

```java
package cl.ufro.dci.naivepayapi.autentificacion.util;

public class BearerTokenExtractor {
    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * Extrae token, devuelve null si el header es inválido.
     */
    public static String extractOrNull(String authHeader) {
        if (authHeader == null || authHeader.isBlank()) {
            return null;
        }
        if (!authHeader.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return null;
        }
        return authHeader.substring(BEARER_PREFIX.length()).trim();
    }

    /**
     * Extrae token, lanza excepción si el header es inválido.
     */
    public static String extractOrThrow(String authHeader) {
        String token = extractOrNull(authHeader);
        if (token == null) {
            throw new IllegalArgumentException("Invalid or missing Authorization header");
        }
        return token;
    }
}
```

**Archivos a actualizar:**
- `AuthService.java:323` → usar `BearerTokenExtractor.extractOrNull()`
- `DeviceTokenUtil.java:49` → usar `BearerTokenExtractor.extractOrThrow()`
- `JwtAuthFilter.java:78-85` → usar `BearerTokenExtractor.extractOrNull()`

---

### 3. **Constante `BEARER_PREFIX` duplicada**

**Ubicaciones:**
1. `AuthService.java:33`
2. `DeviceTokenUtil.java:23`
3. `JwtAuthFilter.java:33`

**Solución:**
Se eliminará al consolidar en `BearerTokenExtractor` (ver punto 2).

---

### 4. **Método `getAuthenticatedUserId()` duplicado**

**Ubicaciones:**
1. `DeviceController.java:153-159` → método privado
2. `AuthUtils.java:28-39` → método `getUserId()` (similar pero con validaciones diferentes)

**Código:**
```java
// DeviceController.java:153-159
private Long getAuthenticatedUserId() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || auth.getPrincipal() == null) {
        throw new IllegalStateException("No authenticated user");
    }
    return Long.valueOf(auth.getPrincipal().toString());
}

// AuthUtils.java:28-39
public static Long getUserId(Authentication auth) {
    if (auth == null || auth.getName() == null) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Unable to retrieve userId from authentication context");
    }
    try {
        return Long.parseLong(auth.getName());
    } catch (NumberFormatException e) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Invalid userId: " + auth.getName());
    }
}
```

**Impacto:** Bajo - Lógica similar con diferencias en manejo de errores.

**Solución:**
Unificar en `AuthUtils`:
```java
// Agregar método sin parámetros que obtiene auth del contexto
public static Long getCurrentUserId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    return getUserId(auth);
}
```

Luego en `DeviceController.java:153-159`:
```java
private Long getAuthenticatedUserId() {
    return AuthUtils.getCurrentUserId();
}
```

---

## 🟠 CÓDIGO MUERTO / NO UTILIZADO

### 5. **Métodos no utilizados en `DeviceTokenUtil`**

**Ubicación:** `dispositivos/configuration/DeviceTokenUtil.java`

**Métodos públicos NO utilizados:**

| Método | ¿Se usa? | Líneas |
|--------|----------|--------|
| `extractBearerTokenFromHeader()` | ✅ Sí (indirectamente) | 49-57 |
| `extractBearerTokenFromRequest()` | ✅ Sí (llamado por otros métodos) | 66-68 |
| `validateTokenNotExpired()` | ❌ No (solo interno) | 76-81 |
| `extractUserIdFromJwt()` | ⚠️ Interno | 92-100 |
| `extractUserIdFromRequest()` | ✅ **SÍ** (usado en DeviceController:39, 75) | 109-112 |
| `extractDeviceFingerprintFromJwt()` | ⚠️ Interno | 123-129 |
| `extractFingerprintFromHeader()` | ✅ **SÍ** (usado en DeviceController:43, 97, 118) | 138-143 |
| `resolveDeviceFingerprint()` | ❌ **NO** (nunca usado) | 158-172 |
| `safeExtractBearerTokenOrNull()` | ⚠️ Interno | 181-187 |

**Dependencias inyectadas NO utilizadas:**
```java
private final DeviceRepository deviceRepository;  // ← NUNCA SE USA
private final PasswordEncoder passwordEncoder;    // ← NUNCA SE USA
```

**Impacto:** Bajo - Aumenta complejidad sin aportar valor.

**Solución:**
- **Eliminar** método `resolveDeviceFingerprint()` (líneas 158-172) - no se usa
- **Eliminar** método `validateTokenNotExpired()` (líneas 76-81) - solo se usa internamente, puede quedar inline
- **Eliminar** dependencias no utilizadas: `DeviceRepository` y `PasswordEncoder`
- **Consolidar** extracción de Bearer token en `BearerTokenExtractor` (ver punto 2)

---

## 🔵 MEJORAS DE CALIDAD

### 6. **System.out.println en código de producción**

**Ubicaciones:**
1. `JwtAuthFilter.java:146`
2. `UserRegistrationListener.java` (ubicación exacta pendiente)
3. `ReportController.java` (ubicación exacta pendiente)

**Ejemplo:**
```java
// JwtAuthFilter.java:146
private boolean isPublic(String uri) {
    for (String pattern : PUBLIC_PATHS) {
        if (PATH_MATCHER.match(pattern, uri)) {
            System.out.println("Ruta pública detectada: " + uri);  // ← MAL
            return true;
        }
    }
    return false;
}
```

**Impacto:** Bajo - No se debe usar `System.out` en producción.

**Solución:**
```java
// Agregar logger
private static final Logger logger = LoggerFactory.getLogger(JwtAuthFilter.class);

// Reemplazar
logger.debug("Ruta pública detectada: {}", uri);
```

---

### 7. **`AuthUtils` en paquete incorrecto**

**Ubicación:** `reporte/util/AuthUtils.java`

**Problema:**
- Es una utilidad de autenticación
- Está en el paquete `reporte.util` (módulo de reportes)
- Debería estar en `autentificacion.util`

**Impacto:** Bajo - Afecta organización y cohesión del código.

**Solución:**
- Mover a `cl.ufro.dci.naivepayapi.autentificacion.util.AuthUtils`
- Actualizar imports en archivos que lo usan

---

### 8. **Métodos helper duplicados en controllers**

**DeviceController.java:141-151 tiene métodos que podrían ser utilitarios:**

```java
private static String clientIp(HttpServletRequest request) { ... }
private static boolean isBlank(String s) { ... }  // ← YA IDENTIFICADO EN PUNTO 1
private static String nullSafe(String s) { ... }
private static String firstNonBlank(String a, String b) { ... }
```

**Impacto:** Bajo - Métodos triviales pero podrían ser reutilizables.

**Solución (opcional):**
Si se repiten en otros controllers, moverlos a clase utilitaria `HttpUtils` o `RequestUtils`.

---

## 📋 PLAN DE EJECUCIÓN

### Fase 1: Eliminar Código Muerto (Prioridad Alta) 🟠

**1.1. Limpiar DeviceTokenUtil**
- ✅ Eliminar método `resolveDeviceFingerprint()` (líneas 158-172)
- ✅ Eliminar método `validateTokenNotExpired()` (líneas 76-81) - hacer inline si se necesita
- ✅ Eliminar inyección de `DeviceRepository`
- ✅ Eliminar inyección de `PasswordEncoder`
- ⏱️ Tiempo estimado: 10 minutos

**1.2. Eliminar System.out.println**
- ✅ Reemplazar en `JwtAuthFilter.java:146`
- ✅ Buscar y reemplazar en `UserRegistrationListener.java`
- ✅ Buscar y reemplazar en `ReportController.java`
- ⏱️ Tiempo estimado: 5 minutos

---

### Fase 2: Consolidar Código Duplicado (Prioridad Media) 🟡

**2.1. Crear `BearerTokenExtractor`**
- ✅ Crear `autentificacion/util/BearerTokenExtractor.java`
- ✅ Implementar métodos `extractOrNull()` y `extractOrThrow()`
- ⏱️ Tiempo estimado: 10 minutos

**2.2. Refactorizar usos de Bearer extraction**
- ✅ Actualizar `AuthService.java:323` → usar `BearerTokenExtractor`
- ✅ Actualizar `DeviceTokenUtil.java:49` → usar `BearerTokenExtractor`
- ✅ Actualizar `JwtAuthFilter.java:78-85` → usar `BearerTokenExtractor`
- ✅ Eliminar constante `BEARER_PREFIX` de cada archivo
- ⏱️ Tiempo estimado: 15 minutos

**2.3. Consolidar método `isBlank()`**
- ✅ Opción A: Usar `org.springframework.util.StringUtils.hasText()`
  - Reemplazar en `AuthService.java:313-314`
  - Reemplazar en `LoginRequestValidator.java:79-80`
  - Reemplazar en `DeviceController.java:149`
- ⏱️ Tiempo estimado: 10 minutos

**2.4. Consolidar `getAuthenticatedUserId()`**
- ✅ Agregar método `getCurrentUserId()` en `AuthUtils`
- ✅ Actualizar `DeviceController.java` para usar `AuthUtils.getCurrentUserId()`
- ⏱️ Tiempo estimado: 5 minutos

---

### Fase 3: Reorganización (Prioridad Baja) 🔵

**3.1. Mover AuthUtils al paquete correcto**
- ✅ Mover `reporte/util/AuthUtils.java` → `autentificacion/util/AuthUtils.java`
- ✅ Actualizar imports en todos los archivos que lo usan
- ✅ Buscar referencias con grep y actualizar
- ⏱️ Tiempo estimado: 10 minutos

**3.2. (Opcional) Crear HttpUtils para helpers de controllers**
- ⚠️ Solo si se encuentran otros controllers que dupliquen `clientIp()`, `firstNonBlank()`, etc.
- ⏱️ Tiempo estimado: 15 minutos (si aplica)

---

## ⏱️ Tiempo Total Estimado

| Fase | Tareas | Tiempo |
|------|--------|--------|
| Fase 1: Código Muerto | 2 tareas | **15 min** |
| Fase 2: Duplicación | 4 tareas | **40 min** |
| Fase 3: Reorganización | 1-2 tareas | **10-25 min** |
| **TOTAL** | 7-8 tareas | **65-80 minutos** |

---

## 🎯 Orden Recomendado de Ejecución

1. ✅ **Eliminar `System.out.println`** (5 min, bajo riesgo)
2. ✅ **Limpiar `DeviceTokenUtil`** (10 min, bajo riesgo)
3. ✅ **Crear `BearerTokenExtractor`** (10 min)
4. ✅ **Refactorizar usos de Bearer** (15 min)
5. ✅ **Consolidar `isBlank()`** (10 min)
6. ✅ **Consolidar `getAuthenticatedUserId()`** (5 min)
7. ✅ **Mover `AuthUtils` al paquete correcto** (10 min)
8. ⚠️ **(Opcional) Crear `HttpUtils`** (15 min, si aplica)

---

## 🧪 Testing Requerido

### Tests Unitarios:
- ✅ `BearerTokenExtractor.extractOrNull()` - casos: válido, inválido, null, sin "Bearer"
- ✅ `BearerTokenExtractor.extractOrThrow()` - verificar excepciones
- ✅ `AuthUtils.getCurrentUserId()` - verificar extracción desde SecurityContext

### Tests de Integración:
- ✅ Login flow completo (verificar que sigue funcionando)
- ✅ Logout flow (verificar extracción de Bearer token)
- ✅ Filtro JWT (verificar que las rutas públicas y privadas funcionan)
- ✅ Device linking (verificar extracción de userId desde token)

### Tests de Regresión:
- ✅ Ejecutar suite completa de tests antes y después
- ✅ Verificar que no hay tests rotos

---

## 📊 Métricas de Mejora Esperadas

| Métrica | Antes | Después | Mejora |
|---------|-------|---------|--------|
| Implementaciones de `isBlank()` | 3 | 0 (usa Spring) | -100% |
| Implementaciones de extracción Bearer | 3 | 1 centralizada | -67% |
| Líneas de código duplicado | ~40 | ~0 | -100% |
| System.out.println en producción | 3 | 0 | -100% |
| Clases con dependencias no utilizadas | 1 | 0 | -100% |
| Métodos públicos no utilizados | 2 | 0 | -100% |

---

## 📝 Archivos que Serán Modificados

### Archivos a crear:
- ✅ `autentificacion/util/BearerTokenExtractor.java` (NUEVO)

### Archivos a modificar:
- ✅ `AuthService.java` (eliminar `extractBearer()` y `isBlank()`)
- ✅ `LoginRequestValidator.java` (eliminar `isBlank()`)
- ✅ `DeviceController.java` (eliminar `isBlank()` y `getAuthenticatedUserId()`)
- ✅ `DeviceTokenUtil.java` (eliminar métodos no usados y refactor Bearer)
- ✅ `JwtAuthFilter.java` (refactor extracción Bearer, reemplazar println)
- ✅ `AuthUtils.java` (mover de `reporte/util` a `autentificacion/util`, agregar `getCurrentUserId()`)
- ⚠️ `UserRegistrationListener.java` (eliminar println)
- ⚠️ `ReportController.java` (eliminar println)
- ⚠️ Todos los archivos que importan `AuthUtils` (actualizar import)

---

## ⚠️ Consideraciones Importantes

### Compatibilidad:
- ✅ Cambios son **backward compatible** (solo refactorización interna)
- ✅ No afectan APIs públicas ni contratos de endpoints
- ✅ No requieren cambios en el frontend

### Deployment:
- ✅ Puede hacerse sin downtime
- ✅ No requiere migraciones de base de datos
- ✅ No requiere invalidar sesiones activas

### Rollback:
- ✅ Fácil rollback con git revert (cambios son independientes)
- ✅ Cada fase puede comitearse por separado

---

## 📚 Resumen de Hallazgos

### ✅ Código que funciona bien:
- Sistema de sesiones (AuthSessionService)
- Manejo de intentos fallidos (AccountLockService.handleFailedAuthentication)
- Validación de dispositivos (DeviceService)
- Estructura de excepciones (AuthenticationFailedException)
- Recuperación de contraseñas (PasswordRecoveryService)
- Manejo de MDC para logging estructurado

### ⚠️ Áreas de mejora identificadas:
- Duplicación de utilidades comunes (isBlank, extractBearer)
- Uso de System.out.println en lugar de logger
- Dependencias inyectadas pero no utilizadas
- Métodos públicos que nunca se llaman
- Organización de paquetes (AuthUtils en paquete incorrecto)

### 🚫 No se encontraron:
- ✅ Bugs críticos
- ✅ Vulnerabilidades de seguridad evidentes
- ✅ Problemas de lógica de negocio
- ✅ Memory leaks o problemas de performance

---

**Autor:** Claude (Análisis automatizado)
**Fecha:** 2025-11-15
**Versión:** 2.0 (Actualizado con código sin roles)
**Rama:** `claude/naive-pay-session-management-011CUz6ywdvoZ94taKQNBQHP`

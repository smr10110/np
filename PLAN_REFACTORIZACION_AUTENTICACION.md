# Plan de Refactorización - Módulo de Autenticación
## Análisis enfocado exclusivamente en el paquete `autentificacion`

---

## 📦 Alcance del Análisis

**Paquete analizado:** `cl.ufro.dci.naivepayapi.autentificacion`

**Archivos incluidos (29 archivos):**
```
autentificacion/
├── controller/
│   ├── AuthController.java
│   └── PasswordRecoveryController.java
├── service/
│   ├── AuthService.java
│   ├── JWTService.java (interfaz)
│   ├── AccountLockService.java
│   ├── AuthAttemptService.java
│   ├── AuthSessionService.java
│   ├── LoginRequestValidator.java
│   ├── PasswordRecoveryService.java
│   ├── RutUtils.java
│   └── impl/
│       └── JWTServiceImpl.java
├── configuration/security/
│   ├── SecurityConfig.java
│   ├── JwtAuthFilter.java
│   ├── RestAuthenticationEntryPoint.java
│   └── GlobalExceptionHandler.java
├── domain/
│   ├── Session.java
│   ├── AuthAttempt.java
│   ├── PasswordRecovery.java
│   └── enums/
│       ├── SessionStatus.java
│       ├── AuthAttemptReason.java
│       └── PasswordRecoveryStatus.java
├── repository/
│   ├── SessionRepository.java
│   ├── AuthAttemptRepository.java
│   └── PasswordRecoveryRepository.java
├── dto/
│   ├── LoginRequest.java
│   ├── LoginResponse.java
│   ├── ForgotPasswordRequest.java
│   └── ResetPasswordRequest.java
└── exception/
    └── AuthenticationFailedException.java
```

---

## 🟡 CÓDIGO DUPLICADO

### 1. **Método `isBlank()` duplicado**

**Ubicaciones dentro de `autentificacion`:**
1. `service/AuthService.java:313-314`
2. `service/LoginRequestValidator.java:79-80`

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
```

**Impacto:** Bajo - Código trivial duplicado en 2 lugares.

**Solución:**
Usar utilidad de Spring Framework (ya disponible en el proyecto):

```java
import org.springframework.util.StringUtils;

// Reemplazar:
// isBlank(str)
// por:
// !StringUtils.hasText(str)
```

**Archivos a modificar:**
- ✅ `AuthService.java` → línea 167, 324
- ✅ `LoginRequestValidator.java` → línea 50, 65
- ✅ Eliminar método privado `isBlank()` de ambos archivos

---

### 2. **Extracción de Bearer Token duplicada**

**Ubicaciones dentro de `autentificacion`:**
1. `service/AuthService.java:323-328` → método `extractBearer()`
2. `configuration/security/JwtAuthFilter.java:78-85` → implementación inline

**Código duplicado:**
```java
// AuthService.java:323-328
private String extractBearer(String authHeader) {
    if (isBlank(authHeader) || !authHeader.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
        return null;
    }
    return authHeader.substring(BEARER_PREFIX.length()).trim();
}

// JwtAuthFilter.java:78-85
final String header = request.getHeader(AUTH_HEADER);
if (header == null || !header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
    chain.doFilter(request, response);
    return;
}
final String token = header.substring(BEARER_PREFIX.length()).trim();
```

**Impacto:** Medio - Lógica de seguridad duplicada.

**Solución:**
Crear clase utilitaria dentro del módulo de autenticación:

```java
// Crear: autentificacion/util/BearerTokenExtractor.java
package cl.ufro.dci.naivepayapi.autentificacion.util;

public class BearerTokenExtractor {

    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * Extrae el token JWT de un header Authorization.
     *
     * @param authHeader Header Authorization completo (ej: "Bearer eyJhbGci...")
     * @return Token JWT sin el prefijo "Bearer ", o null si es inválido
     */
    public static String extract(String authHeader) {
        if (authHeader == null || authHeader.isBlank()) {
            return null;
        }

        if (!authHeader.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return null;
        }

        return authHeader.substring(BEARER_PREFIX.length()).trim();
    }

    /**
     * Verifica si un header contiene un token Bearer válido.
     */
    public static boolean isValid(String authHeader) {
        return extract(authHeader) != null;
    }
}
```

**Archivos a modificar:**
- ✅ Crear `autentificacion/util/BearerTokenExtractor.java`
- ✅ `AuthService.java:323-328` → reemplazar método con `BearerTokenExtractor.extract()`
- ✅ `JwtAuthFilter.java:78-85` → usar `BearerTokenExtractor.extract()`
- ✅ Eliminar constante `BEARER_PREFIX` de ambos archivos

---

### 3. **Constante `BEARER_PREFIX` duplicada**

**Ubicaciones:**
1. `service/AuthService.java:33`
2. `configuration/security/JwtAuthFilter.java:33`

**Solución:**
Se eliminará automáticamente al crear `BearerTokenExtractor` (ver punto 2).

---

## 🔵 MEJORAS DE CALIDAD

### 4. **`System.out.println` en código de producción**

**Ubicación:** `configuration/security/JwtAuthFilter.java:146`

**Código problemático:**
```java
private boolean isPublic(String uri) {
    for (String pattern : PUBLIC_PATHS) {
        if (PATH_MATCHER.match(pattern, uri)) {
            System.out.println("Ruta pública detectada: " + uri);  // ← PROBLEMA
            return true;
        }
    }
    return false;
}
```

**Impacto:** Bajo - Los logs no se capturan en sistemas de monitoreo.

**Solución:**
```java
// Agregar logger si no existe
private static final Logger logger = LoggerFactory.getLogger(JwtAuthFilter.class);

// Reemplazar System.out.println
logger.debug("Ruta pública detectada: {}", uri);
```

**Archivos a modificar:**
- ✅ `JwtAuthFilter.java:146`

---

### 5. **Inconsistencia en manejo de excepciones**

**Ubicación:** `service/AuthService.java:71-74`

**Análisis:**
El método `login()` tiene un try-catch que maneja dos tipos de excepciones:
1. `AuthenticationFailedException` → método `handleAuthenticationFailure()`
2. `ResponseStatusException` → método `handleResponseStatusException()`

Ambos métodos hacen logging y retornan `ResponseEntity` con el error.

**Estado:** ✅ Bien implementado - No requiere cambios.

---

### 6. **Documentación de seguridad**

**Observación:** El módulo tiene buena documentación JavaDoc en general, especialmente en:
- `AccountLockService.java` → excelente documentación
- `AuthService.java` → bien documentado
- `JwtAuthFilter.java` → falta documentación de clase

**Sugerencia (opcional):**
Agregar JavaDoc a la clase `JwtAuthFilter` explicando:
- Su propósito (validar JWT en cada request)
- Rutas públicas excluidas
- Proceso de validación

---

## 📋 PLAN DE EJECUCIÓN

### Fase 1: Crear Utilidades (10 min) 🔧

**1.1. Crear BearerTokenExtractor**
```bash
# Crear directorio si no existe
mkdir -p naive-pay-api/src/main/java/cl/ufro/dci/naivepayapi/autentificacion/util

# Crear clase BearerTokenExtractor.java
```

- ✅ Implementar método `extract(String authHeader)`
- ✅ Implementar método `isValid(String authHeader)`
- ✅ Agregar tests unitarios
- ⏱️ Tiempo: 10 minutos

---

### Fase 2: Refactorizar Código Duplicado (25 min) 🔄

**2.1. Consolidar extracción de Bearer Token**
- ✅ Actualizar `AuthService.java`:
  - Línea 33: eliminar `BEARER_PREFIX`
  - Línea 91: cambiar `extractBearer(authHeader)` → `BearerTokenExtractor.extract(authHeader)`
  - Líneas 323-328: eliminar método `extractBearer()`

- ✅ Actualizar `JwtAuthFilter.java`:
  - Línea 33: eliminar `BEARER_PREFIX`
  - Líneas 78-85: reemplazar lógica inline con `BearerTokenExtractor.extract()`

- ⏱️ Tiempo: 15 minutos

**2.2. Consolidar método isBlank()**
- ✅ Actualizar `AuthService.java`:
  - Línea 167: cambiar `isBlank(register.getRegHashedLoginPassword())` → `!StringUtils.hasText(register.getRegHashedLoginPassword())`
  - Línea 324: cambiar `isBlank(authHeader)` → `!StringUtils.hasText(authHeader)`
  - Líneas 313-315: eliminar método `isBlank()`

- ✅ Actualizar `LoginRequestValidator.java`:
  - Línea 50: cambiar `isBlank(identifier)` → `!StringUtils.hasText(identifier)`
  - Línea 65: cambiar `isBlank(password)` → `!StringUtils.hasText(password)`
  - Líneas 79-81: eliminar método `isBlank()`

- ⏱️ Tiempo: 10 minutos

---

### Fase 3: Mejoras de Calidad (5 min) ✨

**3.1. Reemplazar System.out.println**
- ✅ `JwtAuthFilter.java`:
  - Verificar que existe logger (ya existe en línea ~28)
  - Línea 146: cambiar `System.out.println(...)` → `logger.debug(...)`

- ⏱️ Tiempo: 2 minutos

**3.2. (Opcional) Agregar JavaDoc a JwtAuthFilter**
- ⚠️ Agregar documentación de clase
- ⏱️ Tiempo: 3 minutos

---

## ⏱️ Tiempo Total Estimado

| Fase | Tareas | Tiempo |
|------|--------|--------|
| Fase 1: Crear Utilidades | 1 tarea | **10 min** |
| Fase 2: Refactorizar Duplicación | 2 tareas | **25 min** |
| Fase 3: Mejoras de Calidad | 1-2 tareas | **5 min** |
| **TOTAL** | 4-5 tareas | **40 minutos** |

---

## 🎯 Orden Recomendado

1. ✅ **Crear BearerTokenExtractor** (10 min) - establece la base
2. ✅ **Refactorizar Bearer extraction** (15 min) - usa la nueva clase
3. ✅ **Consolidar isBlank()** (10 min) - usa Spring Utils
4. ✅ **Reemplazar System.out.println** (2 min) - quick win
5. ⚠️ **(Opcional) JavaDoc** (3 min) - si hay tiempo

---

## 🧪 Tests Requeridos

### Tests Unitarios Nuevos:
```java
// BearerTokenExtractorTest.java
@Test
void extract_validBearerToken_returnsToken() {
    String token = BearerTokenExtractor.extract("Bearer abc123");
    assertEquals("abc123", token);
}

@Test
void extract_invalidFormat_returnsNull() {
    assertNull(BearerTokenExtractor.extract("InvalidFormat"));
    assertNull(BearerTokenExtractor.extract(null));
    assertNull(BearerTokenExtractor.extract(""));
}

@Test
void extract_caseInsensitive_works() {
    String token = BearerTokenExtractor.extract("bearer abc123");
    assertEquals("abc123", token);
}

@Test
void isValid_validToken_returnsTrue() {
    assertTrue(BearerTokenExtractor.isValid("Bearer abc123"));
}

@Test
void isValid_invalidToken_returnsFalse() {
    assertFalse(BearerTokenExtractor.isValid(null));
    assertFalse(BearerTokenExtractor.isValid(""));
    assertFalse(BearerTokenExtractor.isValid("InvalidFormat"));
}
```

### Tests de Regresión:
- ✅ Ejecutar todos los tests existentes del módulo de autenticación
- ✅ Verificar que `AuthServiceTest` sigue pasando
- ✅ Verificar que tests de integración de login/logout funcionan
- ✅ Verificar que `JwtAuthFilterTest` sigue funcionando (si existe)

---

## 📊 Métricas de Mejora

| Métrica | Antes | Después | Mejora |
|---------|-------|---------|--------|
| Implementaciones de `isBlank()` | 2 | 0 (usa Spring) | -100% |
| Implementaciones de extracción Bearer | 2 | 1 centralizada | -50% |
| Constantes duplicadas `BEARER_PREFIX` | 2 | 1 centralizada | -50% |
| `System.out.println` en producción | 1 | 0 | -100% |
| Líneas de código duplicado | ~15 | 0 | -100% |
| Clases utilitarias en `autentificacion/util` | 0 | 1 | +1 |

---

## 📝 Archivos que Serán Modificados

### Dentro del módulo `autentificacion`:

**Archivos a crear:**
- ✅ `util/BearerTokenExtractor.java` (NUEVO - ~40 líneas)

**Archivos a modificar:**
- ✅ `service/AuthService.java`
  - Eliminar líneas 313-315 (método `isBlank()`)
  - Eliminar líneas 323-328 (método `extractBearer()`)
  - Eliminar línea 33 (constante `BEARER_PREFIX`)
  - Actualizar imports y usos
  - **Líneas netas:** -20 líneas

- ✅ `service/LoginRequestValidator.java`
  - Eliminar líneas 79-81 (método `isBlank()`)
  - Actualizar imports y usos
  - **Líneas netas:** -3 líneas

- ✅ `configuration/security/JwtAuthFilter.java`
  - Eliminar línea 33 (constante `BEARER_PREFIX`)
  - Refactorizar líneas 78-85 (extracción Bearer)
  - Cambiar línea 146 (System.out → logger)
  - **Líneas netas:** -5 líneas

**Balance total:**
- Líneas agregadas: +40 (BearerTokenExtractor)
- Líneas eliminadas: -28 (código duplicado)
- **Neto: +12 líneas** (pero código más mantenible y sin duplicación)

---

## 📚 Resumen del Análisis

### ✅ Fortalezas del Módulo de Autenticación:

1. **Excelente separación de responsabilidades:**
   - Servicios bien definidos (Auth, Session, Attempts, Lock, Recovery)
   - DTOs claros
   - Repositorios dedicados

2. **Buena gestión de excepciones:**
   - Excepción custom `AuthenticationFailedException` con información de intentos
   - Manejo centralizado en `AuthService`
   - `GlobalExceptionHandler` para respuestas consistentes

3. **Documentación JavaDoc:**
   - `AccountLockService` → ejemplar
   - `AuthService` → bien documentado
   - Métodos complejos tienen explicaciones claras

4. **Seguridad bien implementada:**
   - Bloqueo automático de cuentas
   - Tracking de intentos fallidos
   - Sesiones con expiración
   - JWT con validación robusta

5. **Logging estructurado:**
   - Uso de MDC para contexto
   - Niveles apropiados (debug, info, warn, error)
   - Mensajes informativos

### ⚠️ Áreas de Mejora Identificadas:

1. **Código duplicado** (menor):
   - Método `isBlank()` en 2 lugares
   - Extracción de Bearer token en 2 lugares
   - Constante duplicada

2. **Uso de System.out** (1 caso):
   - En `JwtAuthFilter` línea 146

3. **Falta de documentación JavaDoc**:
   - Clase `JwtAuthFilter` no tiene JavaDoc de clase

### 🚫 No se encontraron:

- ✅ Bugs críticos
- ✅ Vulnerabilidades de seguridad
- ✅ Código muerto significativo
- ✅ Problemas de lógica de negocio
- ✅ Memory leaks
- ✅ Problemas de performance

---

## 🔐 Notas de Seguridad

**El módulo implementa correctamente:**

1. ✅ **Autenticación multi-factor implícita**
   - Usuario + contraseña + dispositivo autorizado

2. ✅ **Rate limiting por intentos fallidos**
   - Máximo 5 intentos en ventana de 30 minutos
   - Bloqueo automático de cuenta

3. ✅ **Gestión de sesiones segura**
   - JWT con expiración (15 minutos)
   - Session tracking en BD
   - Cierre automático de sesiones expiradas

4. ✅ **Recuperación de contraseña segura**
   - Códigos de 6 dígitos
   - Expiración de 10 minutos
   - Invalidación de códigos previos

5. ✅ **Validación de dispositivos**
   - Fingerprint hasheado con BCrypt
   - One-device-per-user policy
   - Tracking de cambios de dispositivo

---

## ⚠️ Consideraciones de Deployment

### Compatibilidad:
- ✅ Cambios son **backward compatible**
- ✅ No afectan contratos de API
- ✅ No requieren cambios en frontend
- ✅ No requieren migraciones de BD

### Deployment:
- ✅ Puede hacerse sin downtime
- ✅ No invalida sesiones existentes
- ✅ No requiere restart de servicios dependientes

### Rollback:
- ✅ Fácil rollback con `git revert`
- ✅ Cada cambio puede comitearse independientemente
- ✅ Sin dependencias entre fases

---

**Autor:** Claude
**Fecha:** 2025-11-15
**Versión:** 3.0 (Solo módulo autenticación)
**Rama:** `claude/naive-pay-session-management-011CUz6ywdvoZ94taKQNBQHP`
**Alcance:** `cl.ufro.dci.naivepayapi.autentificacion` únicamente
